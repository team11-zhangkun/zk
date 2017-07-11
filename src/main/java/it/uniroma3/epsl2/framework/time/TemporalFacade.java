package it.uniroma3.epsl2.framework.time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.uniroma3.epsl2.framework.microkernel.ApplicationFrameworkContainer;
import it.uniroma3.epsl2.framework.microkernel.ApplicationFrameworkObject;
import it.uniroma3.epsl2.framework.microkernel.annotation.inject.FrameworkLoggerPlaceholder;
import it.uniroma3.epsl2.framework.microkernel.annotation.inject.framework.TemporalNetworkPlaceholder;
import it.uniroma3.epsl2.framework.microkernel.annotation.inject.framework.TemporalSolverPlaceholder;
import it.uniroma3.epsl2.framework.microkernel.annotation.lifecycle.PostConstruct;
import it.uniroma3.epsl2.framework.microkernel.lang.ex.ConsistencyCheckException;
import it.uniroma3.epsl2.framework.microkernel.query.QueryManager;
import it.uniroma3.epsl2.framework.microkernel.query.TemporalQuery;
import it.uniroma3.epsl2.framework.microkernel.query.TemporalQueryFactory;
import it.uniroma3.epsl2.framework.microkernel.query.TemporalQueryType;
import it.uniroma3.epsl2.framework.time.ex.InconsistentIntervaEndTimeException;
import it.uniroma3.epsl2.framework.time.ex.InconsistentIntervalDurationException;
import it.uniroma3.epsl2.framework.time.ex.InconsistentIntervalStartTimeException;
import it.uniroma3.epsl2.framework.time.ex.TemporalConstraintPropagationException;
import it.uniroma3.epsl2.framework.time.ex.TemporalIntervalCreationException;
import it.uniroma3.epsl2.framework.time.ex.TimePointCreationException;
import it.uniroma3.epsl2.framework.time.lang.FixIntervalDurationConstraint;
import it.uniroma3.epsl2.framework.time.lang.FixTimePointConstraint;
import it.uniroma3.epsl2.framework.time.lang.TemporalConstraint;
import it.uniroma3.epsl2.framework.time.lang.TemporalConstraintFactory;
import it.uniroma3.epsl2.framework.time.lang.TemporalConstraintType;
import it.uniroma3.epsl2.framework.time.lang.allen.AfterIntervalConstraint;
import it.uniroma3.epsl2.framework.time.lang.allen.BeforeIntervalConstraint;
import it.uniroma3.epsl2.framework.time.lang.allen.ContainsIntervalConstraint;
import it.uniroma3.epsl2.framework.time.lang.allen.DuringIntervalConstraint;
import it.uniroma3.epsl2.framework.time.lang.allen.EndsDuringIntervalConstraint;
import it.uniroma3.epsl2.framework.time.lang.allen.EqualsIntervalConstraint;
import it.uniroma3.epsl2.framework.time.lang.allen.MeetsIntervalConstraint;
import it.uniroma3.epsl2.framework.time.lang.allen.MetByIntervalConstraint;
import it.uniroma3.epsl2.framework.time.lang.allen.StartsDuringIntervalConstraint;
import it.uniroma3.epsl2.framework.time.lang.query.ComputeMakespanQuery;
import it.uniroma3.epsl2.framework.time.lang.query.IntervalDistanceQuery;
import it.uniroma3.epsl2.framework.time.lang.query.IntervalOverlapQuery;
import it.uniroma3.epsl2.framework.time.lang.query.IntervalPseudoControllabilityQuery;
import it.uniroma3.epsl2.framework.time.lang.query.IntervalScheduleQuery;
import it.uniroma3.epsl2.framework.time.solver.TemporalSolver;
import it.uniroma3.epsl2.framework.time.tn.TemporalNetwork;
import it.uniroma3.epsl2.framework.time.tn.TimePoint;
import it.uniroma3.epsl2.framework.time.tn.TimePointDistanceConstraint;
import it.uniroma3.epsl2.framework.time.tn.ex.InconsistentDistanceConstraintException;
import it.uniroma3.epsl2.framework.time.tn.ex.InconsistentTpValueException;
import it.uniroma3.epsl2.framework.time.tn.ex.TemporalConsistencyCheckException;
import it.uniroma3.epsl2.framework.time.tn.lang.query.TimePointDistanceQuery;
import it.uniroma3.epsl2.framework.time.tn.lang.query.TimePointQuery;
import it.uniroma3.epsl2.framework.time.tn.lang.query.TimePointScheduleQuery;
import it.uniroma3.epsl2.framework.utils.log.FrameworkLogger;

