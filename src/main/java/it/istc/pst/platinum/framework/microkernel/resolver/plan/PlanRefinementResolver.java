package it.istc.pst.platinum.framework.microkernel.resolver.plan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.istc.pst.platinum.framework.domain.component.Decision;
import it.istc.pst.platinum.framework.domain.component.DomainComponent;
import it.istc.pst.platinum.framework.domain.component.ex.DecisionPropagationException;
import it.istc.pst.platinum.framework.domain.component.ex.FlawSolutionApplicationException;
import it.istc.pst.platinum.framework.domain.component.ex.RelationPropagationException;
import it.istc.pst.platinum.framework.domain.component.pdb.ParameterSynchronizationConstraint;
import it.istc.pst.platinum.framework.domain.component.pdb.SynchronizationConstraint;
import it.istc.pst.platinum.framework.domain.component.pdb.SynchronizationRule;
import it.istc.pst.platinum.framework.domain.component.pdb.TemporalSynchronizationConstraint;
import it.istc.pst.platinum.framework.domain.component.pdb.TokenVariable;
import it.istc.pst.platinum.framework.microkernel.ConstraintCategory;
import it.istc.pst.platinum.framework.microkernel.lang.ex.ConsistencyCheckException;
import it.istc.pst.platinum.framework.microkernel.lang.flaw.Flaw;
import it.istc.pst.platinum.framework.microkernel.lang.flaw.FlawSolution;
import it.istc.pst.platinum.framework.microkernel.lang.relations.Relation;
import it.istc.pst.platinum.framework.microkernel.lang.relations.parameter.BindParameterRelation;
import it.istc.pst.platinum.framework.microkernel.lang.relations.parameter.EqualParameterRelation;
import it.istc.pst.platinum.framework.microkernel.lang.relations.parameter.NotEqualParameterRelation;
import it.istc.pst.platinum.framework.microkernel.lang.relations.parameter.ParameterRelation;
import it.istc.pst.platinum.framework.microkernel.lang.relations.temporal.TemporalRelation;
import it.istc.pst.platinum.framework.microkernel.resolver.Resolver;
import it.istc.pst.platinum.framework.microkernel.resolver.ResolverType;
import it.istc.pst.platinum.framework.microkernel.resolver.ex.UnsolvableFlawException;
import it.istc.pst.platinum.framework.microkernel.resolver.plan.GoalJustification.JustificationType;
import it.istc.pst.platinum.framework.utils.properties.FilePropertyReader;

/**
 * 
 * @author anacleto
 *
 */
public class PlanRefinementResolver extends Resolver<DomainComponent>
{	
	private double expansionCost;
	private double unificationCost;
	
	/**
	 * 
	 */
	protected PlanRefinementResolver() {
		super(ResolverType.PLAN_REFINEMENT.getLabel(), 
				ResolverType.PLAN_REFINEMENT.getFlawTypes());
		
		// get deliberative property file
		FilePropertyReader properties = new FilePropertyReader(FilePropertyReader.DEFAULT_DELIBERATIVE_PROPERTY);
		this.expansionCost = Double.parseDouble(properties.getProperty("expansion-cost"));
		this.unificationCost = Double.parseDouble(properties.getProperty("unification-cost"));
	}
	
	/**
	 * 
	 */
	@Override
	protected List<Flaw> doFindFlaws() 
	{
		// list of goals
		List<Flaw> flaws = new ArrayList<>();
		// check pending decisions
		for (Decision decision : this.component.getPendingDecisions()) 
		{
			// add sub-goal
			Goal goal = new Goal(FLAW_COUNTER.getAndIncrement(), this.component, decision);
			// check if external component
			if (decision.getComponent().isExternal()) {
				// set mandatory unification
				goal.setMandatoryUnification();
			}
			
			// add goal to flaws
			flaws.add(goal);
		}
		
		
		// get flaws
		return flaws;
	}
	
	/**
	 * 
	 */
	@Override
	protected void doComputeFlawSolutions(Flaw flaw) 
			throws UnsolvableFlawException 
	{
		// get goal
		Goal goal = (Goal) flaw;
		
		// check solving information
		if (!goal.isMandatoryExpansion()) {
			// compute unification solutions
			this.doComputeUnificationSolutions(goal);
		}
		
		// check solving information
		if (!goal.isMandatoryUnification()) {
			// compute expansion solutions
			this.doComputeExpansionSolutions(goal);
		}
		 
		
		// check if solvable
		if (!goal.isSolvable()) {
			// simply throw exception
			throw new UnsolvableFlawException("Unsolvable flaw found on component " + this.component.getName() + ":"
					+ "\n" + flaw + "\n");
		}
	}
	
	
	/**
	 * 
	 * @param solution
	 * @throws Exception
	 */
	@Override
	protected void doApply(FlawSolution solution) 
			throws FlawSolutionApplicationException 
	{
		// get goal justification
		GoalJustification just = (GoalJustification) solution;
		// check type 
		switch (just.getJustificationType()) 
		{
			// expansion step
			case EXPANSION : {
				// apply solution
				this.doApplyExpansion((GoalExpansion) just);
			}
			break;
			
			// unification step
			case UNIFICATION : {
				// apply solution
				this.doApplyUnification((GoalUnification) just);
			}
			break;
		}
	}
	
	/**
	 * 
	 */
	@Override
	protected void doRestore(FlawSolution solution) 
			throws DecisionPropagationException, RelationPropagationException 
	{
		// get goal justification
		GoalJustification just = (GoalJustification) solution;
		// check if unification 
		if (just.getJustificationType().equals(JustificationType.UNIFICATION)) {
			// restore unification
			this.doRestoreUnification((GoalUnification) just);
		}
		else {
			// "standard" way of restoring a flaw solution
			super.doRestore(solution);
		}
	}
	
