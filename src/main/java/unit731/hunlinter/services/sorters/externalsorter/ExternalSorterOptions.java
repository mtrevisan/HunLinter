package unit731.hunlinter.services.sorters.externalsorter;

import java.nio.charset.Charset;
import java.util.Comparator;
import org.apache.commons.lang3.builder.Builder;


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
	/** Number of lines to skip before sorting starts (the header line are copied as-is in the output file) */
	private final int skipHeaderLines;
	/** Whether to make a parallel sort */
	private final boolean sortInParallel;
	/** Maximum number of temporary files allowed */
	private final int maxTemporaryFiles;
	/** Whether to use ZIP for temporary files */
	private final boolean useZip;
	/** ZIP buffer size */
	private final int zipBufferSize;


	private ExternalSorterOptions(final Charset charset, final Comparator<String> comparator, final boolean removeDuplicates,
			final int skipHeaderLines, final boolean sortInParallel, final int maxTemporaryFiles, final boolean useZip,
			final int zipBufferSize){
		this.charset = charset;
		this.comparator = comparator;
		this.removeDuplicates = removeDuplicates;
		this.skipHeaderLines = skipHeaderLines;
		this.sortInParallel = sortInParallel;
		this.maxTemporaryFiles = maxTemporaryFiles;
		this.useZip = useZip;
		this.zipBufferSize = zipBufferSize;
	}

	public static ExternalSorterOptionsBuilder builder(){
		return new ExternalSorterOptionsBuilder();
	}

	public static class ExternalSorterOptionsBuilder implements Builder<ExternalSorterOptions>{

		private Charset charset;
		private Comparator<String> comparator;
		private boolean removeDuplicates;
		private int skipHeaderLines;
		private boolean sortInParallel;
		private int maxTemporaryFiles;
		private boolean maxTemporaryFiles$set;
		private boolean useZip;
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

		public ExternalSorterOptionsBuilder removeDuplicates(final boolean removeDuplicates){
			this.removeDuplicates = removeDuplicates;
			return this;
		}

		public ExternalSorterOptionsBuilder skipHeaderLines(final int skipHeaderLines){
			this.skipHeaderLines = skipHeaderLines;
			return this;
		}

		public ExternalSorterOptionsBuilder sortInParallel(final boolean sortInParallel){
			this.sortInParallel = sortInParallel;
			return this;
		}

		public ExternalSorterOptionsBuilder maxTemporaryFiles(final int maxTemporaryFiles){
			this.maxTemporaryFiles = maxTemporaryFiles;
			maxTemporaryFiles$set = true;
			return this;
		}

		public ExternalSorterOptionsBuilder useZip(final boolean useZip){
			this.useZip = useZip;
			return this;
		}

		public ExternalSorterOptionsBuilder zipBufferSize(final int zipBufferSize){
			this.zipBufferSize = zipBufferSize;
			zipBufferSize$set = true;
			return this;
		}

		@Override
		public ExternalSorterOptions build(){
			return new ExternalSorterOptions(charset, comparator, removeDuplicates, skipHeaderLines, sortInParallel,
				(maxTemporaryFiles$set? maxTemporaryFiles: 1024), useZip, (zipBufferSize$set? zipBufferSize: 2048));
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

	public int getSkipHeaderLines(){
		return skipHeaderLines;
	}

	public boolean isSortInParallel(){
		return sortInParallel;
	}

	public int getMaxTemporaryFiles(){
		return maxTemporaryFiles;
	}

	public boolean isUseZip(){
		return useZip;
	}

	public int getZipBufferSize(){
		return zipBufferSize;
	}

}
