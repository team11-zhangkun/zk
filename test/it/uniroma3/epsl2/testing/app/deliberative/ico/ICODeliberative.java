package it.uniroma3.epsl2.testing.app.deliberative.ico;

import it.uniroma3.epsl2.deliberative.Planner;
import it.uniroma3.epsl2.deliberative.PlannerBuilder;
import it.uniroma3.epsl2.deliberative.heuristic.fsh.FlawSelectionHeuristicType;
import it.uniroma3.epsl2.deliberative.search.SearchStrategyType;
import it.uniroma3.epsl2.deliberative.solver.SolverType;
import it.uniroma3.epsl2.framework.lang.ex.NoSolutionFoundException;
import it.uniroma3.epsl2.framework.lang.ex.ProblemInitializationException;
import it.uniroma3.epsl2.framework.lang.plan.Plan;
import it.uniroma3.epsl2.framework.microkernel.annotation.planner.cfg.PlannerConfiguration;

/**
 * 
 * @author anacleto
 *
 */
@PlannerConfiguration(
		
	// set heuristic
	heuristic = FlawSelectionHeuristicType.TFSH,
	
	// set solver
	solver = SolverType.PSEUDO_CONTROLLABILITY_AWARE,
	
	// set strategy
	strategy = SearchStrategyType.DFS
)
public class ICODeliberative extends Planner
{
	private static final String DDL = "domains/ico/ico_1guest_1mug.ddl";
	private static final String PDL = "domains/ico/ico_1guest_1mug_1task.pdl";
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) 
	{ 
		try 
		{
			Planner planner = PlannerBuilder.build(ICODeliberative.class.getName(), DDL, PDL);	
			// start planning
			Plan plan = planner.plan();
			// solution found
			System.out.println("... solution found after " + plan.getSolvingTime() + " msecs\n");
			// print the resulting plan
			System.out.println(plan);
			
			// display the resulting plant
			planner.display();
		}
		catch (NoSolutionFoundException ex) {
			// no solution found
			System.err.println(ex.getMessage());
		}
		catch (ProblemInitializationException ex) {
			System.err.println(ex.getMessage());
			ex.printStackTrace(System.err);
		}
		catch (Exception ex) {
			System.err.println(ex.getMessage());
			ex.printStackTrace(System.err);
		}
	}
}
