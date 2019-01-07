package it.istc.pst.platinum.deliberative.solver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import it.istc.pst.platinum.framework.microkernel.lang.flaw.Flaw;
import it.istc.pst.platinum.framework.microkernel.lang.plan.Agenda;
import it.istc.pst.platinum.framework.microkernel.lang.plan.PartialPlan;

/**
 * 
 * @author anacleto
 *
 */
public class SearchSpaceNode implements Comparable<SearchSpaceNode>
{
	private static AtomicInteger ID_COUNTER = new AtomicInteger(0);
	private int id;
	private List<Operator> operators;	// node generation trace
	private Agenda agenda;				// plan agenda
	private PartialPlan pPlan;			// partial plan associated to the node
	private double cost; 				// partial plan cost
	
	/**
	 * Create a root node
	 * 
	 * @param id
	 */
	protected SearchSpaceNode() {
		// set node's id
		this.id = ID_COUNTER.getAndIncrement();
		// initialize operators
		this.operators = new ArrayList<>();
		// initialize the agenda
		this.agenda = new Agenda();
		// initialize the partial plan
		this.pPlan = new PartialPlan();
		// initialize partial plan cost
		this.cost = 0.0;
	}
	
	/**
	 * Create a child node generated by means of the specified operator
	 * 
	 * @param parent
	 * @param op
	 */
	protected SearchSpaceNode(SearchSpaceNode parent, Operator op) {
		this();
		// set operators
		this.operators = new ArrayList<>(parent.getOperators());
		// add generator
		this.operators.add(op);
		// setup node agenda
		this.agenda = op.getFlawSolution().getAgenda();
		// setup partial plan and cost
		this.pPlan = op.getFlawSolution().getPartialPlan();
		
	}
	
//	/**
//	 * 
//	 * @param parent
//	 * @param op
//	 */
//	private void setupNodeAgenda(SearchSpaceNode parent, Operator op) {
//		// set child node agenda taking into account the parent node agenda
//		Set<Decision> goals = new HashSet<>(parent.getAgenda().getGoals());
//		// remove solved goals from the agenda
//		for (Decision decision : op.getFlawSolution().getActivatedDecisisons()) {
//			goals.remove(decision.getValue());
//		}
//		
//		// add created pending decisions (subgoals)
//		for (Decision g : op.getFlawSolution().getCreatedDecisions()) {
//			goals.add(g);
//		}
//
//		// add resulting pending decisions to the agenda 
//		for (Decision g : goals) {
//			this.agenda.add(g);
//		}
//	}
	
//	/**
//	 * 
//	 */
//	private void setupPartialPlanAndCost()
//	{
//		// add partial plan decisions and relations
//		for (Operator o : this.operators) 
//		{
//			// add activated decisions
//			for (Decision dec : o.getFlawSolution().getActivatedDecisisons()) {
//				this.pPlan.addDecision(dec);
//			}
//			
//			// add activated relations
//			for (Relation rel : o.getFlawSolution().getActivatedRelations()) {
//				this.pPlan.addRelation(rel);
//			}
//			
//			// update partial plan cost
//			this.cost += o.getCost();
//		}
//	}

	
	/**
	 * 
	 * @return
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getDepth() {
		return this.operators.size();
	}
	
	/**
	 * 
	 * @return
	 */
	public Agenda getAgenda() {
		return this.agenda; 
	}
	
	/**
	 * 
	 * @return
	 */
	public PartialPlan getPartialPlan() {
		// initialize the partial plan 
		return this.pPlan;
	}
	
	/**
	 * 
	 * @return
	 */
	public double getCost() {
		// get the generation cost of the associated partial plan
		return this.cost;
	}
	
	/**
	 * Get the flaw that has been solved to generate the node 
	 * 
	 * @return
	 */
	public Flaw getGeneratingFlaw() {
		// verify whether the node is root
		return this.isRootNode() ? null : this.getGenerator().getFlaw();
	}
	
	/**
	 * Verify whether the node is root, i.e. not generator operator has been applied.
	 * 
	 * @return
	 */
	public boolean isRootNode() {
		// check if root node
		return this.getGenerator() == null;
	}
	
	/**
	 * The method returns the order list of operators that have been applied to generated the node. 
	 * 
	 * The last operator of the list is the node generator operator (i.e. the last applied operator).
	 * 
	 * @return
	 */
	public List<Operator> getOperators() {
		// get list of operators
		return new ArrayList<>(this.operators);
	}
	
	/**
	 * The method returns the node generator operator.
	 * 
	 * The method returns null for the root node of the search space
	 * 
	 * @return
	 */
	public Operator getGenerator() 
	{
		// get generator
		Operator operator = null;
		if (!this.operators.isEmpty()) {
			// get last applied operator
			operator = this.operators.get(this.operators.size() - 1);
		}
		
		// get generator operator
		return operator;
	}
	
	/**
	 * Get the list of applied operators from the more recent to the specified one (not included).
	 * 
	 * The method returns the list of operators that have been applied after the specified one starting with the more recent. The first
	 * element of the list is the node generator operator.
	 * 
	 * @param operator
	 * @return
	 */
	public List<Operator> getOperatorsUpTo(Operator operator) {
		// list of operators
		List<Operator> list = new ArrayList<>();
		if (operator == null) {
			// add all operators
			list.addAll(this.operators);
			// reverse order
			Collections.reverse(list);
		}
		else {
			// get index of the operator
			int index = this.operators.indexOf(operator);
			for (int i = this.operators.size() - 1; i > index; i--) {
				// add operator
				list.add(this.operators.get(i));
			}
		}
		// get list of operators
		return list;
	}
	
	/**
	 * Get the list of applied operators starting from the selected operator (not included).
	 * 
	 * The method returns the orderer list of operators that have been applied after the specified one. The 
	 * last operator of the list is the node generator operator.
	 * 
	 * 
	 * @param operator
	 * @return
	 */
	public List<Operator> getOperatorsFrom(Operator operator) {
		// list of operators
		List<Operator> list = new ArrayList<>();
		if (operator == null) {
			// add all operators
			list.addAll(this.operators);
		}
		else {
			// get index of the operator
			for (int index = this.operators.indexOf(operator) + 1; index < this.operators.size(); index++) {
				// add operator
				list.add(this.operators.get(index));
			}
		}
		// get list of operators
		return list;
	}
	
	/**
	 * 
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	/**
	 * 
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SearchSpaceNode other = (SearchSpaceNode) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	/**
	 * 
	 * @param o
	 * @return
	 */
	@Override
	public int compareTo(SearchSpaceNode o) {
		// compare nodes by their ID
		return this.id <= o.id ? -1 : 1;
	}
	
	/**
	 * 
	 */
	@Override
	public String toString() {
		return "[SearchSpaceNode id= " + this.id + ", depth= " + this.getDepth() + ", cost= " + this.getCost() + "]";
	}
}
