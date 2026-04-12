package io.github.mtrevisan.hunlinter;

import java.io.*;
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
 * - aggressive pruning
 * - rarity-based word ordering
 * - no array cloning per node
 * - explicit stack (no recursion)
 * - race-free checkpointing
 */
public class Main{

	// ===== CONFIGURATION =====
	private static final int K = 5;
	private static final long LOG_EVERY = 1_000_000_000l;
	private static final String CHECKPOINT_FILE = "checkpoint.bin";
	private static final String SOLUTIONS_FILE = "solutions.txt";

	// Fixed alphabet (27 letters)
	private static final List<Character> ALPHABET = List.of(
		'Ï', 'Ü', 'C', 'Đ', 'Ñ', 'J', 'B', 'Ŧ', 'Ò', 'F', 'G', 'X', 'È',
		'U', 'M', 'V', 'P', 'D', 'K', 'S', 'L', 'T', 'R', 'N', 'I', 'O', 'A', 'E'
	);

	private static final int ALPHABET_SIZE = ALPHABET.size();
	private static final long FULL_MASK = (1l << ALPHABET_SIZE) - 1;


	private static List<String> words;
	private static long[] wordMasks;
	private static long[] suffixMasks;

	private static long runs = 0;
	private static volatile boolean stopRequested = false;
	private static BufferedWriter solutionsWriter;


	// ===== DATA STRUCTURES =====

	/**
	 * One explicit DFS node.
	 * 'choice' is the index chosen at this depth; the full path
	 * is reconstructed only when needed.
	 */
	private static final class Frame implements Serializable{
		final int start;
		final int depth;
		final long mask;
		final int choice;
		final Frame parent;

		Frame(final int start, final int depth, final long mask, final int choice, final Frame parent){
			this.start = start;
			this.depth = depth;
			this.mask = mask;
			this.choice = choice;
			this.parent = parent;
		}

	}

	private static final class Checkpoint implements Serializable{
		final Deque<Frame> stack;
		final long runs;

		Checkpoint(final Deque<Frame> stack, final long runs){
			this.stack = stack;
			this.runs = runs;
		}

	}


	// ===== MAIN =====