	/**
	 * 
	 * @param solution
	 * @throws RelationPropagationException
	 */
	private void doRestoreUnification(GoalUnification solution) 
			throws RelationPropagationException
	{
		// restore relation translation
		for (Relation rel : solution.getTranslatedReferenceGoalRelations())
		{
			// check relation category
			if (rel.getCategory().equals(ConstraintCategory.TEMPORAL_CONSTRAINT)) 
			{
				// replace reference
				rel.setReference(solution.getUnificationDecision());
			}
			
			if (rel.getCategory().equals(ConstraintCategory.PARAMETER_CONSTRAINT))
			{
				// check relation type
				switch (rel.getType())
				{
					// bind parameter
					case BIND_PARAMETER: 
					{
						// the goal can be only the reference of the relation
						ParameterRelation pRel = (ParameterRelation) rel;
						
						// get relation reference parameter label
						String refParamLabel = pRel.getReferenceParameterLabel();
						// get label index
						int refParameterIndex = pRel.getReference().getParameterIndexByLabel(refParamLabel);
						// get unification decision parameter label
						String label = solution.getUnificationDecision().getParameterLabelByIndex(refParameterIndex);

						// update reference decision 
						pRel.setReference(solution.getUnificationDecision());
						// update reference label of the relation 
						pRel.setReferenceParameterLabel(label);
					}
					break;
					
					case EQUAL_PARAMETER : 
					{
						// get parameter relation
						EqualParameterRelation eqRel = (EqualParameterRelation) rel;
						// get relation reference parameter label
						String refParamLabel = eqRel.getReferenceParameterLabel();
						// get label index
						int refParameterIndex = eqRel.getReference().getParameterIndexByLabel(refParamLabel);
						// get unification decision parameter label
						String label = solution.getUnificationDecision().getParameterLabelByIndex(refParameterIndex);

						// update reference decision 
						eqRel.setReference(solution.getUnificationDecision());
						// update reference label of the relation 
						eqRel.setReferenceParameterLabel(label);
					}
					break;
					
					case NOT_EQUAL_PARAMETER : 
					{
						// get parameter relation
						NotEqualParameterRelation neqRel = (NotEqualParameterRelation) rel;
						// get relation reference parameter label
						String refParamLabel = neqRel.getReferenceParameterLabel();
						// get label index
						int refParameterIndex = neqRel.getReference().getParameterIndexByLabel(refParamLabel);
						// get unification decision parameter label
						String label = solution.getUnificationDecision().getParameterLabelByIndex(refParameterIndex);

						// update reference decision 
						neqRel.setReference(solution.getUnificationDecision());
						// update reference label of the relation 
						neqRel.setReferenceParameterLabel(label);
					}
					break;
					
					
					default:
						// unknown parameter relation
						throw new RuntimeException("Unknown Parameter relation type : " + rel.getType() + "\n");
				}
			}
		}
		
		
		// restore relation translation
		for (Relation rel : solution.getTranslatedTargetGoalRelations())
		{
			// check relation category
			if (rel.getCategory().equals(ConstraintCategory.TEMPORAL_CONSTRAINT)) 
			{
				// replace reference
				rel.setTarget(solution.getUnificationDecision());
			}
			
			if (rel.getCategory().equals(ConstraintCategory.PARAMETER_CONSTRAINT))
			{
				// check relation type
				switch (rel.getType())
				{
					case EQUAL_PARAMETER : 
					{
						// get parameter relation
						EqualParameterRelation eqRel = (EqualParameterRelation) rel;
						// get relation reference parameter label
						String refParamLabel = eqRel.getTargetParameterLabel();
						// get label index
						int refParameterIndex = eqRel.getTarget().getParameterIndexByLabel(refParamLabel);
						// get unification decision parameter label
						String label = solution.getUnificationDecision().getParameterLabelByIndex(refParameterIndex);

						// update reference decision 
						eqRel.setTarget(solution.getUnificationDecision());
						// update reference label of the relation 
						eqRel.setTargetParameterLabel(label);
					}
					break;
					
					case NOT_EQUAL_PARAMETER : 
					{
						// get parameter relation
						NotEqualParameterRelation neqRel = (NotEqualParameterRelation) rel;
						// get relation reference parameter label
						String refParamLabel = neqRel.getTargetParameterLabel();
						// get label index
						int refParameterIndex = neqRel.getTarget().getParameterIndexByLabel(refParamLabel);
						// get unification decision parameter label
						String label = solution.getUnificationDecision().getParameterLabelByIndex(refParameterIndex);

						// update reference decision 
						neqRel.setTarget(solution.getUnificationDecision());
						// update reference label of the relation 
						neqRel.setTargetParameterLabel(label);
					}
					break;
					
					
					default:
						// unknown parameter relation
						throw new RuntimeException("Unknown Parameter relation type : " + rel.getType() + "\n");
				}
			}
		}

		
		
		// list of committed parameter constraints
		Set<Relation> committed = new HashSet<>();
		try	
		{
			// get goal component
			DomainComponent gComp = solution.getGoalDecision().getComponent();
			// remove original goal: PENDING -> SILENT
			gComp.free(solution.getGoalDecision());
			
			// activate translated relations
			for (Relation rel : solution.getActivatedRelations()) 
			{
				// check if can be activated
				if (rel.canBeActivated()) {
					// activate relation
					rel.getReference().getComponent().activate(rel);
					// add relation to the committed list
					committed.add(rel);
				}
			}
		}
		catch (RelationPropagationException ex) 
		{
			// get goal component
			DomainComponent gComp = solution.getGoalDecision().getComponent();
			// restore goal: SILENT -> PENDING
			gComp.restore(solution.getGoalDecision());
			
			
			
			// deactivate committed relations
			for (Relation rel : committed) {
				// get reference component
				DomainComponent refComp = rel.getReference().getComponent();
				refComp.deactivate(rel);
			}
			
			// translated back relations
			for (Relation rel : solution.getTranslatedReferenceGoalRelations())
			{
				// check category
				if (rel.getCategory().equals(ConstraintCategory.PARAMETER_CONSTRAINT)) 
				{
					// get parameter relation
					ParameterRelation pRel = (ParameterRelation) rel;
					
					// get relation reference parameter label
					String refParamLabel = pRel.getReferenceParameterLabel();
					// get label index
					int pIndex = pRel.getReference().getParameterIndexByLabel(refParamLabel);
					// get goal decision parameter label
					String label = solution.getGoalDecision().getParameterLabelByIndex(pIndex);
					
					// update relation
					pRel.setReference(solution.getGoalDecision());
					pRel.setReferenceParameterLabel(label);
				}
				
				if (rel.getCategory().equals(ConstraintCategory.TEMPORAL_CONSTRAINT)) {
					// update relation
					rel.setReference(solution.getGoalDecision());
				}
			}
			
			
			// translated back parameter relations
			for (Relation rel : solution.getTranslatedTargetGoalRelations())
			{
				// check relation category 
				if (rel.getCategory().equals(ConstraintCategory.PARAMETER_CONSTRAINT))
				{
					// check relation
					switch (rel.getType())
					{
						case EQUAL_PARAMETER : 
						{
							// get equal relation
							EqualParameterRelation eqRel = (EqualParameterRelation) rel;
							// get relation reference parameter label
							String tarParamLabel = eqRel.getTargetParameterLabel();
							// get label index
							int pIndex = eqRel.getTarget().getParameterIndexByLabel(tarParamLabel);
							// get goal decision parameter label
							String label = solution.getGoalDecision().getParameterLabelByIndex(pIndex);
							
							// update relation
							eqRel.setTarget(solution.getGoalDecision());
							eqRel.setTargetParameterLabel(label);
						}
						break;
							
						case NOT_EQUAL_PARAMETER : 
						{
							// get equal relation
							NotEqualParameterRelation neqRel = (NotEqualParameterRelation) rel;
							// get relation reference parameter label
							String tarParamLabel = neqRel.getTargetParameterLabel();
							// get label index
							int pIndex = neqRel.getTarget().getParameterIndexByLabel(tarParamLabel);
							// get goal decision parameter label
							String label = solution.getGoalDecision().getParameterLabelByIndex(pIndex);
							
							// update relation
							neqRel.setTarget(solution.getGoalDecision());
							neqRel.setTargetParameterLabel(label);
						}
						break;
						
						default:
							// unknown parameter relation
							throw new RuntimeException("Unknown Parameter relation type : " + rel.getType() + "\n"); 
							
					}
				}
				
				// check temporal relation
				if (rel.getCategory().equals(ConstraintCategory.TEMPORAL_CONSTRAINT))
				{
					// update relation
					rel.setTarget(solution.getGoalDecision());
				}
			}

			// not feasible solution
			throw new RelationPropagationException(ex.getMessage());
		}
	}
	
