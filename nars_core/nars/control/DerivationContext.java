/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.control;

import java.util.ArrayList;
import java.util.List;
import nars.util.Events;
import nars.storage.Memory;
import nars.NAR;
import nars.config.Parameters;
import nars.util.Plugin;
import nars.entity.BudgetValue;
import nars.entity.Concept;
import nars.entity.Sentence;
import nars.entity.Stamp;
import nars.entity.Task;
import nars.entity.TaskLink;
import nars.entity.TermLink;
import nars.entity.TruthValue;
import nars.inference.TruthFunctions;
import nars.language.CompoundTerm;
import nars.language.Interval;
import nars.language.Term;
import nars.language.Variable;
import nars.operator.Operation;

/**
 * NAL Reasoner Process.  Includes all reasoning process state.
 */
public class DerivationContext {

    public interface DerivationFilter extends Plugin {
        /** returns null if allowed to derive, or a String containing a short rejection reason for logging */
        public String reject(DerivationContext nal, Task task, boolean revised, boolean single, Task parent, Sentence otherBelief);

        @Override
        public default boolean setEnabled(NAR n, boolean enabled) {
            return true;
        }
        
    }
    
    public boolean evidentalOverlap = false;
    public final Memory memory;
    protected Term currentTerm;
    protected Concept currentConcept;
    protected Task currentTask;
    protected TermLink currentBeliefLink;
    protected TaskLink currentTaskLink;
    protected Sentence currentBelief;
    protected Stamp newStamp;
    public StampBuilder newStampBuilder;
    protected List<DerivationFilter> derivationFilters = null;
    
    public DerivationContext(Memory mem) {
        super();
        this.memory = mem;
        this.derivationFilters = mem.param.getDerivationFilters();
    }

    public void setDerivationFilters(List<DerivationFilter> derivationFilters) {
        this.derivationFilters = derivationFilters;
    }
   
