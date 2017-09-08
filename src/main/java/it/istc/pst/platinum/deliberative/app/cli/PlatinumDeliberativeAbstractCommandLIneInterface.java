package it.istc.pst.platinum.deliberative.app.cli;

import it.istc.pst.platinum.deliberative.app.Planner;
import it.istc.pst.platinum.deliberative.app.PlannerBuilder;
import it.istc.pst.platinum.deliberative.app.cli.ex.DeliberativeCommandLineInterfaceInitializationException;
import it.istc.pst.platinum.framework.microkernel.lang.ex.NoSolutionFoundException;
import it.istc.pst.platinum.framework.microkernel.lang.ex.ProblemInitializationException;
import it.istc.pst.platinum.framework.microkernel.lang.ex.SynchronizationCycleException;
import it.istc.pst.platinum.framework.microkernel.lang.plan.SolutionPlan;
import it.istc.pst.platinum.protocol.lang.PlanProtocolDescriptor;
import it.istc.pst.platinum.protocol.lang.ProtocolLanguageFactory;
import it.istc.pst.platinum.protocol.lang.TokenProtocolDescriptor;
import it.istc.pst.platinum.protocol.query.ProtocolQueryFactory;


/**
 * 
 * @author alessandroumbrico
 *
 */
public abstract class PlatinumDeliberativeAbstractCommandLIneInterface 
{
	protected ProtocolLanguageFactory langFactory;
	protected ProtocolQueryFactory queryFactory;
	protected Planner planner;
	protected SolutionPlan currentSolution;
	
	/**
	 * 
	 * @param horizon
	 */
	protected PlatinumDeliberativeAbstractCommandLIneInterface(long horizon) {
		this.langFactory = ProtocolLanguageFactory.getSingletonInstance(horizon);
		this.queryFactory = ProtocolQueryFactory.getSingletonInstance();
		this.currentSolution = null;
	}
	
	/**
	 * 
	 * @param ddl
	 * @param pdl
	 * @throws DeliberativeCommandLineInterfaceInitializationException
	 */
	protected void init(String ddl, String pdl) 
			throws DeliberativeCommandLineInterfaceInitializationException 
	{
		try {
			// initialize the planner
			this.planner = PlannerBuilder.build(ddl, pdl);
		}
		catch (SynchronizationCycleException | ProblemInitializationException ex) {
			// command line interface initialization exception
			throw new DeliberativeCommandLineInterfaceInitializationException(ex.getMessage());
		}
	}
	
	/**
	 * 
	 * @param goal
	 * @throws DeliberativeCommandLineInterfaceInitializationException
	 * @throws NoSolutionFoundException
	 */
	protected void plan(TokenProtocolDescriptor goal) 
			throws DeliberativeCommandLineInterfaceInitializationException, NoSolutionFoundException 
	{
		// check planner
		if (this.planner == null) {
			throw new DeliberativeCommandLineInterfaceInitializationException("Planner Not Initialized yet!");
		}
		
		
		// run the planner on the desired goal
		this.currentSolution = this.planner.plan();
	}
	
	/**
	 * 
	 * @return
	 * @throws DeliberativeCommandLineInterfaceInitializationException
	 */
	protected PlanProtocolDescriptor export() 
			throws DeliberativeCommandLineInterfaceInitializationException
	{
		// check if a solution has been generated
		if (this.currentSolution == null) {
			throw new DeliberativeCommandLineInterfaceInitializationException("No soluion to extract found!");
		}
		
		// generate plan descriptor 
		PlanProtocolDescriptor plan = this.planner.export(this.currentSolution);
		// get the plan
		return plan;
	}
	
//	/**
//	 * FIXME : TO IMPLEMENT!!! 
//	 *
//	 * @param query
//	 * @throws DeliberativeCommandLineInterfaceInitializationException
//	 */
//	protected void query(ProtocolQuery query) 
//			throws DeliberativeCommandLineInterfaceInitializationException
//	{
//		// check planner
//		if (this.planner == null) {
//			throw new DeliberativeCommandLineInterfaceInitializationException("Planner Not Initialized yet!");
//		}
//		
//		try {
//			// process query
//			this.planner.query(query);
//		}
//		catch (InterruptedException ex) {
//			// interrupted
//			System.err.println(ex.getMessage());
//		}
//	}
}
