package it.istc.pst.platinum.deliberative.app;

import java.util.HashMap;
import java.util.Map;

import it.istc.pst.platinum.framework.domain.component.Token;
import it.istc.pst.platinum.framework.domain.component.pdb.PlanDataBase;
import it.istc.pst.platinum.framework.domain.component.sv.StateVariable;
import it.istc.pst.platinum.framework.microkernel.ApplicationFrameworkContainer;
import it.istc.pst.platinum.framework.microkernel.ApplicationFrameworkObject;
import it.istc.pst.platinum.framework.microkernel.ConstraintCategory;
import it.istc.pst.platinum.framework.microkernel.annotation.inject.FrameworkLoggerPlaceholder;
import it.istc.pst.platinum.framework.microkernel.annotation.inject.deliberative.PlanDataBasePlaceholder;
import it.istc.pst.platinum.framework.microkernel.lang.ex.NoSolutionFoundException;
import it.istc.pst.platinum.framework.microkernel.lang.plan.SolutionPlan;
import it.istc.pst.platinum.framework.microkernel.lang.plan.Timeline;
import it.istc.pst.platinum.framework.microkernel.lang.relations.Relation;
import it.istc.pst.platinum.framework.microkernel.lang.relations.temporal.TemporalRelation;
import it.istc.pst.platinum.framework.parameter.lang.EnumerationParameter;
import it.istc.pst.platinum.framework.parameter.lang.NumericParameter;
import it.istc.pst.platinum.framework.parameter.lang.Parameter;
import it.istc.pst.platinum.framework.parameter.lang.ParameterType;
import it.istc.pst.platinum.framework.utils.log.FrameworkLogger;
import it.istc.pst.platinum.protocol.lang.ParameterTypeDescriptor;
import it.istc.pst.platinum.protocol.lang.PlanProtocolDescriptor;
import it.istc.pst.platinum.protocol.lang.ProtocolLanguageFactory;
import it.istc.pst.platinum.protocol.lang.TimelineProtocolDescriptor;
import it.istc.pst.platinum.protocol.lang.TokenProtocolDescriptor;
import it.istc.pst.platinum.protocol.lang.relation.RelationProtocolDescriptor;

/**
 * 
 */
public abstract class Planner extends ApplicationFrameworkObject 
{
	@FrameworkLoggerPlaceholder(lookup = ApplicationFrameworkContainer.FRAMEWORK_SINGLETON_DELIBERATIVE_LOGGER)
	protected FrameworkLogger loggger;
	
	@PlanDataBasePlaceholder(lookup = ApplicationFrameworkContainer.FRAMEWORK_SINGLETON_PLANDATABASE)
	protected PlanDataBase pdb;
	
	/**
	 * 
	 */
	protected Planner() {}
	
	/**
	 * Display the current plan
	 */
	public void display() {
		// display the current plan
		this.pdb.display();
	}
	
	/**
	 * 
	 * @return
	 * @throws NoSolutionFoundException
	 */
	public abstract SolutionPlan plan() 
			throws NoSolutionFoundException;
	
	/**
	 * 
	 * @param plan
	 * @return
	 */
	public PlanProtocolDescriptor export(SolutionPlan plan) {
		// generate protocol plan descriptor
		return this.generatePlanDescriptor(plan);
	}
	
