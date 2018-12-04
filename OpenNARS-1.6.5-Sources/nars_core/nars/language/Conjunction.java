/*
 * Conjunction.java
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
package nars.language;

import static java.lang.System.arraycopy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import nars.config.Parameters;
import nars.inference.TemporalRules;
import nars.io.Symbols.NativeOperator;

/**
 * Conjunction of statements
 */
public class Conjunction extends CompoundTerm {

    public final int temporalOrder;

    public static Term[] removeFirstInterval(Term[] arg) {
        if(arg[0] instanceof Interval) {
            Term[] argNew = new Term[arg.length - 1];
            for(int i=1;i<arg.length;i++) {
                argNew[i - 1] = arg[i];
            }
            return argNew;
        }
        return arg;
    }
    
    /**
     * Constructor with partial values, called by make
     *
     * @param arg The component list of the term
     * @param order
     * @param normalized
     */
    protected Conjunction(Term[] arg, final int order, boolean normalized) {
        super(arg);
        
        temporalOrder = order;
        init(this.term);

        /*if (normalized)
            setNormalized(true);*/
    }

    @Override
    final public int getMinimumRequiredComponents() {
        return 1;
    }


    @Override public Term clone(Term[] t) {        
        return make(t, temporalOrder);
    }

    /**
     * Clone an object
     *
     * @return A new object
     */
    @Override
    public Conjunction clone() {
        return new Conjunction(term, temporalOrder, isNormalized());
    }
    
    
    /**
     * Get the operator of the term.
     *
     * @return the operator of the term
     */
    @Override
    public NativeOperator operator() {
        switch (temporalOrder) {
            case TemporalRules.ORDER_FORWARD:
                return NativeOperator.SEQUENCE;
            case TemporalRules.ORDER_CONCURRENT:
                return NativeOperator.PARALLEL;
            default:
                return NativeOperator.CONJUNCTION;
        }
    }

    /**
     * Check if the compound is commutative.
     *
     * @return true for commutative
     */
    @Override
    public boolean isCommutative() {
        return temporalOrder != TemporalRules.ORDER_FORWARD;
    }

    /**
     * Try to make a new compound from a list of term. Called by StringParser.
     *
     * @return the Term generated from the arguments
     * @param argList the list of arguments
     * @param memory Reference to the memory
     */
    final public static Term make(final Term[] argList) {
        return make(argList, TemporalRules.ORDER_NONE);
    }

    public static boolean isConjunctionAndHasSameOrder(Term t, int order) {
        if(t instanceof Conjunction) {
            Conjunction c=(Conjunction) t;
            if(c.getTemporalOrder()==order) {
                return true;
            }
        }
        return false;
    }
    
    public static Term[] flatten(Term[] args, int order) { //flatten only same order!
        //determine how many there are with same order
        int sz=0;
        for(int i=0;i<args.length;i++) {
            Term a=args[i];
            if(isConjunctionAndHasSameOrder(a, order)) {
                sz+=((Conjunction)a).term.length;
            } else {
                sz+=1;
            }
        }
        Term[] ret=new Term[sz];
        int k=0;
        for(int i=0;i<args.length;i++) {
            Term a=args[i];
            if(isConjunctionAndHasSameOrder(a, order)) {
                Conjunction c=((Conjunction)a);
                for(Term t: c.term) {
                    ret[k]=t;
                    k++;
                }
            } else {
                ret[k]=a;
                k++;
            }
        }
        return ret;
    }
    
    /**
     * Try to make a new compound from a list of term. Called by StringParser.
     *
     * @param temporalOrder The temporal order among term
     * @param argList the list of arguments
     * @param memory Reference to the memory
     * @return the Term generated from the arguments, or null if not possible
     */
    final public static Term make(final Term[] argList, final int temporalOrder) {
        if (Parameters.DEBUG) {  Terms.verifyNonNull(argList);}
        
        if (argList.length == 0) {
            return null;
        }                         // special case: single component
        if (argList.length == 1) {
            return argList[0];
        }                         // special case: single component
        
        if (temporalOrder == TemporalRules.ORDER_FORWARD) {
            Term[] newArgList = removeFirstInterval(flatten(argList, temporalOrder));
            if(newArgList.length == 1) {
                return newArgList[0];
            }
            return new Conjunction(newArgList, temporalOrder, false);
            
        } else {
            
            // sort/merge arguments
            final TreeSet<Term> set = new TreeSet<>();
            for (Term t : argList) {
                if(!(t instanceof Interval)) { //intervals only for seqs
                    set.add(t);
                }
            }
            
            if (set.size() == 1) {
                return set.first();
            }
            
            return new Conjunction(set.toArray(new Term[set.size()] ), temporalOrder, false);
        }
    }

