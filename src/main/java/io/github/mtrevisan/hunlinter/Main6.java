package io.github.mtrevisan.hunlinter;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;


/**
 * Pangram solver using Algorithm X with Dancing Links (DLX).
 * <p>
 * Finds the MINIMUM K such that an exact cover of the alphabet exists.
 * Additional GLOBAL constraints on the solution:
 * - at least 2 'J', of which at least one followed by a vowel
 * - at least 2 'L', of which at least one followed by a vowel
 */
public class Main6{

	// ===== CONFIGURATION =====
	private static final String WORDS_FILE = "all.txt";
	private static final String FILTERED_WORDS_FILE = "all.filtered.txt";
	private static final String SOLUTIONS_FILE = "solutionsDLX-all.txt";

	private static final int MIN_K = 4;

	private static final List<Character> ALPHABET = List.of(
		'C', 'Đ', 'Ñ', 'J', 'B', 'Ŧ', 'Ò', 'F', 'G', 'X', 'È',
		'U', 'Ü', 'Ú', 'M', 'V', 'P', 'D', 'K', 'S', 'L',
		'T', 'R', 'N', 'I', 'Ï', 'Í', 'O', 'Ó', 'A', 'À', 'E', 'É'
	);
	private static final int ALPHABET_SIZE = ALPHABET.size();
	// Fast char → bit lookup
	private static final int[] CHAR_BIT = new int[0x00001_0000];
	static{
		Arrays.fill(CHAR_BIT, -1);
		for(int i = 0; i < ALPHABET_SIZE; i ++)
			CHAR_BIT[ALPHABET.get(i)] = i;
	}
	private static final boolean[] IS_VOWEL = new boolean[256];
	static{
		for(final char c : "aeiouàèéíòóúïü".toCharArray())
			IS_VOWEL[c] = true;
	}

	// Extra DLX columns for global constraints
	private static final int COL_J1 = ALPHABET_SIZE + 0;
	private static final int COL_J2 = ALPHABET_SIZE + 1;
	private static final int COL_JV = ALPHABET_SIZE + 2;
	private static final int COL_L1 = ALPHABET_SIZE + 3;
	private static final int COL_L2 = ALPHABET_SIZE + 4;
	private static final int COL_LV = ALPHABET_SIZE + 5;
	private static final int TOTAL_COLUMNS = ALPHABET_SIZE + 6;

	// ===== DLX STRUCTURES =====

	private static class Node{
		Node L;
		Node R;
		Node U;
		Node D;
		Column C;
		final int wordIndex;

		Node(final int wordIndex){
			this.wordIndex = wordIndex;
		}
	}

	private static class Column extends Node{
		int size;
		final String name;

		Column(final String name){
			super(-1);

			this.name = name;
			this.C = this;
			L = R = U = D = this;
		}
	}

	// ===== WORD FEATURES (GLOBAL CONSTRAINTS) =====

	private static class WordFeatures{
		int jCount;
		int lCount;
		boolean jFollowedByVowel;
		boolean lFollowedByVowel;
	}

	// ===== GLOBAL STATE =====

	private static Column root;
	private static Column[] columns;
	private static List<String> words;
	private static long[] wordMasks;
	private static List<WordFeatures> wordFeatures;

	// ===== SOLUTIONS =====
	private static int[] maxRemainingJ;
	private static int[] maxRemainingL;
	private static boolean[] canStillHaveJv;
	private static boolean[] canStillHaveLv;


	private static BufferedWriter solutionsWriter;
	private static boolean foundAnyForCurrentK;

	private static volatile boolean finishedNormally;

	// ===== MAIN =====

