package it.cnr.istc.pst.platinum.ai.framework.microkernel.resolver.resource.reservoir;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.cnr.istc.pst.platinum.ai.framework.domain.component.Decision;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.ex.DecisionPropagationException;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.ex.FlawSolutionApplicationException;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.ex.RelationPropagationException;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.ex.ResourceProfileComputationException;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.resource.ResourceEvent;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.resource.reservoir.ConsumptionResourceEvent;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.resource.reservoir.ProductionResourceEvent;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.resource.reservoir.ReservoirResource;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.resource.reservoir.ReservoirResourceProfile;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.resource.reservoir.ResourceProductionValue;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.resource.reservoir.ResourceUsageProfileSample;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.ex.ConsistencyCheckException;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.flaw.Flaw;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.flaw.FlawSolution;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.relations.Relation;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.relations.RelationType;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.relations.parameter.BindParameterRelation;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.relations.temporal.BeforeRelation;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.resolver.Resolver;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.resolver.ResolverType;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.resolver.ex.UnsolvableFlawException;
import it.cnr.istc.pst.platinum.ai.framework.time.TemporalInterval;
import it.cnr.istc.pst.platinum.ai.framework.time.ex.TemporalConstraintPropagationException;
import it.cnr.istc.pst.platinum.ai.framework.time.ex.TemporalIntervalCreationException;
import it.cnr.istc.pst.platinum.ai.framework.time.lang.TemporalConstraintType;
import it.cnr.istc.pst.platinum.ai.framework.time.lang.allen.BeforeIntervalConstraint;
import it.cnr.istc.pst.platinum.ai.framework.time.tn.TimePoint;
import it.cnr.istc.pst.platinum.ai.framework.utils.properties.FilePropertyReader;

/**
 * 
 * @author alessandro
 *
 */
public class ReservoirResourceSchedulingResolver extends Resolver<ReservoirResource> 
{
	private double schedulingCost;
	
	
	/**
	 * 
	 */
	protected ReservoirResourceSchedulingResolver() {
		super(ResolverType.RESERVOIR_RESOURCE_SCHEDULING_RESOLVER.getLabel(),
				ResolverType.RESERVOIR_RESOURCE_SCHEDULING_RESOLVER.getFlawTypes());
		
		// get deliberative property file
		FilePropertyReader properties = new FilePropertyReader(
				FRAMEWORK_HOME + FilePropertyReader.DEFAULT_DELIBERATIVE_PROPERTY);
		
		this.schedulingCost = Double.parseDouble(properties.getProperty("scheduling-cost"));
	}
	
	/**
	 * 
	 */
	@Override
	protected List<Flaw> doFindFlaws() 
	{
		// list of flaws
		List<Flaw> flaws = new ArrayList<>();
		try
		{
			// check pessimistic resource profile
			ReservoirResourceProfile prp = this.component.
					computePessimisticResourceProfile();
			/*
			 * Analyze the pessimistic profile and find peaks if 
			 * any and generate production checkpoints
			 */
			flaws = this.doComputeProfilePeaks(prp);
		}
		catch (ResourceProfileComputationException ex) {
			// profile computation error
			throw new RuntimeException("Resource profile computation error:\n- " + ex.getMessage() + "\n");
		}
		
		// get list of flaws detected
		return flaws;
	}
	
