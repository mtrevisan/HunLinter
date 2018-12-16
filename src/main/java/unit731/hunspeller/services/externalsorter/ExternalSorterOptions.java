package unit731.hunspeller.services.externalsorter;

import java.nio.charset.Charset;
import java.util.Comparator;
import org.apache.commons.lang3.builder.Builder;


public class ExternalSorterOptions{

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
	private int maxTemporaryFiles = 1024;
	/** Whether to use ZIP for temporary files */
	private final boolean useZip;
	/** ZIP buffer size */
	private int zipBufferSize = 2048;


	private ExternalSorterOptions(Charset charset, Comparator<String> comparator, boolean removeDuplicates, int skipHeaderLines,
			boolean sortInParallel, int maxTemporaryFiles, boolean useZip, int zipBufferSize){
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

		public ExternalSorterOptionsBuilder charset(Charset charset){
			this.charset = charset;
			return this;
		}

		public ExternalSorterOptionsBuilder comparator(Comparator<String> comparator){
			this.comparator = comparator;
			return this;
		}

		public ExternalSorterOptionsBuilder removeDuplicates(boolean removeDuplicates){
			this.removeDuplicates = removeDuplicates;
			return this;
		}

		public ExternalSorterOptionsBuilder skipHeaderLines(int skipHeaderLines){
			this.skipHeaderLines = skipHeaderLines;
			return this;
		}

		public ExternalSorterOptionsBuilder sortInParallel(boolean sortInParallel){
			this.sortInParallel = sortInParallel;
			return this;
		}

		public ExternalSorterOptionsBuilder maxTemporaryFiles(int maxTemporaryFiles){
			this.maxTemporaryFiles = maxTemporaryFiles;
			maxTemporaryFiles$set = true;
			return this;
		}

		public ExternalSorterOptionsBuilder useZip(boolean useZip){
			this.useZip = useZip;
			return this;
		}

		public ExternalSorterOptionsBuilder zipBufferSize(int zipBufferSize){
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
