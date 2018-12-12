/*
 * NotEqual.java
 */
package nars.language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import nars.config.Parameters;
import nars.io.Symbols.NativeOperator;
import nars.operator.Operation;
import nars.operator.Operator;
import nars.operator.NotEqualOperation;

/**
 * A Statement about an Inheritance relation.
 */
public class NotEqual extends Statement {
	
	public static Vector<Term> v1 = new Vector<Term>();
	public static Vector<Term> v2 = new Vector<Term>();
	public static boolean contains(final Term subject, final Term predicate) {
		for(int i = 0; i < NotEqual.v1.size(); i++) {
	        if((NotEqual.v1.get(i).equals(subject) && NotEqual.v2.get(i).equals(predicate)) ||
	        		(NotEqual.v2.get(i).equals(subject) && NotEqual.v1.get(i).equals(predicate))) {
	    		return true;
	        }
    	}
		return false;
	}
	
	public static void reset() {
		v1.clear();
		v2.clear();
	}
	
    /**
     * Constructor with partial values, called by make
     * @param n The name of the term
     * @param arg The component list of the term
     */
    protected NotEqual(final Term[] arg) {
        super(arg);  
        
        init(arg);
    }
    
    protected NotEqual(final Term subj, final Term pred) {
        this(new Term[] { subj, pred} );
    }


    /**
     * Clone an object
     * @return A new object, to be casted into a SetExt
     */
    @Override public NotEqual clone() {
        return make(getSubject(), getPredicate());
    }

    @Override public NotEqual clone(Term[] t) {
        if (t.length!=2)
            throw new RuntimeException("Invalid terms for " + getClass().getSimpleName() + ": " + Arrays.toString(t));
                
        return make(t[0], t[1]);
    }

    /** alternate version of Inheritance.make that allows equivalent subject and predicate
     * to be reduced to the common term.      */
    public static Term makeTerm(final Term subject, final Term predicate) {            
        return make(subject, predicate);        
    }

    /**
     * Try to make a new compound from two term. Called by the inference rules.
     * @param subject The first compoment
     * @param predicate The second compoment
     * @param memory Reference to the memory
     * @return A compound generated or null
     */
    public static NotEqual make(final Term subject, final Term predicate) {
    	v1.add(subject.clone());
    	v2.add(predicate.clone());
    	
        if (subject==null || predicate==null || invalidStatement(subject, predicate)) {            
            return null;
        }
        
        boolean subjectProduct = subject instanceof Product;
        boolean predicateOperator = predicate instanceof Operator;
        
        if (Parameters.DEBUG) {
            if (!predicateOperator && predicate.toString().startsWith("^")) {
                throw new RuntimeException("operator term detected but is not an operator: " + predicate);
            }
        }
        
        if (subjectProduct && predicateOperator) {
            //name = Operation.makeName(predicate.name(), ((CompoundTerm) subject).term);
            return NotEqualOperation.make((Operator)predicate, ((CompoundTerm)subject).term, true);
        } else {            
            return new NotEqual(subject, predicate);
        }
         
    }

    /**
     * Get the operator of the term.
     * @return the operator of the term
     */
    @Override
    public NativeOperator operator() {
        return NativeOperator.NOTEQUAL;
    }

}

