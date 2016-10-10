package it.uniroma3.epsl2.framework.time.tn.solver.lang.event;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import it.uniroma3.epsl2.framework.time.tn.TimePoint;

/**
 * 
 * @author alessandroumbrico
 *
 */
public class AddTimePointTemporalNetworkNotification extends TemporalNetworkNotification 
{
	private List<TimePoint> points;
	
	/**
	 * 
	 */
	protected AddTimePointTemporalNetworkNotification() {
		super(TemporalNetworkNotificationTypes.ADD_TP);
		this.points = new LinkedList<TimePoint>();
	}
	
	/**
	 * 
	 * @param tp
	 */
	public void addTimePoint(TimePoint tp) {
		this.points.add(tp);
	}
	
	/**
	 * 
	 * @return
	 */
	public List<TimePoint> getPoints() {
		return new ArrayList<>(this.points);
	}
}