/**
 * 
 * @author anacleto
 *
 */
public abstract class TemporalFacade extends ApplicationFrameworkObject implements QueryManager<TemporalQuery>
{
	@FrameworkLoggerPlaceholder(lookup = ApplicationFrameworkContainer.FRAMEWORK_SINGLETON_PLANDATABASE_LOGGER)
	protected FrameworkLogger logger;
	
	@TemporalNetworkPlaceholder
	protected TemporalNetwork tn;						// temporal network

	@TemporalSolverPlaceholder
	protected TemporalSolver<TimePointQuery> solver;	// time point reasoner
	
	protected Set<TemporalInterval> intervals;			// set of created temporal intervals
	protected TemporalQueryFactory qf;					// temporal query factory
	protected TemporalConstraintFactory cf;	 			// temporal constraint factory
	
	/**
	 * 
	 */
	protected TemporalFacade() {
		// get query factory instance
		this.qf = TemporalQueryFactory.getInstance();
		this.cf = TemporalConstraintFactory.getInstance();
		this.intervals = new HashSet<>();
	}
	
	/**
	 * 
	 */
	@PostConstruct
	protected void init() {
		// bind temporal solver to the temporal network
		this.solver.setTemporalNetwork(this.tn);
	}
	
	/**
	 * 
	 * @return
	 */
	public long getOrigin() {
		return this.tn.getOrigin();
	}
	
	/**
	 * 
	 * @return
	 */
	public long getHorizon() {
		return this.tn.getHorizon();
	}
	
	/**
	 * Get the equivalent Minimal Network
	 * 
	 * @return
	 */
	public String getTemporalNetworkDescription() {
		return this.tn.toString();
	}
	
	/**
	 * Create a flexible time point
	 * 
	 * @return
	 * @throws TimePointCreationException
	 */
	public final TimePoint createTimePoint() 
			throws TimePointCreationException 
	{
		// time point to create
		TimePoint point = null;
		try {
			// create a time point
			point = this.tn.addTimePoint();
		} catch (InconsistentDistanceConstraintException ex) {
			throw new TimePointCreationException(ex.getMessage());
		}
		// get time point
		return point;
	}
	
	/**
	 * Create a scheduled time point
	 * 
	 * @param at
	 * @return
	 * @throws TimePointCreationException
	 */
	public final TimePoint createTimePoint(long at) 
			throws TimePointCreationException {
		// time point to create
		TimePoint point = null;
		try {
			// create a fixed time point
			point = this.tn.addTimePoint(at);
		} catch (InconsistentTpValueException | InconsistentDistanceConstraintException  ex) {
			throw new TimePointCreationException(ex.getMessage());
		}
		// get time point
		return point;
	}
	
	/**
	 * Create a flexible time point within the specified bound
	 * 
	 * @param bounds
	 * @return
	 * @throws TimePointCreationException
	 */
	public final TimePoint createTimePoint(long[] bounds) 
			throws TimePointCreationException {
		// time point to create
		TimePoint point = null;
		try {
			// create a time point within bound
			point = this.tn.addTimePoint(bounds[0], bounds[1]);
		} catch (InconsistentDistanceConstraintException | InconsistentTpValueException ex) {
			throw new TimePointCreationException(ex.getMessage());
		}
		// get created time point
		return point;
	}

	/**
	 * Create a controllable temporal interval
	 * 
	 * @return
	 * @throws TemporalIntervalCreationException
	 */
	public final TemporalInterval createTemporalInterval(boolean controllable) 
			throws TemporalIntervalCreationException {
		// create temporal interval
		return this.createTemporalInterval(new long[] {this.getOrigin(), this.getHorizon()}, 
				new long[] {this.getOrigin(), this.getHorizon()}, 
				new long[] {1, this.getHorizon()}, 
				controllable);		
	}
	
	/**
	 * Create a flexible interval with duration within the specified bound
	 * 
	 * @param duration
	 * @return
	 * @throws TemporalIntervalCreationException
	 */
	public final TemporalInterval createTemporalInterval(long[] duration, boolean controllable) 
			throws TemporalIntervalCreationException 
	{
		// create temporal interval
		return this.createTemporalInterval(new long[] {this.getOrigin(), this.getHorizon()}, 
				new long[] {this.getOrigin(), this.getHorizon()}, 
				duration, 
				controllable);
	}
	