    public void emit(final Class c, final Object... o) {
        memory.emit(c, o);
    }


    
    /**
     * Derived task comes from the inference rules.
     *
     * @param task the derived task
     * @param overlapAllowed //https://groups.google.com/forum/#!topic/open-nars/FVbbKq5En-M
     */
    public boolean derivedTask(final Task task, final boolean revised, final boolean single, Task parent,Sentence occurence2, boolean overlapAllowed) {                        

        /*if(task.sentence.truth.getConfidence() > 0.98 && (task.getTerm() instanceof Implication) && 
                (((Implication) task.getTerm()).getSubject() instanceof Conjunction) && 
                ((Conjunction) ((Implication) task.getTerm()).getSubject()).term.length == 4) {
            int a = 3;
            a = 0;
            System.out.println(a);
        }*/
        
        /*if(task.sentence.term instanceof Implication) {
            Implication imp = (Implication) task.sentence.term;
            if(imp.getSubject() instanceof Conjunction) {
                Conjunction conj = (Conjunction) imp.getSubject();
                boolean lastWasIval = false;
                for(Term t : conj.term) {
                    if(t instanceof Interval) {
                        if(lastWasIval) {
                            int a = 0;
                            System.out.println(a); //only decompose compound is allowed to do this!
                        } //this debug code helps identifying cases
                        lastWasIval = true;
                    } else {
                        lastWasIval = false;
                    }
                }
            }
        }*/
        
        
        if (derivationFilters!=null) {            
            for (int i = 0; i < derivationFilters.size(); i++) {
                DerivationFilter d = derivationFilters.get(i);
                String rejectionReason = d.reject(this, task, revised, single, parent, occurence2);
                if (rejectionReason!=null) {
                    memory.removeTask(task, rejectionReason);
                    return false;
                }
            }
        }
        
        final Sentence occurence = parent!=null ? parent.sentence : null;

        
        if (!task.budget.aboveThreshold()) {
            memory.removeTask(task, "Insufficient Budget");
            return false;
        }
        
        if (task.sentence != null && task.sentence.truth != null) {
            float conf = task.sentence.truth.getConfidence();
            if (conf == 0) {
                //no confidence - we can delete the wrongs out that way.
                memory.removeTask(task, "Ignored (zero confidence)");
                return false;
            }
        }
        

    
        if (task.sentence.term instanceof Operation) {
            Operation op = (Operation) task.sentence.term;
            if (op.getSubject() instanceof Variable || op.getPredicate() instanceof Variable) {
                memory.removeTask(task, "Operation with variable as subject or predicate");
                return false;
            }
        }
        
        
        
        final Stamp stamp = task.sentence.stamp;
        if (occurence != null && !occurence.isEternal()) {
            stamp.setOccurrenceTime(occurence.getOccurenceTime());
        }
        if (occurence2 != null && !occurence2.isEternal()) {
            stamp.setOccurrenceTime(occurence2.getOccurenceTime());
        }
        
        //its revision, of course its cyclic, apply evidental base policy
        if(!overlapAllowed) { //todo reconsider
            final int stampLength = stamp.baseLength;
            for (int i = 0; i < stampLength; i++) {
                final long baseI = stamp.evidentialBase[i];
                for (int j = 0; j < stampLength; j++) {
                    if (this.evidentalOverlap || ((i != j) && (baseI == stamp.evidentialBase[j]))) {
                        memory.removeTask(task, "Overlapping Evidenctal Base");
                        //"(i=" + i + ",j=" + j +')' /* + " in " + stamp.toString()*/
                        return false;
                    }
                }
            }
        }
        
        //deactivated, new anticipation handling is attempted instead
        /*if(task.sentence.getOccurenceTime()>memory.time() && ((this.getCurrentTask()!=null && (this.getCurrentTask().isInput() || this.getCurrentTask().sentence.producedByTemporalInduction)) || (this.getCurrentBelief()!=null && this.getCurrentBelief().producedByTemporalInduction))) {
            Anticipate ret = ((Anticipate)memory.getOperator("^anticipate"));
            if(ret!=null) {
                ret.anticipate(task.sentence.term, memory, task.sentence.getOccurenceTime(),task);
            }
        }*/
        
        task.setElemOfSequenceBuffer(false);
        if(!revised) {
            task.getBudget().setDurability(task.getBudget().getDurability()*Parameters.DERIVATION_DURABILITY_LEAK);
            task.getBudget().setPriority(task.getBudget().getPriority()*Parameters.DERIVATION_PRIORITY_LEAK);
        }
        memory.event.emit(Events.TaskDerive.class, task, revised, single, occurence, occurence2);
        //memory.logic.TASK_DERIVED.commit(task.budget.getPriority());
        
        addTask(task, "Derived");
        return true;
    }