    final public static Term make(final Term prefix, final List<Interval> suffix, final int temporalOrder) {
        Term[] t = new Term[suffix.size()+1];
        int i = 0;
        t[i++] = prefix;
        for (Term x : suffix)
            t[i++] = x;
        return make(t, temporalOrder);        
    }
    
    final public static Term make(final Term prefix, final List<Interval> ival, final Term suffix, final int temporalOrder) {
        Term[] t = new Term[ival.size()+2];
        int i = 0;
        t[i++] = prefix;
        for (Term x : ival)
            t[i++] = x;
        t[i++] = suffix;
        return make(t, temporalOrder);        
    }
    
    /**    
     *
     * @param set a set of Term as term
     * @param memory Reference to the memory
     * @return the Term generated from the arguments
     */
    final private static Term make(final Collection<Term> set, int temporalOrder) {
        Term[] argument = set.toArray(new Term[set.size()]);
        return make(argument, temporalOrder);
    }

    @Override
    protected CharSequence makeName() {
        return makeCompoundName( operator(),  term);
    }

    
    // overload this method by term type?
    /**
     * Try to make a new compound from two term. Called by the inference rules.
     *
     * @param term1 The first component
     * @param term2 The second component
     * @param memory Reference to the memory
     * @return A compound generated or a term it reduced to
     */
    final public static Term make(final Term term1, final Term term2) {
        return make(term1, term2, TemporalRules.ORDER_NONE);
    }

    final public static Term make(final Term term1, final Term term2, int temporalOrder) {
        if (temporalOrder == TemporalRules.ORDER_FORWARD) {
            
            final Term[] components;
            
            if ((term1 instanceof Conjunction) && (term1.getTemporalOrder() == TemporalRules.ORDER_FORWARD)) {
                
                CompoundTerm cterm1 = (CompoundTerm) term1;
                
                ArrayList<Term> list = new ArrayList<>(cterm1.size());
                cterm1.addTermsTo(list);
                        
                if ((term2 instanceof Conjunction) && (term2.getTemporalOrder() == TemporalRules.ORDER_FORWARD)) { 
                    // (&/,(&/,P,Q),(&/,R,S)) = (&/,P,Q,R,S)
                    ((CompoundTerm) term2).addTermsTo(list);
                } 
                else {
                    // (&,(&,P,Q),R) = (&,P,Q,R)
                    list.add(term2);
                }
                
                components = list.toArray(new Term[list.size()]);
                
            } else if ((term2 instanceof Conjunction) && (term2.getTemporalOrder() == TemporalRules.ORDER_FORWARD)) {
                CompoundTerm cterm2 = (CompoundTerm) term2;
                components = new Term[((CompoundTerm) term2).size() + 1];
                components[0] = term1;
                arraycopy(cterm2.term, 0, components, 1, cterm2.size());
            } else {
                components = new Term[] { term1, term2 };
            }
            return make(components, temporalOrder);
            
        } else {
            
            final List<Term> set = new ArrayList();
            if (term1 instanceof Conjunction) {                
                ((CompoundTerm) term1).addTermsTo(set);
                if (term2 instanceof Conjunction) {                    
                    // (&,(&,P,Q),(&,R,S)) = (&,P,Q,R,S)
                    ((CompoundTerm) term2).addTermsTo(set);
                } 
                else {
                    // (&,(&,P,Q),R) = (&,P,Q,R)
                    set.add(term2);
                }                          
                
            } else if (term2 instanceof Conjunction) {
                ((CompoundTerm) term2).addTermsTo(set);
                set.add(term1);                              // (&,R,(&,P,Q)) = (&,P,Q,R)
            } else {                
                set.add(term1);
                set.add(term2);
            }
            
            return make(set, temporalOrder);
        }
    }

    @Override
    public int getTemporalOrder() {
        return temporalOrder;
    }

}