	/**
	 * 
	 * @param end
	 * @param duration
	 * @param controllable
	 * @return
	 * @throws TemporalIntervalCreationException
	 */
	public final TemporalInterval createTemporalInterval(long[] end, long[] duration, boolean controllable) 
			throws TemporalIntervalCreationException 
	{
		// create temporal interval
		return this.createTemporalInterval(new long[] {this.getOrigin(), this.getHorizon()}, 
				end, 
				duration, 
				controllable);
	}
	
	/**
	 * 
	 * @param end
	 * @param duration
	 * @param controllable
	 * @return
	 * @throws TemporalIntervalCreationException
	 */
	public final TemporalInterval createTemporalInterval(long[] start, long[] end, long[] duration, boolean controllable) 
			throws TemporalIntervalCreationException 
	{
		// interval's start time
		TimePoint s = null;
		try {
			// create flexible start time
			s = this.tn.addTimePoint(start[0], start[1]);
		} catch (InconsistentDistanceConstraintException | InconsistentTpValueException ex) {
			throw new InconsistentIntervalStartTimeException(ex.getMessage());
		}
		
		// interval's end time
		TimePoint e = null;
		try 
		{
			// create flexible end time
			e = this.tn.addTimePoint(end[0], end[1]);
		} catch (InconsistentDistanceConstraintException | InconsistentTpValueException ex) {
			// delete start time
			this.tn.removeTimePoint(s);
			throw new InconsistentIntervaEndTimeException(ex.getMessage());
		
		}
		
		// interval's duration
		TimePointDistanceConstraint d = null;
		try 
		{
			// create distance constraint
			d = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
			d.setReference(s);
			d.setTarget(e);
			d.setDistanceLowerBound(duration[0]);
			d.setDistanceUpperBound(duration[1]);
			d.setControllable(controllable);
			
			// propagate distance constraint
			this.tn.addDistanceConstraint(d);
		} 
		catch (InconsistentDistanceConstraintException ex) 
		{
			// remove start and end time points
			this.tn.removeTimePoint(s);
			this.tn.removeTimePoint(e);
			throw new InconsistentIntervalDurationException(ex.getMessage());
		}
		
		// create temporal interval 
		TemporalInterval interval = new TemporalInterval(d);
		// record interval
		this.intervals.add(interval);
		// get created interval
		return interval;
	}
	
	/**
	 * 
	 * @param i
	 */
	public final void deleteTemporalInterval(TemporalInterval i) {
		// list of time points to remove
		List<TimePoint> list = new ArrayList<>();
		// get start time
		list.add(i.getStartTime());
		// get end time
		list.add(i.getEndTime());
		// the network will automatically remove all constraints concerning the two time points
		this.tn.removeTimePoints(list);
		// remove interval 
		this.intervals.remove(i);
	}
	