	/**
	 * 
	 */
	@Override
	protected void doComputeFlawSolutions(Flaw flaw) 
			throws UnsolvableFlawException 
	{
		// check flaw type
		switch (flaw.getType()) 
		{
			// resource peak
			case RESERVOIR_OVERFLOW : 
			{
				// get peak
				ReservoirOverflow overflow = (ReservoirOverflow) flaw;
				// solvable condition
				boolean solvable = true;
				// check the size of the critical set
				if (overflow.getCriticalSet().size() > 1) 
				{
					// set hypotheses
					solvable = false;
					// first check solvable condition
					for (int i = 0; i < overflow.getCriticalSet().size() - 1 && !solvable; i++) 
					{
						// get two consecutive events
						ResourceEvent<?> e1 = overflow.getCriticalSet().get(i);
						ResourceEvent<?> e2 = overflow.getCriticalSet().get(i + 1);
						// check solvable condition
						solvable = e1.getAmount() < 0 && e2.getAmount() > 0 || 
								e1.getAmount() > 0 && e2.getAmount() < 0;
					}
				}
				else {
					// unexpected situation
					warning("Found a critical set with an event only:\n"
							+ "- overflow= " + overflow + "\n");
				}
				
				
				
				
				// sample the peak
				List<MinimalCriticalSet> MCSs = this.doSamplePeak(peak);
				// try to solve the peak through scheduling if possible
				if (!peak.getProductionCheckpoints().isEmpty()) 
				{
					// compute possible scheduling solutions
					for (MinimalCriticalSet mcs : MCSs) {
						// analyze possible schedules that solve the MCS
						this.doComputeSchedulingSolutions(mcs, peak.getProductionCheckpoints());
					}
					
					// heuristically sort the MCSs
					Collections.sort(MCSs, new Comparator<MinimalCriticalSet>() {
						
						/**
						 * 
						 */
						@Override
						public int compare(MinimalCriticalSet o1, MinimalCriticalSet o2) 
						{
							// compute average preserved space and make-span
							double preserved1 = 0;
							double makespan1 = 0;
							for (ConsumptionScheduling schedule : o1.getSchedulingSolutions()) {
								preserved1 += schedule.getPreserved();
//								makespan1 += schedule.getMakespan();
							}
							
							// set average values 
							preserved1 = preserved1 / o1.getSchedulingSolutions().size();
							makespan1 = makespan1 / o1.getSchedulingSolutions().size();
							
							// compute average preserved space and make-span
							double preserved2 = 0;
							double makespan2 = 0;
							for (ConsumptionScheduling schedule : o2.getSchedulingSolutions()) {
								preserved2 += schedule.getPreserved();
//								makespan2 += schedule.getMakespan();
							}
							
							// set average values
							preserved2 = preserved2 / o2.getSchedulingSolutions().size();
							makespan2 = makespan2 / o2.getSchedulingSolutions().size();
							// compare resulting values
							return makespan1 < makespan2 ? -1 : 
								makespan1 == makespan2 && preserved1 >= preserved2 ? -1 : 1;
						}
					});
					// keep the most promising MCS
					MinimalCriticalSet mcs = MCSs.get(0);
					// add MCS's solutions to the peak
					for (ConsumptionScheduling schedule : mcs.getSchedulingSolutions()) { 
						peak.addSolution(schedule);
					}
				}
				
				// compute possible planning solutions
				for (MinimalCriticalSet mcs : MCSs) {
					// when computing these solutions we assume that no previous productions are available to solve a peak
					this.doComputePlanningSolutions(mcs);
				}
				
				// heuristically sort the MCSs
				Collections.sort(MCSs);
//				, new Comparator<MinimalCriticalSet>() {
//					
//					/**
//					 * 
//					 */
//					public int compare(MinimalCriticalSet o1, MinimalCriticalSet o2) 
//					{
//						// compute average make-span
//						double makespan1 = 0;
//						for (ConsumptionScheduling schedule : o1.getSchedulingSolutions()) {
////							makespan1 += schedule.getMakespan();
//						}
//						
//						// set average value
//						makespan1 = makespan1 / o1.getSchedulingSolutions().size();
//						
//						// compute average make-span
//						double makespan2 = 0;
//						for (ConsumptionScheduling schedule : o2.getSchedulingSolutions()) {
////							makespan2 += schedule.getMakespan();
//						}
//						
//						// set average value
//						makespan2 = makespan2 / o2.getSchedulingSolutions().size();
//						// compare resulting make-span
//						return makespan1 <= makespan2 ? -1 : 1;
//					};
//				});
				
				// keep the most promising MCS
				MinimalCriticalSet mcs = MCSs.get(0);
				// add MCS's solutions to the peak
				for (ProductionPlanning prod : mcs.getPlanningSolutions()) {
					peak.addSolution(prod);
				}
				
			}
			break;
			
			default : {
				throw new RuntimeException("Resolver [" + this.getClass().getSimpleName() + "] cannot handle flaws of type: " + flaw.getType() + "\n");
			}
		}
		
		// check solutions found
		if (flaw.getSolutions().isEmpty()) {
			throw new UnsolvableFlawException("No feasible solutions found the following peak on reservoir resource \"" + this.component.getName() + "\":\n- flaw: " + flaw + "\n");
		}
	}
	
	/**
	 * 
	 * @param peak
	 * @return
	 */
	private List<MinimalCriticalSet> doSamplePeak(Peak peak) 
	{
		// get resource level before the peak
		double level = peak.getInitialLevel();
		// list of MCS
		List<MinimalCriticalSet> list = new ArrayList<>();
		// get critical set
		List<ConsumptionResourceEvent> CS = peak.getCriticalSet();
		// sample MCSs
		for (int i= 0; i < CS.size() - 1; i++)
		{
			// get reference
			ConsumptionResourceEvent reference = CS.get(i);
			// initialize MCS
			MinimalCriticalSet mcs = new MinimalCriticalSet(peak);
			mcs.addEvent(reference);
			// update level 
			level -= reference.getAmount();
			// check other activities of the critical set
			for (int j= i + 1; j < CS.size(); j++)
			{
				// get event 
				ConsumptionResourceEvent event = CS.get(j);
				mcs.addEvent(event);
				// update level
				level -= event.getAmount();
				// check the resulting level of the resource 
				if (level < this.component.getMinCapacity())
				{
					// add MCS to list
					list.add(mcs);
					// clear and back reference
					mcs = new MinimalCriticalSet(peak);
					mcs.addEvent(reference);
					// reset level
					level = peak.getInitialLevel();
					level -= reference.getAmount();
				}
			}
		}
		
		// get sampled MCSs
		return list;
	}
	
