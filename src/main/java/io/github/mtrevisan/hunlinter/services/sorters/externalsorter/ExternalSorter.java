/**
 * Copyright (c) 2019-2025 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.services.sorters.externalsorter;

import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.dictionary.MappedExternalSorter;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;


/**
 * @see <a href="https://github.com/Dgleish/ExternalSort/blob/master/src/uk/ac/cam/amd96/fjava/tick0/ExternalSort.java">DGleish External Sort</a>
 * @see <a href="https://github.com/lemire/externalsortinginjava">External-Memory Sorting in Java</a>, version 0.4.4, 11/3/2020
 */
public final class ExternalSorter{

	private ExternalSorter(){}

	/**
	 * Sort the input file into the output file, choosing the fastest available method.
	 *
	 * @param inputFile	The input text file path.
	 * @param outputFile	The output text file path.
	 * @param options	Sorting options (charset, comparator, temp, removeDuplicates, etc.)
	 */
	public static void sort(final File inputFile, final File outputFile, final ExternalSorterOptions options)
			throws IOException, InterruptedException{
		//try OS-native sort first
		final String os = System.getProperty("os.name")
			.toLowerCase(Locale.ROOT);

		boolean success = false;
		if(isWindows(os))
			//Windows: use cmd sort via redirection
			success = sortWindows(inputFile.toPath(), outputFile.toPath(), options);
		else if(isUnixLike(os))
			//Linux/macOS/Unix-like: use GNU/BSD sort
			success = sortUnixLike(inputFile.toPath(), outputFile.toPath(), options);

		//fallback: pure Java external merge sort (portable fallback)
		if(!success)
			success = sortMappedJava(inputFile.toPath(), outputFile.toPath(), options);

		//fallback: pure Java external merge sort (portable fallback)
		if(!success)
			sortExternalJava(inputFile.toPath(), outputFile.toPath(), options);
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
	private static boolean sortWindows(final Path input, final Path output, final ExternalSorterOptions options){
		try{
			if(!input.equals(output))
				//delete target first to ensure redirection creates it cleanly
				Files.deleteIfExists(output);

			//use cmd redirection for fastest path:
			final String uniqueParam = (options.isRemoveDuplicates()? "/unique": StringUtils.EMPTY);
			final Process p = new ProcessBuilder("cmd.exe", "/c",
					"sort", uniqueParam, "/o", output.toString(), input.toString())
				.redirectError(ProcessBuilder.Redirect.INHERIT)
				.start();
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
	private static boolean sortUnixLike(final Path input, final Path output, final ExternalSorterOptions options){
		try{
			//build command:
			//prefer GNU syntax: sort --parallel=N -S 2048M input -o output
			//if unsupported, BSD sort will ignore unknown flags or fail; we detect failure and fallback.
			final StringJoiner cmd = new StringJoiner(StringUtils.SPACE);
			cmd.add("LC_ALL=C")
				.add("sort");
			//try to set memory and parallelism (best effort)
			final int parallelism = options.getParallelism();
			final int osSortMemory = options.getOSSortMemory();
			if(parallelism > 0)
				//GNU-only
				cmd.add("--parallel=" + parallelism);
			if(osSortMemory > 0)
				//GNU-only
				cmd.add("-S " + osSortMemory + "M");
			//input and output
			cmd.add("\"" + input.toAbsolutePath() + "\" -o \"" + output.toAbsolutePath() + "\"");

			final ProcessBuilder pb = new ProcessBuilder("bash", "-lc", cmd.toString());
			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
			pb.environment().put("LANG", "C");
			pb.environment().put("LC_ALL", "C");

			final Process p = pb.start();
			final int code = p.waitFor();
			return (code == 0 && Files.exists(output));
		}
		catch(final Exception ignored){
			return false;
		}
	}


	/**
	 * Try the memory-mapped external sorter (NIO FileChannel.map) to maximize throughput
	 * on large files and SSD/NVMe. Returns true if completed successfully, false otherwise.
	 */
	private static boolean sortMappedJava(final Path input, final Path output, final ExternalSorterOptions options){
		try{
			MappedExternalSorter.sort(input, output, options.getCharset(), Math.max(256, options.getMappedMemory()),
				Math.max(128, options.getMappedWindow()), options.getComparator());

			return (Files.exists(output) && Files.size(output) > 0);
		}
		catch(final Exception ignored){
			return false;
		}
	}

	/**
	 * Pure Java external merge sort fallback, portable and robust.
	 * Strategy:
	 * - Read the input file in chunks (based on estimated blockSize).
	 * - Sort each chunk in-memory and write a temp sorted run.
	 * - Merge all runs with a k-way merge (PriorityQueue).
	 */
	private static void sortExternalJava(final Path input, final Path output, final ExternalSorterOptions options)
			throws IOException{
		//extract uncompressed file size
		final long dataLength = FileHelper.getFileSize(input.toFile());
		final long blockSize = estimateBestBlockSize(dataLength);

		final List<File> runs = new ArrayList<>((int)Math.ceil((double)dataLength / Math.max(1, blockSize)));

		//phase 1: make sorted runs
		try(final BufferedReader reader = FileHelper.createBufferedReader(input, options.getCharset(),
				options.getZipBufferSize(), options.getReaderBufferSize())){
			//[B]
			long currentBlockSize = 0l;

			final StringArrayList buffer = new StringArrayList(1_000_000, 1.5f);
			String line;
			while((line = reader.readLine()) != null){
				buffer.add(line);

				currentBlockSize += StringSizeEstimator.estimatedSizeOf(line);

				//if there is not enough memory
				if(currentBlockSize >= blockSize){
					//sort and write chunk
					final File chunkFile = writeSortedChunk(buffer, options);

					//add chunk to the list of chunks
					runs.add(chunkFile);

					currentBlockSize = 0l;
					buffer.clear();
				}
			}

			if(buffer.size() > 0){
				//sort and write chunk
				final File chunkFile = writeSortedChunk(buffer, options);

				//add chunk to the list of chunks
				runs.add(chunkFile);

				buffer.clear();
			}
		}

		//phase 2: k-way merge with PriorityQueue
		mergeWithFanInLimit(runs, output.toFile(), options, 256);
	}

	/** Sorts lines in-memory and writes a temporary sorted run file. */
	private static File writeSortedChunk(final StringArrayList lines, final ExternalSorterOptions options)
			throws IOException{
		//sort in-place
		final Comparator<String> comparator = options.getComparator();
		if(lines.size() >= options.getParallelSortThreshold())
			lines.parallelSort(comparator);
		else
			lines.sort(comparator);

		//store chunk
		final File chunkFile = FileHelper.createDeleteOnExitFile("hunlinter-pos-chunk", ".dat");
		OutputStream out = new FileOutputStream(chunkFile);
		if(options.isUseTemporaryAsZip())
			out = new MyGZIPOutputStream(out, options);
		saveChunk(lines, out, options);

		return chunkFile;
	}

	/**
	 * Estimates a good block size for external sorting using only the file size.
	 *
	 * Heuristics:
	 *  - Use ~70% of currently available memory to avoid OOM with String overheads.
	 *  - Target number of runs ~ 2 × effective cores (keeps CPU busy without creating too many runs).
	 *  - Effective cores are capped to avoid over-parallelization on systems with many logical CPUs.
	 *  - Enforce a minimum block size to avoid tiny runs that bloat the merge phase.
	 *
	 * @param sizeOfFile how much data (in bytes) can we expect
	 * @return the estimate [B]
	 */
	private static long estimateBestBlockSize(final long sizeOfFile){
		//1) Memory target: ~70% of available heap, not below a sane minimum
		final long availableMemory = JavaHelper.estimateAvailableMemory();
		final long memoryTarget = Math.max((long)(availableMemory * 0.70), ExternalSorterOptions.MINIMUM_MEMORY_TARGET);

		//2) Effective cores: cap logical CPUs to avoid over-parallelization
		// Rationale:
		//  - Hyper-Threading inflates availableProcessors(); we cap to a reasonable top.
		//  - Without device hints (NVMe/SSD/HDD), use a conservative but effective cap.
		final int logicalCores = Math.max(1, Runtime.getRuntime().availableProcessors());
		//conservative cap without storage hints
		final int effectiveCores = Math.min(logicalCores, 8);

		//3) Target runs ~ 2 × effective cores (stable scheduling, avoids too many runs)
		final long targetRuns = Math.max(2l * (long)effectiveCores, 2l);

		//4) Run size from runs count, but never below the minimum block size
		final long runSizeByCores = Math.max(sizeOfFile / targetRuns, ExternalSorterOptions.MINIMUM_MEMORY_TARGET);

		//5) Final block size: limited by memory target, but large enough
		return Math.max(Math.min(memoryTarget, runSizeByCores), ExternalSorterOptions.MINIMUM_MEMORY_TARGET);
	}

	/**
	 * Save a sorted list to a temporary file
	 *
	 * @param sortedLines	Data to be sorted
	 * @param out	The output stream
	 * @param options	Sorting options
	 * @throws IOException	Generic IO exception
	 */
	private static void saveChunk(final StringArrayList sortedLines, final OutputStream out,
			final ExternalSorterOptions options) throws IOException{
		try(final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, options.getCharset()),
				Math.max(1 << 20, options.getZipBufferSize()))){
			final boolean removeDuplicates = options.isRemoveDuplicates();
			String lastLine = null;
			for(int i = 0, length = sortedLines.size(); i < length; i ++){
				final String line = sortedLines.get(i);

				//skip duplicated lines
				if(!removeDuplicates || !line.equals(lastLine)){
					writer.write(line);
					writer.write(options.getLineSeparator());

					lastLine = line;
				}
			}
		}
	}

	private static void mergeWithFanInLimit(final Collection<File> files, final File outputFile,
			final ExternalSorterOptions options, final int fanInLimit) throws IOException{
		List<File> current = new ArrayList<>(files);
		final List<File> next = new ArrayList<>();
		while(current.size() > fanInLimit){
			next.clear();

			for(int i = 0; i < current.size(); i += fanInLimit){
				final List<File> group = current.subList(i, Math.min(i + fanInLimit, current.size()));
				final File intermediateFile = FileHelper.createDeleteOnExitFile("hunlinter-pos-chunk-intermediate", ".dat");
				mergeSortedFiles(group, intermediateFile, options);

				next.add(intermediateFile);
			}

			current = new ArrayList<>(next);
		}

		//final merge
		mergeSortedFiles(current, outputFile, options);
	}

	/**
	 * This merges a bunch of temporary flat files
	 *
	 * @param files The {@link List} of sorted {@link File}s to be merged
	 * @param options	Sorting options
	 * @param outputFile The output {@link File} to merge the results to
	 * @throws IOException generic IO exception
	 */
	private static void mergeSortedFiles(final Collection<File> files, final File outputFile,
			final ExternalSorterOptions options) throws IOException{
		//min-heap by line content
		final Comparator<String> comparator = options.getComparator();
		final Queue<BinaryFileBuffer> queue = new PriorityQueue<>(files.size(),
			(i, j) -> comparator.compare(i.peek(), j.peek()));

		final int runReadBuffer = Math.max(1 << 20, options.getZipBufferSize());
		//prime the heap
		for(final File file : files){
			if(file.length() == 0)
				continue;

			final BufferedReader reader = FileHelper.createBufferedReader(file.toPath(), options.getCharset(),
				options.getZipBufferSize(), runReadBuffer);
			final BinaryFileBuffer bfb = new BinaryFileBuffer(reader);
			if(!bfb.isEmpty())
				queue.add(bfb);
			else
				bfb.close();
		}

		//merge into output (optionally GZIP)
		OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile),
			Math.max(1 << 20, options.getZipBufferSize()));
		if(options.isWriteOutputAsZip())
			out = new MyGZIPOutputStream(out, options);
		mergeSortedFiles(out, options, queue);

