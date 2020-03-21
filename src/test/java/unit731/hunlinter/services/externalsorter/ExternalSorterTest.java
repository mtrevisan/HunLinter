package unit731.hunlinter.services.externalsorter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


class ExternalSorterTest{

	private final ExternalSorter sorter = new ExternalSorter();
	private final Comparator<String> DEFAULT_COMPARATOR = Comparator.naturalOrder();

	private File inputFile;
	private File outputFile;


	@BeforeEach
	void setUp() throws Exception{
		inputFile = new File(getClass().getClassLoader().getResource("external-sorter-test-file.txt").toURI());

		outputFile = File.createTempFile("sort", ".out");
		outputFile.deleteOnExit();
	}

	@Test
	void emptyFile() throws Exception{
		File in = File.createTempFile("sort", ".in");
		in.deleteOnExit();
		ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(StandardCharsets.UTF_8)
			.comparator(DEFAULT_COMPARATOR)
			.build();

		sorter.sort(in, options, outputFile);

		Assertions.assertEquals(0, outputFile.length());
	}

	@Test
	void simpleSort() throws Exception{
		ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(StandardCharsets.UTF_8)
			.comparator(DEFAULT_COMPARATOR)
			.build();

		sorter.sort(inputFile, options, outputFile);

		Assertions.assertEquals(27, outputFile.length());
	}

	@Test
	void sortDistinctFileResult() throws Exception{
		ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(StandardCharsets.UTF_8)
			.comparator(DEFAULT_COMPARATOR)
			.build();

		sorter.sort(inputFile, options, outputFile);

		Assertions.assertEquals(27, outputFile.length());
	}

	@Test
	void sortDistinctFileDistinctResult() throws Exception{
		ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(StandardCharsets.UTF_8)
			.comparator(DEFAULT_COMPARATOR)
			.removeDuplicates(true)
			.build();

		sorter.sort(inputFile, options, outputFile);

		Assertions.assertEquals(21, outputFile.length());
		Assertions.assertEquals("a\r\nc\r\ne\r\ng\r\ni\r\nj\r\nk\r\n", Files.readString(outputFile.toPath()));
	}

	@Test
	void skipHeader() throws Exception{
		ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(StandardCharsets.UTF_8)
			.comparator(DEFAULT_COMPARATOR)
			.skipHeaderLines(2)
			.build();

		sorter.sort(inputFile, options, outputFile);

		Assertions.assertEquals(27, outputFile.length());
		Assertions.assertEquals("g\r\na\r\na\r\nc\r\ne\r\ni\r\ni\r\nj\r\nk\r\n", Files.readString(outputFile.toPath()));
	}

}