	/**
	 * Solve a resource over consumption by adding a production activity.
	 * This method tries to introduce a new activity into the plan in order to generate the 
	 * amount of resource needed to execute the set of activities generating the peak. 
	 * 
	 * @param mcs
	 */
	private void doComputePlanningSolutions(MinimalCriticalSet mcs)
	{
		// get activities composing the MCS
		List<ConsumptionResourceEvent> consumptions = mcs.getConsumptions();
		// compute a production solution for each possible partition of an MCS
		for (ConsumptionResourceEvent head : consumptions) 
		{
			// get the subset of consumptions excluding the "head"
			Set<ConsumptionResourceEvent> rest = new HashSet<>(consumptions);
			rest.remove(head);
			
			// prepare the set of decisions that go before production
			List<Decision> beforeProduction = new ArrayList<>();
			for (ConsumptionResourceEvent event : rest) {
				beforeProduction.add(event.getDecision());
			}
			
			// prepare the list of decisions that go after production
			List<Decision> afterProduction = new ArrayList<>();
			afterProduction.add(head.getDecision());
			
			// create temporal constraints and check feasibility
			List<BeforeIntervalConstraint> constraints = new ArrayList<>();
			// production interval
			TemporalInterval iProduction = null; 
			try
			{
				// create temporal interval
				iProduction = this.tdb.createTemporalInterval(true);
				// propagate "before" contains
				for (Decision dec : beforeProduction)
				{
					// propagate constraint "decision < production"
					BeforeIntervalConstraint before = this.tdb.createTemporalConstraint(TemporalConstraintType.BEFORE);
					// set reference and target intervals
					before.setReference(dec.getToken().getInterval());
					before.setTarget(iProduction);
					// set bounds
					before.setLowerBound(0);
					before.setUpperBound(this.tdb.getHorizon());
					
					// propagate constraint
					this.tdb.propagate(before);
					// add constraint to the list
					constraints.add(before);
				}
			
				// propagate "after" constraints
				for (Decision dec : afterProduction) 
				{
					// propagate constraint "production < head-decision"
					BeforeIntervalConstraint before = this.tdb.createTemporalConstraint(TemporalConstraintType.BEFORE);
					// set reference and target intervals
					before.setReference(iProduction);
					before.setTarget(dec.getToken().getInterval());
					// set bounds
					before.setLowerBound(0);
					before.setUpperBound(this.tdb.getHorizon());
					
					// propagate constraint
					this.tdb.propagate(before);
					// add constraint to the list
					constraints.add(before);
				}
				
				// check consistency
				this.tdb.verify();
				// compute the resulting make-span
//				ComputeMakespanQuery query = this.tdb.createTemporalQuery(TemporalQueryType.COMPUTE_MAKESPAN);
//				this.tdb.process(query);
//				// get resulting make-span
//				double makespan = query.getMakespan();
				
				// create resource planning solution
				ProductionPlanning pp = new ProductionPlanning(
						mcs.getPeak(), 
						head.getAmount(), 
						beforeProduction, 
						afterProduction, 
						this.planningCost);
				
				
				// set resulting make-span
//				pp.setMakespan(makespan);
				// add solution to the peak
				mcs.addPlanningSolution(pp);
			}
			catch (ConsistencyCheckException | TemporalConstraintPropagationException ex) {
				debug("It is not possible to schedule new production in order to solve resource over consumption:\n- before-production: " + beforeProduction + "\n- after-production: " + afterProduction + "\n");
			}
			catch (TemporalIntervalCreationException ex) {
				debug("Erorr while creating temporal interval for checking the temporal feasibility of production planning\n");
			}
			finally 
			{
				// retract all temporal constraints
				for (BeforeIntervalConstraint constraint : constraints) {
					this.tdb.retract(constraint);
				}
				
				// retract created production interval if necessary
				if (iProduction != null) {
					// delete temporal interval
					this.tdb.deleteTemporalInterval(iProduction);
				}
			}
		}
	}
	