    /* --------------- new task building --------------- */
    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     */
    public boolean doublePremiseTaskRevised(final Term newContent, final TruthValue newTruth, final BudgetValue newBudget) {
        Sentence newSentence = new Sentence(newContent, getCurrentTask().sentence.punctuation, newTruth, getTheNewStamp());
        Task newTask = new Task(newSentence, newBudget, getCurrentTask(), getCurrentBelief());
        return derivedTask(newTask, true, false, null, null, true); //allows overlap since overlap was already checked on revisable( function
    }                                                               //which is not the case for other single premise tasks

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     * @param temporalInduction
     * @param overlapAllowed // https://groups.google.com/forum/#!topic/open-nars/FVbbKq5En-M
     */
    public List<Task> doublePremiseTask(final Term newContent, final TruthValue newTruth, final BudgetValue newBudget, boolean temporalInduction, boolean overlapAllowed) {
                
        List<Task> ret = new ArrayList<Task>();
        if(newContent == null) {
            return null;
        }
        
        if (!newBudget.aboveThreshold()) {
            return null;
        }
        
        if ((newContent != null) && (!(newContent instanceof Interval)) && (!(newContent instanceof Variable))) {
            
            if(newContent.subjectOrPredicateIsIndependentVar()) {
                return null;
            }

            try {
                final Sentence newSentence = new Sentence(newContent, getCurrentTask().sentence.punctuation, newTruth, getTheNewStamp());
                newSentence.producedByTemporalInduction=temporalInduction;
                final Task newTask = Task.make(newSentence, newBudget, getCurrentTask(), getCurrentBelief());
                
                if (newTask!=null) {
                    boolean added = derivedTask(newTask, false, false, null, null, overlapAllowed);
                    if(added) {
                        ret.add(newTask);
                    }
                }
            }
            catch (CompoundTerm.UnableToCloneException e) {
                return null;
            }
            
            
            //"Since in principle it is always valid to eternalize a tensed belief"
            if(temporalInduction && Parameters.IMMEDIATE_ETERNALIZATION) { //temporal induction generated ones get eternalized directly
                
                try {

                TruthValue truthEt=TruthFunctions.eternalize(newTruth);               
                Stamp st=getTheNewStamp().clone();
                st.setEternal();
                final Sentence newSentence = new Sentence(newContent, getCurrentTask().sentence.punctuation, truthEt, st);
                newSentence.producedByTemporalInduction=temporalInduction;
                final Task newTask = Task.make(newSentence, newBudget, getCurrentTask(), getCurrentBelief());
                if (newTask!=null) {
                    boolean added = derivedTask(newTask, false, false, null, null, overlapAllowed);
                    if(added) {
                        ret.add(newTask);
                    }
                }
                
            }
            catch (CompoundTerm.UnableToCloneException e) {
                return null;
            }
                
            }
            return ret;
        }
        return null;
    }

    /**
     * Shared final operations by all double-premise rules, called from the
     * rules except StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     * @param revisible Whether the sentence is revisible
     */
    //    public void doublePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget, boolean revisible) {
    //        if (newContent != null) {
    //            Sentence taskSentence = currentTask.getSentence();
    //            Sentence newSentence = new Sentence(newContent, taskSentence.getPunctuation(), newTruth, newStamp, revisible);
    //            Task newTask = new Task(newSentence, newBudget, currentTask, currentBelief);
    //            derivedTask(newTask, false, false);
    //        }
    //    }
    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     */
    public boolean singlePremiseTask(Term newContent, TruthValue newTruth, BudgetValue newBudget) {
        return singlePremiseTask(newContent, getCurrentTask().sentence.punctuation, newTruth, newBudget);
    }

    /**
     * Shared final operations by all single-premise rules, called in
     * StructuralRules
     *
     * @param newContent The content of the sentence in task
     * @param punctuation The punctuation of the sentence in task
     * @param newTruth The truth value of the sentence in task
     * @param newBudget The budget value in task
     */
    public boolean singlePremiseTask(final Term newContent, final char punctuation, final TruthValue newTruth, final BudgetValue newBudget) {
        
        if (!newBudget.aboveThreshold())
            return false;
        
        Task parentTask = getCurrentTask().getParentTask();
        if (parentTask != null) {
            if (parentTask.getTerm() == null) {
                return false;
            }
            if (newContent == null) {
                return false;
            }
            if (newContent.equals(parentTask.getTerm())) {
                return false;
            }
        }
        Sentence taskSentence = getCurrentTask().sentence;
        if (taskSentence.isJudgment() || getCurrentBelief() == null) {
            setTheNewStamp(new Stamp(taskSentence.stamp, getTime()));
        } else {
            // to answer a question with negation in NAL-5 --- move to activated task?
            setTheNewStamp(new Stamp(getCurrentBelief().stamp, getTime()));
        }
        
        if(newContent.subjectOrPredicateIsIndependentVar()) {
            return false;
        }
        
        if(newContent instanceof Interval) {
            return false;
        }
        Sentence newSentence = new Sentence(newContent, punctuation, newTruth, getTheNewStamp());
        Task newTask = Task.make(newSentence, newBudget, getCurrentTask());
        if (newTask!=null) {
            return derivedTask(newTask, false, true, null, null, false);
        }
        return false;
    }