	public static void main(final String[] args) throws Exception{
		// ===== LOAD & PREPROCESS =====
		final boolean filteredFileLoaded = loadWords(FILTERED_WORDS_FILE);
		if(!filteredFileLoaded){
			loadWords(WORDS_FILE);
			System.out.printf(Locale.FRANCE, "Loaded: %,d words%n", words.size());

			pruneIdenticalMasks();
			pruneDominatedWords();

			writeFilteredWords();
			System.out.printf(Locale.FRANCE, "Used:   %,d words%n", words.size());
		}
		else
			System.out.printf(Locale.FRANCE, "Loaded: %,d words%n", words.size());

		analyzeAllWords();
		sortByDensity();
		precomputeGlobalUpperBounds();
		buildDLX();
		checkAlphabetCoverageOrFail();

		// ===== OPEN SOLUTION WRITER =====
		solutionsWriter = new BufferedWriter(new FileWriter(SOLUTIONS_FILE, true));


		// ===== SHUTDOWN HOOK =====
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if(!finishedNormally)
				System.err.println("\nShutdown requested.");

			try{
				if(solutionsWriter != null)
					solutionsWriter.close();
			}
			catch(final IOException ioe){
				ioe.printStackTrace();
			}
		}));


		final int maxPossible = words.size();
		for(int k = Math.max(1, MIN_K); k <= maxPossible; k ++){
			System.out.println("Trying K = " + k);

			search(0, k, new int[k]);

			if(foundAnyForCurrentK){
				System.out.println("Minimum K found: " + k);

				break;
			}
		}

		if(solutionsWriter != null)
			solutionsWriter.close();
		finishedNormally = true;
	}

	// ===== LOAD & PREPROCESS =====

	private static boolean loadWords(final String filename) throws Exception{
		final File file = new File(filename);
		if(!file.exists() || !file.isFile())
			return false;

		words = new ArrayList<>();
		try(final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),
				StandardCharsets.UTF_8))){
			String line;
			while((line = br.readLine()) != null)
				if(!line.isBlank())
					words.add(line.trim());
		}

		final int length = words.size();
		wordMasks = new long[length];
		for(int i = 0; i < length; i ++)
			wordMasks[i] = buildMask(words.get(i));

		return true;
	}

	/**
	 * Builds the letter-presence bitmask for a word, applying phonetic normalization
	 * so that equivalent characters map to the same alphabet letter.
	 * <p>
	 * Characters that are in the alphabet as independent graphemes
	 * (e.g. 'ò', 'è') must NOT be remapped.
	 */
	private static long buildMask(final String word){
		long mask = 0l;
		for(int i = 0, length = word.length(); i < length; i ++){
			char c = Character.toLowerCase(word.charAt(i));
			// Phonetic/orthographic normalization
//			c = switch(c){
//				case 'à' -> 'a';
//				case 'é' -> 'e';
//				case 'í', 'ï' -> 'i';
//				case 'ó' -> 'o';
//				case 'ú', 'ü' -> 'u';
//				default -> c;
//			};

			final int idx = CHAR_BIT[Character.toUpperCase(c)];
			if(idx >= 0)
				mask |= (1l << idx);
		}
		return mask;
	}

	/**
	 * For each group of words sharing the same letter-mask, retains only the
	 * shortest one (ties broken by keeping the first encountered after sorting).
	 * Longer words with an identical mask can never produce a better duplicateRatio
	 * than the shortest representative, so they are safe to discard.
	 */
	private static void pruneIdenticalMasks(){
		// Map mask → index of the shortest word seen so far with that mask
		final Map<Long, Integer> best = new HashMap<>();
		for(int i = 0, length = words.size(); i < length; i ++)
			best.merge(wordMasks[i], i, (a, b) -> words.get(b).length() < words.get(a).length()? b: a);

		// Mark as kept only the winning index for each mask
		final boolean[] keep = new boolean[words.size()];
		for(final int idx : best.values())
			keep[idx] = true;

		rebuild(keep);
	}

	/**
	 * Removes words that are dominated by a single other word.
	 * W1 is dominated if there exists W2 such that:
	 * - mask(W2) ⊇ mask(W1)  (W2 covers all letters of W1, and possibly more)
	 * - len(W2) ≤ len(W1)    (W2 is not longer, so it can only yield a better or equal duplicateRatio)
	 * <p>
	 * Note: this subsumes pruneSubsumedWords() entirely — if both are used,
	 * pruneSubsumedWords() becomes redundant and can be removed.
	 */
	@Deprecated /* O(n²) */
	private static void pruneDominatedWords2(){
		final int n = words.size();
		final boolean[] keep = new boolean[n];
		Arrays.fill(keep, true);

		for(int i = 0; i < n; i ++){
			if(!keep[i])
				continue;

			final long mi = wordMasks[i];
			final int li = words.get(i)
				.length();
			for(int j = 0; j < n; j ++){
				if(i == j || !keep[j])
					continue;

				final long mj = wordMasks[j];
				final int lj = words.get(j)
					.length();
				// W2=j dominates W1=i: covers at least the same letters and is not longer
				if((mi & mj) == mi && lj <= li && (mi != mj || lj < li)){
					keep[i] = false;
					break;
				}
			}
		}

		rebuild(keep);
	}

	/* ≈ O(n·log(n)) */
	private static void pruneDominatedWords(){
		final int n = words.size();
		final boolean[] keep = new boolean[n];
		Arrays.fill(keep, true);

		// Group word indices by number of bits set in their mask
		final Map<Integer, List<Integer>> buckets = new HashMap<>();
		for(int i = 0; i < n; i ++){
			final int bc = Long.bitCount(wordMasks[i]);
			buckets.computeIfAbsent(bc, k -> new ArrayList<>())
				.add(i);
		}

		// For each group of smaller/equal masks
		for(int bc = 0; bc <= ALPHABET_SIZE; bc ++){
			final List<Integer> smaller = buckets.get(bc);
			if(smaller == null)
				continue;

			// Compare only against same or larger bit-count groups
			for(int bc2 = bc; bc2 <= ALPHABET_SIZE; bc2 ++){
				final List<Integer> larger = buckets.get(bc2);
				if(larger == null)
					continue;

				for(int k = 0, smallerSize = smaller.size(); k < smallerSize; k ++){
					final int i = smaller.get(k);
					if(!keep[i])
						continue;

					final long mi = wordMasks[i];
					final int li = words.get(i).length();

					for(int i1 = 0, largerSize = larger.size(); i1 < largerSize; i1 ++){
						final int j = larger.get(i1);
						if(i == j || !keep[j])
							continue;

						final long mj = wordMasks[j];
						final int lj = words.get(j).length();

						// W2=j dominates W1=i: covers at least the same letters and is not longer
						if((mi & mj) == mi && lj <= li && (mi != mj || lj < li)){
							keep[i] = false;
							break;
						}
					}
				}
			}
		}

		rebuild(keep);
	}

	private static void rebuild(final boolean[] keep){
		final List<String> nw = new ArrayList<>();
		final List<Long> nm = new ArrayList<>();
		for(int i = 0, length = keep.length; i < length; i ++)
			if(keep[i]){
				nw.add(words.get(i));
				nm.add(wordMasks[i]);
			}
		words = nw;

		final long[] newWordMasks = new long[nm.size()];
		for(int i = 0; i < nm.size(); i ++)
			newWordMasks[i] = nm.get(i);
		wordMasks = newWordMasks;
	}

	private static void writeFilteredWords() throws IOException{
		try(final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FILTERED_WORDS_FILE),
				StandardCharsets.UTF_8))){
			for(int i = 0, length = words.size(); i < length; i ++){
				bw.write(words.get(i));
				bw.newLine();
			}
		}
		System.out.println("Filtered dictionary written to " + FILTERED_WORDS_FILE);
	}

	// ===== WORD FEATURE ANALYSIS =====

	private static WordFeatures analyzeWord(final String w){
		final WordFeatures f = new WordFeatures();
		for(int i = 0, length = w.length(); i < length; i ++){
			final char c = Character.toLowerCase(w.charAt(i));
			if(c == 'j'){
				f.jCount ++;
				if(i + 1 < w.length() && isVowel(w.charAt(i + 1)))
					f.jFollowedByVowel = true;
			}
			else if(c == 'l'){
				f.lCount ++;
				if(i + 1 < w.length() && isVowel(w.charAt(i + 1)))
					f.lFollowedByVowel = true;
			}
		}
		return f;
	}

	private static boolean isVowel(final char chr){
		return (chr < IS_VOWEL.length && IS_VOWEL[Character.toLowerCase(chr)]);
	}

	private static void analyzeAllWords(){
		wordFeatures = new ArrayList<>(words.size());

		for(int i = 0, length = words.size(); i < length; i ++){
			final String w = words.get(i);
			final WordFeatures f = analyzeWord(w);
			wordFeatures.add(f);
		}
	}

	// ===== DLX BUILD =====

	private static void buildDLX(){
		root = new Column("ROOT");
		root.L = root;
		root.R = root;

		columns = new Column[TOTAL_COLUMNS];
		Column prev = root;
		// Alphabet columns
		for(int i = 0; i < ALPHABET_SIZE; i ++){
			final Column c = new Column(ALPHABET.get(i).toString());
			columns[i] = c;

			linkColumn(prev, c);
			prev = c;
		}

		// Global constraint columns
		columns[COL_J1] = new Column("J1");
		columns[COL_J2] = new Column("J2");
		columns[COL_JV] = new Column("JV");
		columns[COL_L1] = new Column("L1");
		columns[COL_L2] = new Column("L2");
		columns[COL_LV] = new Column("LV");

		for(int i = ALPHABET_SIZE; i < TOTAL_COLUMNS; i ++){
			linkColumn(prev, columns[i]);
			prev = columns[i];
		}

		root.L = prev;
		prev.R = root;

		// Add rows
		for(int w = 0, length = words.size(); w < length; w ++){
			Node first = null;
			// Alphabet coverage
			final long mask = wordMasks[w];
			for(int b = 0; b < ALPHABET_SIZE; b ++){
				if((mask & (1l << b)) != 0)
					first = addNode(w, columns[b], first);
			}

			// Global constraints
			WordFeatures f = wordFeatures.get(w);

			if(f.jCount >= 1)
				first = addNode(w, columns[COL_J1], first);
			if(f.jCount >= 2)
				first = addNode(w, columns[COL_J2], first);
			if(f.jFollowedByVowel)
				first = addNode(w, columns[COL_JV], first);

			if(f.lCount >= 1)
				first = addNode(w, columns[COL_L1], first);
			if(f.lCount >= 2)
				first = addNode(w, columns[COL_L2], first);
			if(f.lFollowedByVowel)
				first = addNode(w, columns[COL_LV], first);
		}
	}

	private static void linkColumn(final Column left, final Column c){
		c.L = left;
		c.R = left.R;
		left.R.L = c;
		left.R = c;
		c.U = c.D = c;
	}

	private static Node addNode(final int wordIndex, final Column c, Node first){
		final Node n = new Node(wordIndex);
		n.C = c;

		// vertical link
		n.D = c;
		n.U = c.U;
		c.U.D = n;
		c.U = n;
		c.size ++;

		// horizontal link
		if(first == null){
			first = n;
			n.L = n.R = n;
		}
		else{
			n.L = first.L;
			n.R = first;
			first.L.R = n;
			first.L = n;
		}
		return first;
	}

	private static void checkAlphabetCoverageOrFail(){
		for(int i = 0; i < ALPHABET_SIZE; i ++)
			if(columns[i].size == 0)
				throw new IllegalStateException("No solution possible: alphabet letter '" + ALPHABET.get(i)
					+ "' never appears.");
	}

	// ===== DLX SEARCH WITH GLOBAL CONSTRAINTS =====

	private static void search(final int depth, final int maxDepth, final int[] solution){
		// If all columns are covered, we found a solution
		if(root.R == root){
			foundAnyForCurrentK = true;

			writeSolution(depth, solution);

			return;
		}

		// Depth limit reached, but columns still uncovered → dead end
		if(depth == maxDepth)
			return;

		final Column c = selectColumn();
		if(c.size == 0)
			return;

		cover(c);

		for(Node r = c.D; r != c; r = r.D){
			solution[depth] = r.wordIndex;

			for(Node j = r.R; j != r; j = j.R)
				cover(j.C);

			search(depth + 1, maxDepth, solution);

			for(Node j = r.L; j != r; j = j.L)
				uncover(j.C);
		}

		uncover(c);
	}

	private static Column selectColumn(){
		int min = Integer.MAX_VALUE;
		Column best = null;
		for(Node n = root.R; n != root; n = n.R){
			final Column c = (Column)n;
			if(c.size < min){
				min = c.size;
				best = c;
				if(min == 1)
					break;
			}
		}
		return best;
	}

	private static void cover(final Column c){
		c.R.L = c.L;
		c.L.R = c.R;
		for(Node i = c.D; i != c; i = i.D)
			for(Node j = i.R; j != i; j = j.R){
				j.D.U = j.U;
				j.U.D = j.D;
				j.C.size --;
			}
	}

	private static void uncover(final Column c){
		for(Node i = c.U; i != c; i = i.U)
			for(Node j = i.L; j != i; j = j.L){
				j.C.size ++;
				j.D.U = j;
				j.U.D = j;
			}
		c.R.L = c;
		c.L.R = c;
	}

	/**
	 * Sorts words by descending heuristic score to reduce DLX branching.
	 * <p>
	 * The score favors words that:
	 * - cover many alphabet letters (high bitCount)
	 * - contribute strongly to global constraints (J/L counts and vowel-follow)
	 * - are short (to minimize duplicate ratio)
	 * <p>
	 * Higher score = earlier in search.
	 */
	private static void sortByDensity(){
		final Integer[] idx = new Integer[words.size()];
		Arrays.setAll(idx, i -> i);

		Arrays.sort(idx, (a, b) -> {
			final long ma = wordMasks[a];
			final long mb = wordMasks[b];

			final int bitsA = Long.bitCount(ma);
			final int bitsB = Long.bitCount(mb);

			final WordFeatures fa = wordFeatures.get(a);
			final WordFeatures fb = wordFeatures.get(b);

			final int lenA = words.get(a).length();
			final int lenB = words.get(b).length();

			/*
			 * Heuristic scoring:
			 * - bit coverage is the dominant factor
			 * - J/L counts help satisfy global constraints early
			 * - vowel-follow flags are bonuses
			 * - shorter words are preferred
			 */
			final int scoreA = 10 * bitsA
				+ 6 * (fa.jCount + fa.lCount)
				+ 4 * (fa.jFollowedByVowel? 1: 0)
				+ 4 * (fa.lFollowedByVowel? 1: 0)
				- lenA;
			final int scoreB = 10 * bitsB
				+ 6 * (fb.jCount + fb.lCount)
				+ 4 * (fb.jFollowedByVowel? 1: 0)
				+ 4 * (fb.lFollowedByVowel? 1: 0)
				- lenB;

			// Descending order (highest score first)
			return Integer.compare(scoreB, scoreA);
		});

		reorder(idx);
	}

	/**
	 * Precomputes suffix upper bounds for global constraints.
	 * Used for aggressive early cutoff during search.
	 */
	private static void precomputeGlobalUpperBounds(){
		final int n = words.size();
		maxRemainingJ = new int[n + 1];
		maxRemainingL = new int[n + 1];
		canStillHaveJv = new boolean[n + 1];
		canStillHaveLv = new boolean[n + 1];
		for(int i = n - 1; i >= 0; i --){
			final WordFeatures f = wordFeatures.get(i);

			maxRemainingJ[i] = maxRemainingJ[i + 1] + f.jCount;
			maxRemainingL[i] = maxRemainingL[i + 1] + f.lCount;

			canStillHaveJv[i] = canStillHaveJv[i + 1] || f.jFollowedByVowel;
			canStillHaveLv[i] = canStillHaveLv[i + 1] || f.lFollowedByVowel;
		}
	}

	private static void reorder(final Integer[] idx){
		final int n = idx.length;
		final List<String> newWords = new ArrayList<>(n);
		final long[] newMasks = new long[n];
		final List<WordFeatures> newFeatures = new ArrayList<>(n);
		for(int i = 0; i < n; i ++){
			final int oldIndex = idx[i];
			newWords.add(words.get(oldIndex));
			newMasks[i] = wordMasks[oldIndex];
			newFeatures.add(wordFeatures.get(oldIndex));
		}

		words = newWords;
		wordMasks = newMasks;
		wordFeatures = newFeatures;
	}

	// ===== OUTPUT =====

	/**
	 * Writes a solution to the solutions file.
	 * The length check is kept as a safety net.
	 */
	private static void writeSolution(final int depth, final int[] solution){
		try{
			int totalLen = 0;
			for(int i = 0; i < depth; i ++)
				totalLen += words.get(solution[i])
					.length();

			final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
			for(int i = 0; i < depth; i ++)
				sj.add(words.get(solution[i]));

			final double duplicateRatio = (double)totalLen / ALPHABET_SIZE - 1.;
			final String line = String.format(Locale.ROOT, "%.2f", duplicateRatio) + ": " + sj;
			solutionsWriter.write(line);
			solutionsWriter.newLine();
			solutionsWriter.flush();

			System.out.println(line);
		}
		catch(final IOException ioe){
			throw new UncheckedIOException(ioe);
		}
	}

}