	/**
	 * Solve a reservoir resource peak by scheduling before a previous production if any.
	 * This method tries to leverages past production activities in order to avoid new productions. The consumption events 
	 * that compose the peak are scheduled according to the available productions if the resulting plan is temporally consistent 
	 * 
	 * @param peak
	 */
	private void doComputeSchedulingSolutions(MinimalCriticalSet mcs, List<ProductionCheckpoint> points)
	{
		// try to solve an MCS by scheduling one of its consumption activities
		for (ConsumptionResourceEvent consumption : mcs.getConsumptions()) 
		{
			// check available checkpoints
			for (int index = 0; index < points.size(); index++)
			{
				// get current checkpoint
				ProductionCheckpoint checkpoint = points.get(index);
				// analyze "potential capacity" of the checkpoint
				if (checkpoint.getPotentialCapacity() >= consumption.getAmount())
				{
					// prepare and check the feasibility of the precedence constraints needed to solve the peak
					Map<Decision, Decision> precedences = new HashMap<>();
					// list of temporal constraints to test
					List<BeforeIntervalConstraint> constraints = new ArrayList<>();
					
					// create precedence constraint "checkpoint(i) < consumption(j)"
					BeforeIntervalConstraint before = this.tdb.createTemporalConstraint(TemporalConstraintType.BEFORE);
					before.setReference(checkpoint.getProduction().getDecision().getToken().getInterval());
					before.setTarget(consumption.getDecision().getToken().getInterval());
					before.setLowerBound(0);
					before.setUpperBound(this.tdb.getHorizon());
					// add constraint
					constraints.add(before);
					
					// add entry to precedence map
					precedences.put(checkpoint.getProduction().getDecision(), consumption.getDecision());
					
					// check previous productions
					if (index < points.size() - 1) 
					{
						// get previous checkpoint
						ProductionCheckpoint next = points.get(index + 1);
						// create precedence constraint "consumption(j) < checkpoint(i+1) "
						before = this.tdb.createTemporalConstraint(TemporalConstraintType.BEFORE);
						before.setReference(consumption.getDecision().getToken().getInterval());
						before.setTarget(next.getProduction().getDecision().getToken().getInterval());
						before.setLowerBound(0);
						before.setUpperBound(this.tdb.getHorizon());
						// add constraint
						constraints.add(before);
						
						// add entry to precedence map
						precedences.put(consumption.getDecision(), next.getProduction().getDecision());
					}
					
					try
					{
						// try to propagate computed constraints
						for (BeforeIntervalConstraint constraint : constraints) {
							// propagate constraint
							this.tdb.propagate(constraint);
						}
						
						// check the feasibility
						this.tdb.verify();
						
						// compute the preserved space of the involved time points
						double preserved = 0;
						for (BeforeIntervalConstraint c : constraints) {
							// compute preserved space heuristic value
							preserved += this.doComputePreservedSpaceHeuristicValue(c.getReference().getEndTime(), c.getTarget().getStartTime());
							
						}
						// get average
						preserved = preserved / constraints.size();
						
						// compute the resulting makespan of the temporal network
//						ComputeMakespanQuery query = this.tdb.createTemporalQuery(TemporalQueryType.COMPUTE_MAKESPAN);
//						// process query
//						this.tdb.process(query);
//						// get computed makespan
//						double makespan = query.getMakespan();
						
						// compute the amount of resource the production must generate to maintain the "potential capacity" of next production checkpoint (if any)
						double amount = checkpoint.getProduction().getAmount() + consumption.getAmount();
						
						// add consumption scheduling solution
						ConsumptionScheduling scheduling = new ConsumptionScheduling(
								mcs.getPeak(), 
								checkpoint.getProduction().getDecision(),
								checkpoint.getProduction().getAmount(),
								amount,
								precedences, 
								preserved,
								this.schedulingCost);
						
						// set resulting make-span
//						scheduling.setMakespan(makespan);
						// add scheduling solution
						mcs.addSchedulingSolution(scheduling);
					}
					catch (ConsistencyCheckException | TemporalConstraintPropagationException ex) {
						debug("Not valid schedule found to solve peak:\n- peak: " + mcs.getPeak() + "\n"
								+ "- schedule: " + checkpoint.getProduction()+ " < " + consumption  + "\n");
					}
					finally 
					{
						// retract constraints
						for (BeforeIntervalConstraint constraint : constraints) {
							this.tdb.retract(constraint);
						}
						// clear constraints
						constraints.clear();
						precedences.clear();
					}
				}
				
				// analyze "potential production" of the checkpoint
				if (checkpoint.getPotentialProduction() >= consumption.getAmount())
				{
					// prepare and check the feasibility of the precedence constraints needed to solve the peak
					Map<Decision, Decision> precedences = new HashMap<>();
					// list of temporal constraints to test
					List<BeforeIntervalConstraint> constraints = new ArrayList<>();
					
					// create precedence constraint "consumption(j) < checkpoint(i)"
					BeforeIntervalConstraint before = this.tdb.createTemporalConstraint(TemporalConstraintType.BEFORE);
					before.setReference(consumption.getDecision().getToken().getInterval());
					before.setTarget(checkpoint.getProduction().getDecision().getToken().getInterval());
					before.setLowerBound(0);
					before.setUpperBound(this.tdb.getHorizon());
					// add constraint
					constraints.add(before);
					
					// add entry to precedence map
					precedences.put(consumption.getDecision(), checkpoint.getProduction().getDecision());
					
					// check previous productions
					if (index > 0) 
					{
						// get previous checkpoint
						ProductionCheckpoint previous = points.get(index - 1);
						// create precedence constraint "checkpoint(i-1) < consumption(j)"
						before = this.tdb.createTemporalConstraint(TemporalConstraintType.BEFORE);
						before.setReference(previous.getProduction().getDecision().getToken().getInterval());
						before.setTarget(consumption.getDecision().getToken().getInterval());
						before.setLowerBound(0);
						before.setUpperBound(this.tdb.getHorizon());
						// add constraint
						constraints.add(before);
						
						// add entry to precedence map
						precedences.put(previous.getProduction().getDecision(), consumption.getDecision());
					}
					
					try
					{
						// try to propagate computed constraints
						for (BeforeIntervalConstraint constraint : constraints) {
							// propagate constraint
							this.tdb.propagate(constraint);
						}
						
						// check the feasibility
						this.tdb.verify();
						
						// compute the preserved space of the involved time points
						double preserved = 0;
						for (BeforeIntervalConstraint c : constraints) {
							// compute preserved space heuristic value
							preserved += this.doComputePreservedSpaceHeuristicValue(c.getReference().getEndTime(), c.getTarget().getStartTime());
							
						}
						// get average
						preserved = preserved / constraints.size();
						
						// compute the resulting make-span of the temporal network
//						ComputeMakespanQuery query = this.tdb.createTemporalQuery(TemporalQueryType.COMPUTE_MAKESPAN);
//						// process query
//						this.tdb.process(query);
//						// get computed makespan
//						double makespan = query.getMakespan();
						
						// compute the amount of resource the production must generate to maintain the "expected" level of resource
						double amount = checkpoint.getProduction().getAmount() + consumption.getAmount();
						
						// add consumption scheduling solution
						ConsumptionScheduling scheduling = new ConsumptionScheduling(
								mcs.getPeak(), 
								checkpoint.getProduction().getDecision(),
								checkpoint.getProduction().getAmount(),
								amount,
								precedences, 
								preserved,
								this.schedulingCost);
						
						// set resulting make-span
//						scheduling.setMakespan(makespan);
						// add scheduling solution
						mcs.addSchedulingSolution(scheduling);
					}
					catch (ConsistencyCheckException | TemporalConstraintPropagationException ex) {
						debug("Not valid schedule found to solve peak:\n- peak: " + mcs.getPeak() + "\n"
								+ "- schedule: " + consumption + " < " + checkpoint.getProduction() + "\n");
					}
					finally 
					{
						// retract constraints
						for (BeforeIntervalConstraint constraint : constraints) {
							this.tdb.retract(constraint);
						}
						// clear constraints
						constraints.clear();
						precedences.clear();
					}
				}
			}
		}
	}
	
