/*
 * Memory.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.storage;

import com.google.common.util.concurrent.AtomicDouble;
import nars.util.Events;
import nars.util.EventEmitter;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import nars.NAR;
import nars.config.RuntimeParameters;
import nars.config.Parameters;
import nars.util.Events.ResetEnd;
import nars.util.Events.ResetStart;
import nars.util.Events.TaskRemove;
import nars.control.DerivationContext;
import nars.control.GeneralInferenceControl;
import nars.control.TemporalInferenceControl;
import nars.plugin.mental.Emotions;
import nars.entity.BudgetValue;
import nars.entity.Concept;
import static nars.entity.Concept.successfulOperationHandler;
import nars.entity.Item;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TruthValue;
import nars.inference.BudgetFunctions;
import static nars.inference.BudgetFunctions.truthToQuality;
import nars.io.Output.IN;
import nars.io.Output.OUT;
import nars.io.Symbols;
import nars.language.Tense;
import nars.language.Term;
import nars.operator.Operation;
import nars.operator.Operator;
import nars.io.Echo;
import nars.io.PauseInput;
import nars.io.Reset;
import nars.io.SetDecisionThreshold;
import nars.io.SetVolume;
import nars.language.CompoundTerm;
import nars.language.Interval;


/**
 * Memory consists of the run-time state of a NAR, including:
 *   * term and concept memory
 *   * clock
 *   * reasoner state
 *   * etc.
 * 
 * Excluding input/output channels which are managed by a NAR.  
 * 
 * A memory is controlled by zero or one NAR's at a given time.
 * 
 * Memory is serializable so it can be persisted and transported.
 */
public class Memory implements Serializable, Iterable<Concept> {
    
    //emotion meter keeping track of global emotion
    public final Emotions emotion = new Emotions();   
    
    public long decisionBlock = 0;
    public Task lastDecision = null;
    public boolean allowExecution = true;

    public static long randomSeed = 1;
    public static Random randomNumber = new Random(randomSeed);
    public static void resetStatic() {
        randomNumber.setSeed(randomSeed);    
    }
    
    //todo make sense of this class and de-obfuscate
    public final Bag<Concept,Term> concepts;
    public final EventEmitter event;
    
    /* InnateOperator registry. Containing all registered operators of the system */
    public final HashMap<CharSequence, Operator> operators;
    
    /* New tasks with novel composed terms, for delayed and selective processing*/
    public final Bag<Task<Term>,Sentence<Term>> novelTasks;
    
    /* Input event tasks that were either input events or derived sequences*/
    public Bag<Task<Term>,Sentence<Term>> sequenceTasks;

    /* List of new tasks accumulated in one cycle, to be processed in the next cycle */
    public final Deque<Task> newTasks;
    
    /* The remaining number of steps to be carried out (stepLater mode)*/
    private int inputPausedUntil;
    
    /* System clock, relatively defined to guarantee the repeatability of behaviors */
    private long cycle;
    
    /* System parameters that can be changed at runtime */
    public final RuntimeParameters param;
    
    /* ---------- Constructor ---------- */
    /**
     * Create a new memory
     *
     * @param initialOperators - initial set of available operators; more may be added during runtime
     */
    public Memory(RuntimeParameters param, Bag<Concept,Term> concepts, Bag<Task<Term>,Sentence<Term>> novelTasks,
            Bag<Task<Term>,Sentence<Term>> sequenceTasks) {                

        this.param = param;
        this.event = new EventEmitter();
        this.concepts = concepts;
        this.novelTasks = novelTasks;                
        this.newTasks = new ArrayDeque<>();
        this.sequenceTasks = sequenceTasks;
        this.operators = new HashMap<>();
        reset();
    }
    
    public void reset() {
        event.emit(ResetStart.class);
        decisionBlock = 0;
        concepts.clear();
        novelTasks.clear();
        newTasks.clear();    
        sequenceTasks.clear();
        cycle = 0;
        inputPausedUntil = 0;
        emotion.set(0.5f, 0.5f);
        resetStatic();
        event.emit(ResetEnd.class);
    }

