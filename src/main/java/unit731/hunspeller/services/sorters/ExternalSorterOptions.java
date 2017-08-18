package unit731.hunspeller.services.sorters;

import java.nio.charset.Charset;
import java.util.Comparator;
import lombok.Builder;
import lombok.Getter;


@Builder
@Getter
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
	@Builder.Default private final int maxTemporaryFiles = 1024;
	/** Whether to use ZIP for temporary files */
	private final boolean useZip;
	/** ZIP buffer size */
	@Builder.Default private final int zipBufferSize = 2048;

}