	/**
	 * 
	 */
	@Override
	protected void doRetract(FlawSolution solution) 
	{
		// check solution type
		GoalJustification justif = (GoalJustification) solution;
		// check if unification solution
		if (justif.getJustificationType().equals(JustificationType.UNIFICATION)) {
			// special management of unification
			this.doRetractUnification((GoalUnification) solution);
		}
		else {
			// "standard" management of flaw solution
			super.doRetract(solution);
		}
	}
	
	/**
	 * 
	 * @param goal
	 */
	private void doComputeUnificationSolutions(Goal goal) 
	{
		// get goal component
		DomainComponent gComp = goal.getComponent();
		// search active decisions that can be unified with the goal
		for (Decision unif : gComp.getActiveDecisions()) 
		{
			// check predicate and temporal unification
			if (this.isPredicateUnificationFeasible(goal.getDecision(), unif) && 
					this.isTemporalUnificationFeasible(goal.getDecision(), unif))
			{
				// possible unification found
				GoalUnification unification = new GoalUnification(goal, unif, this.unificationCost);
				// add unification solution
				goal.addSolution(unification);
				info("Feasible unification found:\n"
						+ "- planning goal: " + goal + "\n"
						+ "- unification decision: " + unification + "\n");
			}
			else {
				// unification not feasible
				debug("No feasible unification:\n"
						+ "- planning goal: " + goal + "\n"
						+ "- decision : \"" + unif + "\"\n");
			}
		}
	}

