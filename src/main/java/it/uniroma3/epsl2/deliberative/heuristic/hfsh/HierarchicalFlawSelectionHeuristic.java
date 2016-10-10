package it.uniroma3.epsl2.deliberative.heuristic.hfsh;

import it.uniroma3.epsl2.deliberative.heuristic.FlawSelectionHeuristic;
import it.uniroma3.epsl2.deliberative.heuristic.FlawSelectionHeuristicType;
import it.uniroma3.epsl2.deliberative.heuristic.filter.FlawFilterType;
import it.uniroma3.epsl2.framework.microkernel.annotation.planner.cfg.FlawSelectionHeuristicConfiguration;

/**
 * 
 * @author anacleto
 *
 */
@FlawSelectionHeuristicConfiguration(
	// set pipeline of filters
	pipeline = {
		FlawFilterType.HFF,			// hierarchy-based flaw filter
		FlawFilterType.TFF,			// type-based flaw filter
		FlawFilterType.FFF			// fail-first flaw filter
	}
)
public class HierarchicalFlawSelectionHeuristic extends FlawSelectionHeuristic {

	/**
	 * 
	 */
	protected HierarchicalFlawSelectionHeuristic() {
		super(FlawSelectionHeuristicType.HFSH);
	}
}