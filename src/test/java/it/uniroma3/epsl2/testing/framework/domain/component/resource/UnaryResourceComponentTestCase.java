package it.uniroma3.epsl2.testing.framework.domain.component.resource;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import it.uniroma3.epsl2.framework.domain.component.ComponentValue;
import it.uniroma3.epsl2.framework.domain.component.DomainComponentFactory;
import it.uniroma3.epsl2.framework.domain.component.DomainComponentType;
import it.uniroma3.epsl2.framework.domain.component.ex.DecisionPropagationException;
import it.uniroma3.epsl2.framework.domain.component.ex.FlawSolutionApplicationException;
import it.uniroma3.epsl2.framework.domain.component.resource.costant.UnaryResource;
import it.uniroma3.epsl2.framework.lang.ex.ConsistencyCheckException;
import it.uniroma3.epsl2.framework.lang.flaw.Flaw;
import it.uniroma3.epsl2.framework.lang.flaw.FlawSolution;
import it.uniroma3.epsl2.framework.lang.plan.Decision;
import it.uniroma3.epsl2.framework.microkernel.query.TemporalQueryType;
import it.uniroma3.epsl2.framework.microkernel.resolver.ex.UnsolvableFlawFoundException;
import it.uniroma3.epsl2.framework.parameter.ParameterDataBaseFacadeFactory;
import it.uniroma3.epsl2.framework.parameter.ParameterDataBaseFacadeType;
import it.uniroma3.epsl2.framework.time.TemporalDataBaseFacade;
import it.uniroma3.epsl2.framework.time.TemporalDataBaseFacadeFactory;
import it.uniroma3.epsl2.framework.time.TemporalDataBaseFacadeType;
import it.uniroma3.epsl2.framework.time.lang.query.IntervalScheduleQuery;
import it.uniroma3.epsl2.framework.utils.log.FrameworkLoggerFactory;
import it.uniroma3.epsl2.framework.utils.log.FrameworkLoggingLevel;

/**
 * 
 * @author anacleto
 *
 */
public class UnaryResourceComponentTestCase 
{
	private static final int ORIGIN = 0;
	private static final int HORIZON = 100;
	private TemporalDataBaseFacade facade;
	private UnaryResource resource;
	
	/**
	 * 
	 */
	@Before
	public void init() {
		System.out.println("**********************************************************************************");
		System.out.println("************************* Unary Resource Component Test Case ***********************");
		System.out.println("**********************************************************************************");
		
		// create logger
		FrameworkLoggerFactory lf = new FrameworkLoggerFactory();
		lf.createFrameworkLogger(FrameworkLoggingLevel.DEBUG);
		
		// get temporal facade
		TemporalDataBaseFacadeFactory factory = new TemporalDataBaseFacadeFactory();
		// create temporal facade
		this.facade = factory.createSingleton(TemporalDataBaseFacadeType.UNCERTAINTY_TEMPORAL_FACADE, ORIGIN, HORIZON);
		
		// get parameter facade
		ParameterDataBaseFacadeFactory pf = new ParameterDataBaseFacadeFactory();
		pf.createSingleton(ParameterDataBaseFacadeType.CSP_PARAMETER_FACADE);
		
		// create unary resource
		DomainComponentFactory df = new DomainComponentFactory();
		this.resource = df.create("Unary1", DomainComponentType.RESOURCE_UNARY);
		// add requirement value
		this.resource.addRequirementValue("REQUIREMENT");
	}
	
	/**
	 * 
	 */
	@After
	public void clear() {
		this.resource = null;
		this.facade = null;
		System.gc();
		System.out.println();
		System.out.println("**********************************************************************************");
		System.out.println();
	}
	
	/**
	 * 
	 */
	@Test
	public void createUnaryResourceTest() {
		System.out.println("[Test]: createUnaryResourceTest() --------------------");
		System.out.println();
		
		// check state variable object
		Assert.assertNotNull(this.resource);
		// check values
		List<ComponentValue> values = this.resource.getValues();
		Assert.assertNotNull(values);
		Assert.assertTrue(values.size() == 1);
		// check min capacity
		Assert.assertTrue(this.resource.getMinCapacity() == 0);
		// check max capacity
		Assert.assertTrue(this.resource.getMaxCapacity() == 1);
		// add transitions
		System.out.println(this.resource);
	}
	
	/**
	 * 
	 */
	@Test
	public void addResourceRequirementsTest() {
		System.out.println("[Test]: addResourceRequirementsTest() --------------------");
		System.out.println();
		
		// get value
		ComponentValue requirement = this.resource.getValues().get(0);
		Assert.assertNotNull(requirement);
		Assert.assertTrue(requirement.getLabel().equals("REQUIREMENT"));
		
		// create decision
		Decision dec = this.resource.create(requirement, new String[] {});
		Assert.assertNotNull(dec);
		Assert.assertNull(dec.getToken());
		System.out.println(dec);
		try
		{
			// add decision
			this.resource.add(dec);
			Assert.assertNotNull(dec.getToken());
		}
		catch (DecisionPropagationException ex) {
			System.err.println(ex.getMessage());
			Assert.assertTrue(false);
		}
	}
	
