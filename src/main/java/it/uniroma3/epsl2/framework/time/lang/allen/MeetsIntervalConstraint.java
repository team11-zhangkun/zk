package it.uniroma3.epsl2.framework.time.lang.allen;

import it.uniroma3.epsl2.framework.time.lang.TemporalConstraint;
import it.uniroma3.epsl2.framework.time.lang.TemporalConstraintType;

/**
 * 
 * @author alessandroumbrico
 *
 */
public final class MeetsIntervalConstraint extends TemporalConstraint 
{
	private long lb;
	private long ub;
	
	/**
	 * 
	 */
	protected MeetsIntervalConstraint() {
		super(TemporalConstraintType.MEETS);
		this.lb = 0;
		this.ub = 0;
	}
	
	/**
	 * 
	 */
	@Override
	public void setBounds(long[][] bounds) {
		this.lb = 0;
		this.ub = 0;
	}
	
	/**
	 * 
	 * @return
	 */
	public long getLb() {
		return lb;
	}
	
	/**
	 * 
	 * @return
	 */
	public long getUb() {
		return ub;
	}
}