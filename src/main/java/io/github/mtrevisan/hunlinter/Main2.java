package io.github.mtrevisan.hunlinter;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;


/**
 * Highly optimized iterative DFS that finds combinations of K words
 * covering a fixed alphabet.
 * <p>
 * Optimizations applied:
 * <ul>
 *   <li>Flat array-based stack — no object allocation, zero GC pressure.</li>
 *   <li>Anticipated pre-push suffix pruning.</li>
 *   <li>Suffix-mask lower-bound pruning (classic).</li>
 *   <li>Zero-contribution pruning (word adds no new letter).</li>
 *   <li>Length lower-bound pruning: if current length + minimum possible
 *       completion length exceeds {@code MAX_TOTAL_LEN}, the branch is cut
 *       both at pop time and pre-push.</li>
 *   <li>Rarity-based word ordering (rarest letters first).</li>
 *   <li>O(|word|) mask building via a char-to-bit lookup table (no indexOf).</li>
 *   <li>Optional subset-word pruning at startup ({@code -Dprune.subsets=true}).</li>
 *   <li>Race-free atomic checkpointing.</li>
 *   <li>Inline ancestor path stored per stack frame (no pointer aliasing).</li>
 * </ul>
 *
 * <b>Flat stack layout</b> — six parallel arrays indexed by {@code top}:
 * <pre>
 *   sStart [top]                             first candidate word index
 *   sDepth [top]                             number of words chosen so far
 *   sMask  [top]                             bitmask of letters covered
 *   sLen   [top]                             total character length so far
 *   sChoice[top]                             word chosen to arrive here (-1 = root)
 *   sPath  [top * K .. top * K + depth - 1]  word-index path from root to this node
 * </pre>
 * {@code topPath} mirrors {@code top * K} and is updated with {@code +K}/{@code -K}
 * instead of a multiply, keeping it in a CPU register across the entire search loop.
 */
public class Main2{

	// ===== CONFIGURATION =====
	private static final int K = 5;
	private static final long LOG_EVERY = 100_000_000l;
	private static final long CHECKPOINT_EVERY = 100_000_000l;
	private static final String WORDS_FILE = "words.txt";
	private static final String CHECKPOINT_FILE = "checkpoint" + K + ".bin";
	private static final String SOLUTIONS_FILE = "solutions" + K + ".txt";
	private static final double THRESHOLD = 0.24;

	// Fixed alphabet
	private static final List<Character> ALPHABET = List.of(
		'C', 'Đ', 'Ñ', 'J', 'B', 'Ŧ', 'Ò', 'F', 'G', 'X', 'È',
		'U', 'M', 'V', 'P', 'D', 'K', 'S', 'L', 'T', 'R', 'N', 'I', 'O', 'A', 'E'
	);

	private static final int ALPHABET_SIZE = ALPHABET.size();
	private static final long FULL_MASK = (1l << ALPHABET_SIZE) - 1l;
	/**
	 * Maximum total character length for a valid solution.
	 * Derived from THRESHOLD: totalLength ≤ (THRESHOLD + 1) * ALPHABET_SIZE.
	 * Any branch whose minimum possible completion exceeds this is pruned.
	 */
	private static final int MAX_TOTAL_LEN = (int)((THRESHOLD + 1.) * ALPHABET_SIZE);

	/**
	 * Direct lookup: Unicode code point → bit index in the mask, or -1 if not in alphabet.
	 * Covers the full BMP (U+0000–U+FFFF).
	 */
	private static final int[] CHAR_BIT = new int[0x1_0000];
	static{
		Arrays.fill(CHAR_BIT, -1);
		for(int i = 0; i < ALPHABET_SIZE; i ++)
			CHAR_BIT[Character.toLowerCase(ALPHABET.get(i))] = i;
	}

	private static List<String> words;
	private static long[] wordMasks;
	/** {@code suffixMasks[i] = wordMasks[i] | … | wordMasks[n-1];  suffixMasks[n] = 0} */
	private static long[] suffixMasks;
	/**
	 * {@code minLenSum[k]} = sum of the {@code k} shortest word lengths in the
	 * (post-pruning, pre-sort) dictionary.  Used as the tightest possible lower
	 * bound on the total length of any k-word completion.
	 */
	private static int[] minLenSum;

