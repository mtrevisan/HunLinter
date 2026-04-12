package io.github.mtrevisan.hunlinter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * Finds the shortest pangram (minimum total characters) using exactly K = 5
 * distinct dictionary words that together cover every letter of the fixed
 * alphabet.
 *
 * <h2>Algorithm</h2>
 * <ol>
 *   <li><b>Greedy warm-up</b> — produces an initial upper bound {@code bestLen}
 *       in O(n) by repeatedly picking the shortest word covering the most
 *       uncovered bits.</li>
 *   <li><b>Lower-bound precomputation</b> — builds {@code dp[k][mask]} = minimum
 *       total character count to cover all bits in {@code mask} using exactly
 *       {@code k} words, for k = 1 … K.
 *       <ul>
 *         <li>{@code dp[1]} is filled via a SOS (Sum-over-Subsets) DP in
 *             O(ALPHABET_SIZE × 2^ALPHABET_SIZE) ≈ 1.7 s.</li>
 *         <li>{@code dp[k]} for k ≥ 2 is filled lazily and memoized: each
 *             (k, mask) entry is computed at most once.</li>
 *       </ul>
 *       Total table size: K × 2^ALPHABET_SIZE × 2 bytes ≈ 670 MB.</li>
 *   <li><b>Branch-and-bound DFS</b> — iterative flat-stack search.
 *       At each node with {@code covered} bits and {@code remaining} slots,
 *       the pruning condition is:
 *       {@code currentLen + dp[remaining][FULL_MASK & ~covered] ≥ bestLen}.</li>
 * </ol>
 *
 * <h2>Flat stack layout</h2>
 * <pre>
 *   sStart [top]                          first candidate index
 *   sDepth [top]                          words chosen so far
 *   sMask  [top]                          bits covered so far
 *   sLen   [top]                          total characters so far
 *   sPath  [top * K .. top * K + depth-1] word indices chosen
 * </pre>
 */
public class Main3{

	// ===== CONFIGURATION =====
	private static final int K = 5;
	private static final String WORDS_FILE = "words.txt";
	private static final String SOLUTION_FILE = "solution_shortest.txt";
	private static final long LOG_EVERY = 100_000_000l;

	// Fixed alphabet
	private static final List<Character> ALPHABET = List.of(
		'C', 'Đ', 'Ñ', 'J', 'B', 'Ŧ', 'Ò', 'F', 'G', 'X', 'È',
		'U', 'M', 'V', 'P', 'D', 'K', 'S', 'L', 'T', 'R', 'N', 'I', 'O', 'A', 'E'
	);

	private static final int ALPHABET_SIZE = ALPHABET.size();
	private static final long FULL_MASK = (1l << ALPHABET_SIZE) - 1l;
	/** Sentinel meaning "unreachable / not yet computed". */
	private static final short DP_INF = Short.MAX_VALUE;

	/** Direct lookup: code point → bit index, or -1. Covers full BMP. */
	private static final int[] CHAR_BIT = new int[0x0001_0000];
	static{
		Arrays.fill(CHAR_BIT, -1);
		for(int i = 0; i < ALPHABET_SIZE; i ++)
			CHAR_BIT[Character.toLowerCase(ALPHABET.get(i))] = i;
	}

	private static List<String> words;
	private static long[] wordMasks;
	/** suffixMasks[i] = wordMasks[i] | … | wordMasks[n-1];  suffixMasks[n] = 0 */
	private static long[] suffixMasks;
	/** Cached word lengths to avoid repeated String.length() calls. */
	private static int[] wordLens;

	// ===== LOWER-BOUND TABLE =====
	/**
	 * dp[k][mask] = minimum total character count to cover all bits in mask
	 * using exactly k words from the dictionary.
	 * Indexed as dp[k-1][mask] (k = 1 → index 0).
	 * Values are stored as short to save memory; DP_INF = unreachable.
	 * Total size: K × 2^ALPHABET_SIZE × 2 bytes ≈ 670 MB for K = 5, ALPHABET_SIZE = 26.
	 */
	private static short[][] dp;

	// ===== SEARCH STATE =====
	private static volatile boolean stopRequested = false;
	private static volatile int bestLen = Integer.MAX_VALUE;
	private static int[] bestPath;
	private static int bestPathLen;
	private static long runs = 0l;