	/**
	 * 
	 */
	@Override
	protected void doRestore(FlawSolution flawSolution) 
			throws RelationPropagationException, DecisionPropagationException
	{
		// perform "default" operations
		super.doRestore(flawSolution);
		// check flaw type
		switch (flawSolution.getFlaw().getType())
		{
			case RESOURCE_PRODUCTION_UPDATE : 
			{
				// get flaw solution
				ProductionUpdate update = (ProductionUpdate) flawSolution;
				// get production decision
				Decision production = update.getProduction();
				// get parameter relation
				for (Relation rel : this.component.getRelations(production)) 
				{
					// check parameter relation
					if (rel.getType().equals(RelationType.BIND_PARAMETER)) 
					{
						try
						{
							// bind parameter relation
							BindParameterRelation bind = (BindParameterRelation) rel;
							// retract binding constraint
							this.component.deactivate(bind);
							// update binding constraint
							bind.setValue(Integer.toString((int) update.getAmount()));
							// propagate bind constraint
							this.component.activate(bind);
						}
						catch (RelationPropagationException ex) {
							throw new RuntimeException("Error while retracting consumption scheduling solution\n:- mesasge= " + ex.getMessage());
						}
					}
				}
			}
			break;
		
			case RESOURCE_PRODUCTION_PLANNING : 
			{
				// get general solution
				ResourceOverConsumptionSolution overconsumption = (ResourceOverConsumptionSolution) flawSolution;
				// check type
				if (overconsumption.getType().equals(ResourceOverConsumptionSolutionType.CONSUMPTION_SCHEDULING)) 
				{
					// restore the production binding constraint
					ConsumptionScheduling solution = (ConsumptionScheduling) overconsumption;
					// get production activity
					Decision production = solution.getProduction();
					// get parameter relation
					for (Relation rel : this.component.getRelations(production)) 
					{
						// check parameter relation
						if (rel.getType().equals(RelationType.BIND_PARAMETER)) 
						{
							try
							{
								// bind parameter relation
								BindParameterRelation bind = (BindParameterRelation) rel;
								// retract binding constraint
								this.component.deactivate(bind);
								// update binding constraint
								bind.setValue(Integer.toString((int) solution.getProductionAmount()));
								// propagate bind constraint
								this.component.activate(bind);
							}
							catch (RelationPropagationException ex) {
								throw new RelationPropagationException("Error while retracting consumption scheduling solution\n:- mesasge= " + ex.getMessage());
							}
						}
					}
				}
			}
			break;
			
			default : {
				throw new RuntimeException("Reservoir resource resolver cannot handle flaws of type: " + flawSolution.getFlaw().getType());
			}
		}
	}
	
