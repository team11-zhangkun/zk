package it.istc.pst.platinum.framework.microkernel.annotation.cfg.deliberative;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import it.istc.pst.platinum.deliberative.solver.PlannerSolver;

/**
 * 
 * @author anacleto
 *
 */
@Target({
	ElementType.TYPE,
	ElementType.CONSTRUCTOR
})
@Retention(RetentionPolicy.RUNTIME)
public @interface PlannerSolverConfiguration {

	// reference to solver algorithm
	Class<? extends PlannerSolver> solver();
	
	// set a timeout for the solver
	long timeout() default -1;
}