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
package io.github.mtrevisan.hunlinter.services.sorters.externalsorter;

import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;


/**
 * @see <a href="https://github.com/Dgleish/ExternalSort/blob/master/src/uk/ac/cam/amd96/fjava/tick0/ExternalSort.java">DGleish External Sort</a>
 * @see <a href="https://github.com/lemire/externalsortinginjava">External-Memory Sorting in Java</a>, version 0.4.4, 11/3/2020
 */
public class ExternalSorter{

	@SuppressWarnings("OverlyBroadThrowsClause")
	public static void sort(final File inputFile, final ExternalSorterOptions options, final File outputFile) throws IOException{
		final List<File> files = splitAndSortFiles(inputFile, options);

		if(!files.isEmpty())
			mergeSortedFiles(files, options, outputFile);
	}

	/**
	 * This will simply load the file by blocks of lines, then sort them in-memory,
	 * and write the result to temporary files that have to be
	 * merged later.
	 *
	 * @param file	Some flat file.
	 * @param options	Sorting options.
	 * @return	A list of temporary flat files.
	 * @throws IOException	Generic IO exception.
	 */
	@SuppressWarnings("OverlyBroadThrowsClause")
	private static List<File> splitAndSortFiles(final File file, final ExternalSorterOptions options) throws IOException{
		//extract uncompressed file size
		final long dataLength = FileHelper.getFileSize(file);
		final long availableMemory = JavaHelper.estimateAvailableMemory();
		final long blockSize = estimateBestSizeOfBlocks(dataLength, options, availableMemory);

		final List<File> files = new ArrayList<>((int)Math.ceil((double)dataLength / blockSize));
		try(final Scanner scanner = FileHelper.createScanner(file.toPath(), options.getCharset(), options.getZipBufferSize())){
			final StringArrayList temporaryList = new StringArrayList(5_000_000);
			while(scanner.hasNextLine()){
				//[B]
				long currentBlockSize = 0l;
				//as long as there is enough memory
				while(currentBlockSize < blockSize && scanner.hasNextLine()){
					final String line = scanner.nextLine();
					temporaryList.add(line);

					currentBlockSize += StringSizeEstimator.estimatedSizeOf(line);
				}

				//sort list
				final Comparator<String> comparator = options.getComparator();
				if(options.isSortInParallel())
					temporaryList.parallelSort(comparator);
				else
					temporaryList.sort(comparator);

				//store chunk
				final File chunkFile = FileHelper.createDeleteOnExitFile("hunlinter-pos-chunk", ".dat");
				OutputStream out = new FileOutputStream(chunkFile);
				if(options.isUseTemporaryAsZip())
					out = new MyGZIPOutputStream(out, options);
				saveChunk(temporaryList, options, out);

				//add chunk to list of chunks
				files.add(chunkFile);

				//prepare for next iteration
				temporaryList.clear();
			}
		}
		return files;
	}

	/**
	 * Divide the file into small blocks.
	 * If the blocks are too small, we shall create too many temporary files. If they are too big, we shall
	 * be using too much memory.
	 *
	 * @param sizeOfFile how much data (in bytes) can we expect
	 * @param options	Sorting options
	 * @param maxMemory Maximum memory to use (in bytes)
	 * @return the estimate [B]
	 */
	private static long estimateBestSizeOfBlocks(final long sizeOfFile, final ExternalSorterOptions options, final long maxMemory){
		//we don't want to open up much more than maxTemporaryFiles temporary files, better run out of memory first
		final long maxTemporaryFiles = options.getMaxTemporaryFiles();
		long blockSize = sizeOfFile / maxTemporaryFiles + (sizeOfFile % maxTemporaryFiles == 0l? 0l: 1l);

		//on the other hand, we don't want to create many temporary files for naught: if {@code blockSize} is smaller
		//than half the free memory, grow it
		if((blockSize << 1) < maxMemory)
			blockSize = maxMemory >> 1;

		final long maxTemporaryFileSize = options.getMaxTemporaryFileSize();
		if(maxTemporaryFileSize != ExternalSorterOptions.MAX_TEMPORARY_FILE_SIZE_UNLIMITED && blockSize > maxTemporaryFileSize)
			blockSize = maxTemporaryFileSize;

		return blockSize;
	}

	/**
	 * Save a sorted list to a temporary file
	 *
	 * @param sortedLines	Data to be sorted
	 * @param options	Sorting options
	 * @param out	The output stream
	 * @throws IOException generic IO exception
	 */
	private static void saveChunk(final StringArrayList sortedLines, final ExternalSorterOptions options, final OutputStream out)
			throws IOException{
		try(final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, options.getCharset()))){
			final boolean removeDuplicates = options.isRemoveDuplicates();
			String lastLine = null;
			for(int i = 0; i < sortedLines.size(); i ++){
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

	/**
	 * This merges a bunch of temporary flat files
	 *
	 * @param files The {@link List} of sorted {@link File}s to be merged
	 * @param options	Sorting options
	 * @param outputFile The output {@link File} to merge the results to
	 * @throws IOException generic IO exception
	 */
	@SuppressWarnings("OverlyBroadThrowsClause")
	private static void mergeSortedFiles(final Collection<File> files, final ExternalSorterOptions options, final File outputFile)
			throws IOException{
		final Comparator<String> comparator = options.getComparator();
		final Queue<BinaryFileBuffer> queue = new PriorityQueue<>(files.size(),
			(i, j) -> comparator.compare(i.peek(), j.peek()));
		for(final File file : files){
			if(file.length() == 0)
				continue;

			final Scanner scanner = FileHelper.createScanner(file.toPath(), options.getCharset());
			if(scanner.hasNextLine())
				queue.add(new BinaryFileBuffer(scanner));
			else
				scanner.close();
		}

		OutputStream out = new FileOutputStream(outputFile);
		if(options.isWriteOutputAsZip())
			out = new MyGZIPOutputStream(out, options);
		mergeSortedFiles(out, options, queue);
		for(final File file : files)
			file.delete();
	}

	/**
	 * This merges several BinaryFileBuffer to an output writer.
	 *
	 * @param out	The output stream where writing the data
	 * @param options	Sorting options
	 * @param queue	Where the data should be read
	 * @throws IOException generic IO exception
	 */
	private static void mergeSortedFiles(final OutputStream out, final ExternalSorterOptions options, final Queue<BinaryFileBuffer> queue)
			throws IOException{
		try(final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, options.getCharset()))){
			mergeSort(queue, options.isRemoveDuplicates(), writer, options.getLineSeparator());
		}
		finally{
			for(final BinaryFileBuffer buffer : queue)
				buffer.close();
		}
	}

	private static void mergeSort(final Queue<BinaryFileBuffer> queue, final boolean removeDuplicates, final BufferedWriter writer,
			final String lineSeparator) throws IOException{
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

			def.setLevel(Deflater.BEST_SPEED);
		}
	}

}