	private static long runs = 0L;
	private static volatile boolean stopRequested = false;
	private static BufferedWriter solutionsWriter;

	// ===== FLAT STACK =====
	private static int[] sStart;
	private static int[] sDepth;
	private static long[] sMask;
	private static int[] sLen;    // total character length accumulated so far
	private static int[] sChoice;
	/**
	 * Inline path storage: {@code sPath[top*K .. top*K + depth - 1]} holds the word
	 * indices chosen at depths 0, 1, … depth-1 to reach this node.
	 * Storing the path inline avoids parent-pointer aliasing caused by slot reuse.
	 */
	private static int[] sPath;


	// ===== CHECKPOINT =====
	private static final class Checkpoint implements Serializable{
		@Serial
		private static final long serialVersionUID = 4l;

		final int top;
		final int[] start;
		final int[] depth;
		final long[] mask;
		final int[] len;
		final int[] choice;
		final int[] path;   // flat array, length = (top + 1) * K
		final long runs;

		Checkpoint(final int top, final int[] start, final int[] depth, final long[] mask,
			final int[] len, final int[] choice, final int[] path,
			final long runs){
			this.top = top;
			this.start = Arrays.copyOf(start, top + 1);
			this.depth = Arrays.copyOf(depth, top + 1);
			this.mask = Arrays.copyOf(mask, top + 1);
			this.len = Arrays.copyOf(len, top + 1);
			this.choice = Arrays.copyOf(choice, top + 1);
			this.path = Arrays.copyOf(path, (top + 1) * K);
			this.runs = runs;
		}
	}


	// ===== MAIN =====