	public static void main(final String[] args) throws Exception{
		final Deque<Frame> stack = init();

		solutionsWriter = new BufferedWriter(new FileWriter(SOLUTIONS_FILE, true));

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.err.println("\nShutdown requested.");
			stopRequested = true;
		}));

		runIterativeSearch(stack);
	}


	// ===== SEARCH =====

	private static void runIterativeSearch(final Deque<Frame> stack){
		while(!stopRequested){
			final Frame f = stack.poll();
			if(f == null)
				break;

			runs ++;
			if(runs % LOG_EVERY == 0)
				System.out.printf("Run %d: %s%n", runs / LOG_EVERY, formatCombo(f));
			//periodic checkpoint
			if((runs & 0x3FFF_FFFF) == 0){
				try{
					saveCheckpoint(stack);
				}
				catch(final Exception e){
					e.printStackTrace();
				}
			}

			//classic suffix pruning
			if(Long.bitCount(f.mask | suffixMasks[f.start]) < ALPHABET_SIZE)
				continue;

			//solution
			if(f.depth == K){
				if(f.mask == FULL_MASK)
					writeSolution(f);

				continue;
			}

			final int remainingSlots = K - f.depth;

			//try next candidates
			for(int i = words.size() - remainingSlots; i >= f.start; i --){
				final long added = wordMasks[i] & ~f.mask;
				if(added == 0)
					//word adds nothing: prune immediately
					continue;

				final long newMask = f.mask | wordMasks[i];

				stack.push(new Frame(
					i + 1,
					f.depth + 1,
					newMask,
					i,
					f
				));
			}
		}

		try{
			saveCheckpoint(stack);
			System.err.println("Checkpoint saved.");
		}
		catch(final Exception e){
			e.printStackTrace();
		}
	}


	// ===== INIT / CHECKPOINT =====

	private static Deque<Frame> init() throws Exception{
		loadWords();

		final File f = new File(CHECKPOINT_FILE);
		if(!f.exists() || f.length() == 0){
			final Deque<Frame> stack = new ArrayDeque<>();
			stack.push(new Frame(0, 0, 0l, -1, null));
			return stack;
		}

		try(final ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))){
			final Checkpoint cp = (Checkpoint)in.readObject();
			System.out.printf("Checkpoint loaded, runs=%,d%n", cp.runs);
			return cp.stack;
		}
	}

	private static void saveCheckpoint(final Deque<Frame> stack) throws Exception{
		final Path target = Paths.get(CHECKPOINT_FILE);
		final Path tmp = Paths.get(CHECKPOINT_FILE + ".tmp");
		try(final ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp, StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING))){
			out.writeObject(new Checkpoint(new ArrayDeque<>(stack), runs));
			out.flush();
		}

		try{
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch(final AtomicMoveNotSupportedException e){
			//fallback non-atomic but safe
			Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
		}

//		System.out.println("Checkpoint written to " + target.toAbsolutePath());
	}


	// ===== WORD LOADING & PREPROCESSING =====

	private static void loadWords() throws Exception{
		final InputStream is = Main.class.getResourceAsStream("/words.txt");
		if(is == null)
			throw new IllegalStateException("words.txt not found");

		final Set<String> unique = new LinkedHashSet<>();
		try(final BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){
			String line;
			while((line = br.readLine()) != null)
				if(!line.isBlank())
					unique.add(line.trim());
		}

		words = new ArrayList<>(unique);
words = new ArrayList<>(List.of("furbïòco", "maŧapïòci", "đüekin", "dexgatéjis", "vañèl"));
		final int size = words.size();
		wordMasks = new long[size];
		for(int i = 0; i < size; i ++)
			wordMasks[i] = buildMask(words.get(i));

		// --- Letter frequency (rarity heuristic)
		final int[] freq = new int[ALPHABET_SIZE];
		for(int j = 0, wordMasksLength = wordMasks.length; j < wordMasksLength; j ++){
			final long m = wordMasks[j];
			for(int i = 0; i < ALPHABET_SIZE; i ++)
				if((m & (1L << i)) != 0)
					freq[i] ++;
		}

		// --- Sort by rarity score
		final Integer[] order = new Integer[size];
		for(int i = 0; i < order.length; i ++)
			order[i] = i;

		Arrays.sort(order, (a, b) -> {
			final double sa = score(wordMasks[a], freq);
			final double sb = score(wordMasks[b], freq);
			return Double.compare(sb, sa);
		});

		words = reorder(words, order);
		wordMasks = reorder(wordMasks, order);

		// --- Suffix masks
		suffixMasks = new long[wordMasks.length + 1];
		for(int i = wordMasks.length - 1; i >= 0; i --)
			suffixMasks[i] = suffixMasks[i + 1] | wordMasks[i];
	}

	private static double score(final long mask, final int[] freq){
		double score = 0.;
		for(int i = 0; i < ALPHABET_SIZE; i ++)
			if((mask & (1l << i)) != 0)
				score += 1. / freq[i];
		return score;
	}


	// ===== UTILITIES =====

	private static long buildMask(final String word){
		long mask = 0l;
		final String lower = word.toLowerCase(Locale.ROOT);
		for(int i = 0; i < ALPHABET_SIZE; i ++){
			final char c = Character.toLowerCase(ALPHABET.get(i));
			if(lower.indexOf(c) >= 0)
				mask |= (1l << i);
		}
		return mask;
	}

	private static <T> List<T> reorder(final List<T> src, final Integer[] order){
		final List<T> result = new ArrayList<>(order.length);
		for(int j = 0, length = order.length; j < length; j ++)
			result.add(src.get(order[j]));
		return result;
	}

	private static long[] reorder(final long[] src, final Integer[] order){
		final long[] result = new long[order.length];
		for(int i = 0, length = order.length; i < length; i ++)
			result[i] = src[order[i]];
		return result;
	}

	private static String formatCombo(final Frame f){
		final List<String> tmp = new ArrayList<>();
		Frame cur = f;
		while(cur != null && cur.choice >= 0){
			tmp.add(words.get(cur.choice));

			cur = cur.parent;
		}
		Collections.reverse(tmp);
		return tmp.toString();
	}

	/** Writes a valid solution to the solutions file (append-only). */
	private static void writeSolution(final Frame f){
		try{
			int totalLength = 0;
			Frame cur = f;
			while(cur != null && cur.choice >= 0){
				totalLength += words.get(cur.choice)
					.length();

				cur = cur.parent;
			}

			final double duplicateRatio = (double)totalLength / ALPHABET_SIZE - 1.;
			//keep only solutions with a duplicate ratio of 49% or less
			if(duplicateRatio > 0.49)
				return;

			final Deque<String> stack = new ArrayDeque<>();
			cur = f;
			while(cur != null && cur.choice >= 0){
				stack.push(words.get(cur.choice));

				cur = cur.parent;
			}

			final StringJoiner sj = new StringJoiner(" ");
			while(!stack.isEmpty())
				sj.add(stack.pop());

			final String solution = String.format(Locale.ROOT, "%.2f", duplicateRatio) + ": " + sj;
			solutionsWriter.write(solution);
			solutionsWriter.newLine();
			solutionsWriter.flush(); // ensures data is never lost

			System.out.printf("%s%n", solution);
		}
		catch(final IOException e){
			throw new UncheckedIOException(e);
		}
	}

}
