package unit731.hunlinter.services.text;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.services.RegexHelper;
import unit731.hunlinter.services.system.LoopHelper;


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
	/** must be GAP_OPENING_PENALTY < 0 */
	private static final double GAP_OPENING_PENALTY = -1.;
	/** must be GAP_OPENING_PENALTY < GAP_EXTENSION_PENALTY < 0*/
	private static final double GAP_EXTENSION_PENALTY = -1. / 3.;


	//TODO introduce getters
	public static class Trace{
		private int firstIndexA;
		private int firstIndexB;
		private int lastIndexA;
		private int lastIndexB;
		private Deque<Character> operations;
	}


	private final String[] x;
	private final int n;
	private final String[] y;
	private final int m;
	private final double[][] scores;


	public SmithWatermanAlignment(String a, String b){
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

	public Set<Trace> align(){
		//calculate scores:
		double maxScore = 0.;
		for(int j = 1; j <= m; j ++)
			for(int i = 1; i <= n; i ++){
				double m1 = Math.max(0, matching(i, j));
				double m2 = Math.max(insertion(i, j), deletion(i, j));
				scores[i][j] = Math.max(m1, m2);

				maxScore = Math.max(maxScore, scores[i][j]);
			}

		Set<Trace> traces = new HashSet<>();
		Stack<Pair<Integer, Integer>> maxScoreIndices = extractMaxScoreIndices(maxScore);
		//extract edit operations
		LoopHelper.forEach(maxScoreIndices, score -> traces.add(traceback(score.getLeft(), score.getRight())));
		return traces;
	}

	private double matching(int i, int j){
		return scores[i - 1][j - 1] + matchingCost(i, j);
	}

	private double insertion(int i, int j){
		int highest = 0;
		for(int k = 1; k < j; k ++)
			if(scores[i][k] > scores[i][highest])
				highest = k;
		return scores[i][highest] + insertionCost(j - highest);
	}

	private double deletion(int i, int j){
		int highest = 0;
		for(int k = 1; k < i; k ++)
			if(scores[k][j] > scores[highest][j])
				highest = k;
		return scores[highest][j] + deletionCost(i - highest);
	}

	private double matchingCost(int i, int j){
		if(i == 0 || j == 0)
			return COST_GAP;
		return (x[i - 1].equals(y[j - 1])? COST_MATCH: COST_MISMATCH);
	}

	private double insertionCost(double k){
		return GAP_OPENING_PENALTY + GAP_EXTENSION_PENALTY * (k - 1);
	}

	private double deletionCost(double k){
		return GAP_OPENING_PENALTY + GAP_EXTENSION_PENALTY * (k - 1);
	}

	private Stack<Pair<Integer, Integer>> extractMaxScoreIndices(double maxScore){
		//collect max scores:
		Stack<Pair<Integer, Integer>> maxScores = new Stack<>();
		for(int j = 1; j <= m; j ++)
			for(int i = 1; i <= n; i ++)
				if(scores[i][j] == maxScore)
					maxScores.push(Pair.of(i, j));
		return maxScores;
	}

	private Trace traceback(int lastIndexA, int lastIndexB){
		Trace trace = new Trace();
		trace.lastIndexA = lastIndexA - 1;
		trace.lastIndexB = lastIndexB - 1;
		trace.operations = new ArrayDeque<>();

		//backward reconstruct path
		while(lastIndexA != 0 || lastIndexB != 0){
			double tmp = scores[lastIndexA][lastIndexB];
			if(tmp == 0.)
				break;

			trace.firstIndexA = lastIndexA - 1;
			trace.firstIndexB = lastIndexB - 1;

			if(lastIndexA != 0 && lastIndexB != 0 && tmp == matching(lastIndexA, lastIndexB)){
				trace.operations.push(x[lastIndexA - 1].equals(y[lastIndexB - 1])? ' ': '*');
				lastIndexA --;
				lastIndexB --;
			}
			else if(lastIndexA != 0 && tmp == deletion(lastIndexA, lastIndexB)){
				trace.operations.push('-');
				lastIndexA --;
			}
			else if(lastIndexB != 0 && tmp == insertion(lastIndexA, lastIndexB)){
				trace.operations.push('+');
				lastIndexB --;
			}
		}

		return trace;
	}

}