	/**
	 * 
	 * @param goal
	 * @param decision
	 * @return
	 */
	private boolean isPredicateUnificationFeasible(Decision goal, Decision decision) 
	{
		// feasibility flag
		boolean feasible = true;
		// first check if the decisions refer to the same values
		if (!decision.getValue().equals(goal.getValue()) && 
				decision.getComponent().equals(goal.getComponent())) {
			
			// not feasible unification
			feasible = false;
			debug("Not feasible predicate unification:\n"
					+ "- planning goal: " + goal + "\n"
					+ "- unification decision: " + decision + "\n"); 
		}
		else 
		{
			// list of committed parameter constraints
			Set<Relation> committed = new HashSet<>();
			// list of translated parameter relations - reference
			Set<ParameterRelation> translatedReferenceGoalRelations = new HashSet<>();
			// list of translated parameter relations - target
			Set<ParameterRelation> translatedTargetGoalRelations = new HashSet<>();
			
			// get goal component
			DomainComponent goalComp = goal.getComponent();
			// get all (pending) relation concerning the goal decision
			Set<Relation> pending = goalComp.getRelations(goal);
			// check relations
			for (Relation rel : pending)
			{
				// check parameter constraint type
				if (rel.getCategory().equals(ConstraintCategory.PARAMETER_CONSTRAINT))
				{
					// check relation type
					switch (rel.getType())
					{
						// bind parameter
						case BIND_PARAMETER: 
						{
							// the goal can be only the reference of the relation
							ParameterRelation pRel = (ParameterRelation) rel;
							
							// get relation reference parameter label
							String refParamLabel = pRel.getReferenceParameterLabel();
							// get label index
							int refParameterIndex = pRel.getReference().getParameterIndexByLabel(refParamLabel);
							// get unification decision parameter label
							String label = decision.getParameterLabelByIndex(refParameterIndex);

							// update reference decision 
							pRel.setReference(decision);
							
							// update reference label of the relation 
							pRel.setReferenceParameterLabel(label);
							// add relation to the list of translated ones
							translatedReferenceGoalRelations.add(pRel);
						}
						break;
						
						case EQUAL_PARAMETER : 
						{
							// get parameter relation
							EqualParameterRelation eqRel = (EqualParameterRelation) rel;
							// check if the goal is the reference or the parameter constraint 
							if (eqRel.getReference().equals(goal))
							{
								// get relation reference parameter label
								String refParamLabel = eqRel.getReferenceParameterLabel();
								// get label index
								int refParameterIndex = eqRel.getReference().getParameterIndexByLabel(refParamLabel);
								// get unification decision parameter label
								String label = decision.getParameterLabelByIndex(refParameterIndex);

								// update reference decision 
								eqRel.setReference(decision);
								// update reference label of the relation 
								eqRel.setReferenceParameterLabel(label);
								// add relation to the list of translated ones
								translatedReferenceGoalRelations.add(eqRel);
							}
							else // the goal is the target of the relation 
							{
								// get relation reference parameter label
								String refParamLabel = eqRel.getTargetParameterLabel();
								// get label index
								int refParameterIndex = eqRel.getTarget().getParameterIndexByLabel(refParamLabel);
								// get unification decision parameter label
								String label = decision.getParameterLabelByIndex(refParameterIndex);

								// update reference decision 
								eqRel.setTarget(decision);
								// update reference label of the relation 
								eqRel.setTargetParameterLabel(label);
								// add relation to the list of translated ones
								translatedTargetGoalRelations.add(eqRel);
							}
						}
						break;
						
						case NOT_EQUAL_PARAMETER : 
						{
							// get parameter relation
							NotEqualParameterRelation neqRel = (NotEqualParameterRelation) rel;
							// check if the goal is the reference or the parameter constraint 
							if (neqRel.getReference().equals(goal))
							{
								// get relation reference parameter label
								String refParamLabel = neqRel.getReferenceParameterLabel();
								// get label index
								int refParameterIndex = neqRel.getReference().getParameterIndexByLabel(refParamLabel);
								// get unification decision parameter label
								String label = decision.getParameterLabelByIndex(refParameterIndex);

								// update reference decision 
								neqRel.setReference(decision);
								// update reference label of the relation 
								neqRel.setReferenceParameterLabel(label);
								// add relation to the list of translated ones
								translatedReferenceGoalRelations.add(neqRel);
							}
							else // the goal is the target of the relation 
							{
								// get relation reference parameter label
								String refParamLabel = neqRel.getTargetParameterLabel();
								// get label index
								int refParameterIndex = neqRel.getTarget().getParameterIndexByLabel(refParamLabel);
								// get unification decision parameter label
								String label = decision.getParameterLabelByIndex(refParameterIndex);

								// update reference decision 
								neqRel.setTarget(decision);
								// update reference label of the relation 
								neqRel.setTargetParameterLabel(label);
								// add relation to the list of translated ones
								translatedTargetGoalRelations.add(neqRel);
							}
						}
						break;
						
						
						default:
							// unknown parameter relation
							throw new RuntimeException("Unknown Parameter relation type : " + rel.getType() + "\n");
					}
				}
			}
				
			try
			{
				// activate translated relations
				for (Relation rel : translatedReferenceGoalRelations) {
					// check if can be activated
					if (rel.canBeActivated()) {
						// activate relation
						rel.getReference().getComponent().activate(rel);
						// add relation to the committed list
						committed.add(rel);
					}
				}
				
				// activate translated relations
				for (Relation rel : translatedTargetGoalRelations) {
					// check if can be activated
					if (rel.canBeActivated()) {
						// activate relation
						rel.getReference().getComponent().activate(rel);
						// add relation to the committed list
						committed.add(rel);
					}
				}
				
				// check parameter of the plan
				this.pdb.verify();
			}
			catch (ConsistencyCheckException | RelationPropagationException ex) {
				// not feasible 
				feasible = false;
				// not feasible unification
				debug("Not feasible predicate unification:\n"
						+ "- planning goal: " + goal + "\n"
						+ "- unification decision: " + decision + "\n");
			}
			finally 
			{
				// check committed relations
				for (Relation rel : committed) {
					// deactivate relation
					rel.getReference().getComponent().deactivate(rel);
				}

				
				// translated back parameter relations
				for (ParameterRelation rel : translatedReferenceGoalRelations)
				{
					// get relation reference parameter label
					String refParamLabel = rel.getReferenceParameterLabel();
					// get label index
					int pIndex = rel.getReference().getParameterIndexByLabel(refParamLabel);
					// get goal decision parameter label
					String label = goal.getParameterLabelByIndex(pIndex);
					
					// update relation
					rel.setReference(goal);
					rel.setReferenceParameterLabel(label);
				}
				
				// translated back parameter relations
				for (ParameterRelation rel : translatedTargetGoalRelations)
				{
					// check relation
					switch (rel.getType())
					{
						case EQUAL_PARAMETER : 
						{
							// get equal relation
							EqualParameterRelation eqRel = (EqualParameterRelation) rel;
							// get relation reference parameter label
							String tarParamLabel = eqRel.getTargetParameterLabel();
							// get label index
							int pIndex = eqRel.getTarget().getParameterIndexByLabel(tarParamLabel);
							// get goal decision parameter label
							String label = goal.getParameterLabelByIndex(pIndex);
							
							// update relation
							eqRel.setTarget(goal);
							eqRel.setTargetParameterLabel(label);
						}
						break;
							
						case NOT_EQUAL_PARAMETER : 
						{
							// get equal relation
							NotEqualParameterRelation neqRel = (NotEqualParameterRelation) rel;
							// get relation reference parameter label
							String tarParamLabel = neqRel.getTargetParameterLabel();
							// get label index
							int pIndex = neqRel.getTarget().getParameterIndexByLabel(tarParamLabel);
							// get goal decision parameter label
							String label = goal.getParameterLabelByIndex(pIndex);
							
							// update relation
							neqRel.setTarget(goal);
							neqRel.setTargetParameterLabel(label);
						}
						break;
						
						default:
							// unknown parameter relation
							throw new RuntimeException("Unknown Parameter relation type : " + rel.getType() + "\n"); 
							
					}
				}
			}
		}
		
		// get feasibility flag
		return feasible;
	}
	
