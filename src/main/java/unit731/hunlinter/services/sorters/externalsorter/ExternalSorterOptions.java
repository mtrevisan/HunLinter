package unit731.hunlinter.services.sorters.externalsorter;

import java.nio.charset.Charset;
import java.util.Comparator;
import org.apache.commons.lang3.builder.Builder;


/**
 * @see <a href="https://github.com/lemire/externalsortinginjava">External-Memory Sorting in Java</>, version 0.4.4, 11/3/2020
 */
public class ExternalSorterOptions{

	/** Default maximal number of temporary files allowed */
	public static final int MAX_TEMPORARY_FILES_DEFAULT = 1024;
	/** Default ZIP buffer size */
	public static final int ZIP_BUFFER_SIZE_DEFAULT = 2048;


	private final Charset charset;
	/** String comparator */
	private final Comparator<String> comparator;
	/** Whether the duplicate lines should be discarded */
	private final boolean removeDuplicates;
	/** Whether to make a parallel sort */
	private final boolean sortInParallel;
	/** Maximum number of temporary files allowed */
	private final int maxTemporaryFiles;
	/** Whether to use ZIP for input files */
	private final boolean useInputAsZip;
	/** Whether to use ZIP for temporary files */
	private final boolean useTemporaryAsZip;
	/** Whether to use ZIP for output file */
	private final boolean useOutputAsZip;
	/** ZIP buffer size */
	private final int zipBufferSize;


	private ExternalSorterOptions(final Charset charset, final Comparator<String> comparator, final boolean removeDuplicates,
			final boolean sortInParallel, final int maxTemporaryFiles, final boolean useInputAsZip,
			final boolean useTemporaryAsZip, final boolean useOutputAsZip, final int zipBufferSize){
		this.charset = charset;
		this.comparator = comparator;
		this.removeDuplicates = removeDuplicates;
		this.sortInParallel = sortInParallel;
		this.maxTemporaryFiles = maxTemporaryFiles;
		this.useInputAsZip = useInputAsZip;
		this.useTemporaryAsZip = useTemporaryAsZip;
		this.useOutputAsZip = useOutputAsZip;
		this.zipBufferSize = zipBufferSize;
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
		private boolean useInputAsZip;
		private boolean useTemporaryAsZip;
		private boolean useOutputAsZip;
		private int zipBufferSize;
		private boolean zipBufferSize$set;


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

		public ExternalSorterOptionsBuilder useInputAsZip(){
			useInputAsZip = true;
			return this;
		}

		public ExternalSorterOptionsBuilder useTemporaryAsZip(){
			useTemporaryAsZip = true;
			return this;
		}

		public ExternalSorterOptionsBuilder useOutputAsZip(){
			useOutputAsZip = true;
			return this;
		}

		public ExternalSorterOptionsBuilder zipBufferSize(final int zipBufferSize){
			this.zipBufferSize = zipBufferSize;
			zipBufferSize$set = true;
			return this;
		}

		@Override
		public ExternalSorterOptions build(){
			return new ExternalSorterOptions(charset, comparator, removeDuplicates, sortInParallel,
				(maxTemporaryFiles$set? maxTemporaryFiles: MAX_TEMPORARY_FILES_DEFAULT),
				useInputAsZip, useTemporaryAsZip, useOutputAsZip,
				(zipBufferSize$set? zipBufferSize: ZIP_BUFFER_SIZE_DEFAULT));
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

	public boolean isUseInputAsZip(){
		return useInputAsZip;
	}

	public boolean isUseTemporaryAsZip(){
		return useTemporaryAsZip;
	}

	public boolean isUseOutputAsZip(){
		return useOutputAsZip;
	}

	public int getZipBufferSize(){
		return zipBufferSize;
	}

}
