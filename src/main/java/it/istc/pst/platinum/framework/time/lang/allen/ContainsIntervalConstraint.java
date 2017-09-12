package it.istc.pst.platinum.framework.time.lang.allen;

import it.istc.pst.platinum.framework.time.TemporalInterval;
import it.istc.pst.platinum.framework.time.lang.BinaryTemporalConstraint;
import it.istc.pst.platinum.framework.time.lang.TemporalConstraintType;

/**
 * 
 * @author anacleto
 *
 */
public final class ContainsIntervalConstraint extends BinaryTemporalConstraint<TemporalInterval, TemporalInterval>
{
	private long[] startTimeBounds;
	private long[] endTimeBounds;
	
	/**
	 * 
	 */
	protected ContainsIntervalConstraint() {
		 super(TemporalConstraintType.CONTAINS);
		 this.startTimeBounds = new long[2];
		 this.endTimeBounds = new long[2];
	}
	
	/**
	 * 
	 * @param startTimeBounds
	 */
	public void setStartTimeBound(long[] bounds) {
		this.startTimeBounds[0] = bounds[0];
		this.startTimeBounds[1] = bounds[1];
	}
	
	/**
	 * 
	 * @return
	 */
	public long[] getFirstBound() {
		return startTimeBounds;
	}
	
	/**
	 * 
	 * @param endTimeBounds
	 */
	public void setEndTimeBound(long[] bounds) {
		this.endTimeBounds[0] = bounds[0];
		this.endTimeBounds[1] = bounds[1];
	}
	
	/**
	 * 
	 * @return
	 */
	public long[] getSecondBound() {
		return endTimeBounds;
	}
}