	/**
	 * 
	 * @param goal
	 * @param decision
	 * @return
	 */
	private boolean isTemporalUnificationFeasible(Decision goal,  Decision decision) 
	{
		// feasibility flag
		boolean feasible = true;
		// list of translated relations
		Set<Relation> translated = new HashSet<>();
		// list of committed relations
		Set<Relation> committed = new HashSet<>();
		
		// get goal component 
		DomainComponent gComp = goal.getComponent();
		// get all (pending) relations associated to the goal 
		Set<Relation> pRels = gComp.getRelations(goal);
		// translate relations
		for (Relation pRel : pRels)
		{
			// focus on temporal relations only 
			if (pRel.getCategory().equals(ConstraintCategory.TEMPORAL_CONSTRAINT))
			{
				// check relation reference
				if (pRel.getReference().equals(goal)) {
					// replace reference
					pRel.setReference(decision);
					// add translated relation
					translated.add(pRel);
				}
				
				// check relation target
				if (pRel.getTarget().equals(goal)) {
					// replace target
					pRel.setTarget(decision);
					// add translated relation
					translated.add(pRel);
				}
			}
		}
	
		try
		{
			// check translated relation and activate them if possible
			for (Relation tRel : translated) {
				// check if can be activated
				if (tRel.canBeActivated()) {
					// activate relation
					tRel.getReference().getComponent().activate(tRel);
					// add relation to committed list
					committed.add(tRel);
				}
			}
			
			// check temporal consistency after activated relations
			this.tdb.verify();
		}
		catch (ConsistencyCheckException | RelationPropagationException ex) {
			// not feasible unification 
			feasible = false;
			// not feasible unification
			debug("Not feasible temporal unification:\n"
					+ "- planning goal: " + goal + "\n"
					+ "- unification decision: " + decision + "\n");
		}
		finally 
		{
			// deactivate relations
			for (Relation rel : committed) {
				// deactivate relation
				rel.getReference().getComponent().deactivate(rel);
			}
			
			// translate back relations
			for (Relation rel : translated) {
				// check reference 
				if (rel.getReference().equals(decision)) {
					// replace reference
					rel.setReference(goal);
				}
				
				// check target
				if (rel.getTarget().equals(decision)) {
					// replace target
					rel.setTarget(goal);
				}
			}
		}
		
		// get feasibility flag
		return feasible;
	}
	
	/**
	 * 
	 * @param goal
	 */
	private void doComputeExpansionSolutions(Goal goal) 
	{
		// check rules
		List<SynchronizationRule> rules = this.component.getSynchronizationRules(goal.getDecision().getValue());
		if (rules.isEmpty()) 
		{
			// the goal can be justified without applying rules
			GoalExpansion expansion = new GoalExpansion(goal, this.expansionCost);

			// add solution
			goal.addSolution(expansion);
			// print debug message
			debug("Simple goal found no synchronization is triggered after expansion:\n"
					+ "- planning goal: " + goal.getDecision() + "\n");
		}
		else 
		{
			// can do expansion
			for (SynchronizationRule rule : rules) 
			{
				// expansion solution
				GoalExpansion expansion = new GoalExpansion(goal, rule, this.expansionCost);
				// add solution
				goal.addSolution(expansion);
				// print debug message
				debug("Complex goal found:\n"
						+ "- planning goal: " + goal.getDecision() + "\n"
						+ "- synchronization rule: " + rule + "\n");
			}
		}
	}
	
