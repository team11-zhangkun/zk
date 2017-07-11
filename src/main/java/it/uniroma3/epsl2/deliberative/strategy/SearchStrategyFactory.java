package it.uniroma3.epsl2.deliberative.strategy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import it.uniroma3.epsl2.framework.microkernel.ApplicationFrameworkFactory;

/**
 * 
 * @author anacleto
 *
 */
public class SearchStrategyFactory extends ApplicationFrameworkFactory {

	/**
	 * 
	 */
	public SearchStrategyFactory() {
		super();
	}
	
	/**
	 * 
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends SearchStrategy> T create(SearchStrategyType type) 
	{
		// strategy
		T strategy = null;
		try 
		{
			// get class
			Class<T> clazz = (Class<T>) Class.forName(type.getStrategyClassName());
			// get constructor
			Constructor<T> c = clazz.getDeclaredConstructor();
			c.setAccessible(true);
			// create instance
			strategy = c.newInstance();
			
			// inject logger
			this.injectFrameworkLogger(strategy);
			// inject plan data-base
			this.injectPlanDataBase(strategy);
			// complete initialization if needed
			this.doCompleteApplicationObjectInitialization(strategy);
			// add to registry
			this.register(strategy);
		}
		catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
			throw new RuntimeException(ex.getMessage());  
		} 
		catch (InstantiationException | SecurityException | NoSuchMethodException | ClassNotFoundException ex) {
			throw new RuntimeException(ex.getMessage());
		}
		
		// get strategy
		return strategy;
	}
}
