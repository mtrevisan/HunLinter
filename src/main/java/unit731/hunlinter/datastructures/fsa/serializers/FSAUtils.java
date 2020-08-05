/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.datastructures.fsa.serializers;

import com.carrotsearch.hppc.IntIntHashMap;
import unit731.hunlinter.datastructures.fsa.FSA;


/**
 * Other FSA-related utilities not directly associated with the class hierarchy
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class FSAUtils{

	private FSAUtils(){}

	/**
	 * Calculate the size of "right language" for each state in an FSA.
	 * The right language is the number of sequences encoded from a given node in the automaton.
	 *
	 * @param fsa	The automaton to calculate right language for
	 * @return	A map with node identifiers as keys and their right language counts as associated values
	 */
	public static IntIntHashMap rightLanguageForAllStates(final FSA fsa){
		final IntIntHashMap numbers = new IntIntHashMap();
		fsa.visitPostOrder(state -> {
			int thisNodeNumber = 0;
			for(int arc = fsa.getFirstArc(state); arc != 0; arc = fsa.getNextArc(arc))
				thisNodeNumber += (fsa.isArcFinal(arc)? 1: 0)
					+ (fsa.isArcTerminal(arc)? 0: numbers.get(fsa.getEndNode(arc)));
			numbers.put(state, thisNodeNumber);

			return true;
		});
		return numbers;
	}

	/* Returns an n-byte integer encoded in byte-packed representation */
	public static int decodeFromBytes(final byte[] arcs, final int start, final int n){
		int r = 0;
		for(int i = n; -- i >= 0; )
			r = r << 8 | (arcs[start + i] & 0xFF);
		return r;
	}

}
