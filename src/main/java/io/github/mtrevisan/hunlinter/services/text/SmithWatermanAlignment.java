/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.services.text;

import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Smith-Waterman local alignment algorithm with gap penalty
 * Runtime complexity: O(n^2 * m), assuming n &gt; m
 * Space complexity: O(n * m)
 *
 * @see <a href="http://www.cs.bgu.ac.il/~michaluz/seminar/Gotoh.pdf">Gotoh</a>
 * @see <a href="http://www.akira.ruc.dk/~keld/teaching/algoritmedesign_f03/Artikler/05/Hirschberg75.pdf">Hirschberg75</a>
 *
 * https://github.com/mkleen/SmithWaterman/blob/master/SmithWaterman.java
 * https://github.com/eshamusi/Smith-Waterman-Local-Alignment/blob/master/SmithWaterman.java
 *
 * Affine Gap algorithm taken from: http://en.wikipedia.org/wiki/Gap_penalty#Affine_gap_penalty
 */
public class SmithWatermanAlignment{

	private static final Pattern UNICODE_SPLITTER = RegexHelper.pattern("(\\[([^\\]]+)\\]|[^\\u0300-\\u036F\\u025A\\u02B0-\\u02FE\\u1DA3\\u207F][\\u0300-\\u035B\\u035D-\\u0360\\u0362-\\u036F\\u025A\\u02B0-\\u02FE\\u1DA3\\u207F]*(?:[\\u0300-\\u036F\\u025A\\u02B0-\\u02FE\\u1DA3\\u207F]*[\\u035C\\u0361][^\\u0300-\\u036F\\u025A\\u02B0-\\u02FE\\u1DA3\\u207F][\\u0300-\\u036F\\u025A\\u02B0-\\u02FE\\u1DA3\\u207F]*)?)");

	private static final double COST_GAP = -1.;
	private static final double COST_MATCH = 2.;
	private static final double COST_MISMATCH = -1.;
	private static final double COST_DELETION = 0.;
	private static final double COST_INSERTION = 0.;
	/** must be GAP_OPENING_PENALTY < 0. */
	private static final double GAP_OPENING_PENALTY = -1.;
	/** must be GAP_OPENING_PENALTY < GAP_EXTENSION_PENALTY < 0. */
	private static final double GAP_EXTENSION_PENALTY = -1. / 3.;

	private static final Character MINUS = '-';
	private static final Character PLUS = '+';
	private static final Character SPACE = ' ';
	private static final Character START = '*';


	//TODO introduce getters
	public static class Trace{
		private int firstIndexA;
		private int firstIndexB;
		private int lastIndexA;
		private int lastIndexB;
		private Deque<Character> operations;

		@Override
		public final boolean equals(final Object obj){
			if(this == obj)
				return true;
			if(obj == null || getClass() != obj.getClass())
				return false;

			final Trace rhs = (Trace)obj;
			return (firstIndexA == rhs.firstIndexA
				&& firstIndexB == rhs.firstIndexB
				&& lastIndexA == rhs.lastIndexA
				&& lastIndexB == rhs.lastIndexB);
		}

		@Override
		public final int hashCode(){
			int result = Integer.hashCode(firstIndexA);
			result = 31 * result + Integer.hashCode(firstIndexB);
			result = 31 * result + Integer.hashCode(lastIndexA);
			result = 31 * result + Integer.hashCode(lastIndexB);
			return result;
		}
	}


	private final String[] x;
	private final int n;
	private final String[] y;
	private final int m;
	private final double[][] scores;


	public SmithWatermanAlignment(final CharSequence a, final CharSequence b){
		x = RegexHelper.split(a, UNICODE_SPLITTER);
		y = RegexHelper.split(b, UNICODE_SPLITTER);

		n = x.length;
		m = y.length;

		//initialize cost matrix:
		scores = new double[n + 1][m + 1];
		for(int i = 0; i <= n; i ++)
			scores[i][0] = i * COST_DELETION;
		for(int j = 0; j <= m; j ++)
			scores[0][j] = j * COST_INSERTION;
	}

	public final Set<Trace> align(){
		//calculate scores:
		double maxScore = 0.;
		for(int j = 1; j <= m; j ++)
			for(int i = 1; i <= n; i ++){
				final double m1 = Math.max(0, matching(i, j));
				final double m2 = Math.max(insertion(i, j), deletion(i, j));
				scores[i][j] = Math.max(m1, m2);

				maxScore = Math.max(maxScore, scores[i][j]);
			}

		final Deque<IndexDataPair<Integer>> maxScoreIndices = extractMaxScoreIndices(maxScore);
		final Set<Trace> traces = new HashSet<>(maxScoreIndices.size());
		//extract edit operations
		for(final IndexDataPair<Integer> score : maxScoreIndices)
			traces.add(traceback(score.getIndex(), score.getData()));
		return traces;
	}

	private double matching(final int i, final int j){
		return scores[i - 1][j - 1] + matchingCost(i, j);
	}

	private double insertion(final int i, final int j){
		int highest = 0;
		for(int k = 1; k < j; k ++)
			if(scores[i][k] > scores[i][highest])
				highest = k;
		return scores[i][highest] + insertionCost(j - highest);
	}

	private double deletion(final int i, final int j){
		int highest = 0;
		for(int k = 1; k < i; k ++)
			if(scores[k][j] > scores[highest][j])
				highest = k;
		return scores[highest][j] + deletionCost(i - highest);
	}

	private double matchingCost(final int i, final int j){
		if(i == 0 || j == 0)
			return COST_GAP;
		return (x[i - 1].equals(y[j - 1])? COST_MATCH: COST_MISMATCH);
	}

	private double insertionCost(final double k){
		return GAP_OPENING_PENALTY + GAP_EXTENSION_PENALTY * (k - 1);
	}

	private double deletionCost(final double k){
		return GAP_OPENING_PENALTY + GAP_EXTENSION_PENALTY * (k - 1);
	}

	private Deque<IndexDataPair<Integer>> extractMaxScoreIndices(final double maxScore){
		//collect max scores:
		final Deque<IndexDataPair<Integer>> maxScores = new ArrayDeque<>();
		for(int j = 1; j <= m; j ++)
			for(int i = 1; i <= n; i ++)
				if(scores[i][j] == maxScore)
					maxScores.push(IndexDataPair.of(i, j));
		return maxScores;
	}

	private Trace traceback(int lastIndexA, int lastIndexB){
		final Trace trace = new Trace();
		trace.lastIndexA = lastIndexA - 1;
		trace.lastIndexB = lastIndexB - 1;
		trace.operations = new ArrayDeque<>();

		//backward reconstruct path
		while(lastIndexA != 0 || lastIndexB != 0){
			final double tmp = scores[lastIndexA][lastIndexB];
			if(tmp == 0.)
				break;

			trace.firstIndexA = lastIndexA - 1;
			trace.firstIndexB = lastIndexB - 1;

			if(lastIndexA != 0 && lastIndexB != 0 && tmp == matching(lastIndexA, lastIndexB)){
				trace.operations.push(x[lastIndexA - 1].equals(y[lastIndexB - 1])? SPACE: START);
				lastIndexA --;
				lastIndexB --;
			}
			else if(lastIndexA != 0 && tmp == deletion(lastIndexA, lastIndexB)){
				trace.operations.push(MINUS);
				lastIndexA --;
			}
			else if(lastIndexB != 0 && tmp == insertion(lastIndexA, lastIndexB)){
				trace.operations.push(PLUS);
				lastIndexB --;
			}
		}

		return trace;
	}

}