	// ===== MAIN =====

	public static void main(final String[] args) throws Exception{
		loadWords();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			stopRequested = true;
			System.err.println("\nShutdown requested.");
			printBest();
		}));

		// Phase 1: greedy upper bound
		System.out.println("=== Phase 1: greedy warm-up ===");
		greedyUpperBound();
		System.out.printf("Greedy: length = %d  %s%n%n",
			bestLen, formatSolution(bestPath, bestPathLen));

		// Phase 2: precompute lower-bound table
		System.out.println("=== Phase 2: precomputing lower-bound table ===");
		buildDpTable();
		System.out.println("DP table ready.");
		// Tighten upper bound using the DP table itself:
		// dp[K][FULL_MASK] is the global optimum by definition, but we can't
		// compute it directly (too expensive). However we can check if bestLen
		// can be improved immediately.
		System.out.printf("dp[1][FULL_MASK] = %d (single-word lower bound)%n%n",
			dpGet(1, (int)FULL_MASK));

		// Phase 3: branch and bound
		System.out.println("=== Phase 3: branch and bound ===");
		branchAndBound();

		System.out.println("\n=== Optimal solution ===");
		printBest();
		writeSolution();
	}


	// ===== PHASE 1: GREEDY UPPER BOUND =====

	private static void greedyUpperBound(){
		long covered = 0l;
		int totalLen = 0;
		final int[] path = new int[K];
		int pathLen = 0;
		final boolean[] used = new boolean[words.size()];
		while(covered != FULL_MASK && pathLen < K){
			int bestIdx = -1;
			int bestNewBits = -1;
			int bestWLen = Integer.MAX_VALUE;
			for(int i = 0; i < words.size(); i ++){
				if(used[i]) continue;
				final int newBits = Long.bitCount(wordMasks[i] & ~covered);
				final int wlen = wordLens[i];
				if(newBits > bestNewBits || (newBits == bestNewBits && wlen < bestWLen)){
					bestNewBits = newBits;
					bestWLen = wlen;
					bestIdx = i;
				}
			}

			if(bestIdx < 0 || bestNewBits == 0)
				break;

			used[bestIdx] = true;
			covered |= wordMasks[bestIdx];
			totalLen += wordLens[bestIdx];
			path[pathLen++] = bestIdx;
		}

		if(covered == FULL_MASK && totalLen < bestLen){
			bestLen = totalLen;
			bestPath = Arrays.copyOf(path, pathLen);
			bestPathLen = pathLen;
		}
	}


	// ===== PHASE 2: LOWER-BOUND TABLE =====

	/**
	 * Builds dp[1..K][0..2^ALPHABET_SIZE - 1].
	 *
	 * <p><b>dp[1]</b> is filled via SOS (Sum-over-Subsets) DP:</p>
	 * <ol>
	 *   <li>For each word w, set dp[1][w.mask] = min(dp[1][w.mask], len(w)).</li>
	 *   <li>SOS relaxation: for each bit b, for each mask containing b,
	 *       dp[1][mask without b] = min(dp[1][mask without b], dp[1][mask]).
	 *       This propagates: "if I can cover mask with len L, I can also cover
	 *       any subset of mask with len L (or less)".</li>
	 * </ol>
	 *
	 * <p><b>dp[k]</b> for k ≥ 2 is computed lazily in {@link #dpGet}.</p>
	 */
	private static void buildDpTable(){
		final int SIZE = 1 << ALPHABET_SIZE;
		dp = new short[K][SIZE];

		// Initialize all entries to DP_INF
		for(int k = 0; k < K; k ++)
			Arrays.fill(dp[k], DP_INF);

		// dp[0] = dp[1-1]: seed with exact word masks
		final short[] dp1 = dp[0];
		for(int i = 0; i < wordMasks.length; i ++){
			final int m = (int)wordMasks[i];
			final short l = (short)wordLens[i];
			if(l < dp1[m])
				dp1[m] = l;
		}

		// SOS relaxation on dp[1]:
		// After this, dp1[mask] = min length of any single word whose mask
		// is a SUPERset of mask (i.e. covers at least all bits in mask).
		// We want dp1[mask] = min len of word covering ALL bits of mask.
		// Standard SOS gives min over supersets if we iterate in the right direction.
		//
		// Direction: for each bit b, propagate dp1[mask | (1<<b)] → dp1[mask]
		// meaning: "if some superset of mask is achievable, so is mask".
		// This correctly fills: dp1[mask] = min len of word whose mask ⊇ mask.
		for(int b = 0; b < ALPHABET_SIZE; b ++){
			final int bit = 1 << b;
			for(int mask = 0; mask < SIZE; mask ++){
				if((mask & bit) == 0){
					// mask does not contain bit b
					// dp1[mask] can be achieved by any word covering mask|bit
					final short sup = dp1[mask | bit];
					if(sup < dp1[mask])
						dp1[mask] = sup;
				}
			}
		}

		// dp[k] for k=2..K-1 are filled lazily in dpGet().
		// We precompute dp[1] (index 1, i.e. k=2) here to warm up the cache
		// for the most-used level (remaining = K-1 = 4 at depth 1).
		// Precomputing all levels upfront would cost O(K * 3^ALPHABET_SIZE) which
		// is too slow; lazy memoization is used instead.
		System.out.printf("  dp[1] seeded (%,d non-INF entries)%n",
			countNonInf(dp[0]));
	}

	/**
	 * Returns dp[k][mask]: minimum total length to cover all bits of {@code mask}
	 * using exactly {@code k} words.
	 * k=1 is precomputed; k≥2 is computed lazily via:
	 * dp[k][mask] = min over all non-empty submasks s of mask:
	 * dp[1][s] + dp[k-1][mask & ~s]
	 * and memoized.
	 *
	 * @param k    number of words (1 ≤ k ≤ K)
	 * @param mask missing-bits mask (subset of FULL_MASK)
	 * @return minimum total length, or {@link Short#MAX_VALUE} if unreachable
	 */
	private static int dpGet(final int k, final int mask){
		if(mask == 0)
			return 0;

		final short cached = dp[k - 1][mask];
		if(cached != DP_INF || k == 1)
			return cached & 0xFFFF;

		// Lazy computation for k ≥ 2
		// Iterate over all non-empty proper submasks of mask
		int best = DP_INF & 0xFFFF;
		final short[] dp1 = dp[0];
		for(int s = mask; s != 0; s = (s - 1) & mask){
			final int c1 = dp1[s] & 0xFFFF;
			if(c1 >= best)
				// fast path: c1 alone already too big
				continue;

			final int rest = mask & ~s;
			final int ck = dpGet(k - 1, rest);
			if(ck >= best - c1)
				continue;

			final int total = c1 + ck;
			if(total < best)
				best = total;
		}

		final short result = (best >= (DP_INF & 0xFFFF)? DP_INF: (short)best);
		dp[k - 1][mask] = result;
		return best;
	}

	private static int countNonInf(final short[] arr){
		int count = 0;
		for(int i = 0, arrLength = arr.length; i < arrLength; i ++)
			if(arr[i] != DP_INF)
				count ++;
		return count;
	}


	// ===== PHASE 3: BRANCH AND BOUND =====

	private static void branchAndBound(){
		final int nWords = words.size();
		final int maxStack = nWords * (K + 1) + 16;

		final int[] sStart = new int[maxStack];
		final int[] sDepth = new int[maxStack];
		final long[] sMask = new long[maxStack];
		final int[] sLen = new int[maxStack];
		final int[] sPath = new int[maxStack * K];

		sStart[0] = 0;
		sDepth[0] = 0;
		sMask[0] = 0l;
		sLen[0] = 0;
		int top = 0;
		int topPath = 0;

		final int[] pathBuf = new int[K];
		final long[] lWordM = wordMasks;
		final long[] lSuffix = suffixMasks;
		final int[] lLens = wordLens;

		while(!stopRequested && top >= 0){
			// ── Pop ──────────────────────────────────────────────────────────
			final int start = sStart[top];
			final int depth = sDepth[top];
			final long mask = sMask[top];
			final int len = sLen[top];
			System.arraycopy(sPath, topPath, pathBuf, 0, depth);
			top --;
			topPath -= K;

			runs ++;
			if(runs % LOG_EVERY == 0)
				System.out.printf("Run %,dM  bestLen = %d  depth = %d%n",
					runs / 1_000_000L, bestLen, depth);

			// ── Solution check ────────────────────────────────────────────────
			if(mask == FULL_MASK){
				if(depth == K && len < bestLen){
					bestLen = len;
					bestPath = Arrays.copyOf(pathBuf, depth);
					bestPathLen = depth;
					System.out.printf("  New best: length = %d  %s%n",
						bestLen, formatSolution(bestPath, bestPathLen));
				}
				continue;
			}

			// ── Pruning 1: suffix coverage ────────────────────────────────────
			if((mask | lSuffix[start]) != FULL_MASK)
				continue;

			if(depth == K)
				// used all K words, not covered
				continue;

			// ── Pruning 2: DP lower bound ─────────────────────────────────────
			final int remaining = K - depth;
			final int missing = (int)(FULL_MASK & ~mask);
			final int lb = dpGet(remaining, missing);
			if(len + lb >= bestLen)
				continue;

			// ── Expand ───────────────────────────────────────────────────────
			final int limit = nWords - remaining;
			for(int i = limit; i >= start; i --){
				final long wm = lWordM[i];

				// Must cover at least one missing bit
				if((wm & missing) == 0)
					continue;

				final int newLen = len + lLens[i];
				if(newLen >= bestLen)
					continue;

				final long newMask = mask | wm;

				// Anticipated suffix check
				if((newMask | lSuffix[i + 1]) != FULL_MASK)
					continue;

				// Anticipated DP lower bound
				if(remaining > 1){
					final int newMissing = (int)(FULL_MASK & ~newMask);
					if(newMissing != 0){
						final int childLb = dpGet(remaining - 1, newMissing);
						if(newLen + childLb >= bestLen)
							continue;
					}
				}

				// Push
				top++;
				topPath += K;
				sStart[top] = i + 1;
				sDepth[top] = depth + 1;
				sMask[top] = newMask;
				sLen[top] = newLen;
				System.arraycopy(pathBuf, 0, sPath, topPath, depth);
				sPath[topPath + depth] = i;
			}
		}
	}


	// ===== WORD LOADING & PREPROCESSING =====

	private static void loadWords() throws Exception{
		final InputStream is = Main3.class.getResourceAsStream("/" + WORDS_FILE);
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
		wordMasks = new long[words.size()];
		for(int i = 0; i < words.size(); i ++)
			wordMasks[i] = buildMask(words.get(i));

		final int raw = words.size();
		pruneDominatedWords();
		removeRedundantWords();
		pruneIdenticalMasks();
		System.out.printf("Pruning: %,d → %,d words%n", raw, words.size());

		// Sort: shortest first, then most bits (maximises early bestLen tightening)
		final int n = words.size();
		final Integer[] order = new Integer[n];
		for(int i = 0; i < n; i ++)
			order[i] = i;
		Arrays.sort(order, (a, b) -> {
			final int la = words.get(a).length();
			final int lb2 = words.get(b).length();
			if(la != lb2)
				return Integer.compare(la, lb2);

			return Integer.compare(Long.bitCount(wordMasks[b]), Long.bitCount(wordMasks[a]));
		});
		words = reorder(words, order);
		wordMasks = reorder(wordMasks, order);

		// Cache word lengths
		wordLens = new int[words.size()];
		for(int i = 0; i < words.size(); i ++)
			wordLens[i] = words.get(i).length();

		// Suffix-OR masks
		suffixMasks = new long[wordMasks.length + 1];
		for(int i = wordMasks.length - 1; i >= 0; i --)
			suffixMasks[i] = suffixMasks[i + 1] | wordMasks[i];

		System.out.printf("Loaded %,d words, alphabet = %d bits, coverage = %s%n",
			words.size(), ALPHABET_SIZE,
			suffixMasks[0] == FULL_MASK? "FULL": "INCOMPLETE");
	}

	private static void pruneDominatedWords(){
		final int n = words.size();
		final boolean[] keep = new boolean[n];
		Arrays.fill(keep, true);
		for(int i = 0; i < n; i ++){
			if(!keep[i]) continue;
			final long mi = wordMasks[i];
			final int li = words.get(i)
				.length();
			for(int j = 0; j < n; j ++){
				if(i == j || !keep[j])
					continue;

				final long mj = wordMasks[j];
				final int lj = words.get(j).length();
				if((mi & mj) == mi && lj <= li && (mj != mi || lj < li)){
					keep[i] = false;
					break;
				}
			}
		}
		rebuild(keep);
	}

	private static void removeRedundantWords(){
		final int n = words.size();
		final boolean[] redundant = new boolean[n];
		final Map<Integer, List<Integer>> byLength = new HashMap<>();
		for(int i = 0; i < n; i ++)
			byLength.computeIfAbsent(words.get(i).length(), x -> new ArrayList<>()).add(i);

		for(final List<Integer> group : byLength.values()){
			final int g = group.size();
			for(int a = 0; a < g; a ++){
				final int ia = group.get(a);
				if(redundant[ia])
					continue;

				final String wa = words.get(ia);
				final int len = wa.length();
				for(int b = 0; b < g; b ++){
					if(a == b)
						continue;

					final String wb = words.get(group.get(b));
					int diffPos = -1;
					boolean valid = true;
					for(int p = 0; p < len; p ++)
						if(wa.charAt(p) != wb.charAt(p)){
							if(diffPos >= 0){
								valid = false;
								break;
							}
							diffPos = p;
						}
					if(!valid || diffPos < 0)
						continue;

					final char replaced = wa.charAt(diffPos);
					int count = 0;
					for(int p = 0; p < len; p ++)
						if(wa.charAt(p) == replaced && ++ count == 2)
							break;
					if(count >= 2){
						redundant[ia] = true;
						break;
					}
				}
			}
		}
		final boolean[] keep = new boolean[n];
		for(int i = 0; i < n; i ++)
			keep[i] = !redundant[i];
		rebuild(keep);
	}

	private static void pruneIdenticalMasks(){
		final int n = words.size();
		final Map<Long, Integer> best = new HashMap<>();
		for(int i = 0; i < n; i ++){
			final long mask = wordMasks[i];
			best.merge(mask, i, (prev, cur) ->
				words.get(cur).length() < words.get(prev).length()? cur: prev);
		}
		final boolean[] keep = new boolean[n];
		for(final int idx : best.values())
			keep[idx] = true;
		rebuild(keep);
	}

	private static void rebuild(final boolean[] keep){
		int kept = 0;
		for(int i = 0, keepLength = keep.length; i < keepLength; i ++)
			if(keep[i])
				kept ++;
		final List<String> fw = new ArrayList<>(kept);
		final long[] fm = new long[kept];
		int out = 0;
		for(int i = 0; i < keep.length; i ++)
			if(keep[i]){
				fw.add(words.get(i));
				fm[out ++] = wordMasks[i];
			}
		words = fw;
		wordMasks = fm;
	}


	// ===== UTILITIES =====

	private static long buildMask(final String word){
		long mask = 0L;
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

	private static <T> List<T> reorder(final List<T> src, final Integer[] order){
		final List<T> result = new ArrayList<>(order.length);
		for(int i = 0, orderLength = order.length; i < orderLength; i ++)
			result.add(src.get(order[i]));
		return result;
	}

	private static long[] reorder(final long[] src, final Integer[] order){
		final long[] result = new long[order.length];
		for(int i = 0; i < order.length; i ++)
			result[i] = src[order[i]];
		return result;
	}

	private static String formatSolution(final int[] path, final int len){
		if(path == null || len == 0)
			return "(none)";

		final StringJoiner sj = new StringJoiner(" ");
		for(int i = 0; i < len; i ++)
			sj.add(words.get(path[i]));
		return sj.toString();
	}

	private static void printBest(){
		if(bestPath == null)
			System.out.println("No solution found.");
		else
			System.out.printf("Best pangram: totalLength = %d  words = %d%n  %s%n",
				bestLen, bestPathLen, formatSolution(bestPath, bestPathLen));
	}

	private static void writeSolution() throws IOException{
		if(bestPath == null)
			return;

		try(final BufferedWriter bw = new BufferedWriter(new FileWriter(SOLUTION_FILE, false))){
			bw.write("totalLength = " + bestLen + "  words = " + bestPathLen);
			bw.newLine();
			bw.write(formatSolution(bestPath, bestPathLen));
			bw.newLine();
		}
		System.out.println("Solution written to " + SOLUTION_FILE);
	}

}