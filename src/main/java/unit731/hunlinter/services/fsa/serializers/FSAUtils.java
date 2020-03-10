package unit731.hunlinter.services.fsa.serializers;

import com.carrotsearch.hppc.IntIntHashMap;
import unit731.hunlinter.services.fsa.FSA;


/** Other FSA-related utilities not directly associated with the class hierarchy */
public class FSAUtils{

	public final static class IntIntHolder{
		public int a;
		public int b;

		public IntIntHolder(int a, int b){
			this.a = a;
			this.b = b;
		}

		public IntIntHolder(){
		}
	}

	/**
	 * Calculate the size of "right language" for each state in an FSA. The right
	 * language is the number of sequences encoded from a given node in the automaton.
	 *
	 * @param fsa The automaton to calculate right language for.
	 * @return Returns a map with node identifiers as keys and their right language
	 * counts as associated values.
	 */
	public static IntIntHashMap rightLanguageForAllStates(final FSA fsa){
		final IntIntHashMap numbers = new IntIntHashMap();

		fsa.visitInPostOrder(state -> {
			int thisNodeNumber = 0;
			for(int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc))
				thisNodeNumber += (fsa.isArcFinal(arc)? 1: 0) + (fsa.isArcTerminal(arc)? 0: numbers.get(fsa.getEndNode(arc)));
			numbers.put(state, thisNodeNumber);

			return true;
		});

		return numbers;
	}

}
