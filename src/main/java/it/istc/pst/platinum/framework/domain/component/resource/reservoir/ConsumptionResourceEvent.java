package it.istc.pst.platinum.framework.domain.component.resource.reservoir;

import it.istc.pst.platinum.framework.domain.component.resource.ResourceEvent;
import it.istc.pst.platinum.framework.domain.component.resource.ResourceEventType;
import it.istc.pst.platinum.framework.microkernel.lang.plan.Decision;
import it.istc.pst.platinum.framework.time.tn.TimePoint;

/**
 * 
 * @author anacleto
 *
 */
public class ConsumptionResourceEvent extends ResourceEvent<TimePoint>
{
	/**
	 * 
	 * @param activity
	 * @param amount
	 */
	protected ConsumptionResourceEvent(Decision activity, int amount) {
		super(ResourceEventType.CONSUMPTION, activity, amount, activity.getToken().getInterval().getStartTime());
	}
}
