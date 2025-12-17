package io.github.mtrevisan.hunlinter.workers.dictionary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.PriorityQueue;


/**
 * SmartFileSorter: OS-aware large-file sorting utility.
 *
 * Strategy:
 *  - Detect OS.
 *  - On Windows: use native "sort.exe" via ProcessBuilder (fastest).
 *  - On Linux/macOS/Unix-like: use native "sort" (GNU/BSD).
 *  - If OS command is unavailable or fails: fallback to pure-Java external merge sort.
 *
 * Notes:
 *  - Sort is lexicographical on full lines.
 *  - Assumes input is a text file with one record per line.
 *  - Uses UTF-8 by default; adjust if needed.
 */
public class SmartFileSorter{

	private SmartFileSorter(){}


	/**
	 * Sort the input file into the output file, choosing the fastest available method.
	 *
	 * @param input	The input text file path.
	 * @param output	The output text file path (will be overwritten).
	 * @param parallelism	Suggested parallelism for OS sort (ignored by Windows cmd sort).
	 * @param memoryMB	Suggested memory (MB) for OS sort (Linux/macOS) and for Java fallback chunking.
	 */
	public static void sort(Path input, Path output, int parallelism, int memoryMB, Comparator<String> comparator)
			throws IOException, InterruptedException{
		Objects.requireNonNull(input, "input cannot be null");
		Objects.requireNonNull(output, "output cannot be null");

		//ensure output parent exists
		final Path parent = output.toAbsolutePath()
			.getParent();
		if(parent != null)
			Files.createDirectories(parent);

		//try OS-native sort first
		final String os = System.getProperty("os.name")
			.toLowerCase(Locale.ROOT);

		boolean success = false;
		if(isWindows(os))
			//Windows: use cmd sort
			success = sortWindows(input, output);
		else if(isUnixLike(os))
			//Linux/macOS/Unix-like: use GNU/BSD sort
			success = sortUnixLike(input, output, parallelism, memoryMB);

		//fallback: pure Java external merge sort
		if(!success)
			sortExternalJava(input, output, memoryMB, comparator);
	}

	private static boolean isWindows(final String osName){
		return osName.contains("win");
	}

	private static boolean isUnixLike(final String osName){
		return osName.contains("nux")	//Linux
			|| osName.contains("mac")	//macOS
			|| osName.contains("nix")	//generic *nix
			|| osName.contains("aix")	//IBM AIX
			|| osName.contains("bsd")	//FreeBSD/OpenBSD/NetBSD
			;
	}

	/**
	 * Use Windows cmd.exe sort to sort the file.
	 * Equivalent to: cmd /c "sort input > output"
	 *
	 * @return true if completed successfully, false otherwise
	 */
	private static boolean sortWindows(final Path input, final Path output){
		try{
			//delete target first to ensure redirection creates it cleanly
			Files.deleteIfExists(output);

			//use cmd redirection for fastest path:
			//Note: Windows sort does not accept -o; redirection is required.
			final String cmd = String.format("sort \"%s\" > \"%s\"", input.toAbsolutePath(), output.toAbsolutePath());

			final ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", cmd);
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			final Process p = pb.start();
			final int code = p.waitFor();
			return (code == 0 && Files.exists(output));
		}
		catch(final Exception ignored){
			return false;
		}
	}

	/**
	 * Use Unix-like "sort" (GNU/BSD) with optional parallelism and memory.
	 * On GNU sort: --parallel and -S are supported; on BSD sort they may be ignored.
	 *
	 * @return true if completed successfully, false otherwise
	 */
	private static boolean sortUnixLike(final Path input, final Path output, final int parallelism, final int memoryMB){
		try{
			//build command:
			//prefer GNU syntax: sort --parallel=N -S 2048M input -o output
			//if unsupported, BSD sort will ignore unknown flags or fail; we detect failure and fallback.
			final List<String> cmd = new ArrayList<>();
			cmd.add("sort");

			//try to set memory and parallelism (best effort)
			if(parallelism > 0)
				//GNU sort only
				cmd.add("--parallel=" + parallelism);
			if(memoryMB > 0){
				//GNU sort only
				cmd.add("-S");
				cmd.add(memoryMB + "M");
			}

			//input and output
			cmd.add(input.toAbsolutePath().toString());
			cmd.add("-o");
			cmd.add(output.toAbsolutePath().toString());

			final ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			final Process p = pb.start();
			final int code = p.waitFor();
			return (code == 0 && Files.exists(output));
		}
		catch(final Exception ignored){
			return false;
		}
	}

