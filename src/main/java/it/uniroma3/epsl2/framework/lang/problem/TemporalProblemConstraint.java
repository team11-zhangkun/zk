package it.uniroma3.epsl2.framework.lang.problem;

import it.uniroma3.epsl2.framework.lang.plan.RelationType;
import it.uniroma3.epsl2.framework.microkernel.ConstraintCategory;

/**
 * 
 * @author anacleto
 *
 */
public class TemporalProblemConstraint extends ProblemConstraint 
{
	private long[][] bounds;
	
	/**
	 * 
	 * @param type
	 * @param reference
	 * @param target
	 * @param bounds
	 */
	protected TemporalProblemConstraint(RelationType type, ProblemFluent reference, ProblemFluent target, long[][] bounds) {
		super(type, reference, target);
		// check type
		if (type.getCategory().equals(ConstraintCategory.PARAMETER_CONSTRAINT)) {
			throw new RuntimeException("Invalid relation type for temporal constraints " + type);
		}
		// set temporal bounds
		this.bounds = bounds;
	}
	
	/**
	 * 
	 * @return
	 */
	public long[][] getBounds() {
		return bounds;
	}
}