	/**
	 * 
	 * @param unification
	 * @throws Exception
	 */
	private void doApplyUnification(GoalUnification unification) 
			throws FlawSolutionApplicationException 
	{
		// get original goal
		Decision goal = unification.getGoalDecision();
		// get all (pending) relations concerning the planning goal
		DomainComponent goalComp = goal.getComponent();

		// list of committed parameter constraints
		Set<Relation> committed = new HashSet<>();
		// list of translated parameter relations - reference
		Set<Relation> translatedReferenceGoalRelations = new HashSet<>();
		// list of translated parameter relations - target
		Set<Relation> translatedTargetGoalRelations = new HashSet<>();
		
		
		// get pending relations associated to the goal
		Set<Relation> gRels = goalComp.getRelations(goal);
		// translate pending relations by replacing goal's information with unification decision's information
		for (Relation rel : gRels)
		{
			// check relation category
			if (rel.getCategory().equals(ConstraintCategory.TEMPORAL_CONSTRAINT)) 
			{
				// check relation reference
				if (rel.getReference().equals(goal)) {
					// replace reference 
					rel.setReference(unification.getUnificationDecision());
					// add relation to the list
					translatedReferenceGoalRelations.add(rel);
				}

				// check relation target
				if (rel.getTarget().equals(goal)) {
					// replace target
					rel.setTarget(unification.getUnificationDecision());
					// add relation to the list
					translatedTargetGoalRelations.add(rel);
				}
				
			}
			
			if (rel.getCategory().equals(ConstraintCategory.PARAMETER_CONSTRAINT))
			{
				// check relation type
				switch (rel.getType())
				{
					// bind parameter
					case BIND_PARAMETER: 
					{
						// the goal can be only the reference of the relation
						ParameterRelation pRel = (ParameterRelation) rel;
						
						// get relation reference parameter label
						String refParamLabel = pRel.getReferenceParameterLabel();
						// get label index
						int refParameterIndex = pRel.getReference().getParameterIndexByLabel(refParamLabel);
						// get unification decision parameter label
						String label = unification.getUnificationDecision().getParameterLabelByIndex(refParameterIndex);

						// update reference decision 
						pRel.setReference(unification.getUnificationDecision());
						// update reference label of the relation 
						pRel.setReferenceParameterLabel(label);
						// add relation to the list of translated ones
						translatedReferenceGoalRelations.add(pRel);
					}
					break;
					
					case EQUAL_PARAMETER : 
					{
						// get parameter relation
						EqualParameterRelation eqRel = (EqualParameterRelation) rel;
						// check if the goal is the reference or the parameter constraint 
						if (eqRel.getReference().equals(goal))
						{
							// get relation reference parameter label
							String refParamLabel = eqRel.getReferenceParameterLabel();
							// get label index
							int refParameterIndex = eqRel.getReference().getParameterIndexByLabel(refParamLabel);
							// get unification decision parameter label
							String label = unification.getUnificationDecision().getParameterLabelByIndex(refParameterIndex);

							// update reference decision 
							eqRel.setReference(unification.getUnificationDecision());
							// update reference label of the relation 
							eqRel.setReferenceParameterLabel(label);
							// add relation to the list of translated ones
							translatedReferenceGoalRelations.add(eqRel);
						}
						else // the goal is the target of the relation 
						{
							// get relation reference parameter label
							String refParamLabel = eqRel.getTargetParameterLabel();
							// get label index
							int refParameterIndex = eqRel.getTarget().getParameterIndexByLabel(refParamLabel);
							// get unification decision parameter label
							String label = unification.getUnificationDecision().getParameterLabelByIndex(refParameterIndex);

							// update reference decision 
							eqRel.setTarget(unification.getUnificationDecision());
							// update reference label of the relation 
							eqRel.setTargetParameterLabel(label);
							// add relation to the list of translated ones
							translatedTargetGoalRelations.add(eqRel);
						}
					}
					break;
					
					case NOT_EQUAL_PARAMETER : 
					{
						// get parameter relation
						NotEqualParameterRelation neqRel = (NotEqualParameterRelation) rel;
						// check if the goal is the reference or the parameter constraint 
						if (neqRel.getReference().equals(goal))
						{
							// get relation reference parameter label
							String refParamLabel = neqRel.getReferenceParameterLabel();
							// get label index
							int refParameterIndex = neqRel.getReference().getParameterIndexByLabel(refParamLabel);
							// get unification decision parameter label
							String label = unification.getUnificationDecision().getParameterLabelByIndex(refParameterIndex);

							// update reference decision 
							neqRel.setReference(unification.getUnificationDecision());
							// update reference label of the relation 
							neqRel.setReferenceParameterLabel(label);
							// add relation to the list of translated ones
							translatedReferenceGoalRelations.add(neqRel);
						}
						else // the goal is the target of the relation 
						{
							// get relation reference parameter label
							String refParamLabel = neqRel.getTargetParameterLabel();
							// get label index
							int refParameterIndex = neqRel.getTarget().getParameterIndexByLabel(refParamLabel);
							// get unification decision parameter label
							String label = unification.getUnificationDecision().getParameterLabelByIndex(refParameterIndex);

							// update reference decision 
							neqRel.setTarget(unification.getUnificationDecision());
							// update reference label of the relation 
							neqRel.setTargetParameterLabel(label);
							// add relation to the list of translated ones
							translatedTargetGoalRelations.add(neqRel);
						}
					}
					break;
					
					
					default:
						// unknown parameter relation
						throw new RuntimeException("Unknown Parameter relation type : " + rel.getType() + "\n");
				}
			}
		}
		
		
		try	
		{
			// remove original goal: PENDING -> SILENT
			goalComp.free(goal);
			
			// activate translated relations
			for (Relation rel : translatedReferenceGoalRelations) 
			{
				// check if can be activated
				if (rel.canBeActivated()) {
					// activate relation
					rel.getReference().getComponent().activate(rel);
					
					// add activated relations
					unification.addActivatedRelation(rel);
					// add relation to the committed list
					committed.add(rel);
				}
			}
			
			// activate translated relations
			for (Relation rel : translatedTargetGoalRelations) 
			{
				// check if can be activated
				if (rel.canBeActivated()) {
					// activate relation
					rel.getReference().getComponent().activate(rel);
					
					// add activated relations
					unification.addActivatedRelation(rel);
					// add relation to the committed list
					committed.add(rel);
				}
			}

			// set translated relations
			unification.setTranslatedReferenceGoalRelation(translatedReferenceGoalRelations);
			unification.setTranslatedTargetGoalRealtion(translatedTargetGoalRelations);
		}
		catch (RelationPropagationException ex) 
		{
			// restore goal: SILENT -> PENDING
			goalComp.restore(goal);
			
			
			// deactivate committed relations
			for (Relation rel : committed) {
				// get reference component
				DomainComponent refComp = rel.getReference().getComponent();
				refComp.deactivate(rel);
			}
			
			
			// translated back relations
			for (Relation rel : translatedReferenceGoalRelations)
			{
				// check category
				if (rel.getCategory().equals(ConstraintCategory.PARAMETER_CONSTRAINT)) 
				{
					// get parameter relation
					ParameterRelation pRel = (ParameterRelation) rel;
					
					// get relation reference parameter label
					String refParamLabel = pRel.getReferenceParameterLabel();
					// get label index
					int pIndex = pRel.getReference().getParameterIndexByLabel(refParamLabel);
					// get goal decision parameter label
					String label = goal.getParameterLabelByIndex(pIndex);
					
					// update relation
					pRel.setReference(goal);
					pRel.setReferenceParameterLabel(label);
				}
				
				if (rel.getCategory().equals(ConstraintCategory.TEMPORAL_CONSTRAINT)) {
					// update relation
					rel.setReference(goal);
				}
			}
			
			// translated back parameter relations
			for (Relation rel : translatedTargetGoalRelations)
			{
				// check relation category 
				if (rel.getCategory().equals(ConstraintCategory.PARAMETER_CONSTRAINT))
				{
					// check relation
					switch (rel.getType())
					{
						case EQUAL_PARAMETER : 
						{
							// get equal relation
							EqualParameterRelation eqRel = (EqualParameterRelation) rel;
							// get relation reference parameter label
							String tarParamLabel = eqRel.getTargetParameterLabel();
							// get label index
							int pIndex = eqRel.getTarget().getParameterIndexByLabel(tarParamLabel);
							// get goal decision parameter label
							String label = goal.getParameterLabelByIndex(pIndex);
							
							// update relation
							eqRel.setTarget(goal);
							eqRel.setTargetParameterLabel(label);
						}
						break;
							
						case NOT_EQUAL_PARAMETER : 
						{
							// get equal relation
							NotEqualParameterRelation neqRel = (NotEqualParameterRelation) rel;
							// get relation reference parameter label
							String tarParamLabel = neqRel.getTargetParameterLabel();
							// get label index
							int pIndex = neqRel.getTarget().getParameterIndexByLabel(tarParamLabel);
							// get goal decision parameter label
							String label = goal.getParameterLabelByIndex(pIndex);
							
							// update relation
							neqRel.setTarget(goal);
							neqRel.setTargetParameterLabel(label);
						}
						break;
						
						default:
							// unknown parameter relation
							throw new RuntimeException("Unknown Parameter relation type : " + rel.getType() + "\n"); 
							
					}
				}
				
				// check temporal relation
				if (rel.getCategory().equals(ConstraintCategory.TEMPORAL_CONSTRAINT))
				{
					// update relation
					rel.setTarget(goal);
				}
			}

			// not feasible solution
			throw new FlawSolutionApplicationException(ex.getMessage());
		}
	}
	