	/**
	 * 
	 */
	@Test
	public void addDecisionsAndFindPeaksTest1() {
		System.out.println("[Test]: addDecisionsAndFindPeaksTest1() --------------------");
		System.out.println();
		
		// get value
		ComponentValue requirement = this.resource.getValues().get(0);
		Assert.assertNotNull(requirement);
		Assert.assertTrue(requirement.getLabel().equals("REQUIREMENT"));
		
		try
		{
			// create decision
			Decision a1 = this.resource.create(
					requirement, 
					new String[] {},
					new long[] {2, 4},
					new long[] {8, 10},
					new long[] {1, HORIZON});
			// add decision
			this.resource.add(a1);
			System.out.println("a1: " + a1);
			
			// create decision
			Decision a2 = this.resource.create(
					requirement, 
					new String[] {},
					new long[] {4, 6},
					new long[] {16, 18},
					new long[] {1, HORIZON});
			// add decision
			this.resource.add(a2);
			System.out.println("a2: " + a2);
			
			// create decision
			Decision a3 = this.resource.create(
					requirement, 
					new String[] {},
					new long[] {11, 13},
					new long[] {21, 23},
					new long[] {1, HORIZON});
			// add decision
			this.resource.add(a3);
			System.out.println("a3: " + a3);
			
			
			// check peak
			List<Flaw> flaws = this.resource.detectFlaws();
			Assert.assertNotNull(flaws);
			Assert.assertTrue(flaws.size() == 2);
			System.out.println("#" + flaws.size() + " peaks have been found");
			for (Flaw flaw : flaws) {
				// print flaw
				System.out.println("\n"+ flaw + "\n");
			}
		}
		catch (DecisionPropagationException | UnsolvableFlawFoundException ex) {
			System.err.println(ex.getMessage());
			Assert.assertTrue(false);
		}
	}
	
	/**
	 * 
	 */
	@Test
	public void addDecisionsAndFindPeaksTest2() {
		System.out.println("[Test]: addDecisionsAndFindPeaksTest2() --------------------");
		System.out.println();
		
		// get value
		ComponentValue requirement = this.resource.getValues().get(0);
		Assert.assertNotNull(requirement);
		Assert.assertTrue(requirement.getLabel().equals("REQUIREMENT"));
		
		try
		{
			// create decision
			Decision a1 = this.resource.create(
					requirement, 
					new String[] {});
			// add decision
			this.resource.add(a1);
			System.out.println("a1: " + a1);
			
			// create decision
			Decision a2 = this.resource.create(
					requirement, 
					new String[] {});
			// add decision
			this.resource.add(a2);
			System.out.println("a2: " + a2);
			
			// create decision
			Decision a3 = this.resource.create(
					requirement, 
					new String[] {});
			// add decision
			this.resource.add(a3);
			System.out.println("a3: " + a3);
			
			// check peak
			List<Flaw> flaws = this.resource.detectFlaws();
			Assert.assertNotNull(flaws);
			Assert.assertTrue(flaws.size() == 1);
			System.out.println("#" + flaws.size() + " peaks have been found");
			for (Flaw flaw : flaws) {
				// print flaw
				System.out.println("\n" + flaw + "\n");
			}
		}
		catch (DecisionPropagationException | UnsolvableFlawFoundException ex) {
			System.err.println(ex.getMessage());
			Assert.assertTrue(false);
		}
	}
	
	/**
	 * 
	 */
	@Test
	public void detectAndSolvePeakTest() {
		System.out.println("[Test]: detectAndSolvePeakTest() --------------------");
		System.out.println();
		
		// get value
		ComponentValue requirement = this.resource.getValues().get(0);
		Assert.assertNotNull(requirement);
		Assert.assertTrue(requirement.getLabel().equals("REQUIREMENT"));
		
		try
		{
			// create decision
			Decision a1 = this.resource.create(
					requirement, 
					new String[] {});
			// add decision
			this.resource.add(a1);
			System.out.println("a1: " + a1);
			
			// create decision
			Decision a2 = this.resource.create(
					requirement, 
					new String[] {});
			// add decision
			this.resource.add(a2);
			System.out.println("a2: " + a2);
			
			// check peak
			List<Flaw> flaws = this.resource.detectFlaws();
			Assert.assertNotNull(flaws);
			Assert.assertTrue(flaws.size() == 1);
			System.out.println("#" + flaws.size() + " peaks have been found");
			
			
			// get flaw solution
			Flaw flaw = flaws.get(0);
			List<FlawSolution> solutions = flaw.getSolutions();
			Assert.assertNotNull(solutions);
			Assert.assertTrue(solutions.size() > 1);
			System.out.println("#" + solutions.size() + " solutions available");
			// select the first available solution
			FlawSolution sol = solutions.get(1);
			System.out.println("Selected solution\n- " + sol);
			
			try
			{
				// commit solution
				this.resource.commit(sol);
				// check consistency
				this.facade.checkConsistency();
				
				IntervalScheduleQuery query = this.facade.
						createTemporalQuery(TemporalQueryType.INTERVAL_SCHEDULE);
				
				query.setInterval(a1.getToken().getInterval());
				this.facade.process(query);
				query.setInterval(a2.getToken().getInterval());
				this.facade.process(query);
				
				// print resulting schedule
				System.out.println("a1: " + a1);
				System.out.println("a2: " + a2);
				
				// check current flaws
				flaws = this.resource.detectFlaws();
				System.out.println("#" + flaws.size() + " peaks have been found");
				Assert.assertTrue(flaws.isEmpty());
				Assert.assertTrue(flaws.size() == 0); 
			}
			catch (FlawSolutionApplicationException | ConsistencyCheckException ex) {
				System.err.println(ex.getMessage());
				Assert.assertTrue(false);
			}
			
			
		}
		catch (DecisionPropagationException | UnsolvableFlawFoundException ex) {
			System.err.println(ex.getMessage());
			Assert.assertTrue(false);
		}
		
	}
}