	public static void main(final String[] args) throws Exception{
		final int top = init();

		solutionsWriter = new BufferedWriter(new FileWriter(SOLUTIONS_FILE, true));

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.err.println("\nShutdown requested.");
			stopRequested = true;
		}));

		runIterativeSearch(top);
	}


	// ===== SEARCH =====

	private static void runIterativeSearch(int top){
		// Local aliases — keeps all hot arrays as local variables so the JIT
		// can promote them to registers and skip null/bounds re-checks.
		final int[] lStart = sStart;
		final int[] lDepth = sDepth;
		final long[] lMask = sMask;
		final int[] lLen = sLen;
		final int[] lChoice = sChoice;
		final int[] lPath = sPath;
		final long[] lWordM = wordMasks;
		final long[] lSuffix = suffixMasks;
		final int nWords = words.size();
		final int[] lMinLenSum = minLenSum;

		// topPath mirrors top * K but is updated with +K/-K instead of a multiply.
		int topPath = top * K;
		// Scratch buffer: holds the path of the frame just popped.
		final int[] pathBuf = new int[K];
		while(!stopRequested && top >= 0){
			// ── Pop ──────────────────────────────────────────────────────────
			final int start = lStart[top];
			final int depth = lDepth[top];
			final long mask = lMask[top];
			final int len = lLen[top];
			final int choice = lChoice[top];
			// Copy current path
			System.arraycopy(lPath, topPath, pathBuf, 0, depth);
			top --;
			topPath -= K;

			runs ++;
			if(runs % LOG_EVERY == 0)
				System.out.printf("Run %d: choice = %s%n",
					runs / LOG_EVERY,
					(choice >= 0? words.get(choice): "ROOT"));

			// ── Periodic checkpoint ───────────────────────────────────────────
			if(runs % CHECKPOINT_EVERY == 0){
				try{
					saveCheckpoint(top + 1);
//					System.out.println("Checkpoint saved");
				}
				catch(final Exception e){
					e.printStackTrace();
				}
			}

			// ── Pruning 4: length lower bound ─────────────────────────────────
			// current length + cheapest possible completion > MAX_TOTAL_LEN → prune.
			final int remainingSlots = K - depth;
			if(len + lMinLenSum[remainingSlots] > MAX_TOTAL_LEN)
				continue;

			// ── Pruning 1: suffix coverage ────────────────────────────────────
			if((mask | lSuffix[start]) != FULL_MASK)
				continue;

			// ── Leaf: solution check ──────────────────────────────────────────
			if(depth == K){
				if(mask == FULL_MASK)
					writeSolution(pathBuf, depth, len);
				continue;
			}

			// ── Expand ───────────────────────────────────────────────────────
			final int limit = nWords - remainingSlots;

			// Push children in reverse order (limit → start) so the smallest
			// index is popped first, preserving original traversal order.
			for(int i = limit; i >= start; i --){
				// Read wordMask once: avoids a second indexed array load at P2 check
				final long wm = lWordM[i];

				// Pruning 2: word adds no new letter
				final long newMask = mask | wm;
				if(newMask == mask)
					continue;

				final int newLen = len + words.get(i).length();

				// Pruning 4b: anticipated length bound — pre-push
				// newLen + cheapest (remainingSlots-1) words > MAX_TOTAL_LEN → skip
				if(newLen + lMinLenSum[remainingSlots - 1] > MAX_TOTAL_LEN)
					continue;

				// Pruning 3: anticipated suffix check — pre-push
				if((newMask | lSuffix[i + 1]) != FULL_MASK)
					continue;

				// Push
				top ++;
				topPath += K;
				lStart[top] = i + 1;
				lDepth[top] = depth + 1;
				lMask[top] = newMask;
				lLen[top] = newLen;
				lChoice[top] = i;

				// Write inline path: parent's path[0..depth-1] + word i
				System.arraycopy(pathBuf, 0, lPath, topPath, depth);
				lPath[topPath + depth] = i;
			}
		}

		// ── Final checkpoint ──────────────────────────────────────────────────
		try{
			saveCheckpoint(top + 1);
			System.err.println("Final checkpoint saved");
		}
		catch(final Exception e){
			e.printStackTrace();
		}

		try{
			if(solutionsWriter != null)
				solutionsWriter.close();
		}
		catch(final IOException ignored){}
	}


	// ===== INIT / CHECKPOINT =====

	private static int init() throws Exception{
		loadWords();

		// Maximum live stack size: at most words.size() frames per depth level, K+1 levels.
		final int maxStack = words.size() * (K + 1) + 16;
		sStart = new int[maxStack];
		sDepth = new int[maxStack];
		sMask = new long[maxStack];
		sLen = new int[maxStack];
		sChoice = new int[maxStack];
		sPath = new int[maxStack * K];

		final File cpFile = new File(CHECKPOINT_FILE);
		if(!cpFile.exists() || cpFile.length() == 0){
			// Push root frame
			sStart[0] = 0;
			sDepth[0] = 0;
			sMask[0] = 0l;
			sLen[0] = 0;
			sChoice[0] = -1;
			return 0;
		}

		try(final ObjectInputStream in = new ObjectInputStream(new FileInputStream(cpFile))){
			final Checkpoint cp = (Checkpoint)in.readObject();
			runs = cp.runs;
			final int size = cp.top + 1;
			System.out.printf("Checkpoint loaded: runs = %,d, stack entries = %,d%n", cp.runs / CHECKPOINT_EVERY, size);
			System.arraycopy(cp.start, 0, sStart, 0, size);
			System.arraycopy(cp.depth, 0, sDepth, 0, size);
			System.arraycopy(cp.mask, 0, sMask, 0, size);
			System.arraycopy(cp.len, 0, sLen, 0, size);
			System.arraycopy(cp.choice, 0, sChoice, 0, size);
			System.arraycopy(cp.path, 0, sPath, 0, size * K);
			return cp.top;
		}
	}

	private static void saveCheckpoint(final int size) throws Exception{
		final Path target = Paths.get(CHECKPOINT_FILE);
		final Path tmp = Paths.get(CHECKPOINT_FILE + ".tmp");
		try(final ObjectOutputStream out = new ObjectOutputStream(
			Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))){
			out.writeObject(new Checkpoint(size - 1, sStart, sDepth, sMask, sLen, sChoice, sPath, runs));
			out.flush();
		}
		try{
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch(final AtomicMoveNotSupportedException e){
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}


	// ===== WORD LOADING & PREPROCESSING =====

	private static void loadWords() throws Exception{
		final InputStream is = Main2.class.getResourceAsStream("/" + WORDS_FILE);
		if(is == null)
			throw new IllegalStateException(WORDS_FILE + " not found in classpath");

		final Set<String> unique = new LinkedHashSet<>();
		try(final BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){
			String line;
			while((line = br.readLine()) != null)
				if(!line.isBlank())
					unique.add(line.trim());
		}

		words = new ArrayList<>(unique);
		final int size = words.size();
		wordMasks = new long[size];
		for(int i = 0; i < size; i ++)
			wordMasks[i] = buildMask(words.get(i));

		System.out.printf("Pure combinations C(%,d, %d) = %,d%n", words.size(), K, countCombinations());
		// Optional: remove words whose mask is a strict subset of another word's mask.
		// Activate with -Dprune.subsets=true.  O(n²), runs once at startup.
		if(Boolean.getBoolean("prune.subsets"))
			pruneSubsumedWords();
		pruneDominatedWords();
		removeRedundantWords();
		pruneIdenticalMasks();

		System.out.printf("Pure combinations C(%,d, %d) = %,d%n", words.size(), K, countCombinations());

		// ── Precompute minLenSum ──────────────────────────────────────────────
		// Must be done BEFORE the rarity sort so it reflects the actual word lengths
		// available regardless of ordering.
		final int[] sortedLens = new int[words.size()];
		for(int i = 0; i < sortedLens.length; i ++)
			sortedLens[i] = words.get(i).length();
		Arrays.sort(sortedLens);
		minLenSum = new int[K + 1];
		for(int k = 1; k <= K; k ++)
			minLenSum[k] = minLenSum[k - 1] + sortedLens[k - 1];
		System.out.printf("MAX_TOTAL_LEN = %d, minLenSum[K] = %d, minLenSum: %s%n",
			MAX_TOTAL_LEN, minLenSum[K], Arrays.toString(minLenSum));

		// ── Rarity sort ───────────────────────────────────────────────────────
		final int[] freq = new int[ALPHABET_SIZE];
		for(final long m : wordMasks)
			for(int i = 0; i < ALPHABET_SIZE; i ++)
				if((m & (1l << i)) != 0)
					freq[i] ++;

		final long[] sortKeys = new long[words.size()];
		for(int idx = 0; idx < words.size(); idx ++){
			double s = 0.;
			final long m = wordMasks[idx];
			for(int i = 0; i < ALPHABET_SIZE; i ++)
				if((m & (1L << i)) != 0)
					s += 1. / freq[i];
			// Negate score so that Arrays.sort (ascending) gives descending rarity order.
			// Multiply by a large constant to preserve fixed-point precision.
			final long scoreBits = (long)(-s * (1l << 30));
			sortKeys[idx] = (scoreBits << 32) | (idx & 0xFFFFFFFFl);
		}

		Arrays.sort(sortKeys);

		final List<String> sortedWords = new ArrayList<>(words.size());
		final long[] sortedMasks = new long[words.size()];
		for(int rank = 0; rank < sortKeys.length; rank ++){
			final int origIdx = (int)(sortKeys[rank] & 0xFFFFFFFFl);
			sortedWords.add(words.get(origIdx));
			sortedMasks[rank] = wordMasks[origIdx];
		}
		words = sortedWords;
		wordMasks = sortedMasks;

		// Build suffix-OR masks
		suffixMasks = new long[wordMasks.length + 1];
		for(int i = wordMasks.length - 1; i >= 0; i --)
			suffixMasks[i] = suffixMasks[i + 1] | wordMasks[i];

		System.out.printf("Loaded %,d words, alphabet = %d bits, MAX_TOTAL_LEN = %d, suffix[0] = %s%n",
			words.size(), ALPHABET_SIZE,MAX_TOTAL_LEN,
			(suffixMasks[0] == FULL_MASK
				? "FULL (search possible)"
				: "INCOMPLETE — no solution exists with this word list!"));
	}

	/**
	 * Computes C(n, K) — the total number of pure combinations without pruning.
	 * Uses an iterative formula to avoid overflow and intermediate factorials:
	 * C(n, K) = n! / (K! * (n-K)!) = product(n-i, i=0..K-1) / K!
	 * The result is returned as a {@link BigInteger} since C(n, K) with n in the
	 * thousands and K=5 can exceed {@code long} range.
	 */
	private static BigInteger countCombinations(){
		final int n = words.size();
		if(n < K)
			return BigInteger.ZERO;

		// C(n, K) = (n * (n-1) * ... * (n-K+1)) / K!
		// Compute numerator and denominator separately, then divide.
		// Division is exact (C(n,K) is always an integer).
		BigInteger num = BigInteger.ONE;
		for(int i = 0; i < K; i ++)
			num = num.multiply(BigInteger.valueOf(n - i));
		BigInteger den = BigInteger.ONE;
		for(int i = 2; i <= K; i ++)
			den = den.multiply(BigInteger.valueOf(i));
		return num.divide(den);
	}

	/**
	 * For each group of words sharing the same letter-mask, retains only the
	 * shortest one (ties broken by keeping the first encountered after sorting).
	 * Longer words with an identical mask can never produce a better duplicateRatio
	 * than the shortest representative, so they are safe to discard.
	 */
	private static void pruneIdenticalMasks(){
		final int n = words.size();

		// Map mask → index of the shortest word seen so far with that mask
		final Map<Long, Integer> bestByMask = new HashMap<>();
		for(int i = 0; i < n; i ++){
			final long mask = wordMasks[i];
			bestByMask.merge(mask, i, (prev, cur) ->
				words.get(cur).length() < words.get(prev).length()? cur: prev);
		}

		// Mark as kept only the winning index for each mask
		final boolean[] keep = new boolean[n];
		for(final int idx : bestByMask.values())
			keep[idx] = true;

		rebuild(keep);
	}

	/**
	 * Removes words whose letter-mask is a proper subset of another word's mask.
	 * Any solution containing such a word can replace it with the dominating word,
	 * so these words can never appear in an optimal solution.
	 * O(n²) — runs once.
	 */
	private static void pruneSubsumedWords(){
		final int n = words.size();
		final boolean[] keep = new boolean[n];
		Arrays.fill(keep, true);
		for(int i = 0; i < n; i ++){
			if(!keep[i])
				continue;

			final long mi = wordMasks[i];
			for(int j = 0; j < n; j ++){
				if(i == j || !keep[j]) continue;
				// mask[i] is a proper subset of mask[j]: drop i
				final long mj = wordMasks[j];
				if(mi != mj && (mi & mj) == mi){
					keep[i] = false;
					break;
				}
			}
		}
		rebuild(keep);
		System.out.printf("Subset pruning: %,d words retained%n", words.size());
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
			if(!keep[i]) continue;
			final long mi = wordMasks[i];
			final int li = words.get(i).length();
			for(int j = 0; j < n; j ++){
				if(i == j || !keep[j])
					continue;

				final long mj = wordMasks[j];
				final int lj = words.get(j).length();
				// W2=j dominates W1=i: covers at least the same letters and is not longer
				if((mi & mj) == mi && lj <= li && (mj != mi || lj < li)){
					keep[i] = false;
					break;
				}
			}
		}
		rebuild(keep);
	}

	/**
	 * Removes redundant words from the word list.
	 * A word W1 is redundant if there exists another word W2 such that:
	 * - W1 and W2 have the same length
	 * - W1 and W2 differ in exactly one position p
	 * - The character W1[p] (the one being replaced) appears at least twice in W1,
	 * meaning it is already covered elsewhere — W1 adds no letter that W2 doesn't.
	 * <p>
	 * Example: "lana" vs "lano" → W1="lana", W2="lano", position p=3, char 'a'
	 * 'a' appears twice in "lana" → "lana" is redundant and is removed.
	 */
	private static void removeRedundantWords(){
		final int n = words.size();
		final boolean[] redundant = new boolean[n];

		// Group word indices by length for O(n * avgLen) instead of O(n² * avgLen)
		final Map<Integer, List<Integer>> byLength = new HashMap<>();
		for(int i = 0; i < n; i ++)
			byLength.computeIfAbsent(words.get(i).length(), x -> new ArrayList<>()).add(i);

		for(final List<Integer> group : byLength.values()){
			final int g = group.size();
			for(int a = 0; a < g; a ++){
				final int ia = group.get(a);
				if(redundant[ia]) continue;
				final String wa = words.get(ia);
				final int len = wa.length();

				for(int b = 0; b < g; b ++){
					if(a == b) continue;
					final String wb = words.get(group.get(b));
					// Find the single differing position
					int diffPos = -1;
					boolean valid = true;
					for(int p = 0; p < len; p ++){
						if(wa.charAt(p) != wb.charAt(p)){
							if(diffPos >= 0){
								valid = false;
								break;
							}  // more than one diff
							diffPos = p;
						}
					}
					if(!valid || diffPos < 0)
						continue;  // 0 or 2+ differences

					// Check that the replaced char appears at least twice in wa
					final char replaced = wa.charAt(diffPos);
					int count = 0;
					for(int p = 0; p < len; p ++)
						if(wa.charAt(p) == replaced && ++count == 2) break;

					if(count >= 2){
						redundant[ia] = true;
						break;  // no need to check other partners for wa
					}
				}
			}
		}

		final boolean[] keep = new boolean[n];
		for(int i = 0; i < n; i ++)
			keep[i] = !redundant[i];
		rebuild(keep);
	}

	/** Shared helper: compacts words/wordMasks keeping only entries where keep[i]=true. */
	private static void rebuild(final boolean[] keep){
		int kept = 0;
		for(final boolean k : keep)
			if(k)
				kept ++;

		final List<String> fw = new ArrayList<>(kept);
		final long[] fm = new long[kept];
		int out = 0;
		for(int i = 0; i < keep.length; i ++)
			if(keep[i]){
				fw.add(words.get(i));
				fm[out++] = wordMasks[i];
			}
		words = fw;
		wordMasks = fm;
	}


	// ===== UTILITIES =====

	/**
	 * Builds the letter-presence bitmask for {@code word} using the {@link #CHAR_BIT}
	 * lookup table.  O(|word|) with a single array access per character — no indexOf scans.
	 * <p>
	 * The {@code switch} block performs explicit phonetic normalization of accented vowels
	 * that are NOT members of the alphabet as independent graphemes (e.g. {@code ï → i},
	 * {@code ü → u}).  Graphemes that ARE in the alphabet (e.g. {@code ò}, {@code è}) are
	 * looked up directly and must NOT be remapped here.
	 */
	private static long buildMask(final String word){
		long mask = 0l;
		for(int i = 0, len = word.length(); i < len; i ++){
			char c = Character.toLowerCase(word.charAt(i));
			c = switch(c){
				case 'à' -> 'a';
				case 'é' -> 'e';
				case 'í', 'ï' -> 'i';
				case 'ó' -> 'o';
				case 'ú', 'ü' -> 'u';
				default -> c;
			};
			final int bit = CHAR_BIT[c];
			if(bit >= 0)
				mask |= (1l << bit);
		}
		return mask;
	}

	/**
	 * Writes a solution to the solutions file.
	 * The length check is now redundant (Pruning 4 already guarantees len ≤ MAX_TOTAL_LEN)
	 * but kept as a safety net.
	 */
	private static void writeSolution(final int[] path, final int depth, final int totalLen){
		try{
			final double duplicateRatio = (double)totalLen / ALPHABET_SIZE - 1.;
			if(duplicateRatio > THRESHOLD)
				return;

			final StringJoiner sj = new StringJoiner(" ");
			for(int i = 0; i < depth; i ++)
				sj.add(words.get(path[i]));

			final String solution = String.format(Locale.ROOT, "%.2f", duplicateRatio) + ": " + sj;
			solutionsWriter.write(solution);
			solutionsWriter.newLine();
			solutionsWriter.flush();

			System.out.printf("%s%n", solution);
		}
		catch(final IOException e){
			throw new UncheckedIOException(e);
		}
	}

}