	/**
	 * 
	 * @return
	 */
	protected PlanProtocolDescriptor generatePlanDescriptor(SolutionPlan solution) 
	{
		// get language factory
		ProtocolLanguageFactory factory = ProtocolLanguageFactory.getSingletonInstance(solution.getHorizon());
		
		// create plan descriptor
		PlanProtocolDescriptor plan = factory.createPlanDescriptor(this.pdb.getOrigin(), solution.getHorizon());
		// create an index
		Map<Token, TokenProtocolDescriptor> index = new HashMap<>();
		// create timeline descriptors
		for (Timeline tl : solution.getTimelines()) 
		{
			// get the state variable related to the timeline
			StateVariable comp = tl.getComponent();
			// initialize descriptor
			TimelineProtocolDescriptor timelineDescriptor = factory.createTimelineDescriptor(comp.getName(), tl.getName(), tl.isObservation());
			
			// get tokens of the timeline
			for (Token token : tl.getTokens()) 
			{
				// prepare the array of parameter names, values, and types
				String[] paramNames = new String[token.getPredicate().getParameters().length];
				ParameterTypeDescriptor[] paramTypes = new ParameterTypeDescriptor[token.getPredicate().getParameters().length];
				long[][] paramBounds = new long[token.getPredicate().getParameters().length][];
				String[][] paramValues = new String[token.getPredicate().getParameters().length][];
				for (int i = 0; i < token.getPredicate().getParameters().length; i++)
				{
					// get parameter
					Parameter<?> param = token.getPredicate().getParameterByIndex(i);
					// check parameter type
					if (param.getType().equals(ParameterType.NUMERIC_PARAMETER_TYPE)) {
						// get numeric parameter
						NumericParameter numPar = (NumericParameter) param;
						// set lower bound and upper bound
						paramBounds[i] = new long[] {
								numPar.getLowerBound(),
								numPar.getUpperBound()
						};
						// set default value to parameter values
						paramValues[i] = new String[] {};
					}
					else if (param.getType().equals(ParameterType.ENUMERATION_PARAMETER_TYPE)) {
						// enumeration parameter
						EnumerationParameter enuPar = (EnumerationParameter) param;
						// one single value is expected
						paramValues[i] = new String[] {
								enuPar.getValues()[0]
						};
						// set default value to parameter bounds
						paramBounds[i] = new long[] {};
					}
					else {
						throw new RuntimeException("Unknown parameter type:\n- type: " + param.getType() + "\n");
					}
				}
			
				// create token descriptor
				TokenProtocolDescriptor tokenDescriptor = factory.createTokenDescriptor(
						timelineDescriptor, 
						token.getPredicate().getValue().getLabel().replaceFirst("_", ""), 
						new long [] {
								token.getInterval().getStartTime().getLowerBound(), 
								token.getInterval().getEndTime().getUpperBound()
						}, 
						new long[] {
								token.getInterval().getEndTime().getLowerBound(),
								token.getInterval().getEndTime().getUpperBound()
						}, 
						new long[] {
								token.getInterval().getDurationLowerBound(),
								token.getInterval().getDurationUpperBound()
						}, 
						paramNames, paramTypes, paramBounds, paramValues);

				// update index
				index.put(token, tokenDescriptor);
			}
			
			// add an undefined gap for the last token if necessary
			Token last = tl.getTokens().get(tl.getTokens().size() - 1);
			// check schedule
			if (last.getInterval().getEndTime().getLowerBound() < solution.getHorizon()) {
				// create "empty" token description
				factory.createUndefinedTokenDescriptor(timelineDescriptor, 
						new long[] {
								last.getInterval().getEndTime().getLowerBound(),
								last.getInterval().getEndTime().getUpperBound()
						}, 
						new long [] {
								solution.getHorizon(),
								solution.getHorizon()
						}, 
						new long [] {
								(solution.getHorizon() - last.getInterval().getEndTime().getUpperBound()),
								(solution.getHorizon() - last.getInterval().getEndTime().getLowerBound())
						});
			}
			
			// add timeline to plan
			plan.addTimeline(timelineDescriptor);
		}
		
		// create timeline descriptors
		for (Timeline tl : solution.getObservations()) 
		{
			// get the state variable related to the timeline
			StateVariable comp = tl.getComponent();
			// initialize descriptor
			TimelineProtocolDescriptor timelineDescriptor = factory.createTimelineDescriptor(comp.getName(), tl.getName(), tl.isObservation());
			
			// get tokens of the timeline
			for (Token token : tl.getTokens()) 
			{
				// prepare the array of parameter names, values, and types
				String[] paramNames = new String[token.getPredicate().getParameters().length];
				ParameterTypeDescriptor[] paramTypes = new ParameterTypeDescriptor[token.getPredicate().getParameters().length];
				long[][] paramBounds = new long[token.getPredicate().getParameters().length][];
				String[][] paramValues = new String[token.getPredicate().getParameters().length][];
				for (int i = 0; i < token.getPredicate().getParameters().length; i++)
				{
					// get parameter
					Parameter<?> param = token.getPredicate().getParameterByIndex(i);
					// check parameter type
					if (param.getType().equals(ParameterType.NUMERIC_PARAMETER_TYPE)) {
						// get numeric parameter
						NumericParameter numPar = (NumericParameter) param;
						// set lower bound and upper bound
						paramBounds[i] = new long[] {
								numPar.getLowerBound(),
								numPar.getUpperBound()
						};
						// set default value to parameter values
						paramValues[i] = new String[] {};
					}
					else if (param.getType().equals(ParameterType.ENUMERATION_PARAMETER_TYPE)) {
						// enumeration parameter
						EnumerationParameter enuPar = (EnumerationParameter) param;
						// one single value is expected
						paramValues[i] = new String[] {
								enuPar.getValues()[0]
						};
						// set default value to parameter bounds
						paramBounds[i] = new long[] {};
					}
					else {
						throw new RuntimeException("Unknown parameter type:\n- type: " + param.getType() + "\n");
					}
				}
			
				// create token descriptor
				TokenProtocolDescriptor tokenDescriptor = factory.createTokenDescriptor(
						timelineDescriptor, 
						token.getPredicate().getValue().getLabel().replaceFirst("_", ""), 
						new long [] {
								token.getInterval().getStartTime().getLowerBound(), 
								token.getInterval().getEndTime().getUpperBound()
						}, 
						new long[] {
								token.getInterval().getEndTime().getLowerBound(),
								token.getInterval().getEndTime().getUpperBound()
						}, 
						new long[] {
								token.getInterval().getDurationLowerBound(),
								token.getInterval().getDurationUpperBound()
						}, 
						paramNames, paramTypes, paramBounds, paramValues);

				// update index
				index.put(token, tokenDescriptor);
			}
			
			// add an undefined gap for the last token if necessary
			Token last = tl.getTokens().get(tl.getTokens().size() - 1);
			// check schedule
			if (last.getInterval().getEndTime().getLowerBound() < solution.getHorizon()) {
				// create "empty" token description
				factory.createUndefinedTokenDescriptor(timelineDescriptor, 
						new long[] {
								last.getInterval().getEndTime().getLowerBound(),
								last.getInterval().getEndTime().getUpperBound()
						}, 
						new long [] {
								solution.getHorizon(),
								solution.getHorizon()
						}, 
						new long [] {
								(solution.getHorizon() - last.getInterval().getEndTime().getUpperBound()),
								(solution.getHorizon() - last.getInterval().getEndTime().getLowerBound())
						});
			}
			
			// add timeline to plan
			plan.addTimeline(timelineDescriptor);
		}
		
		// create relation descriptors
		for (Relation relation : solution.getRelations())
		{
			// export temporal relations only
			if (relation.getCategory().equals(ConstraintCategory.TEMPORAL_CONSTRAINT))
			{
				// get temporal relation
				TemporalRelation trel = (TemporalRelation) relation;
				// create relation description 
				RelationProtocolDescriptor relDescriptor = factory.createRelationDescriptor(
						relation.getType().name().toUpperCase(), 
						index.get(relation.getReference().getToken()), 
						index.get(relation.getTarget().getToken()));
				
				// set bounds
				relDescriptor.setBounds(trel.getBounds());
				// add relation descriptor to plan
				plan.addRelation(relDescriptor);
			}
		}
		
		// return plan descriptor
		return plan;
	}
}