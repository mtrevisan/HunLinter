package io.github.mtrevisan.hunlinter;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
 * Primary columns: alphabet letters.
 * Rows: words covering the letters they contain.
 * <p>
 * Dictionary is pre-filtered using:
 * - identical mask pruning (keep shortest word)
 * - dominated word pruning (safe dominance elimination)
 * - DLX finds canonical exact-cover solutions (by letter masks)
 * - Dictionary is pruned safely (identical masks + dominated words)
 */
public class Main5{

	// ===== CONFIGURATION =====
//	private static final String WORDS_FILE = "words.txt";
	private static final String WORDS_FILE = "all.txt";
	private static final String FILTERED_WORDS_FILE = "all.filtered.txt";
	private static final String SOLUTIONS_FILE = "solutionsDLX-all.txt";

	private static final int MIN_K = 4;

	//	private static final List<Character> ALPHABET = List.of(
//		'C', 'Đ', 'Ñ', 'J', 'B', 'Ŧ', 'Ò', 'F', 'G', 'X', 'È',
//		'U', 'M', 'V', 'P', 'D', 'K', 'S', 'L', 'T', 'R', 'N', 'I', 'O', 'A', 'E'
//	);
	private static final List<Character> ALPHABET = List.of(
		'C', 'Đ', 'Ñ', 'J', 'B', 'Ŧ', 'Ò', 'F', 'G', 'X', 'È',
		'U', 'Ü', 'Ú', 'M', 'V', 'P', 'D', 'K', 'S', 'L', 'T', 'R', 'N', 'I', 'Ï', 'Í', 'O', 'Ó', 'A', 'À', 'E', 'É'
	);
	private static final int ALPHABET_SIZE = ALPHABET.size();
	// Fast char → bit lookup
	private static final int[] CHAR_BIT = new int[0x00001_0000];
	static{
		Arrays.fill(CHAR_BIT, -1);
		for(int i = 0; i < ALPHABET_SIZE; i ++)
			CHAR_BIT[ALPHABET.get(i)] = i;
	}

	// ===== DLX STRUCTURES =====

	private static class Node{
		Node L, R, U, D;
		Column C;
		int wordIndex;

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
			this.size = 0;
			this.C = this;
			L = R = U = D = this;
		}

	}

	// ===== GLOBAL STATE =====

	private static Column root;
	private static Column[] columns;
	private static List<String> words;
	private static long[] wordMasks;

	// ===== SOLUTIONS =====

	private static boolean foundAnyForCurrentK;

	private static BufferedWriter solutionsWriter;

	private static volatile boolean finishedNormally = false;


	// ===== MAIN =====

	public static void main(final String[] args) throws Exception{
		// ===== LOAD & PREPROCESS =====
		final boolean filteredFileLoaded = loadWords(FILTERED_WORDS_FILE);
		if(!filteredFileLoaded){
			loadWords(WORDS_FILE);
			System.out.println("Loaded: " + words.size() + " words");

			pruneIdenticalMasks();
			pruneDominatedWords();
			System.out.println("Used:   " + words.size() + " words");

			writeFilteredWords();
		}
		else
			System.out.println("Loaded: " + words.size() + " words");

		buildDLX();
		checkAlphabetCoverageOrFail();

		// ===== OPEN SOLUTION WRITER =====
		solutionsWriter = new BufferedWriter(new FileWriter(SOLUTIONS_FILE, true));


		// ===== SHUTDOWN HOOK =====
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if(!finishedNormally)
				System.err.println("\nShutdown requested.");

			try{
				if(solutionsWriter != null){
					solutionsWriter.close();
					solutionsWriter = null;
				}
			}
			catch(final IOException ioe){
				ioe.printStackTrace();
			}
		}));


		final int maxPossible = words.size();
		for(int k = Math.max(1, MIN_K); k <= maxPossible; k ++){
			System.out.println("Trying K = " + k);

			foundAnyForCurrentK = false;

			final int[] solution = new int[k];

			search(0, k, solution);

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
		File file = new File(filename);
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

		wordMasks = new long[words.size()];
		for(int i = 0, length = words.size(); i < length; i ++)
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
			best.merge(wordMasks[i], i, (a, b) ->
				words.get(b).length() < words.get(a).length()? b: a);

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
	private static void pruneDominatedWords(){
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

	private static void rebuild(final boolean[] keep){
		final List<String> nw = new ArrayList<>();
		final List<Long> nm = new ArrayList<>();
		for(int i = 0, length = keep.length; i < length; i ++)
			if(keep[i]){
				nw.add(words.get(i));
				nm.add(wordMasks[i]);
			}

		words = nw;
		long[] arr = new long[10];
		int count = 0;
		for(int i = 0, nmSize = nm.size(); i < nmSize; i ++){
			if(arr.length == count)
				arr = Arrays.copyOf(arr, count * 2);
			arr[count ++] = nm.get(i);
		}
		arr = Arrays.copyOfRange(arr, 0, count);
		wordMasks = arr;
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

	// ===== DLX BUILD =====

	private static void buildDLX(){
		root = new Column("ROOT");
		root.L = root.R = root;

		columns = new Column[ALPHABET_SIZE];
		Column prev = root;
		for(int i = 0; i < ALPHABET_SIZE; i ++){
			final Column c = new Column(ALPHABET.get(i).toString());
			columns[i] = c;

			c.L = prev;
			c.R = root;
			prev.R = c;
			root.L = c;
			prev = c;
		}

		for(int w = 0, length = words.size(); w < length; w ++){
			final long mask = wordMasks[w];
			Node first = null;
			Node prevNode = null;
			for(int b = 0; b < ALPHABET_SIZE; b ++){
				if((mask & (1l << b)) == 0)
					continue;

				final Column c = columns[b];
				final Node n = new Node(w);
				n.C = c;

				n.D = c;
				n.U = c.U;
				c.U.D = n;
				c.U = n;
				c.size ++;

				if(first == null){
					first = n;
					n.L = n.R = n;
				}
				else{
					n.L = prevNode;
					n.R = first;
					prevNode.R = n;
					first.L = n;
				}
				prevNode = n;
			}
		}
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
