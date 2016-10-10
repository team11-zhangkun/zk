package it.uniroma3.epsl2.framework.domain.component.sv;

import java.util.ArrayList;
import java.util.List;

import it.uniroma3.epsl2.framework.domain.component.DomainComponentType;
import it.uniroma3.epsl2.framework.lang.plan.Decision;
import it.uniroma3.epsl2.framework.microkernel.annotation.framework.cfg.DomainComponentConfiguration;
import it.uniroma3.epsl2.framework.microkernel.query.TemporalQueryType;
import it.uniroma3.epsl2.framework.microkernel.resolver.ResolverType;
import it.uniroma3.epsl2.framework.time.lang.query.CheckPseudoControllabilityQuery;
import it.uniroma3.epsl2.framework.time.tn.stnu.ex.PseudoControllabilityCheckException;

/**
 * 
 * @author anacleto
 *
 */
@DomainComponentConfiguration(
		
		// set resolvers to manage external variables
		resolvers = {
				
				ResolverType.OBSERVATION_CHECKING_RESOLVER,
				
				ResolverType.BEHAVIOR_CHECKING_RESOLVER
	}
)
public final class ExternalStateVariable extends StateVariable {
	
	/**
	 * 
	 * @param name
	 * @param tdb
	 */
	protected ExternalStateVariable(String name) {
		super(name, DomainComponentType.SV_EXTERNAL);
	}

	/**
	 * 
	 */
	@Override
	public boolean isExternal() {
		// external component
		return true;
	}
	
	/**
	 * 
	 * @param value
	 * @param duration
	 * @param controllable
	 * @return
	 */
	@Override
	protected StateVariableValue doCreateValue(String value, long[] duration, boolean controllable) {
		// create and add value
		StateVariableValue v = new StateVariableValue(value, duration, false, this);
		this.values.add(v);
		return v;
	}
	
	/**
	 * 
	 */
	@Override
	public void checkPseudoControllability() 
			throws PseudoControllabilityCheckException 
	{
		// list of squeezed values
		List<Decision> issues = new ArrayList<>();
		// check every value of the state variable
		for (Decision dec : this.getActiveDecisions()) {
			// create query
			CheckPseudoControllabilityQuery query = this.queryFactory.
					create(TemporalQueryType.CHECK_PSEUDO_CONTROLLABILITY);
			
			// set related temporal interval
			query.setInterval(dec.getToken().getInterval());
			// process query
			this.tdb.process(query);
			// check
			if (!query.isPseudoControllable()) {
				// add issue
				issues.add(dec);
			}
		}
		
		// check issues
		if (!issues.isEmpty()) {
			// create exception
			PseudoControllabilityCheckException ex = new PseudoControllabilityCheckException("Controllability issues found on component " + this.name);
			// add issues
			for (Decision issue : issues) {
				ex.addIssue(issue);
			}
			// throw exception
			throw ex;
		}
	}
}