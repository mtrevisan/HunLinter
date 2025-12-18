/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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

import org.apache.commons.lang3.builder.Builder;

import java.nio.charset.Charset;
import java.util.Comparator;


/**
 * @see <a href="https://github.com/lemire/externalsortinginjava">External-Memory Sorting in Java</a>, version 0.4.4, 11/3/2020
 */
public final class ExternalSorterOptions{

	public static final long MINIMUM_MEMORY_TARGET = 64l * 1024l * 1024l;

	/** Default ZIP buffer size [B]. */
	public static final int ZIP_BUFFER_SIZE_DEFAULT = 256 * 1024;
	/** Default Reader buffer size [B]. */
	public static final int READER_BUFFER_SIZE_DEFAULT = 64 * 1024;

	private static final String LINE_SEPARATOR_DEFAULT = System.lineSeparator();


	private final Charset charset;
	/** String comparator. */
	private final Comparator<String> comparator;
	private final int parallelSortThreshold;
	/** Whether the duplicate lines should be discarded. */
	private final boolean removeDuplicates;
	/** Whether to use ZIP for temporary files. */
	private final boolean useTemporaryAsZip;
	/** Whether to use ZIP for output file. */
	private final boolean writeOutputAsZip;
	/** ZIP buffer size [B]. */
	private final int zipBufferSize;
	/** Reader buffer size [B]. */
	private final int readerBufferSize;
	/** Line separator for output file. */
	private final String lineSeparator;

	private final int parallelism;
	/** RAM budget [MB] for mapped sorter (in-memory chunk sizing). */
	private final int mappedMemory;
	/** Memory-mapped window size [MB] for sliding reads. */
	private final int mappedWindow;
	/** OS sort memory [MB] passed to GNU sort (best-effort). */
	private final int osSortMemory;


	private ExternalSorterOptions(final Charset charset, final Comparator<String> comparator,
			final boolean removeDuplicates, final boolean useTemporaryAsZip, final boolean writeOutputAsZip,
			final int zipBufferSize, final int readerBufferSize, final String lineSeparator, final int parallelism,
			final int mappedMemory, final int mappedWindow, final int osSortMemory){
		this.charset = charset;
		this.comparator = comparator;
		parallelSortThreshold = 2 * 1024 * 1024;
		//TODO move this under a menu `calibration`, store value until a new calibration is requested
//		parallelSortThreshold = ParallelSortTuner.estimateThresholdForStrings(comparator);
		this.removeDuplicates = removeDuplicates;
		this.useTemporaryAsZip = useTemporaryAsZip;
		this.writeOutputAsZip = writeOutputAsZip;
		this.zipBufferSize = zipBufferSize;
		this.readerBufferSize = readerBufferSize;
		this.lineSeparator = (lineSeparator != null? lineSeparator: LINE_SEPARATOR_DEFAULT);

		this.parallelism = parallelism;
		this.mappedMemory = mappedMemory;
		this.mappedWindow = mappedWindow;
		this.osSortMemory = osSortMemory;
	}

	public static ExternalSorterOptionsBuilder builder(){
		return new ExternalSorterOptionsBuilder();
	}

	public static class ExternalSorterOptionsBuilder implements Builder<ExternalSorterOptions>{

		private Charset charset;
		private Comparator<String> comparator = Comparator.naturalOrder();
		private boolean removeDuplicates;
		private boolean useTemporaryAsZip;
		private boolean writeOutputAsZip;
		private int zipBufferSize;
		private boolean zipBufferSize$set;
		private int readerBufferSize;
		private boolean readerBufferSize$set;
		private String lineSeparator;
		private int parallelism = Runtime.getRuntime().availableProcessors();
		private int mappedMemory;
		private int mappedWindow;
		private int osSortMemory;


		ExternalSorterOptionsBuilder(){}

		public final ExternalSorterOptionsBuilder charset(final Charset charset){
			this.charset = charset;
			return this;
		}

		public final ExternalSorterOptionsBuilder comparator(final Comparator<String> comparator){
			this.comparator = comparator;
			return this;
		}

		public final ExternalSorterOptionsBuilder removeDuplicates(){
			removeDuplicates = true;
			return this;
		}

		public final ExternalSorterOptionsBuilder useTemporaryAsZip(){
			useTemporaryAsZip = true;
			return this;
		}

		public final ExternalSorterOptionsBuilder writeOutputAsZip(){
			writeOutputAsZip = true;
			return this;
		}

		public final ExternalSorterOptionsBuilder zipBufferSize(final int zipBufferSize){
			this.zipBufferSize = zipBufferSize;
			zipBufferSize$set = true;
			return this;
		}

		public final ExternalSorterOptionsBuilder readerBufferSize(final int readerBufferSize){
			this.readerBufferSize = readerBufferSize;
			readerBufferSize$set = true;
			return this;
		}

		public final ExternalSorterOptionsBuilder lineSeparator(final String lineSeparator){
			this.lineSeparator = lineSeparator;
			return this;
		}

		public final ExternalSorterOptionsBuilder parallelism(final int parallelism){
			this.parallelism = parallelism;
			return this;
		}

		public final ExternalSorterOptionsBuilder mappedMemory(final int mappedMemory){
			this.mappedMemory = mappedMemory;
			return this;
		}

		public final ExternalSorterOptionsBuilder mappedWindow(final int mappedWindow){
			this.mappedWindow = mappedWindow;
			return this;
		}

		public final ExternalSorterOptionsBuilder osSortMemory(final int osSortMemory){
			this.osSortMemory = osSortMemory;
			return this;
		}

		@Override
		public final ExternalSorterOptions build(){
			return new ExternalSorterOptions(charset, comparator, removeDuplicates,
				useTemporaryAsZip, writeOutputAsZip,
				(zipBufferSize$set? zipBufferSize: ZIP_BUFFER_SIZE_DEFAULT),
				(readerBufferSize$set? readerBufferSize: READER_BUFFER_SIZE_DEFAULT),
				lineSeparator,
				parallelism, mappedMemory, mappedWindow, osSortMemory);
		}

	}

	public Charset getCharset(){
		return charset;
	}

	public Comparator<String> getComparator(){
		return comparator;
	}

	public int getParallelSortThreshold(){
		return parallelSortThreshold;
	}

	public boolean isRemoveDuplicates(){
		return removeDuplicates;
	}

	public boolean isUseTemporaryAsZip(){
		return useTemporaryAsZip;
	}

	public boolean isWriteOutputAsZip(){
		return writeOutputAsZip;
	}

	public int getZipBufferSize(){
		return zipBufferSize;
	}

	public int getReaderBufferSize(){
		return readerBufferSize;
	}

	public String getLineSeparator(){
		return lineSeparator;
	}

	public int getParallelism(){
		return parallelism;
	}

	public int getMappedMemory(){
		return mappedMemory;
	}

	public int getMappedWindow(){
		return mappedWindow;
	}

	public int getOSSortMemory(){
		return osSortMemory;
	}

}