		//delete temporary runs
		for(final File file : files){
			try{
				Files.deleteIfExists(file.toPath());
			}
			catch(final Exception ignored){}
		}
	}

	/**
	 * This merges several BinaryFileBuffer to an output writer.
	 *
	 * @param out	The output stream where writing the data
	 * @param options	Sorting options
	 * @param queue	Where the data should be read
	 * @throws IOException generic IO exception
	 */
	private static void mergeSortedFiles(final OutputStream out, final ExternalSorterOptions options,
			final Queue<BinaryFileBuffer> queue) throws IOException{
		try(final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, options.getCharset()),
				Math.max(1 << 20, options.getZipBufferSize()))){
			mergeSort(queue, options.isRemoveDuplicates(), writer, options.getLineSeparator());
		}
	}

	private static void mergeSort(final Queue<BinaryFileBuffer> queue, final boolean removeDuplicates,
			final BufferedWriter writer, final String lineSeparator) throws IOException{
		String lastLine = null;
		while(!queue.isEmpty()){
			final BinaryFileBuffer buffer = queue.poll();
			final String line = buffer.pop();

			//skip duplicated lines
			if(!removeDuplicates || !line.equals(lastLine)){
				writer.write(line);
				writer.write(lineSeparator);
				lastLine = line;
			}

			if(buffer.isEmpty())
				buffer.close();
			else
				//add it back
				queue.add(buffer);
		}
	}

	private static final class MyGZIPOutputStream extends GZIPOutputStream{
		private MyGZIPOutputStream(final OutputStream out, final ExternalSorterOptions options) throws IOException{
			super(out, options.getZipBufferSize());

			//favor speed over ratio
			def.setLevel(Deflater.BEST_SPEED);
		}
	}

}
