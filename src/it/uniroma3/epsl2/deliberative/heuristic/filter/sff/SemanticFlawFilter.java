package it.uniroma3.epsl2.deliberative.heuristic.filter.sff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.uniroma3.epsl2.deliberative.heuristic.filter.FlawFilter;
import it.uniroma3.epsl2.deliberative.heuristic.filter.FlawFilterType;
import it.uniroma3.epsl2.framework.domain.PlanDataBase;
import it.uniroma3.epsl2.framework.domain.PlanDataBaseObserver;
import it.uniroma3.epsl2.framework.lang.flaw.Flaw;
import it.uniroma3.epsl2.framework.lang.flaw.FlawSolution;
import it.uniroma3.epsl2.framework.lang.flaw.FlawType;
import it.uniroma3.epsl2.framework.lang.plan.Agenda;
import it.uniroma3.epsl2.framework.lang.plan.Decision;
import it.uniroma3.epsl2.framework.lang.plan.Plan;
import it.uniroma3.epsl2.framework.lang.plan.Relation;
import it.uniroma3.epsl2.framework.microkernel.annotation.framework.inject.PlanDataBaseReference;
import it.uniroma3.epsl2.framework.microkernel.annotation.framework.lifcycle.PostConstruct;
import it.uniroma3.epsl2.framework.microkernel.resolver.plan.Goal;

/**
 * 
 * @author anacleto
 *
 */
public class SemanticFlawFilter extends FlawFilter implements PlanDataBaseObserver
{
	@PlanDataBaseReference
	private PlanDataBase pdb;
	
	private JenaTemporalSemanticReasoner reasoner;			// knowledge reasoner
	
	/**
	 * 
	 */
	protected SemanticFlawFilter() {
		super(FlawFilterType.SFF);
	}
	
	/**
	 * 
	 */
	@PostConstruct
	protected void init() 
	{
		// subscribe to the plan data base
		this.pdb.subscribe(this);
		// setup knowledge reasoner
		this.reasoner = new JenaTemporalSemanticReasoner();
		// setup the knowledge on the initial plan and the initial agenda
		Plan plan = this.pdb.getPlan();
		for (Decision decision : plan.getDecisions()) {
			this.reasoner.add(decision);
		}
		for (Relation rel : plan.getRelations()) {
			this.reasoner.add(rel);
		}
		
		Agenda agenda = this.pdb.getAgenda();
		for (Decision goal : agenda.getGoals()) {
			this.reasoner.add(goal);
		}
		for (Relation rel : agenda.getRelations()) {
			this.reasoner.add(rel);
		}
	}
	
	/**
	 * 
	 */
	@Override
	public void propagated(FlawSolution solution) 
	{
		// check added decisions (either active or pending)
		List<Decision> decisions = solution.getCreatedDecisions();
		decisions.addAll(solution.getActivatedDecisisons());
		// add decisions to the knowledge
		for (Decision dec : decisions) {
			this.reasoner.add(dec);
		}
		
		// check added relations (either active or pending)
		List<Relation> relations = solution.getCreatedRelations();
		relations.addAll(solution.getActivatedRelations());			// this could be not necessary
		// add relations to the knowledge
		for (Relation rel : relations) {
			this.reasoner.add(rel);
		}
	}
	
	/**
	 * 
	 */
	@Override
	public void retracted(FlawSolution solution) 
	{
		// remove only created relations
		List<Relation> relations = solution.getCreatedRelations();
		// remove relations from the knowledge
		for (Relation rel : relations) {
			this.reasoner.delete(rel);
		}
		
		// remove only created decisions
		List<Decision> decisions = solution.getCreatedDecisions();
		// remove decisions from the knowledge
		for (Decision dec : decisions) {
			this.reasoner.delete(dec);
		}
	}
	
