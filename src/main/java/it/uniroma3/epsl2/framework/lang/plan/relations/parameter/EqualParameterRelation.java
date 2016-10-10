package it.uniroma3.epsl2.framework.lang.plan.relations.parameter;

import it.uniroma3.epsl2.framework.lang.plan.Decision;
import it.uniroma3.epsl2.framework.lang.plan.RelationType;
import it.uniroma3.epsl2.framework.parameter.lang.constraints.EqualParameterConstraint;
import it.uniroma3.epsl2.framework.parameter.lang.constraints.ParameterConstraint;
import it.uniroma3.epsl2.framework.parameter.lang.constraints.ParameterConstraintType;

/**
 * 
 * @author anacleto
 *
 */
public class EqualParameterRelation extends ParameterRelation 
{
	/**
	 * 
	 * @param refrence
	 * @param target
	 */
	protected EqualParameterRelation(Decision reference, Decision target) {
		super(RelationType.EQUAL_PARAMETER, reference, target);
	}
	
	/**
	 * 
	 */
	@Override
	public ParameterConstraintType getConstraintType() {
		return ParameterConstraintType.EQUAL;
	}
	
	/**
	 * 
	 */
	@Override
	public ParameterConstraint create() {
		// create constraint
		EqualParameterConstraint constraint = this.factory.
				createParameterConstraint(ParameterConstraintType.EQUAL);
		// get index
		int index = this.reference.getParameterIndexByLabel(this.referenceParameterLabel);
		// set reference parameter
		constraint.setReference(this.reference.getParameterByIndex(index));
		// get index
		index = this.target.getParameterIndexByLabel(this.targetParameterLabel);
		// set target parameter
		constraint.setTarget(this.target.getParameterByIndex(index));
		// set and get constraint
		this.constraint = constraint;
		return constraint;
	}
	
	/**
	 * 
	 */
	@Override
	public String toString() {
		return "[Relation type= " + this.getType() +" reference= " + this.reference.getId() + ":" + this.reference.getValue().getLabel() +" "
				+ "referenceParameter= " + this.referenceParameterLabel + " target= " +  this.target.getId() + ":" + this.target.getValue().getLabel() + " "
						+ "targetParameter= " + this.targetParameterLabel + "]";
	}
}