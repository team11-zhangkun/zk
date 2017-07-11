package it.uniroma3.epsl2.deliberative.solver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import it.uniroma3.epsl2.deliberative.heuristic.FlawSelectionHeuristic;
import it.uniroma3.epsl2.deliberative.heuristic.FlawSelectionHeuristicFactory;
import it.uniroma3.epsl2.deliberative.strategy.SearchStrategy;
import it.uniroma3.epsl2.deliberative.strategy.SearchStrategyFactory;
import it.uniroma3.epsl2.framework.microkernel.ApplicationFrameworkFactory;
import it.uniroma3.epsl2.framework.microkernel.annotation.inject.deliberative.FlawSelectionHeuristicModule;
import it.uniroma3.epsl2.framework.microkernel.annotation.inject.deliberative.SearchStrategyModule;

/**
 * 
 * @author anacleto
 *
 */
public class SolverFactory extends ApplicationFrameworkFactory 
{
	private static SolverFactory INSTANCE = null;
	private SearchStrategyFactory strategyFactory;
	private FlawSelectionHeuristicFactory heuristicFactory;

	/**
	 * 
	 */
	public SolverFactory() {
		// set heuristic factory
		this.heuristicFactory = new FlawSelectionHeuristicFactory();
		// set strategy factory
		this.strategyFactory = new SearchStrategyFactory();
	}
	
	/**
	 * 
	 */
	public static SolverFactory getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new SolverFactory();
		}
		return INSTANCE;
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends Solver> T create(SolverType type) {
		// set solver
		T solver;
		try 
		{
			// get class
			Class<T> clazz = (Class<T>) Class.forName(type.getSolverClassName());
			// get constructor
			Constructor<T> c = clazz.getDeclaredConstructor();
			c.setAccessible(true);
			// create instance
			solver = c.newInstance();
			
			// inject logger
			this.injectFrameworkLogger(solver);
			// inject plan data base reference
			this.injectPlanDataBase(solver);
			
			// inject search strategy
			List<Field> fields = this.findFieldsAnnotatedBy(clazz, SearchStrategyModule.class);
			for (Field field : fields) {
				// get annotation
				SearchStrategyModule annotation = field.getAnnotation(SearchStrategyModule.class);
				// create search strategy
				SearchStrategy strategy = this.strategyFactory.create(annotation.strategy());
				// set field
				field.setAccessible(true);
				field.set(solver, strategy);
			}
			
			// inject heuristic
			fields = this.findFieldsAnnotatedBy(clazz, FlawSelectionHeuristicModule.class);
			for (Field field : fields) {
				// get annotation
				FlawSelectionHeuristicModule annotation = field.getAnnotation(FlawSelectionHeuristicModule.class);
				// create heuristics
				FlawSelectionHeuristic heuristic = this.heuristicFactory.create(annotation.heuristics());
				// set field
				field.setAccessible(true);
				field.set(solver, heuristic);
			}
			
			// complete initialization if needed
			this.doCompleteApplicationObjectInitialization(solver);
			// add to registry
			this.register(solver);
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
			throw new RuntimeException(ex.getMessage());  
		} 
		catch (InstantiationException | SecurityException | NoSuchMethodException | ClassNotFoundException ex) {
			throw new RuntimeException(ex.getMessage());
		}
		
		// get solver
		return solver;
	}
}