	/**
	 * 
	 */
	@Override
	public Set<Flaw> filter(Collection<Flaw> flaws) 
	{
		// check type of flaws
		Set<Goal> goals = new HashSet<Goal>();
		for (Flaw flaw : flaws) {
			if (flaw.getType().equals(FlawType.PLAN_REFINEMENT)) {
				// add to goal to flaws to be filtered
				goals.add((Goal) flaw);
			}
		}
		
		// next flaws to filter
		Set<Flaw> toFilter = new HashSet<>();
		// check goals to analyze
		if (goals.size() == 1) {
			// no filter is needed
			toFilter.addAll(goals);
			this.logger.debug("Only one planning goal found\n " + toFilter);
		}
		else if (goals.size() > 1) 
		{
			// list planning goals to analyze
			String str = "Planning goals to analyze:\n";
			for (Goal goal : goals) {
				str += "- " + goal + "\n";
			}
			this.logger.debug(str);
			
			// get the inferred "ordering" graph
			Map<Decision, List<Decision>> graph = this.reasoner.getOrderingGraph();
			// compute the hierarchy
			List<Decision>[] hierarchy = this.computeHierarchy(graph);
			// do filter flaws
			for (int hlevel = 0; hlevel < hierarchy.length && toFilter.isEmpty(); hlevel++)
			{
				// check flaws belonging to the current hierarchical level
				for (Goal goal : goals) {
					// check hierarchy of goal
					if (hierarchy[hlevel].contains(goal.getDecision())) {
						// add flaw to filtered
						toFilter.add(goal);
					}
				}
			}
			
			// print filtered flaws
			str = "Resulting goals after the analysis:\n";
			for (Flaw flaw : toFilter) {
				str += "- " + flaw;
			}
			this.logger.debug(str);
		}
		else {
			// no goals to analyze
			toFilter.addAll(flaws);
			this.logger.debug("No planning goals to analyze");
		}
		
		// get resulting sub-set of flaws
		return toFilter;
	}
	
	/**
	 * Given an acyclic ordering graph, the method builds the 
	 * hierarchy by means of a topological sort algorithm
	 * 
	 * @param graph
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Decision>[] computeHierarchy(Map<Decision, List<Decision>> graph) 
	{
		// invert the graph
		Map<Decision, Set<Decision>> inverted = new HashMap<>();
		for (Decision key : graph.keySet()) 
		{
			// add entry
			if (!inverted.containsKey(key)) {
				inverted.put(key, new HashSet<Decision>());
			}
			// check edges
			for (Decision target : graph.get(key)) 
			{
				// add entry to the inverted graph if needed
				if (!inverted.containsKey(target)) {
					inverted.put(target, new HashSet<Decision>());
				}
				
				// add edge to the inverted graph
				inverted.get(target).add(key);
			}
		}
		
		// compute hierarchy by means of topological sort algorithm on the inverted graph
		List<Decision> S = new ArrayList<>();	// root decisions
		for (Decision dec : inverted.keySet()) 
		{
			// check if root
			if (inverted.get(dec).isEmpty()) {
				S.add(dec);
			}
 		}

		// setup the hierarchy
		List<Decision>[] hierarchy = new ArrayList[inverted.keySet().size()];
		// initialize the hierarchy
		for (int index = 0; index < hierarchy.length; index++) {
			hierarchy[index] = new ArrayList<>();
		}
		
		// top hierarchy level
		int hlevel = 0;
		// start building hierarchy
		while (!S.isEmpty()) 
		{
			// get current root decision
			Decision root = S.remove(0);
			// add decision
			hierarchy[hlevel].add(root);
			// update hierarchy degree
			hlevel++;
			
			// update the graph and look for new roots
			inverted.remove(root);
			for (Decision dec : inverted.keySet()) 
			{
				// remove edge
				if (inverted.get(dec).contains(root)) {
					inverted.get(dec).remove(root);
				}
				
				// check if new root
				if (inverted.get(dec).isEmpty() && !S.contains(dec)) {
					// add root
					S.add(dec);
				}
			}
		}
		
		// get the hierarchy
		return hierarchy;
	}
}