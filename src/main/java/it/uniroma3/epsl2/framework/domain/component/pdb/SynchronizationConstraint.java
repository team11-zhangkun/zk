package it.uniroma3.epsl2.framework.domain.component.pdb;

import it.uniroma3.epsl2.framework.lang.plan.RelationType;
import it.uniroma3.epsl2.framework.microkernel.ConstraintCategory;

/**
 * 
 * @author anacleto
 *
 */
public abstract class SynchronizationConstraint 
{
	protected RelationType type;
	protected TokenVariable source;
	protected TokenVariable target;
	
	/**
	 * 
	 * @param type
	 * @param source
	 * @param target
	 */
	protected SynchronizationConstraint(RelationType type, TokenVariable source, TokenVariable target) {
		this.source = source;
		this.target = target;
		this.type = type;
	}
	
	/**
	 * 
	 * @return
	 */
	public TokenVariable getSource() {
		return source;
	}
	
	/**
	 * 
	 * @return
	 */
	public TokenVariable getTarget() {
		return target;
	}
	
	/**
	 * 
	 * @return
	 */
	public RelationType getType() {
		return type;
	}
	
	/**
	 * 
	 * @return
	 */
	public ConstraintCategory getCategory() {
		return this.type.getCategory();
	}
}