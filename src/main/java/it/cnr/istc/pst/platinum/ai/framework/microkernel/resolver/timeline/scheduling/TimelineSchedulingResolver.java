package it.cnr.istc.pst.platinum.ai.framework.microkernel.resolver.timeline.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.cnr.istc.pst.platinum.ai.framework.domain.component.Decision;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.ex.FlawSolutionApplicationException;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.ex.RelationPropagationException;
import it.cnr.istc.pst.platinum.ai.framework.domain.component.sv.StateVariable;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.ex.ConsistencyCheckException;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.flaw.Flaw;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.flaw.FlawSolution;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.relations.Relation;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.relations.RelationType;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.lang.relations.temporal.BeforeRelation;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.query.TemporalQueryType;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.resolver.Resolver;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.resolver.ResolverType;
import it.cnr.istc.pst.platinum.ai.framework.microkernel.resolver.ex.UnsolvableFlawException;
import it.cnr.istc.pst.platinum.ai.framework.time.lang.query.IntervalOverlapQuery;
import it.cnr.istc.pst.platinum.ai.framework.utils.properties.FilePropertyReader;

/**
 * 
 * @author anacleto
 *
 */
public final class TimelineSchedulingResolver extends Resolver<StateVariable>
{
	private boolean load;
	private double schedulingCost;
	
	/**
	 * 
	 */
	protected TimelineSchedulingResolver() {
		super(ResolverType.TIMELINE_SCHEDULING_RESOLVER.getLabel(), 
				ResolverType.TIMELINE_SCHEDULING_RESOLVER.getFlawTypes());
		// set load flag
		this.load = false;
	}
	
	/**
	 * 
	 */
	private void load() {
		// get deliberative property file
		FilePropertyReader properties = new FilePropertyReader(
				FRAMEWORK_HOME + FilePropertyReader.DEFAULT_DELIBERATIVE_PROPERTY);
		// get weight
		this.schedulingCost = Double.parseDouble(properties.getProperty("scheduling-cost"));
		// set flag
		this.load = true;
	}
	