	/**
	 * 
	 * @throws ConsistencyCheckException
	 */
	public final void checkConsistency() 
			throws ConsistencyCheckException {
		// check temporal network consistency
		if (!this.solver.isConsistent()) {
			throw new TemporalConsistencyCheckException("The network is not temporally consistent");
		}
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final <T extends TemporalQuery> T createTemporalQuery(TemporalQueryType type) {
		// query instance
		T query = this.qf.create(type);
		// get created instance
		return query;
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	public final <T extends TemporalConstraint> T createTemporalConstraint(TemporalConstraintType type) {
		// create constraint
		T cons = this.cf.create(type);
		// get created constraint
		return cons;
	}
	
	/**
	 * 
	 */
	@Override
	public final void process(TemporalQuery query) 
	{
		// check query type
		switch (query.getType()) 
		{
			// compute the makespan of the temporal network
			case COMPUTE_MAKESPAN : 
			{
				// get query
				ComputeMakespanQuery mkQuery = (ComputeMakespanQuery) query;
				// get subset of intervals if any
				double mk = this.computeMakespan(mkQuery.getSubset());
				// set the value
				mkQuery.setMakespan(mk);
			}
			break;
		
			// check distance between intervals
			case INTERVAL_DISTANCE : 
			{
				// get query
				IntervalDistanceQuery dQuery = (IntervalDistanceQuery) query;
				// get intervals
				TemporalInterval a = dQuery.getSource();
				TemporalInterval b = dQuery.getTarget();
				// create time point query
				TimePointDistanceQuery tpQuery = this.qf.create(TemporalQueryType.TP_DISTANCE);
				// set source and target
				tpQuery.setSource(a.getEndTime());
				tpQuery.setTarget(b.getStartTime());
				// process query
				this.solver.process(tpQuery);
				// set bounds
				dQuery.setDistanceLowerBound(tpQuery.getDistanceLowerBound());
				dQuery.setDistanceUpperBOund(tpQuery.getDistanceUpperBound());
			}
			break;
			
			// check overlapping intervals
			case INTERVAL_OVERLAP :
			{
				// get query
				IntervalOverlapQuery overlap = (IntervalOverlapQuery) query;
				// get intervals
				TemporalInterval a = overlap.getReference();
				TemporalInterval b = overlap.getTarget();
				
				// check distance between the end of A and the start of B
				IntervalDistanceQuery eAsB = this.qf.
						create(TemporalQueryType.INTERVAL_DISTANCE);
				// set intervals
				eAsB.setSource(a);
				eAsB.setTarget(b);
				//process query
				this.process(eAsB);

				
				// check distance between the end of B and the start of A
				IntervalDistanceQuery eBsA = this.qf.
						create(TemporalQueryType.INTERVAL_DISTANCE);
				// set intervals
				eBsA.setSource(b);
				eBsA.setTarget(a);
				//process query
				this.process(eBsA);
				
				// set overlapping condition
				overlap.setOverlapping((eAsB.getDistanceLowerBound() < 0 && eAsB.getDistanceUpperBound() > 0) ||
						(eBsA.getDistanceLowerBound() < 0 && eBsA.getDistanceUpperBound() > 0) ||
						(eAsB.getDistanceLowerBound() < 0 && eAsB.getDistanceUpperBound() < 0 && eBsA.getDistanceLowerBound() < 0 && eBsA.getDistanceUpperBound() < 0) ||
						(eAsB.getDistanceLowerBound() > 0 && eAsB.getDistanceUpperBound() > 0 && eBsA.getDistanceLowerBound() > 0 && eBsA.getDistanceUpperBound() > 0));
			}
			break;
			
			// check interval schedule 
			case INTERVAL_SCHEDULE : 
			{
				// get query
				IntervalScheduleQuery scheduleQuery = (IntervalScheduleQuery) query;
				// get interval
				TemporalInterval i = scheduleQuery.getInterval();
				
				// create time point bound query
				TimePointScheduleQuery sQuery = this.qf.create(TemporalQueryType.TP_SCHEDULE);
				// set point 
				sQuery.setTimePoint(i.getStartTime());
				// check start schedule
				this.solver.process(sQuery);
				
				// process end time
				TimePointScheduleQuery eQuery = this.qf.create(TemporalQueryType.TP_SCHEDULE);
				// set point
				eQuery.setTimePoint(i.getEndTime());
				// check end schedule
				this.solver.process(eQuery);
				
				// check time point distance
				TimePointDistanceQuery dQuery= this.qf.create(TemporalQueryType.TP_DISTANCE);
				// set points
				dQuery.setSource(i.getStartTime());
				dQuery.setTarget(i.getEndTime());
				// process query
				this.solver.process(dQuery);
				
				// set interval duration
				i.setDurationLowerBound(dQuery.getDistanceLowerBound());
				i.setDurationUpperBound(dQuery.getDistanceUpperBound());
			}
			break;
			
			// check if squeezed interval
			case INTERVAL_PSEUDO_CONTROLLABILITY : 
			{
				// get query
				IntervalPseudoControllabilityQuery pseudoQuery = (IntervalPseudoControllabilityQuery) query;
				// get temporal interval
				TemporalInterval i = pseudoQuery.getInterval();
				// check the schedule
				IntervalScheduleQuery squery = this.qf.create(TemporalQueryType.INTERVAL_SCHEDULE);
				squery.setInterval(i);
				this.process(squery);
				// check if pseudo-controllability condition
				pseudoQuery.setPseudoControllable(i.getDurationLowerBound() == i.getNominalDurationLowerBound() && 
						i.getDurationUpperBound() == i.getNominalDurationUpperBound());
			}
			break;
			
			// time point queries
			case TP_SCHEDULE :
			case TP_DISTANCE : 
			case TP_DISTANCE_FROM_ORIGIN : 
			case TP_DISTANCE_TO_HORIZON : 
			{
				// propagate time point query to the reasoner
				this.solver.process((TimePointQuery) query);
			}
			break;
		}
	}
	
	/**
	 * This method propagates an interval constraint to the underlying temporal network.
	 * 
	 * When propagating constraints no contingent link can be affected or overwritten. Thus,
	 * the method throws an exception if a contingent link is affected by the propagated 
	 * constraint.
	 * 
	 * @param constraint
	 * @throws Exception
	 */
	public final void propagate(TemporalConstraint constraint) 
			throws TemporalConstraintPropagationException 
	{
		try 
		{
			// check temporal constraint type
			switch (constraint.getType()) 
			{
				// create BEFORE constraint into the temporal network
				case BEFORE : 
				{
					// get constraint
					BeforeIntervalConstraint before = (BeforeIntervalConstraint) constraint;
					// propagate constraint
					TimePointDistanceConstraint[] c = new TimePointDistanceConstraint[] { 
						this.doPropagateBeforeConstraint(before)
					};
					// set propagated constraint
					before.setPropagatedConstraints(c);
				}
				break;
				
				// create AFTER constraint into the temporal network
				case AFTER : 
				{
					AfterIntervalConstraint after = (AfterIntervalConstraint) constraint;
					// propagate constraint
					TimePointDistanceConstraint[] c = new TimePointDistanceConstraint[] {
						this.doPropagateAfterConstraint(after)
					};
					// set propagated constraint
					after.setPropagatedConstraints(c);
				}
				break;
				
				// create MEETS constraint into the temporal network
				case MEETS : 
				{
					// get constraint
					MeetsIntervalConstraint meets = (MeetsIntervalConstraint) constraint;
					// propagate constraint
					TimePointDistanceConstraint[] c = new TimePointDistanceConstraint[] { 
							this.doPropagateMeetsConstraint(meets)	
					};
					// set propagated constraints
					meets.setPropagatedConstraints(c);
				}
				break;
				
				// create MET-BY constraint into the temporal network
				case MET_BY : 
				{
					// get constraint
					MetByIntervalConstraint metby = (MetByIntervalConstraint) constraint;
					// propagate constraint
					TimePointDistanceConstraint[] c = new TimePointDistanceConstraint[] {
							this.doPropagateMetByConstraint(metby)
					};
					// set propagated constraints
					metby.setPropagatedConstraints(c);
				}
				break;
				
				// create CONTAINS constraint into the temporal network
				case CONTAINS : 
				{
					// get constraint
					ContainsIntervalConstraint contains = (ContainsIntervalConstraint) constraint;
					// propagate constraint
					TimePointDistanceConstraint[] c = this.doPropagateContainsConstraint(contains);
					// set propagated constraints
					contains.setPropagatedConstraints(c);
				}
				break;
				
				// create DURING constraint into the temporal network
				case DURING : 
				{
					// get constraint
					DuringIntervalConstraint during = (DuringIntervalConstraint) constraint;
					// propagate constraint
					TimePointDistanceConstraint[] c = this.doPropagateDuringConstraint(during);
					// set propagated constraints
					during.setPropagatedConstraints(c);
				}
				break;
				
				// create STARTS-DURING constraint into the network
				case STARTS_DURING : 
				{
					// get constraint
					StartsDuringIntervalConstraint sdc = (StartsDuringIntervalConstraint) constraint;
					// propagate constraint
					TimePointDistanceConstraint[] c = this.doPropagateStartsDuringConstraint(sdc);
					// set propagated constraints
					sdc.setPropagatedConstraints(c);
				}
				break;
				
				// create ENDS-DURING constraint into the network
				case ENDS_DURING : 
				{
					// get constraint
					EndsDuringIntervalConstraint edc = (EndsDuringIntervalConstraint) constraint;
					// propagate constraint
					TimePointDistanceConstraint[] c = this.doPropagateEndsDuringConstraint(edc);
					// set propagated constraints
					edc.setPropagatedConstraints(c);
				}
				break;
				
				// create EQUALS constraint into the temporal network
				case EQUALS : 
				{
					// get constraint
					EqualsIntervalConstraint equals = (EqualsIntervalConstraint) constraint;
					// propagate constraint
					TimePointDistanceConstraint[] c = this.doPropagateEqualsConstraint(equals);
					// set propagated constraint
					equals.setPropagatedConstraints(c);
				}
				break;
				
				// set the duration of a temporal interval
				case FIX_INTERVAL_DURATION : 
				{
					// get constraint
					FixIntervalDurationConstraint fix = (FixIntervalDurationConstraint) constraint;
					// propagate constraint
					TimePointDistanceConstraint c = this.doPropagateFixIntervalDurationConstraint(fix);
					// set propagate constraints
					fix.setPropagatedConstraints(new TimePointDistanceConstraint[] {c});
				}
				break;
				
				// schedule a time point at a given time
				case FIX_TIME_POINT :
				{
					// get constraint
					FixTimePointConstraint fix = (FixTimePointConstraint) constraint;
					// propagate constraint
					TimePointDistanceConstraint c = this.doPropagateFixTimePointConstraint(fix);
					// set propagated constraint
					fix.setPropagatedConstraints(new TimePointDistanceConstraint[] {c});
				}
				break;
				
				// set a distance constraint between two time points
				case TIME_POINT_DISTANCE :
				{
					// get constraint
					TimePointDistanceConstraint cons = (TimePointDistanceConstraint) constraint;
					// directly propagate distance constraint to the temporal network
					this.tn.addDistanceConstraint(cons);
				}
				break;
			}
		}
		catch (InconsistentDistanceConstraintException ex) {
			throw new TemporalConstraintPropagationException(ex.getMessage());
		}
	}

	/**
	 * 
	 * @param constraint
	 * @throws Exception
	 */
	public final void retract(TemporalConstraint constraint) {
		// retract propagated constraints
		TimePointDistanceConstraint[] toRetract = constraint.getPropagatedConstraints();
		this.tn.removeDistanceConstraint(Arrays.asList(toRetract));
		// clear data structure
		constraint.clear();
	}
	
	/**
	 * 
	 */
	@Override
	public String toString() {
		return this.tn.toString();
	}
	
	/**
	 * 
	 * @param equals
	 * @return
	 * @throws InconsistentDistanceConstraintException
	 */
	protected TimePointDistanceConstraint[] doPropagateEqualsConstraint(EqualsIntervalConstraint equals) 
			throws InconsistentDistanceConstraintException 
	{
		
		// create distance constraint 
		TimePointDistanceConstraint c1 = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		// set constraint data
		c1.setReference(equals.getReference().getStartTime());
		c1.setTarget(equals.getTarget().getStartTime());
		c1.setDistanceLowerBound(0);
		c1.setDistanceUpperBound(0);
		c1.setControllable(true);
		
		// create distance constraint
		TimePointDistanceConstraint c2 = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		// set constraint data
		c2.setReference(equals.getReference().getEndTime());
		c2.setTarget(equals.getTarget().getEndTime());
		c2.setDistanceLowerBound(0);
		c2.setDistanceUpperBound(0);
		c2.setControllable(true);
		
		// add constraints
		this.tn.addDistanceConstraint(new TimePointDistanceConstraint[] {c1, c2});
		
		// get added distance constraints
		return new TimePointDistanceConstraint[] {c1, c2};
	}

	/**
	 * 
	 * @param contains
	 * @return
	 * @throws InconsistentDistanceConstraintException
	 */
	protected TimePointDistanceConstraint[] doPropagateContainsConstraint(ContainsIntervalConstraint contains) 
			throws InconsistentDistanceConstraintException 
	{
		// create distance constraint
		TimePointDistanceConstraint c1 = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		// set data
		c1.setReference(contains.getReference().getStartTime());
		c1.setTarget(contains.getTarget().getStartTime());
		c1.setDistanceLowerBound(contains.getFirstBound()[0]);
		c1.setDistanceUpperBound(contains.getFirstBound()[1]);
		c1.setControllable(true);
		
		// create distance constraint
		TimePointDistanceConstraint c2 = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		// set data
		c2.setReference(contains.getReference().getEndTime());
		c2.setTarget(contains.getTarget().getEndTime());
		c2.setDistanceLowerBound(contains.getSecondBound()[0]);
		c2.setDistanceUpperBound(contains.getSecondBound()[1]);
		c2.setControllable(true);
		
		// add constraints
		this.tn.addDistanceConstraint(new TimePointDistanceConstraint[] {c1, c2});
		
		// get added constraints
		return new TimePointDistanceConstraint[] {c1, c2};
	}

	/**
	 * 
	 * @param during
	 * @return
	 * @throws InconsistentDistanceConstraintException
	 */
	protected TimePointDistanceConstraint[] doPropagateDuringConstraint(DuringIntervalConstraint during) 
			throws InconsistentDistanceConstraintException 
	{
		// create constraint
		TimePointDistanceConstraint c1 = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		c1.setReference(during.getTarget().getStartTime());
		c1.setTarget(during.getReference().getStartTime());
		c1.setDistanceLowerBound(during.getFirstBound()[0]);
		c1.setDistanceUpperBound(during.getFirstBound()[1]);
		c1.setControllable(true);
		
		// create constraint
		TimePointDistanceConstraint c2 = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		c2.setReference(during.getReference().getEndTime());
		c2.setTarget(during.getTarget().getEndTime());
		c2.setDistanceLowerBound(during.getSecondBound()[0]);
		c2.setDistanceUpperBound(during.getSecondBound()[1]);
		c2.setControllable(true);
				
		// add constraint
		this.tn.addDistanceConstraint(new TimePointDistanceConstraint[] {c1, c2});
		
		// get added constraints
		return new TimePointDistanceConstraint[] {c1, c2};
	}
	
	/**
	 * 
	 * @param edc
	 * @return
	 * @throws InconsistentDistanceConstraintException
	 */
	protected TimePointDistanceConstraint[] doPropagateEndsDuringConstraint(EndsDuringIntervalConstraint edc) 
			throws InconsistentDistanceConstraintException 
	{
		// create constraint
		TimePointDistanceConstraint c1 = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		c1.setReference(edc.getTarget().getStartTime());
		c1.setTarget(edc.getReference().getEndTime());
		c1.setDistanceLowerBound(edc.getFirstTimeBound()[0]);
		c1.setDistanceUpperBound(edc.getFirstTimeBound()[1]);
		c1.setControllable(true);
		
		// create constraint
		TimePointDistanceConstraint c2 = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		c2.setReference(edc.getReference().getEndTime());
		c2.setTarget(edc.getTarget().getEndTime());
		c2.setDistanceLowerBound(edc.getSecondTimeBound()[0]);
		c2.setDistanceUpperBound(edc.getSecondTimeBound()[1]);
		c2.setControllable(true);
		
		// add constraint 
		this.tn.addDistanceConstraint(new TimePointDistanceConstraint[] {c1, c2});
		
		// propagate constraints
		return new TimePointDistanceConstraint[] {c1, c2};
	}

	/**
	 * 
	 * @param sdc
	 * @return
	 * @throws InconsistentDistanceConstraintException
	 */
	protected TimePointDistanceConstraint[] doPropagateStartsDuringConstraint(StartsDuringIntervalConstraint sdc) 
			throws InconsistentDistanceConstraintException 
	{
		// create constraint
		TimePointDistanceConstraint c1 = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		c1.setReference(sdc.getTarget().getStartTime());
		c1.setTarget(sdc.getReference().getStartTime());
		c1.setDistanceLowerBound(sdc.getFirstTimeBound()[0]);
		c1.setDistanceUpperBound(sdc.getFirstTimeBound()[1]);
		c1.setControllable(true);
		
		// create constraint
		TimePointDistanceConstraint c2 = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE); 
		c2.setReference(sdc.getReference().getStartTime());
		c2.setTarget(sdc.getTarget().getEndTime());
		c2.setDistanceLowerBound(sdc.getSecondTimeBound()[0]);
		c2.setDistanceUpperBound(sdc.getSecondTimeBound()[1]);
		c2.setControllable(true);
		
		// add constraint
		this.tn.addDistanceConstraint(new TimePointDistanceConstraint[] {c1, c2});
		
		// propagate constraints
		return new TimePointDistanceConstraint[] {c1, c2};
	}

	/**
	 * 
	 * @param before
	 * @return
	 * @throws InconsistentDistanceConstraintException
	 */
	protected TimePointDistanceConstraint doPropagateBeforeConstraint(BeforeIntervalConstraint before)
			throws InconsistentDistanceConstraintException 
	{
		// create constraint
		TimePointDistanceConstraint c = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		c.setReference(before.getReference().getEndTime());
		c.setTarget(before.getTarget().getStartTime());
		c.setDistanceLowerBound(before.getLowerBound());
		c.setDistanceUpperBound(before.getUpperBound());
		c.setControllable(true);
		
		// add constraint
		this.tn.addDistanceConstraint(c);
		
		// get propagated constraint
		return c;
	}
	
	/**
	 * 
	 * @param meets
	 * @return
	 * @throws InconsistentDistanceConstraintException
	 */
	protected TimePointDistanceConstraint doPropagateMeetsConstraint(MeetsIntervalConstraint meets) 
			throws InconsistentDistanceConstraintException 
	{
		// create constraint
		TimePointDistanceConstraint c = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		c.setReference(meets.getReference().getEndTime());
		c.setTarget(meets.getTarget().getStartTime());
		c.setDistanceLowerBound(0);
		c.setDistanceUpperBound(0);
		c.setControllable(true);
		
		// add constraint
		this.tn.addDistanceConstraint(c);
		
		// get propagated constraint
		return c;
	}
	
	/**
	 * 
	 * @param after
	 * @return
	 * @throws InconsistentDistanceConstraintException
	 */
	protected TimePointDistanceConstraint doPropagateAfterConstraint(AfterIntervalConstraint after) 
			throws InconsistentDistanceConstraintException
	{
		// create constraint
		TimePointDistanceConstraint c = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		c.setReference(after.getTarget().getEndTime());
		c.setTarget(after.getReference().getStartTime());
		c.setDistanceLowerBound(after.getLowerBound());
		c.setDistanceUpperBound(after.getUpperBound());
		c.setControllable(true);
		
		// add constraint
		this.tn.addDistanceConstraint(c);
		
		// get propagated constraint
		return c;
	}
	
	/**
	 * 
	 * @param metby
	 * @return
	 * @throws InconsistentDistanceConstraintException
	 */
	protected TimePointDistanceConstraint doPropagateMetByConstraint(MetByIntervalConstraint metby) 
			throws InconsistentDistanceConstraintException 
	{
		// create constraint
		TimePointDistanceConstraint c = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		c.setReference(metby.getTarget().getEndTime());
		c.setTarget(metby.getReference().getStartTime());
		c.setDistanceLowerBound(metby.getLowerBound());
		c.setDistanceUpperBound(metby.getUpperBound());
		c.setControllable(true);
		
		// add constraint
		this.tn.addDistanceConstraint(c);
		
		// get propagated constraint
		return c;
	}
	
	/**
	 * 
	 * @param fix
	 * @return
	 * @throws InconsistentDistanceConstraintException
	 */
	protected TimePointDistanceConstraint doPropagateFixTimePointConstraint(FixTimePointConstraint fix) 
			throws InconsistentDistanceConstraintException
	{
		// create constraint
		TimePointDistanceConstraint c = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		c.setReference(this.tn.getOriginTimePoint());
		c.setTarget(fix.getReference());
		c.setDistanceLowerBound(fix.getTime());
		c.setDistanceUpperBound(fix.getTime());
		c.setControllable(true);
		
		// add constraint
		this.tn.addDistanceConstraint(c);
		
		// get propagated constraint
		return c;
	}

	/**
	 * 
	 * @param reference
	 * @param duration
	 * @return
	 * @throws InconsistentDistanceConstraintException
	 */
	protected TimePointDistanceConstraint doPropagateFixIntervalDurationConstraint(FixIntervalDurationConstraint fix) 
			throws InconsistentDistanceConstraintException 
	{
		// create constraint
		TimePointDistanceConstraint c = this.cf.create(TemporalConstraintType.TIME_POINT_DISTANCE);
		c.setReference(fix.getReference().getStartTime());
		c.setTarget(fix.getReference().getEndTime());
		c.setDistanceLowerBound(fix.getDuration());
		c.setDistanceUpperBound(fix.getDuration());
		c.setControllable(true);
		
		// add constraint
		this.tn.addDistanceConstraint(c);
		
		// get propagated constraint
		return c;
	}
	
	/**
	 * 
	 * @param subset
	 * @return
	 */
	private double computeMakespan(Set<TemporalInterval> subset)
	{
		// initialize the makespan
		double makespan = this.getOrigin();
		// get the list of intervals to take into account
		List<TemporalInterval> data = new ArrayList<>(this.intervals);
		if (!subset.isEmpty()) {
			// take into account only a subset of intervals
			data = new ArrayList<>(subset);
		}
		
		// compute the makespan
		for (TemporalInterval i : data) 
		{
			// check if controllable
			if (i.isControllable()) 
			{
				// check interval schedule
				IntervalScheduleQuery query = this.qf.create(TemporalQueryType.INTERVAL_SCHEDULE);
				query.setInterval(i);
				// process
				this.process(query);
				// update makespan
				makespan = Math.max(makespan, i.getEndTime().getLowerBound());
			}
		}
		
		// get the computed value
		return makespan;
	}
}