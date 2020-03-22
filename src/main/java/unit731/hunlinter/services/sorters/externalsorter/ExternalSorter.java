package unit731.hunlinter.services.sorters.externalsorter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import unit731.hunlinter.services.FileHelper;

import static unit731.hunlinter.services.system.LoopHelper.applyIf;
import static unit731.hunlinter.services.system.LoopHelper.forEach;


/**
 * @see <a href="https://github.com/Dgleish/ExternalSort/blob/master/src/uk/ac/cam/amd96/fjava/tick0/ExternalSort.java">DGleish External Sort</a>
 * @see <a href="https://github.com/lemire/externalsortinginjava">External-Memory Sorting in Java</>, version 0.4.4, 11/3/2020
 */
public class ExternalSorter{

	public void sort(final File inputFile, final ExternalSorterOptions options, final File outputFile) throws IOException{
		final List<File> files = splitAndSortFiles(inputFile, options);
		mergeSortedFiles(files, options, outputFile);
	}

	/**
	 * This will simply load the file by blocks of lines, then sort them in-memory, and write the result to temporary files that have to be
	 * merged later.
	 *
	 * @param file	Some flat file
	 * @param options	Sorting options
	 * @return a list of temporary flat files
	 * @throws IOException generic IO exception
	 */
	private List<File> splitAndSortFiles(final File file, final ExternalSorterOptions options) throws IOException{
		final Scanner scanner = FileHelper.createScanner(file.toPath(), options.getCharset());
		final long dataLength = file.length();
		final long blockSize = estimateBestSizeOfBlocks(dataLength, options.getMaxTemporaryFiles(), estimateAvailableMemory());
		return splitAndSortFiles(scanner, options, blockSize);
	}

	/**
	 * This will simply load the file by blocks of lines, then sort them in-memory, and write the result to temporary files that have to be
	 * merged later.
	 *
	 * @param scanner	Data source
	 * @param options	Sorting options
	 * @param blockSize	Block size [B]
	 * @return a list of temporary flat files
	 * @throws IOException generic IO exception
	 */
	private List<File> splitAndSortFiles(final Scanner scanner, final ExternalSorterOptions options, final long blockSize)
			throws IOException{
		final List<File> files = new ArrayList<>();
		try(scanner){
			final List<String> headers = new ArrayList<>();
			List<String> temporaryList = new ArrayList<>();
			while(scanner.hasNextLine()){
				//[B]
				long currentBlockSize = 0l;
				//as long as there is enough memory
				while(currentBlockSize < blockSize && scanner.hasNextLine()){
					final String line = scanner.nextLine();
					if(headers.size() < options.getSkipHeaderLines())
						headers.add(line);
					else{
						temporaryList.add(line);
						currentBlockSize += StringSizeEstimator.estimatedSizeOf(line);
					}
				}

				temporaryList = sortList(temporaryList, options);
				final File chunkFile = File.createTempFile("chunk", ".dat");
				OutputStream out = new FileOutputStream(chunkFile);
				if(options.isUseZip())
					out = new GZIPOutputStream(out, options.getZipBufferSize()){
						{
							def.setLevel(Deflater.BEST_SPEED);
						}
					};
				saveChunk(headers, temporaryList, options, out);
				files.add(chunkFile);

				temporaryList.clear();
			}
		}
		return files;
	}

	private List<String> sortList(List<String> list, final ExternalSorterOptions options){
		final Comparator<String> comparator = options.getComparator();
		if(options.isSortInParallel())
			list = list.stream().parallel()
				.sorted(comparator)
				.collect(Collectors.toList());
		else
			list.sort(comparator);
		return list;
	}

	/**
	 * This method calls the garbage collector and then returns the free memory.
	 * This avoids problems with applications where the GC hasn't reclaimed memory and reports no available memory.
	 *
	 * @return estimated available memory
	 */
	private long estimateAvailableMemory(){
		System.gc();

		//http://stackoverflow.com/questions/12807797/java-get-available-memory
		final Runtime r = Runtime.getRuntime();
		final long allocatedMemory = r.totalMemory() - r.freeMemory();
		return r.maxMemory() - allocatedMemory;
	}