    public boolean singlePremiseTask(Sentence newSentence, BudgetValue newBudget) {
        if (!newBudget.aboveThreshold()) {
            return false;
        }
        Task newTask = new Task(newSentence, newBudget, getCurrentTask());
        return derivedTask(newTask, false, true, null, null, false);
    }

    public long getTime() {
        return memory.time();
    }

    public Stamp getNewStamp() {
        return newStamp;
    }

    public void setNewStamp(Stamp newStamp) {
        this.newStamp = newStamp;
    }

    /**
     * @return the currentTask
     */
    public Task getCurrentTask() {
        return currentTask;
    }

    /**
     * @param currentTask the currentTask to set
     */
    public void setCurrentTask(Task currentTask) {        
        this.currentTask = currentTask;
    }

    public void setCurrentConcept(Concept currentConcept) {
        this.currentConcept = currentConcept;
    }

    /**
     * @return the newStamp
     */
    public Stamp getTheNewStamp() {
        if (newStamp == null) {
            //if newStamp==null then newStampBuilder must be available. cache it's return value as newStamp
            newStamp = newStampBuilder.build();
            newStampBuilder = null;
        }
        return newStamp;
    }

    /**
     * @param newStamp the newStamp to set
     */
    public Stamp setTheNewStamp(Stamp newStamp) {
        this.newStamp = newStamp;
        this.newStampBuilder = null;
        return newStamp;
    }

    public interface StampBuilder {

        Stamp build();
    }

    /** creates a lazy/deferred StampBuilder which only constructs the stamp if getTheNewStamp() is actually invoked */
    public void setTheNewStamp(final Stamp first, final Stamp second, final long time) {
        newStamp = null;
        newStampBuilder = new StampBuilder() {
            @Override
            public Stamp build() {
                return new Stamp(first, second, time);
            }
        };
    }

    /**
     * @return the currentBelief
     */
    public Sentence getCurrentBelief() {
        return currentBelief;
    }

    /**
     * @param currentBelief the currentBelief to set
     */
    public void setCurrentBelief(Sentence currentBelief) {
        this.currentBelief = currentBelief;
    }

    /**
     * @return the currentBeliefLink
     */
    public TermLink getCurrentBeliefLink() {
        return currentBeliefLink;
    }

    /**
     * @param currentBeliefLink the currentBeliefLink to set
     */
    public void setCurrentBeliefLink(TermLink currentBeliefLink) {
        this.currentBeliefLink = currentBeliefLink;
    }

    /**
     * @return the currentTaskLink
     */
    public TaskLink getCurrentTaskLink() {
        return currentTaskLink;
    }

    /**
     * @param currentTaskLink the currentTaskLink to set
     */
    public void setCurrentTaskLink(TaskLink currentTaskLink) {
        this.currentTaskLink = currentTaskLink;
    }

    /**
     * @return the currentTerm
     */
    public Term getCurrentTerm() {
        return currentTerm;
    }

    /**
     * @param currentTerm the currentTerm to set
     */
    public void setCurrentTerm(Term currentTerm) {
        this.currentTerm = currentTerm;
    }

    /**
     * @return the currentConcept
     */
    public Concept getCurrentConcept() {
        return currentConcept;
    }

    public Memory mem() {
        return memory;
    }
    
    /** tasks added with this method will be remembered by this NAL instance; useful for feedback */
    public void addTask(Task t, String reason) {
        if(t.sentence.term==null) {
            return;
        }
        memory.addNewTask(t, reason);
    }
    
    /**
     * Activated task called in MatchingRules.trySolution and
     * Concept.processGoal
     *
     * @param budget The budget value of the new Task
     * @param sentence The content of the new Task
     * @param candidateBelief The belief to be used in future inference, for
     * forward/backward correspondence
     */
    public void addTask(final Task currentTask, final BudgetValue budget, final Sentence sentence, final Sentence candidateBelief) {        
        addTask(new Task(sentence, budget, currentTask, sentence, candidateBelief),
                "Activated");        
    }    
    
    @Override
    public String toString() {
        return "DerivationContext[" + currentConcept + "," + currentTaskLink + "]";
    }
}
