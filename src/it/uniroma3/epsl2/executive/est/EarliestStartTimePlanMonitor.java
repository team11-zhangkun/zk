package it.uniroma3.epsl2.executive.est;

import it.uniroma3.epsl2.executive.PlanMonitor;
import it.uniroma3.epsl2.executive.pdb.ExecutionNode;
import it.uniroma3.epsl2.executive.pdb.ExecutionNodeStatus;

/**
 * 
 * @author anacleto
 *
 */
public class EarliestStartTimePlanMonitor implements PlanMonitor 
{
	private EarliestStartTimeExecutive executive;
	
	/**
	 * 
	 * @param exec
	 */
	protected EarliestStartTimePlanMonitor(EarliestStartTimeExecutive exec) {
		// set executive
		this.executive = exec;
	}

	/**
	 * 
	 */
	@Override
	public void handleTick(long tick) 
	{
		// simulate execution of nodes
		for (ExecutionNode node : this.executive.getNodes(ExecutionNodeStatus.IN_EXECUTION)) {
			// check node and end condition
			if (this.executive.canEnd(node)) {
				// simulate human task end
				this.doSimulateEndNodeExecution(tick, node);
			}
		}
	}
	
	/**
	 * 
	 * @param tick
	 * @param tau
	 * @param node
	 */
	private void doSimulateEndNodeExecution(long tick, ExecutionNode node) 
	{
		// get tau
		long tau = this.executive.convertTickToTau(tick);
		try 
		{
			// check schedule
			this.executive.checkSchedule(node);
			// check end window
			if (tau >= node.getEnd()[0] && tau <= node.getEnd()[1]) {
				// estimate duration
				long duration = tau - node.getStart()[0];
				// schedule node to estimated duration
				this.executive.scheduleDuration(node, duration);
				// update status
				this.executive.updateNode(node, ExecutionNodeStatus.EXECUTED);
				System.out.println("{tick = " + tick + "} {tau = " + tau + "} {PlanMonitor} -> End executing node with duration= " + node.getDuration()[1] + " - node " + node);
			}
			else if (tau > node.getEnd()[1]) {
				// schedule node to duration upper bound
				this.executive.scheduleDuration(node, node.getDuration()[1]);
				// update status
				this.executive.updateNode(node, ExecutionNodeStatus.EXECUTED);
				System.err.println("{tick = " + tick + "} {tau = " + tau + "} {PlanMonitor} -> End executing node with duration= " + node.getDuration()[1] + " - node " + node + " WARNING {\n- Execution delay tau = " + tau + " ewind= [" + node.getEnd()[0] + ", " + node.getEnd()[1] + "]\n}");
			}
		}
		catch (Exception ex) {
			throw new RuntimeException("{tick = " + tick + "} {tau = " + tau + "} {PlanMonitor} -> Error while scheduling duration for node " + node + " ERROR {\n- message= " + ex.getMessage() + "\n}"); 
		}
	}
	
}