	/**
	 * we divide the file into small blocks. If the blocks are too small, we shall create too many temporary files. If they are too big, we shall
	 * be using too much memory.
	 *
	 * @param sizeOfFile how much data (in bytes) can we expect
	 * @param maxTemporaryFiles how many temporary files can we create (e.g., 1024)
	 * @param maxMemory Maximum memory to use (in bytes)
	 * @return the estimate [B]
	 */
	private long estimateBestSizeOfBlocks(final long sizeOfFile, final int maxTemporaryFiles, final long maxMemory){
		//we don't want to open up much more than maxTemporaryFiles temporary files, better run out of memory first
		long blockSize = sizeOfFile / maxTemporaryFiles + (sizeOfFile % maxTemporaryFiles == 0l? 0l: 1l);

		//on the other hand, we don't want to create many temporary files for naught. If {@code blockSize} is smaller
		//than half the free memory, grow it
		if((blockSize << 1) < maxMemory)
			blockSize = maxMemory >> 1;

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
	private void saveChunk(final List<String> headers, final List<String> sortedLines, final ExternalSorterOptions options,
			final OutputStream out) throws IOException{
		try(final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, options.getCharset()))){
			//copy header
			for(final String r : headers){
				writer.write(r);
				writer.newLine();
			}

			//copy sorted lines
			if(options.isRemoveDuplicates()){
				String lastLine = null;
				final Iterator<String> itr = sortedLines.iterator();
				if(itr.hasNext()){
					lastLine = itr.next();
					writer.write(lastLine);
					writer.newLine();
				}
				while(itr.hasNext()){
					final String r = itr.next();
					//skip duplicated lines
					if(!r.equals(lastLine)){
						writer.write(r);
						writer.newLine();
						lastLine = r;
					}
				}
			}
			else
				for(final String r : sortedLines){
					writer.write(r);
					writer.newLine();
				}
		}
	}

	/**
	 * This merges a bunch of temporary flat files
	 *
	 * @param files The {@link List} of sorted {@link File}s to be merged
	 * @param options	Sorting options
	 * @param outputFile The output {@link File} to merge the results to
	 * @return The number of lines sorted
	 * @throws IOException generic IO exception
	 */
	private int mergeSortedFiles(final List<File> files, final ExternalSorterOptions options, final File outputFile)
			throws IOException{
		final List<BinaryFileBuffer> bfbs = new ArrayList<>(files.size());
		for(final File f : files){
			if(f.length() == 0)
				continue;

			final InputStream in = new FileInputStream(f);
			final InputStreamReader isr;
			if(options.isUseZip())
				isr = new InputStreamReader(new GZIPInputStream(in, options.getZipBufferSize()), options.getCharset());
			else
				isr = new InputStreamReader(in, options.getCharset());
			final BinaryFileBuffer bfb = new BinaryFileBuffer(new BufferedReader(isr));
			bfbs.add(bfb);
		}
		final BufferedWriter fbw = Files.newBufferedWriter(outputFile.toPath(), options.getCharset());
		final int rowCounter = mergeSortedFiles(fbw, options, bfbs);
		forEach(files, File::delete);
		return rowCounter;
	}

	/**
	 * This merges several BinaryFileBuffer to an output writer.
	 *
	 * @param writer	A buffer where we write the data
	 * @param options	Sorting options
	 * @param buffers	Where the data should be read
	 * @return The number of lines sorted
	 * @throws IOException generic IO exception
	 *
	 */
	private int mergeSortedFiles(final BufferedWriter writer, final ExternalSorterOptions options,
			final List<BinaryFileBuffer> buffers) throws IOException{
		final PriorityQueue<BinaryFileBuffer> queue = new PriorityQueue<>(11,
			(i, j) -> options.getComparator().compare(i.peek(), j.peek()));
		applyIf(buffers, Predicate.not(BinaryFileBuffer::empty), queue::add);
		int rowCounter = 0;
		try(writer){
			if(options.isRemoveDuplicates())
				rowCounter = mergeSortRemoveDuplicates(queue, writer, rowCounter);
			else
				rowCounter = mergeSort(queue, writer, rowCounter);
		}
		finally{
			for(final BinaryFileBuffer buffer : queue)
				buffer.close();
		}
		return rowCounter;
	}

	private int mergeSortRemoveDuplicates(final PriorityQueue<BinaryFileBuffer> queue, final BufferedWriter writer,
			int rowCounter) throws IOException{
		String lastLine = null;
		if(!queue.isEmpty()){
			final BinaryFileBuffer buffer = queue.poll();
			lastLine = buffer.pop();
			writer.write(lastLine);
			writer.newLine();
			rowCounter ++;
			if(buffer.empty())
				buffer.br.close();
			else
				//add it back
				queue.add(buffer);
		}
		while(!queue.isEmpty()){
			final BinaryFileBuffer bfb = queue.poll();
			final String line = bfb.pop();
			//skip duplicated lines
			if(!line.equals(lastLine)){
				writer.write(line);
				writer.newLine();
				lastLine = line;
			}
			rowCounter ++;
			if(bfb.empty())
				bfb.br.close();
			else
				//add it back
				queue.add(bfb);
		}
		return rowCounter;
	}

	private int mergeSort(final PriorityQueue<BinaryFileBuffer> queue, final BufferedWriter writer, int rowCounter)
			throws IOException{
		while(!queue.isEmpty()){
			final BinaryFileBuffer buffer = queue.poll();
			final String line = buffer.pop();
			writer.write(line);
			writer.newLine();
			rowCounter ++;
			if(buffer.empty())
				buffer.br.close();
			else
				//add it back
				queue.add(buffer);
		}
		return rowCounter;
	}

}