	/**
	 * 
	 */
	@Override
	protected void doRetract(FlawSolution flawSolution) 
	{
		// perform "default" operations
		super.doRetract(flawSolution);
		// check flaw type
		switch (flawSolution.getFlaw().getType())
		{
			case RESOURCE_PRODUCTION_UPDATE : 
			{
				// get flaw solution
				ProductionUpdate update = (ProductionUpdate) flawSolution;
				// get production decision
				Decision production = update.getProduction();
				// get parameter relation
				for (Relation rel : this.component.getRelations(production)) 
				{
					// check parameter relation
					if (rel.getType().equals(RelationType.BIND_PARAMETER)) 
					{
						try
						{
							// bind parameter relation
							BindParameterRelation bind = (BindParameterRelation) rel;
							// retract binding constraint
							this.component.deactivate(bind);
							// update binding constraint
							bind.setValue(Integer.toString((int) update.getPreviousAmount()));
							// propagate bind constraint
							this.component.activate(bind);
						}
						catch (RelationPropagationException ex) {
							throw new RuntimeException("Error while retracting consumption scheduling solution\n:- mesasge= " + ex.getMessage());
						}
					}
				}
			}
			break;
		
			case RESOURCE_PRODUCTION_PLANNING : 
			{
				// get general solution
				ResourceOverConsumptionSolution overconsumption = (ResourceOverConsumptionSolution) flawSolution;
				// check type
				if (overconsumption.getType().equals(ResourceOverConsumptionSolutionType.CONSUMPTION_SCHEDULING)) 
				{
					// restore the production binding constraint
					ConsumptionScheduling solution = (ConsumptionScheduling) overconsumption;
					// get production activity
					Decision production = solution.getProduction();
					// get parameter relation
					for (Relation rel : this.component.getRelations(production)) 
					{
						// check parameter relation
						if (rel.getType().equals(RelationType.BIND_PARAMETER)) 
						{
							try
							{
								// bind parameter relation
								BindParameterRelation bind = (BindParameterRelation) rel;
								// retract binding constraint
								this.component.deactivate(bind);
								// update binding constraint
								bind.setValue(Integer.toString((int) solution.getOldAmount()));
								// propagate bind constraint
								this.component.activate(bind);
							}
							catch (RelationPropagationException ex) {
								throw new RuntimeException("Error while retracting consumption scheduling solution\n:- mesasge= " + ex.getMessage());
							}
						}
					}
				}
			}
			break;
			
			default : {
				throw new RuntimeException("Reservoir resource resolver cannot handle flaws of type: " + flawSolution.getFlaw().getType());
			}
		}
	}
	
	/**
	 * 
	 * @param solution
	 * @throws FlawSolutionApplicationException
	 */
	private void doApplyResourceSchedulingSolution(ConsumptionScheduling solution) 
			throws FlawSolutionApplicationException 
	{
		// get the set of constraint to propagate
		Map<Decision, Decision> constraints = solution.getPrecedenceConstraints();
		try
		{
			// create and activate precedence constraints
			for (Decision reference : constraints.keySet())
			{
				// get target
				Decision target = constraints.get(reference);
				// create constraint
				BeforeRelation before = this.component.create(RelationType.BEFORE, reference, target);
				// set bounds
				before.setBound(new long[] {
						0, this.tdb.getHorizon()
				});
				
				// set relation as created
				solution.addCreatedRelation(before);
				// add relation to component
				this.component.activate(before);
				// set relation as activated
				solution.addActivatedRelation(before);
			}
			
			// get production activity
			Decision production = solution.getProduction();
			// get parameter relation
			for (Relation rel : this.component.getRelations(production)) 
			{
				// check parameter relation
				if (rel.getType().equals(RelationType.BIND_PARAMETER)) 
				{
					// bind parameter relation
					BindParameterRelation bind = (BindParameterRelation) rel;
					// deactivate binding constraint
					this.component.deactivate(bind);
					// update binding constraint
					bind.setValue(Integer.toString((int) solution.getProductionAmount()));
					// propagate bind constraint
					this.component.activate(bind);
				}
			}
		}
		catch (RelationPropagationException ex) {
			// retract activated relations
			for (Relation relation : solution.getActivatedRelations()) {
				this.component.deactivate(relation);
			}
			// retract created relations
			for (Relation relation : solution.getCreatedRelations()) {
				this.component.delete(relation);
			}
			
			// throw exception
			throw new FlawSolutionApplicationException("Error while applying solution to resource peak:\n- solution: " + solution + "\n- message: " + ex.getMessage() + "\n");
		}
	}
	
