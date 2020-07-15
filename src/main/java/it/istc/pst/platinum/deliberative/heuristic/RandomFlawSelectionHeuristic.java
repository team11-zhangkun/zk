package it.istc.pst.platinum.deliberative.heuristic;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import it.istc.pst.platinum.framework.microkernel.lang.ex.NoFlawFoundException;
import it.istc.pst.platinum.framework.microkernel.lang.flaw.Flaw;
import it.istc.pst.platinum.framework.microkernel.resolver.ex.UnsolvableFlawException;

/**
 * 
 * @author anacleto
 *
 */
public class RandomFlawSelectionHeuristic extends FlawSelectionHeuristic
{
	/**
	 * 
	 */
	protected RandomFlawSelectionHeuristic() {
		super("Heuristics:Random");
	}
	
	/**
	 * 
	 */
	@Override
	public Set<Flaw> choose() 
			throws UnsolvableFlawException, NoFlawFoundException 
	{
		// detect flaws
		List<Flaw> flaws = this.pdb.detectFlaws();
		// check flaws found
		if (flaws.isEmpty()) {
			// throw exception
			throw new NoFlawFoundException("No flaw has been found in the current plan");
		}
		
		// randomly select a flaw to solve
		Random rand = new Random(System.currentTimeMillis());
		int index = rand.nextInt(flaws.size());
		// get randomly selected flaw
		Set<Flaw> set = new HashSet<>();
		set.add(flaws.get(index));
		return set;
	}
}