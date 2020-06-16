/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.services.sorters.externalsorter;

import java.nio.charset.Charset;
import java.util.Comparator;
import org.apache.commons.lang3.builder.Builder;


/**
 * @see <a href="https://github.com/lemire/externalsortinginjava">External-Memory Sorting in Java</a>, version 0.4.4, 11/3/2020
 */
public class ExternalSorterOptions{

	/** Default maximal number of temporary files allowed */
	public static final int MAX_TEMPORARY_FILES_DEFAULT = 1024;
	/** Default maximal size of temporary file allowed [B] */
	public static final int MAX_TEMPORARY_FILE_SIZE_UNLIMITED = -1;
	/** Default ZIP buffer size [B] */
	public static final int ZIP_BUFFER_SIZE_DEFAULT = 2048;

	private static final String LINE_SEPARATOR_DEFAULT = System.lineSeparator();


	private final Charset charset;
	/** String comparator */
	private final Comparator<String> comparator;
	/** Whether the duplicate lines should be discarded */
	private final boolean removeDuplicates;
	/** Whether to make a parallel sort */
	private final boolean sortInParallel;
	/** Maximum number of temporary files allowed */
	private final int maxTemporaryFiles;
	/** Maximum size of temporary file allowed [B] */
	private final long maxTemporaryFileSize;
	/** Whether to use ZIP for temporary files */
	private final boolean useTemporaryAsZip;
	/** Whether to use ZIP for output file */
	private final boolean writeOutputAsZip;
	/** ZIP buffer size [B] */
	private final int zipBufferSize;
	/** Line separator for output file */
	private final String lineSeparator;


	private ExternalSorterOptions(final Charset charset, final Comparator<String> comparator, final boolean removeDuplicates,
			final boolean sortInParallel, final int maxTemporaryFiles, final long maxTemporaryFileSize,
			final boolean useTemporaryAsZip, final boolean writeOutputAsZip, final int zipBufferSize, final String lineSeparator){
		this.charset = charset;
		this.comparator = comparator;
		this.removeDuplicates = removeDuplicates;
		this.sortInParallel = sortInParallel;
		this.maxTemporaryFiles = maxTemporaryFiles;
		this.maxTemporaryFileSize = maxTemporaryFileSize;
		this.useTemporaryAsZip = useTemporaryAsZip;
		this.writeOutputAsZip = writeOutputAsZip;
		this.zipBufferSize = zipBufferSize;
		this.lineSeparator = (lineSeparator != null? lineSeparator: LINE_SEPARATOR_DEFAULT);
	}

	public static ExternalSorterOptionsBuilder builder(){
		return new ExternalSorterOptionsBuilder();
	}

	public static class ExternalSorterOptionsBuilder implements Builder<ExternalSorterOptions>{

		private Charset charset;
		private Comparator<String> comparator;
		private boolean removeDuplicates;
		private boolean sortInParallel;
		private int maxTemporaryFiles;
		private boolean maxTemporaryFiles$set;
		private long maxTemporaryFileSize;
		private boolean maxTemporaryFileSize$set;
		private boolean useTemporaryAsZip;
		private boolean writeOutputAsZip;
		private int zipBufferSize;
		private boolean zipBufferSize$set;
		private String lineSeparator;


		ExternalSorterOptionsBuilder(){}

		public ExternalSorterOptionsBuilder charset(final Charset charset){
			this.charset = charset;
			return this;
		}

		public ExternalSorterOptionsBuilder comparator(final Comparator<String> comparator){
			this.comparator = comparator;
			return this;
		}

		public ExternalSorterOptionsBuilder removeDuplicates(){
			removeDuplicates = true;
			return this;
		}

		public ExternalSorterOptionsBuilder sortInParallel(){
			sortInParallel = true;
			return this;
		}

		public ExternalSorterOptionsBuilder maxTemporaryFiles(final int maxTemporaryFiles){
			this.maxTemporaryFiles = maxTemporaryFiles;
			maxTemporaryFiles$set = true;
			return this;
		}

		public ExternalSorterOptionsBuilder maxTemporaryFileSize(final long maxTemporaryFileSize){
			this.maxTemporaryFileSize = maxTemporaryFileSize;
			maxTemporaryFileSize$set = true;
			return this;
		}

		public ExternalSorterOptionsBuilder useTemporaryAsZip(){
			useTemporaryAsZip = true;
			return this;
		}

		public ExternalSorterOptionsBuilder writeOutputAsZip(){
			writeOutputAsZip = true;
			return this;
		}

		public ExternalSorterOptionsBuilder zipBufferSize(final int zipBufferSize){
			this.zipBufferSize = zipBufferSize;
			zipBufferSize$set = true;
			return this;
		}

		public ExternalSorterOptionsBuilder lineSeparator(final String lineSeparator){
			this.lineSeparator = lineSeparator;
			return this;
		}

		@Override
		public ExternalSorterOptions build(){
			return new ExternalSorterOptions(charset, comparator, removeDuplicates, sortInParallel,
				(maxTemporaryFiles$set? maxTemporaryFiles: MAX_TEMPORARY_FILES_DEFAULT),
				(maxTemporaryFileSize$set? maxTemporaryFileSize: MAX_TEMPORARY_FILE_SIZE_UNLIMITED),
				useTemporaryAsZip, writeOutputAsZip,
				(zipBufferSize$set? zipBufferSize: ZIP_BUFFER_SIZE_DEFAULT),
				lineSeparator);
		}

	}

	public Charset getCharset(){
		return charset;
	}

	public Comparator<String> getComparator(){
		return comparator;
	}

	public boolean isRemoveDuplicates(){
		return removeDuplicates;
	}

	public boolean isSortInParallel(){
		return sortInParallel;
	}

	public int getMaxTemporaryFiles(){
		return maxTemporaryFiles;
	}

	public long getMaxTemporaryFileSize(){
		return maxTemporaryFileSize;
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

	public String getLineSeparator(){
		return lineSeparator;
	}

}