	/**
	 * 
	 */
	@Override
	protected void doApply(FlawSolution solution) 
			throws FlawSolutionApplicationException 
	{
		// get the flaw solution to consider
		DecisionPrecedenceConstraint pc = (DecisionPrecedenceConstraint) solution;
		// get reference and target decisions
		Decision reference = pc.getReference();
		Decision target = pc.getTarget();
			
		// create relation
		BeforeRelation before = this.component.create(RelationType.BEFORE, reference, target);
		// set bounds
		before.setBound(new long[] {
				0, 
				this.component.getHorizon()});
		// add created relation to solution
		solution.addCreatedRelation(before);
		
		try
		{
			// propagate relations
			this.component.activate(before);
			// add activated relations to solution
			solution.addActivatedRelation(before);
			debug("Precedence constraint successfully created and activated:\n"
					+ "- temporal constraint: " + before + "\n");
			
			// check feasibility
			this.tdb.verify();
		}
		catch (RelationPropagationException | ConsistencyCheckException ex) 
		{
			// write error message
			error("Error while applying flaw solution:\n"
					+ "- solution: " + solution + "\n"
					+ "- unfeasible precedence constraint: " + before + "\n");

			// deactivate relation
			this.component.deactivate(before);
			// delete relation
			this.component.delete(before);
			// not feasible solution
			throw new FlawSolutionApplicationException(ex.getMessage());
		}
	}

	
	/**
	 * 
	 */
	@Override
	protected List<Flaw> doFindFlaws() 
	{
		// check flag
		if (!this.load) {
			this.load();
		}
		
		// list of critical sets
		List<Flaw> flaws = new ArrayList<>();
		// list of active decisions
		List<Decision> decisions = this.component.getActiveDecisions();
		// sort decisions
		Collections.sort(decisions);
		// look for peaks
		for (int index = 0; index < decisions.size() - 1; index++)
		{
			// get active decision  
			Decision reference = decisions.get(index);
			// find possibly overlapping decisions
			for (int jndex = index + 1; jndex < decisions.size(); jndex++)
			{
				// get another active decision
				Decision target = decisions.get(jndex);
				// check if intervals can overlap
				IntervalOverlapQuery query = this.tdb.createTemporalQuery(
						TemporalQueryType.INTERVAL_OVERLAP);
				
				// set time points
				query.setReference(reference.getToken().getInterval());
				query.setTarget(target.getToken().getInterval());
				// process query
				this.tdb.process(query);
				// check overlapping 
				if (query.canOverlap())
				{
					// conflict found
					BinaryDecisionConflict c = new BinaryDecisionConflict(
							FLAW_COUNTER.getAndIncrement(), 
							this.component);

					// set overlapping decisions
					c.setDecisions(new Decision[] {
							reference,
							target
					});
					
					// check if decisions overlaps
					debug("Overlapping tokens:\n"
							+ "- component: " + this.component + "\n"
							+ "- reference token: " + reference + "\n"
							+ "- target token: " + target + "\n");
					
					// add conflict
					flaws.add(c);
				}
			}
		}
		
//		if (!flaws.isEmpty()) {
//			
//			// randomly select a scheduling flaw from the component
//			List<Flaw> conflicts = new ArrayList<>();
//			Random rand = new Random(System.currentTimeMillis());
//			conflicts.add(flaws.get(rand.nextInt(flaws.size())));
//			// get conflicts
//			return conflicts;
//		}
//		else {
		
		// return the list of flaws
		return flaws;
		
		
//		}
	}
	

	
	/**
	 * 
	 */
	protected void doComputeFlawSolutions(Flaw flaw) 
		throws UnsolvableFlawException 
	{
		// get detected conflict
		BinaryDecisionConflict conflict = (BinaryDecisionConflict) flaw;

		// check possible precedence constraints
		Decision reference = conflict.getDecisions()[0];
		Decision target = conflict.getDecisions()[1];
		// create possible solutions
		DecisionPrecedenceConstraint pc1 = new DecisionPrecedenceConstraint(conflict, reference, target, this.schedulingCost);
		DecisionPrecedenceConstraint pc2 = new DecisionPrecedenceConstraint(conflict, target, reference, this.schedulingCost);
		
		try
		{
			// create relation reference -> target
			BeforeRelation before = this.component.create(RelationType.BEFORE, reference, target);
			// set bounds
			before.setBound(new long[] {
					0, 
					this.component.getHorizon()});
			
			// add create relation
			pc1.addCreatedRelation(before);
			
			// activate relation
			if (this.component.activate(before)) {
				// add activated relations
				pc1.addActivatedRelation(before);
			}
			
			// check consistency
			this.tdb.verify();
			// add solution and deactivate relation
			conflict.addSolution(pc1);
		}
		catch (RelationPropagationException | ConsistencyCheckException ex) {
			// discard relation
			debug("Unfeasible precedence constraint:\n"
					+ "\t- reference: " + reference + "\n"
					+ "\t- target: " + target + "\n");
		}
		finally {
			
			// deactivate relation
			for (Relation rel : pc1.getActivatedRelations()) {
				// deactivate relation
				this.component.deactivate(rel);
			}
			
			for (Relation rel : pc1.getCreatedRelations()) {
				// delete relation
				this.component.delete(rel);
			}
		}
		
		
		try
		{
			// create relation reference -> target
			BeforeRelation before = this.component.create(RelationType.BEFORE, target, reference);
			// set bounds
			before.setBound(new long[] {
					0, 
					this.component.getHorizon()});
			
			// add created relation
			pc2.addCreatedRelation(before);
			// check if relation is feasible
			if (this.component.activate(before)) {
				// add activated relation
				pc2.addActivatedRelation(before);
			}
			
			// check consistency
			this.tdb.verify();
			// add solution and deactivate relation
			conflict.addSolution(pc2);
		}
		catch (RelationPropagationException | ConsistencyCheckException ex) {
			// discard relation
			debug("Unfeasible precedence constraint:\n"
					+ "\t- reference: " + target + "\n"
					+ "\t- target: " + reference + "\n");
		}
		finally {
			
			// deactivate relation
			for (Relation rel : pc2.getActivatedRelations()) {
				// deactivate relation
				this.component.deactivate(rel);
			}
			
			for (Relation rel : pc2.getCreatedRelations()) {
				// delete relation
				this.component.delete(rel);
			}
		}
		
		
		// check if any solution has been found
		if (conflict.getSolutions().isEmpty()) {
			throw new UnsolvableFlawException("Unsolvable decision conflict on timeline:\n"
					+ "\t- component: " + this.component.getName() + "\n"
					+ "\t- decisions: " + conflict.getDecisions()[0] + ", " + conflict.getDecisions()[1] + "\n");
		}
	}
}