    public long time() {
        return cycle;
    }

    /* ---------- conversion utilities ---------- */
    /**
     * Get an existing Concept for a given name
     * <p>
     * called from Term and ConceptWindow.
     *
     * @param t the name of a concept
     * @return a Concept or null
     */
    public Concept concept(final Term t) {
        return concepts.get(CompoundTerm.cloneDeepReplaceIntervals(t));
    }

    /**
     * Get the Concept associated to a Term, or create it.
     * 
     *   Existing concept: apply tasklink activation (remove from bag, adjust budget, reinsert)
     *   New concept: set initial activation, insert
     *   Subconcept: extract from cache, apply activation, insert
     * 
     * If failed to insert as a result of null bag, returns null
     *
     * A displaced Concept resulting from insert is forgotten (but may be stored in optional  subconcept memory
     * 
     * @param term indicating the concept
     * @return an existing Concept, or a new one, or null 
     */
    public Concept conceptualize(final BudgetValue budget, Term term) {   
        if(term instanceof Interval) {
            return null;
        }
        term = CompoundTerm.cloneDeepReplaceIntervals(term);
        //see if concept is active
        Concept concept = concepts.take(term);
        if (concept == null) {                            
            //create new concept, with the applied budget
            concept = new Concept(budget, term, this);
            //if (memory.logic!=null)
            //    memory.logic.CONCEPT_NEW.commit(term.getComplexity());
            emit(Events.ConceptNew.class, concept);                
        }
        else if (concept!=null) {            
            //apply budget to existing concept
            //memory.logic.CONCEPT_ACTIVATE.commit(term.getComplexity());
            BudgetFunctions.activate(concept.budget, budget, BudgetFunctions.Activating.TaskLink);            
        }
        else {
            //unable to create, ex: has variables
            return null;
        }
        Concept displaced = concepts.putBack(concept, cycles(param.conceptForgetDurations), this);   
        if (displaced == null) {
            //added without replacing anything
            return concept;
        }        
        else if (displaced == concept) {
            //not able to insert
            conceptRemoved(displaced);
            return null;
        }        
        else {
            conceptRemoved(displaced);
            return concept;
        }
    }
    
    /* ---------- new task entries ---------- */
    /**
     * add new task that waits to be processed in the next cycleMemory
     */
    public void addNewTask(final Task t, final String reason) {
        newTasks.add(t);
      //  logic.TASK_ADD_NEW.commit(t.getPriority());
        emit(Events.TaskAdd.class, t, reason);
        output(t);
    }
    
    /* There are several types of new tasks, all added into the
     newTasks list, to be processed in the next cycleMemory.
     Some of them are reported and/or logged. */
    /**
     * Input task processing. Invoked by the outside or inside environment.
 Outside: StringParser (addInput); Inside: InnateOperator (feedback). Input
 tasks with low priority are ignored, and the others are put into task
 buffer.
     *
     * @param t The addInput task
     */
    
