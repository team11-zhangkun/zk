package it.uniroma3.epsl2.executive.lang;

import it.uniroma3.epsl2.executive.pdb.ExecutionNode;

/**
 * 
 * @author anacleto
 *
 */
public class DurationOverflow extends ExecutionFailureCause 
{
	private long observedDuration;
	
	/**
	 * 
	 * @param tick
	 * @param node
	 * @param observedDuration
	 */
	public DurationOverflow(long tick, ExecutionNode node, long observedDuration) {
		super(tick, node, ExecutionFailureCauseType.DURATION_OVERFLOW);
		this.observedDuration = observedDuration;
	}
	
	/**
	 * 
	 * @return
	 */
	public long getObservedDuration() {
		return observedDuration;
	}
	
	/**
	 * 
	 */
	@Override
	public String toString() {
		return "[DurationOverflow] The observed duration exceeds the upper bound of the domain specification\n"
				+ "\t- observed-duration= " + this.observedDuration + "\n"
				+ "\t- node= " + this.getInterruptNode() + "\n";
	}
}