	/**
	 * 
	 * @param expansion
	 * @throws Exception
	 */
	private void doApplyExpansion(GoalExpansion expansion) 
			throws FlawSolutionApplicationException 
	{
		// get goal
		Decision goal = expansion.getGoalDecision();
		// check subgoals from selected synchronization rule if any
		if (expansion.hasSubGoals()) 
		{
			// get the rule to apply
			SynchronizationRule rule = expansion.getSynchronizationRule();
			
			// create an index of token variable
			Map<TokenVariable, Decision> var2dec = new HashMap<>();
			var2dec.put(rule.getTriggerer(), goal);
			// add pending decisions
			for (TokenVariable var : rule.getTokenVariables()) 
			{
				// get target component
				DomainComponent target = var.getValue().getComponent(); 
				// create a pending decision
				Decision pending = target.create(
						var.getValue(),
						var.getParameterLabels());
				
				// set causal link
				pending.setCausalLink(goal);
				// check solving knowledge
				if (var.isMandatoryExpansion()) {
					pending.setMandatoryExpansion();
				}

				// check solving knowledge
				if (var.isMandatoryUnificaiton()) {
					pending.setMandatoryUnification();
				}
				
				// add entry to cache
				var2dec.put(var, pending);
				// add created decision
				expansion.addCreatedDecision(pending);
			}
			
			// add pending relations
			for (SynchronizationConstraint c : rule.getConstraints()) 
			{
				// check category
				switch (c.getCategory()) 
				{
					// temporal category
					case TEMPORAL_CONSTRAINT : 
					{ 
						// temporal constraint
						TemporalSynchronizationConstraint tc = (TemporalSynchronizationConstraint) c;
						// get decisions
						Decision reference = var2dec.get(tc.getReference());
						Decision target = var2dec.get(tc.getTarget());
						// get reference component
						DomainComponent refComp = reference.getComponent();
						// create pending relation
						TemporalRelation rel = refComp.create(tc.getType(), reference, target);
						// set bounds
						rel.setBounds(tc.getBounds());
						// add created relation
						expansion.addCreatedRelation(rel);
					}
					break;
					
					// parameter category
					case PARAMETER_CONSTRAINT: 
					{
						// parameter constraint
						ParameterSynchronizationConstraint pc = (ParameterSynchronizationConstraint) c;
						// get decisions
						Decision reference = var2dec.get(pc.getReference());
						Decision target = var2dec.get(pc.getTarget());
						// get reference component
						DomainComponent refComp = reference.getComponent();
						// create pending relation
						ParameterRelation rel = (ParameterRelation) refComp.create(pc.getType(), reference, target);
						
						// check parameter relation type
						switch (rel.getType())
						{
							// bind parameter relation
							case BIND_PARAMETER : 
							{
								// bind constraint
								BindParameterRelation bind = (BindParameterRelation) rel;
								// set binding value
								bind.setValue(pc.getTargetLabel());
								// set reference label
								if (pc.getReference().equals(rule.getTriggerer())) 
								{
									// get trigger label index
									int index = rule.getTriggerer().getParameterIndexByLabel(pc.getReferenceLabel());
									// set decision's label
									String label = goal.getParameterLabelByIndex(index);
									// set label
									bind.setReferenceParameterLabel(label);
								}
								else {
									bind.setReferenceParameterLabel(pc.getReferenceLabel());
								}
							}
							break;
							
							// equal parameter relation
							case EQUAL_PARAMETER : 
							{
								// get relation
								EqualParameterRelation eq = (EqualParameterRelation) rel;
								
								// check if source is the trigger
								if (pc.getReference().equals(rule.getTriggerer())) 
								{
									// get trigger label index
									int index = rule.getTriggerer().getParameterIndexByLabel(pc.getReferenceLabel());
									// get decions's label
									String label = goal.getParameterLabelByIndex(index);
									// set label
									eq.setReferenceParameterLabel(label);
								}
								else {
									// directly set the label
									eq.setReferenceParameterLabel(pc.getReferenceLabel());
								}
								
								// check if target is the trigger
								if (pc.getTarget().equals(rule.getTriggerer())) 
								{
									// get trigger label index
									int index = rule.getTriggerer().getParameterIndexByLabel(pc.getTargetLabel());
									// get decision's label
									String label = goal.getParameterLabelByIndex(index);
									// set label
									eq.setTargetParameterLabel(label);
								}
								else {
									// directly set the label
									eq.setTargetParameterLabel(pc.getTargetLabel());
								}
							}
							break;
							
							// not-equal parameter relation
							case NOT_EQUAL_PARAMETER : 
							{
								// get relation
								NotEqualParameterRelation neq = (NotEqualParameterRelation) rel;
								
								// check if source is the trigger
								if (pc.getReference().equals(rule.getTriggerer())) 
								{
									// get trigger label index
									int index = rule.getTriggerer().getParameterIndexByLabel(pc.getReferenceLabel());
									// get decions's label
									String label = goal.getParameterLabelByIndex(index);
									// set label
									neq.setReferenceParameterLabel(label);
								}
								else {
									// directly set the label
									neq.setReferenceParameterLabel(pc.getReferenceLabel());
								}
								
								// check if target is the trigger
								if (pc.getTarget().equals(rule.getTriggerer())) 
								{
									// get trigger label index
									int index = rule.getTriggerer().getParameterIndexByLabel(pc.getTargetLabel());
									// get decision's label
									String label = goal.getParameterLabelByIndex(index);
									// set label
									neq.setTargetParameterLabel(label);
								}
								else {
									// directly set the label
									neq.setTargetParameterLabel(pc.getTargetLabel());
								}
							}
							break;
							
							default : {
								throw new RuntimeException("Unknown parameter constraint type - " + rel.getType());
							}
						}
						
						// add created relation
						expansion.addCreatedRelation(rel);
					}
				}
			}
		}
		
		try
		{
			// activate goal decision 
			DomainComponent goalComp = goal.getComponent();
			// get goal-related activated relations
			Set<Relation> list = goalComp.activate(goal);
			// add goal to activated decisions
			expansion.addActivatedDecision(goal);
			// add to activated relations
			expansion.addActivatedRelations(list);
		}
		catch (DecisionPropagationException ex) 
		{
			// deactivate activated relations
			for (Relation rel : expansion.getActivatedRelations()) {
				// get reference component
				DomainComponent refComp = rel.getReference().getComponent();
				refComp.deactivate(rel);
			}
			
			// delete created relations
			for (Relation rel : expansion.getCreatedRelations()) {
				// get reference component
				DomainComponent refComp = rel.getReference().getComponent();
				refComp.delete(rel);
			}

			
			
			// deactivate activated decisions
			for (Decision dec : expansion.getActivatedDecisions()) {
				// get component
				DomainComponent refComp = dec.getComponent();
				refComp.deactivate(dec);
			}
			
			
			// delete created decisions 
			for (Decision dec : expansion.getCreatedDecisions()) {
				// get component
				DomainComponent comp = dec.getComponent();
				comp.free(dec);
			}
			
			// throw exception
			throw new FlawSolutionApplicationException(ex.getMessage());
		}
	}