    boolean checked=false;
    boolean isjUnit=false;
    public static boolean isJUnitTest() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        List<StackTraceElement> list = Arrays.asList(stackTrace);
        for (StackTraceElement element : list) {
            if (element.getClassName().startsWith("org.junit.")) {
                return true;
            }           
        }
        return false;
    }
    
    public void inputTask(final Item t, boolean emitIn) {
        if(!checked) {
            checked=true;
            isjUnit=isJUnitTest();
        }
        if (t instanceof Task) {
            Task task = (Task)t;
            Stamp s = task.sentence.stamp;                        
            if (s.getCreationTime()==-1)
                s.setCreationTime(time(), param.duration.get());

            if(emitIn) {
                emit(IN.class, task);
            }

            if (task.budget.aboveThreshold()) {
                
                addNewTask(task, "Perceived");
                
                if(task.sentence.isJudgment() && !task.sentence.isEternal() && task.sentence.term instanceof Operation) {
                    this.lastDecision = task;
                    //depriorize everything related to the previous decisions:
                    successfulOperationHandler(this);
                }
                
            } else {
                removeTask(task, "Neglected");
            }
        }
        else if (t instanceof PauseInput) {            
            stepLater(((PauseInput)t).cycles);            
            emit(IN.class, t);
        }
        else if (t instanceof Reset) {
            reset();
            emit(OUT.class,((Reset) t).input);
            emit(IN.class, t);
        }
        else if (t instanceof Echo) {
            Echo e = (Echo)t;
            if(!isjUnit) {
                emit(OUT.class,((Echo) t).signal);
            }
            emit(e.channel, e.signal);
        }
        else if (t instanceof SetVolume) {            
            param.noiseLevel.set(((SetVolume)t).volume);
            emit(IN.class, t);
        } 
        else if (t instanceof SetDecisionThreshold) {
            param.decisionThreshold.set(((SetDecisionThreshold)t).volume);
            emit(IN.class, t);
        } 
        else {
            emit(IN.class, "Unrecognized Input Task: " + t);
        }
    }
    
    public void inputTask(final Item t) {
        inputTask(t, true);
    }

    public void removeTask(final Task task, final String reason) {        
        emit(TaskRemove.class, task, reason);
        task.end();        
    }
    
    /**
     * ExecutedTask called in Operator.call
     *
     * @param operation The operation just executed
     */
    public void executedTask(final Operation operation, TruthValue truth) {
        Task opTask = operation.getTask();
       // logic.TASK_EXECUTED.commit(opTask.budget.getPriority());
                
        Stamp stamp = new Stamp(this, Tense.Present); 
        Sentence sentence = new Sentence(operation, Symbols.JUDGMENT_MARK, truth, stamp);
        
        Task task = new Task(sentence, new BudgetValue(Parameters.DEFAULT_FEEDBACK_PRIORITY, Parameters.DEFAULT_FEEDBACK_DURABILITY,
                                        truthToQuality(sentence.getTruth())), operation.getTask());
        task.setElemOfSequenceBuffer(true);
        addNewTask(task, "Executed");
    }

    public void output(final Task t) {
        
        final float budget = t.budget.summary();
        final float noiseLevel = 1.0f - (param.noiseLevel.get() / 100.0f);
        
        if (budget >= noiseLevel) {  // only report significant derived Tasks
            emit(OUT.class, t);
        }        
    }
    
    final public void emit(final Class c, final Object... signal) {        
        event.emit(c, signal);
    }

    final public boolean emitting(final Class channel) {
        return event.isActive(channel);
    }
    
    public void conceptRemoved(Concept c) {
        emit(Events.ConceptForget.class, c);
    }
    
    public void cycle(final NAR inputs) {
    
        event.emit(Events.CycleStart.class);                
        
        /** adds input tasks to newTasks */
        for(int i=0; i<1 && isProcessingInput(); i++) {
            Item t = inputs.nextTask();                    
            if (t!=null) 
                inputTask(t);            
        }
        
        this.processNewTasks();
    //if(noResult()) //newTasks empty
        this.processNovelTask();
    //if(noResult()) //newTasks empty
        GeneralInferenceControl.selectConceptForInference(this);
        
        event.emit(Events.CycleEnd.class);
        event.synch();
        
        cycle++;
    }
    
    public void localInference(Task task) {
        DerivationContext cont = new DerivationContext(this);
        cont.setCurrentTask(task);
        cont.setCurrentTerm(task.getTerm());
        cont.setCurrentConcept(conceptualize(task.budget, cont.getCurrentTerm()));
        if (cont.getCurrentConcept() != null) {
            boolean processed = cont.getCurrentConcept().directProcess(cont, task);
            if (processed) {
                event.emit(Events.ConceptDirectProcessedTask.class, task);
            }
        }
        
        if (!task.sentence.isEternal()) {
            TemporalInferenceControl.eventInference(task, cont);
        }
        
        //memory.logic.TASK_IMMEDIATE_PROCESS.commit();
        emit(Events.TaskImmediateProcess.class, task, cont);
    }
    
    /**
     * Process the newTasks accumulated in the previous workCycle, accept input
     * ones and those that corresponding to existing concepts, plus one from the
     * buffer.
     */
    public void processNewTasks() {
        Task task;
        int counter = newTasks.size();  // don't include new tasks produced in the current workCycle
        while (counter-- > 0) {
            task = newTasks.removeFirst();
            boolean enterDirect = true;
            if (/*task.isElemOfSequenceBuffer() || task.isObservablePrediction() || */ enterDirect ||  task.isInput() || task.sentence.isQuest() || task.sentence.isQuestion() || concept(task.sentence.term)!=null) { // new input or existing concept
                localInference(task);
            } else {
                Sentence s = task.sentence;
                if (s.isJudgment() || s.isGoal()) {
                    double d = s.getTruth().getExpectation();
                    if (s.isJudgment() && d > Parameters.DEFAULT_CREATION_EXPECTATION) {
                        novelTasks.putIn(task);    // new concept formation
                    } else 
                    if(s.isGoal() && d > Parameters.DEFAULT_CREATION_EXPECTATION_GOAL) {
                        novelTasks.putIn(task);    // new concept formation
                    }
                    else
                    {
                        removeTask(task, "Neglected");
                    }
                }
            }
        }
    }
    

    /**
     * Select a novel task to process.
     * @return whether a task was processed
     */
    public void processNovelTask() {
        final Task task = novelTasks.takeNext();
        if (task != null) {            
            localInference(task);
        }
    }

     public Operator getOperator(final String op) {
        return operators.get(op);
     }
     
     public Operator addOperator(final Operator op) {
         operators.put(op.name(), op);
         return op;
     }
     
     public Operator removeOperator(final Operator op) {
         return operators.remove(op.name());
     }

    private long currentStampSerial = 0;
    public long newStampSerial() {
        return currentStampSerial++;
    }

    public boolean isProcessingInput() {
        return time() >= inputPausedUntil;
    }
    
    /**
     * Queue additional cycle()'s to the inference process.
     *
     * @param cycles The number of inference steps
     */
    public void stepLater(final int cycles) {
        inputPausedUntil = (int) (time() + cycles);
    }    
    
    public Task newTask(Term content, char sentenceType, float freq, float conf, float priority, float durability) {
        return newTask(content, sentenceType, freq, conf, priority, durability, (Task)null);
    }
            
            
    public Task newTask(Term content, char sentenceType, float freq, float conf, float priority, float durability, final Task parentTask) {
        return newTask(content, sentenceType, freq, conf, priority, durability, parentTask, Tense.Present);
    }
    
    /** convenience method for forming a new Task from a term */
    public Task newTask(Term content, char sentenceType, float freq, float conf, float priority, float durability, Tense tense) {
        return newTask(content, sentenceType, freq, conf, priority, durability, null, tense);
    }
    
    /** convenience method for forming a new Task from a term */
    public Task newTask(Term content, char sentenceType, float freq, float conf, float priority, float durability, Task parentTask, Tense tense) {
        
        TruthValue truth = new TruthValue(freq, conf);
        Sentence sentence = new Sentence(
                content, 
                sentenceType, 
                truth, 
                new Stamp(this, tense));
        BudgetValue budget = new BudgetValue(Parameters.DEFAULT_JUDGMENT_PRIORITY, Parameters.DEFAULT_JUDGMENT_DURABILITY, truth);
        Task task = new Task(sentence, budget, parentTask);
        return task;
    }

    /** converts durations to cycles */
    public final float cycles(AtomicDouble durations) {
        return param.duration.floatValue() * durations.floatValue();
    }

    @Override
    public Iterator<Concept> iterator() {
        return concepts.iterator();
    }
}
