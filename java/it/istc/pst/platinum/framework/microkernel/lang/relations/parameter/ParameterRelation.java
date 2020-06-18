package it.istc.pst.platinum.framework.microkernel.lang.relations.parameter;

import it.istc.pst.platinum.framework.domain.component.Decision;
import it.istc.pst.platinum.framework.microkernel.lang.relations.Relation;
import it.istc.pst.platinum.framework.microkernel.lang.relations.RelationType;
import it.istc.pst.platinum.framework.parameter.lang.constraints.ParameterConstraint;
import it.istc.pst.platinum.framework.parameter.lang.constraints.ParameterConstraintType;

/**
 * 
 * @author anacleto
 *
 */
public abstract class ParameterRelation extends Relation 
{
	protected String referenceParameterLabel;
	
	/**
	 * 
	 * @param id
	 * @param type
	 * @param reference
	 * @param target
	 */
	protected ParameterRelation(int id, RelationType type, Decision reference, Decision target) {
		super(id, type, reference, target);
	}
	
	/**
	 * 
	 * @param label
	 */
	public void setReferenceParameterLabel(String label) {
		this.referenceParameterLabel = label;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getReferenceParameterLabel() {
		return this.referenceParameterLabel;
	}
	
	/**
	 * 
	 */
	@Override
	public ParameterConstraint getConstraint() {
		// cast constraint
		return (ParameterConstraint) this.constraint;
	}
	
	/**
	 * 
	 * @return
	 */
	public abstract ParameterConstraintType getConstraintType();
	
	/**
	 * 
	 */
	@Override
	public abstract ParameterConstraint create();
	
}