	/**
	 * Pure Java external merge sort fallback, portable and robust.
	 * Strategy:
	 * - Read the input file in chunks (based on memoryMB).
	 * - Sort each chunk in-memory and write a temp sorted run.
	 * - Merge all runs with a k-way merge (PriorityQueue).
	 */
	private static void sortExternalJava(final Path input, final Path output, final int memoryMB,
			final Comparator<String> comparator) throws IOException{
		//choose chunk size in lines (heuristic)
		final int avgLineBytes = 32;
		final long bytesBudget = (long)(memoryMB * 0.2);
		final int maxLinesInMemory = (int)Math.max(200_000, Math.min(Integer.MAX_VALUE, bytesBudget / avgLineBytes));

		final List<Path> runs = new ArrayList<>();

		//phase 1: make sorted runs
		try(final BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)){
			final List<String> buffer = new ArrayList<>(maxLinesInMemory);
			String line;
			while((line = reader.readLine()) != null){
				buffer.add(line);
				if(buffer.size() >= maxLinesInMemory){
					runs.add(writeSortedRun(buffer, comparator));
					buffer.clear();
				}
			}
			if(!buffer.isEmpty()){
				runs.add(writeSortedRun(buffer, comparator));
				buffer.clear();
			}
		}

		//phase 2: k-way merge
		mergeRuns(runs, output);

		//cleanup
		for(final Path p : runs){
			try{
				Files.deleteIfExists(p);
			}
			catch(final IOException ignored){}
		}
	}

	/** Sorts lines in-memory and writes a temporary sorted run file. */
	private static Path writeSortedRun(final List<String> lines, final Comparator<String> comparator) throws IOException{
		//sort in-place
		lines.sort(comparator);

		final Path run = Files.createTempFile("sort-run-", ".txt");
		try(final BufferedWriter w = Files.newBufferedWriter(run, StandardCharsets.UTF_8, StandardOpenOption.WRITE)){
			for(int i = 0, length = lines.size(); i < length; i ++){
				w.write(lines.get(i));
				w.newLine();
			}
		}
		return run;
	}

	/** Merge all sorted runs into the final output (k-way merge with PriorityQueue). */
	private static void mergeRuns(final List<Path> runs, final Path output) throws IOException{
		//open all readers
		final List<BufferedReader> readers = new ArrayList<>(runs.size());
		for(final Path run : runs)
			readers.add(Files.newBufferedReader(run, StandardCharsets.UTF_8));

		//min-heap by line content
		final PriorityQueue<RunEntry> pq = new PriorityQueue<>(Comparator.comparing(e -> e.line));

		//prime the heap
		for(final BufferedReader reader : readers){
			final String line = reader.readLine();
			if(line != null)
				pq.add(new RunEntry(line, reader));
		}

		//merge
		try(final BufferedWriter w = Files.newBufferedWriter(output, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)){
			while(!pq.isEmpty()){
				final RunEntry e = pq.poll();
				w.write(e.line);
				w.newLine();

				final String next = e.reader.readLine();
				if(next != null)
					pq.add(new RunEntry(next, e.reader));
			}
		}

		//close readers
		for(final BufferedReader reader : readers){
			try{
				reader.close();
			}
			catch(final IOException ignored){
			}
		}
	}

	/** Entry used in the k-way merge heap. */
	private static final class RunEntry{
		final String line;
		final BufferedReader reader;

		RunEntry(final String line, final BufferedReader reader){
			this.line = line;
			this.reader = reader;
		}
	}

}