	/**
	 * 
	 * @param solution
	 * @throws FlawSolutionApplicationException
	 */
	private void doApplyResourcePlanningSolution(ProductionPlanning solution) 
			throws FlawSolutionApplicationException 
	{
		// get production value
		ResourceProductionValue value = this.component.getProductionValue();
		// create production decision (it represents a planning goal) 
		Decision goal = this.component.create(value, new String[] {
			"?amount"
		});
		
		// add created decision to flaw solution
		solution.addCreatedDecision(goal);
		// set as mandatory expansion goal
		goal.setMandatoryExpansion();
		
		// add parameter (pending) relation to bind the production parameter
		BindParameterRelation bind = this.component.create(RelationType.BIND_PARAMETER, goal, goal);
		// set the desired amount of resource to produce
		bind.setValue(Long.toString((long) solution.getAmount()));
		bind.setReferenceParameterLabel("?amount");
		// add created relations
		solution.addCreatedRelation(bind);
		
		// create temporal (pending) relations
		for (Decision dec : solution.getDecisionsBeforeProduction()) {
			// create precedence relation
			BeforeRelation rel = this.component.create(RelationType.BEFORE, dec, goal);
			// set relation bounds
			rel.setBound(new long[] {
					0,
					this.tdb.getHorizon()
			});
			// add created relation to flaw solution
			solution.addCreatedRelation(rel);
		}
		
		// create (pending) relations
		for (Decision dec : solution.getDecisionsAfterProduction()) {
			// create precedence relation
			BeforeRelation rel = this.component.create(RelationType.BEFORE, goal, dec);
			// set relation bounds
			rel.setBound(new long[] {
					0, 
					this.tdb.getHorizon()
			});
			// add created relation to flaw solution
			solution.addCreatedRelation(rel);
		}
	}
	
	/**
	 * 
	 */
	@Override
	protected void doApply(FlawSolution solution) 
			throws FlawSolutionApplicationException 
	{
		// check flaw type
		switch (solution.getFlaw().getType())
		{
			case RESOURCE_PRODUCTION_UPDATE : 
			{
				// get production update solution 
				ProductionUpdate update = (ProductionUpdate) solution;
				// get production decision
				Decision production = update.getProduction();
				try
				{
					// check relations
					for (Relation rel : this.component.getRelations(production)) 
					{
						// check relation type 
						if (rel.getType().equals(RelationType.BIND_PARAMETER)) 
						{
							// bind parameter relation
							BindParameterRelation bind = (BindParameterRelation) rel;
							// deactivate binding constraint
							this.component.deactivate(bind);
							// update binding constraint
							bind.setValue(Integer.toString((int) update.getAmount()));
							// propagate bind constraint
							this.component.activate(bind);
						}
					}
				}
				catch (RelationPropagationException ex) {
					throw new FlawSolutionApplicationException(ex.getMessage());
				}
			}
			break;
		
			case RESOURCE_PRODUCTION_PLANNING : 
			{
				// get general solution
				ResourceOverConsumptionSolution overconsumption = (ResourceOverConsumptionSolution) solution;
				// check over consumption type
				switch (overconsumption.getType())
				{
					// scheduling solution
					case CONSUMPTION_SCHEDULING : {
						// get scheduling solution
						ConsumptionScheduling scheduling = (ConsumptionScheduling) overconsumption;
						this.doApplyResourceSchedulingSolution(scheduling);
					}
					break;
					
					// planning solution
					case PRODUCTION_PLANNING : {
						// get planning solution
						ProductionPlanning planning = (ProductionPlanning) overconsumption;
						this.doApplyResourcePlanningSolution(planning);
					}
					break;
					
					default : {
						throw new RuntimeException("Unknownw reservoir resource peak solution type: " + overconsumption.getType() + "\n");
					}
				}
			}
			break;
			
			default : {
				throw new RuntimeException("Reservoir resource resolver cannot handle flaws of type: " + solution.getFlaw().getType());
			}
		}
	}
	