	/**
	 * 
	 * @param unification
	 */
	private void doRetractUnification(GoalUnification unification) 
	{
		// original goal 
		Decision goal = unification.getGoalDecision();
		
		// get goal component
		DomainComponent goalComp = goal.getComponent();
		// restore original planning goal SILENT -> PENDING
		goalComp.restore(goal);

		// deactivate activated relations
		for (Relation rel : unification.getActivatedRelations()) {
			// get reference component
			DomainComponent refComp = rel.getReference().getComponent();
			refComp.deactivate(rel);
		}
		
		// translated back relations
		for (Relation rel : unification.getTranslatedReferenceGoalRelations())
		{
			// check category
			if (rel.getCategory().equals(ConstraintCategory.PARAMETER_CONSTRAINT)) 
			{
				// get parameter relation
				ParameterRelation pRel = (ParameterRelation) rel;
				
				// get relation reference parameter label
				String refParamLabel = pRel.getReferenceParameterLabel();
				// get label index
				int pIndex = pRel.getReference().getParameterIndexByLabel(refParamLabel);
				// get goal decision parameter label
				String label = goal.getParameterLabelByIndex(pIndex);
				
				// update relation
				pRel.setReference(goal);
				pRel.setReferenceParameterLabel(label);
			}
			
			if (rel.getCategory().equals(ConstraintCategory.TEMPORAL_CONSTRAINT)) {
				// update relation
				rel.setReference(goal);
			}
		}
		
		
		// translated back parameter relations
		for (Relation rel : unification.getTranslatedTargetGoalRelations())
		{
			// check relation category 
			if (rel.getCategory().equals(ConstraintCategory.PARAMETER_CONSTRAINT))
			{
				// check relation
				switch (rel.getType())
				{
					case EQUAL_PARAMETER : 
					{
						// get equal relation
						EqualParameterRelation eqRel = (EqualParameterRelation) rel;
						// get relation reference parameter label
						String tarParamLabel = eqRel.getTargetParameterLabel();
						// get label index
						int pIndex = eqRel.getTarget().getParameterIndexByLabel(tarParamLabel);
						// get goal decision parameter label
						String label = goal.getParameterLabelByIndex(pIndex);
						
						// update relation
						eqRel.setTarget(goal);
						eqRel.setTargetParameterLabel(label);
					}
					break;
						
					case NOT_EQUAL_PARAMETER : 
					{
						// get equal relation
						NotEqualParameterRelation neqRel = (NotEqualParameterRelation) rel;
						// get relation reference parameter label
						String tarParamLabel = neqRel.getTargetParameterLabel();
						// get label index
						int pIndex = neqRel.getTarget().getParameterIndexByLabel(tarParamLabel);
						// get goal decision parameter label
						String label = goal.getParameterLabelByIndex(pIndex);
						
						// update relation
						neqRel.setTarget(goal);
						neqRel.setTargetParameterLabel(label);
					}
					break;
					
					default:
						// unknown parameter relation
						throw new RuntimeException("Unknown Parameter relation type : " + rel.getType() + "\n"); 
						
				}
			}
			
			// check temporal relation
			if (rel.getCategory().equals(ConstraintCategory.TEMPORAL_CONSTRAINT))
			{
				// update relation
				rel.setTarget(goal);
			}
		}
	}
}
