package io.github.mtrevisan.hunlinter;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * Simulated Annealing solver for K-word alphabet cover.
 * <p>
 * Optimizations:
 * - Bitmask representation
 * - Incremental cost updates
 * - Weighted random greedy initialization
 * - Feasibility-preserving moves
 * - Restart strategy
 */
public class Main4{

	// ===== CONFIGURATION =====
	private static final int K = 5;
	private static final String WORDS_FILE = "words.txt";
	private static final String SOLUTIONS_FILE = "solutions_sa" + K + ".txt";
	private static final double THRESHOLD = 0.28;

	private static final int MAX_ITER = 200_000_000;
	private static final double T0 = 10.0;
	private static final double ALPHA = 0.9995;
	private static final double T_MIN = 1e-4;

	// Alphabet
	private static final List<Character> ALPHABET = List.of(
		'C', 'Đ', 'Ñ', 'J', 'B', 'Ŧ', 'Ò', 'F', 'G', 'X', 'È',
		'U', 'M', 'V', 'P', 'D', 'K', 'S', 'L', 'T', 'R', 'N', 'I', 'O', 'A', 'E'
	);

	private static final int ALPHABET_SIZE = ALPHABET.size();
	private static final long FULL_MASK = (1L << ALPHABET_SIZE) - 1;

	private static final int[] CHAR_BIT = new int[0x1_0000];

	static{
		Arrays.fill(CHAR_BIT, -1);
		for(int i = 0; i < ALPHABET_SIZE; i++)
			CHAR_BIT[Character.toLowerCase(ALPHABET.get(i))] = i;
	}

	private static List<String> words;
	private static long[] wordMasks;
	private static int[] wordLengths;

	private static BufferedWriter writer;

	// ===== MAIN =====

	public static void main(String[] args) throws Exception{
		loadWords();

		writer = new BufferedWriter(new FileWriter(SOLUTIONS_FILE, true));

		int[] best = simulatedAnnealing();

		writeSolution(best, computeLen(best));

		writer.close();
	}

	// ===== SIMULATED ANNEALING =====

	private static int[] simulatedAnnealing(){
		Random rnd = new Random();

		int[] sol = randomGreedySolution(rnd);
		int len = computeLen(sol);

		int[] bestSol = sol.clone();
		int bestLen = len;

		double T = T0;

		for(int iter = 0; iter < MAX_ITER && T > T_MIN; iter++){

			// restart
			if(iter % 200_000 == 0 && iter > 0){
				sol = randomGreedySolution(rnd);
				len = computeLen(sol);
			}

			int pos = rnd.nextInt(K);
			int oldWord = sol[pos];

			// build mask excluding pos
			long mask = 0L;
			for(int i = 0; i < K; i++){
				if(i != pos)
					mask |= wordMasks[sol[i]];
			}

			int candidate = -1;

			// try limited attempts
			for(int t = 0; t < 32; t++){
				int w = rnd.nextInt(words.size());
				long newMask = mask | wordMasks[w];

				if(newMask == mask)
					continue;

				if((newMask | mask) != FULL_MASK)
					continue;

				candidate = w;
				break;
			}

			if(candidate < 0)
				continue;

			// incremental cost
			int newLen = len - wordLengths[oldWord] + wordLengths[candidate];
			int delta = newLen - len;

			if(delta < 0 || rnd.nextDouble() < Math.exp(-delta / T)){
				sol[pos] = candidate;
				len = newLen;

				if(len < bestLen){
					bestLen = len;
					bestSol = sol.clone();
					System.out.println("Improved: " + bestLen);
				}
			}

			T *= ALPHA;
		}

		return bestSol;
	}

	// ===== INITIAL SOLUTION =====

	private static int[] randomGreedySolution(Random rnd){
		int[] sol = new int[K];
		long mask = 0L;

		for(int i = 0; i < K; i++){

			int bestIdx = -1;
			double bestScore = -1;

			// build candidate list (top-N sampling implicit)
			for(int w = 0; w < words.size(); w++){
				long wm = wordMasks[w];
				long newMask = mask | wm;

				if(newMask == mask)
					continue;

				int gain = Long.bitCount(wm & ~mask);

				double score = (gain * gain) / (double)wordLengths[w];

				// stochastic acceptance
				if(score > bestScore || rnd.nextDouble() < 0.1){
					bestScore = score;
					bestIdx = w;
				}
			}

			if(bestIdx < 0)
				bestIdx = rnd.nextInt(words.size());

			sol[i] = bestIdx;
			mask |= wordMasks[bestIdx];
		}

		return sol;
	}

	// ===== UTIL =====

	private static int computeLen(int[] sol){
		int len = 0;
		for(int i = 0; i < K; i++)
			len += wordLengths[sol[i]];
		return len;
	}

	private static void writeSolution(int[] sol, int len){
		try{
			long mask = 0;
			for(int i = 0; i < K; i++)
				mask |= wordMasks[sol[i]];

			if(mask != FULL_MASK)
				return;

			double ratio = (double)len / ALPHABET_SIZE - 1.;
			if(ratio > THRESHOLD)
				return;

			StringJoiner sj = new StringJoiner(" ");
			for(int i = 0; i < K; i++)
				sj.add(words.get(sol[i]));

			String out = String.format(Locale.ROOT, "%.3f", ratio) + ": " + sj;

			writer.write(out);
			writer.newLine();
			writer.flush();

			System.out.println(out);
		}
		catch(IOException e){
			throw new UncheckedIOException(e);
		}
	}

	// ===== LOADING =====

	private static void loadWords() throws Exception{
		InputStream is = Main4.class.getResourceAsStream("/" + WORDS_FILE);
		if(is == null)
			throw new IllegalStateException("words.txt not found");

		Set<String> set = new LinkedHashSet<>();
		try(BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){
			String line;
			while((line = br.readLine()) != null){
				if(!line.isBlank())
					set.add(line.trim());
			}
		}

		words = new ArrayList<>(set);

		wordMasks = new long[words.size()];
		wordLengths = new int[words.size()];

		for(int i = 0; i < words.size(); i++){
			wordMasks[i] = buildMask(words.get(i));
			wordLengths[i] = words.get(i).length();
		}

		System.out.printf("Loaded %,d words%n", words.size());
	}

	private static long buildMask(String word){
		long mask = 0;
		for(int i = 0; i < word.length(); i++){
			char c = Character.toLowerCase(word.charAt(i));
			c = switch(c){
				case 'à' -> 'a';
				case 'é' -> 'e';
				case 'í', 'ï' -> 'i';
				case 'ó' -> 'o';
				case 'ú', 'ü' -> 'u';
				default -> c;
			};
			int bit = CHAR_BIT[c];
			if(bit >= 0)
				mask |= (1L << bit);
		}
		return mask;
	}

}