	/**
	 * Analyze the profile of a reservoir resource in order to find peaks and compute production checkpoints
	 * 
	 * @param profile
	 * @return
	 */
	private List<Flaw> doComputeProfilePeaks(ReservoirResourceProfile profile)
	{
		// list of flaws found
		List<Flaw> flaws = new ArrayList<>();
		// get profile samples
		List<ResourceUsageProfileSample> samples = profile.getSamples();
		// long start peak level
		long startPeakLevel = 0;
		// reset the current level of resource
		long currentLevel = this.component.getInitialLevel();
		// set of consumptions that may generate a peak
		List<ResourceEvent<?>> criticalSet = new ArrayList<>();
		// peak mode flag
		boolean peakMode = false;
		// analyze the resource profile until a peak is found
		for (int index = 0; index < samples.size() && flaws.isEmpty(); index++)
		{
			// current sample
			ResourceUsageProfileSample sample = samples.get(index);
			// get resource event
			ResourceEvent<?> event = sample.getEvent();
			
			// check peak mode
			if (!peakMode)
			{
				
				// update the start peak level
				startPeakLevel = currentLevel;
				
				// update the current level of the resource
				currentLevel += event.getAmount();					// positive amount in case of production, negative in case of consumption
				// check resource peak condition
				peakMode = currentLevel < this.component.getMinCapacity() || currentLevel > this.component.getMaxCapacity();
				
				// check if a peak is starting 
				if (peakMode) {
					// first event of the peak
					criticalSet.add(event);
				}
			}
			else		// peak mode  
			{
				// add the current event to the critical set
				criticalSet.add(event);
				
				// get current level of the resource
				currentLevel += event.getAmount();				// positive amount in case of production, negative in case of consumption
				// check peak condition
				peakMode = currentLevel < this.component.getMinCapacity() || currentLevel > this.component.getMaxCapacity();
				
				
				// check if exit from peak condition
				if (!peakMode) 
				{
					// create reservoir overflow flaw
					ReservoirOverflow overflow = new ReservoirOverflow(
							FLAW_COUNTER.getAndIncrement(), 
							this.component, 
							criticalSet, 
							startPeakLevel);
					
					// add flaw and stop searching 
					flaws.add(overflow);
				}
			}
		}
		
		
		// check if a peak must be closed - "final peak"
		if (peakMode && flaws.isEmpty())	 
		{
			// create reservoir overflow flaw
			ReservoirOverflow overflow = new ReservoirOverflow(
					FLAW_COUNTER.getAndIncrement(), 
					this.component, 
					criticalSet, 
					startPeakLevel);
			
			// add flaw
			flaws.add(overflow);
		}
		
		// get found peaks - only one element expected
		return flaws;
	}


	/**
	 * Estimate the preserved values of time point domains after propagation of a precedence constraint "tp1 < tp2".
	 * 
	 * The method assumes that the precedence constraint is feasible and that the bounds of the time points have been updated 
	 * according to precedence constraint (i.e. the underlying temporal must encapsulate additional information coming from 
	 * the temporal constraint) 
	 * 
	 * @param tp1
	 * @param tp2
	 * @return
	 */
	private double doComputePreservedSpaceHeuristicValue(TimePoint tp1, TimePoint tp2)
	{
		// initialize value
		double preserved = 0;
		// compute parameters
		double A = (tp2.getUpperBound() - tp2.getLowerBound() + 1) * (tp1.getUpperBound() - tp1.getLowerBound() + 1);
		double B = (tp2.getUpperBound() - tp1.getLowerBound() + 1) * (tp2.getUpperBound() - tp1.getLowerBound() + 2);
		double Cmin = Math.max(0, (tp2.getLowerBound() - tp1.getLowerBound()) * (tp2.getLowerBound() - tp1.getLowerBound() + 1));
		double Cmax = Math.max(0, (tp2.getUpperBound() - tp1.getUpperBound() * (tp2.getUpperBound() - tp1.getUpperBound() + 1)));

		// compute preserved space value
		preserved = (B - Cmin - Cmax) / (2 * A);
		
		// get computed heuristic value
		return preserved;
	}
}

/**
 * 
 * @author anacleto
 *
 */
class MinimalCriticalSet implements Comparable<MinimalCriticalSet>
{
	private Peak peak;
	private List<ConsumptionResourceEvent> consumptions;
	private List<ConsumptionScheduling> schedulingSolutions;
	private List<ProductionPlanning> planningSolutions;
	
	/**
	 * 
	 * @param peak
	 */
	protected MinimalCriticalSet(Peak peak) {
		this.peak = peak;
		this.consumptions = new ArrayList<>();
		this.schedulingSolutions = new ArrayList<>();
		this.planningSolutions = new ArrayList<>();
	}
	
	/**
	 * 
	 * @return
	 */
	public List<ConsumptionScheduling> getSchedulingSolutions() {
		return schedulingSolutions;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<ProductionPlanning> getPlanningSolutions() {
		return planningSolutions;
	}
	
	/**
	 * 
	 * @param sol
	 */
	public void addSchedulingSolution(ConsumptionScheduling sol) {
		this.schedulingSolutions.add(sol);
	}
	
	/**
	 * 
	 * @param sol
	 */
	public void addPlanningSolution(ProductionPlanning sol) {
		this.planningSolutions.add(sol);
	}
	
	/**
	 * 
	 * @return
	 */
	public Peak getPeak() {
		return peak;
	}
	
	/**
	 * 
	 * @param event
	 */
	public void addEvent(ConsumptionResourceEvent event) {
		this.consumptions.add(event);
	}
	
	public List<ConsumptionResourceEvent> getConsumptions() {
		return new ArrayList<>(consumptions);
	}
	
	/**
	 * 
	 * @return
	 */
	public double getResourceConsumption() {
		double total = 0;
		for (ConsumptionResourceEvent event : consumptions) {
			total += event.getAmount();
		}
		return total;
	}
	
	/**
	 * 
	 */
	@Override
	public int compareTo(MinimalCriticalSet o) {
		return this.consumptions.size() > o.consumptions.size() ? -1 : this.consumptions.size() < o.consumptions.size() ? 1 : 0;
	}

}



