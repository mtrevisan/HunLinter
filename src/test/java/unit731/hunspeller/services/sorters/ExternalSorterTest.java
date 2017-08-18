package unit731.hunspeller.services.sorters;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class ExternalSorterTest{

	private final ExternalSorter sorter = new ExternalSorter();
	private final Comparator<String>DEFAULT_COMPARATOR = (r1, r2) -> r1.compareTo(r2);

	private File inputFile;
	private File outputFile;


	@Before
	public void setUp() throws Exception{
		inputFile = new File(getClass().getClassLoader().getResource("test-file.txt").toURI());

		outputFile = File.createTempFile("sort", ".out");
		outputFile.deleteOnExit();
	}

	@Test
	public void emptyFile() throws Exception{
		File in = File.createTempFile("sort", ".in");
		in.deleteOnExit();
		ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(StandardCharsets.UTF_8)
			.comparator(DEFAULT_COMPARATOR)
			.build();

		sorter.sort(in, options, outputFile);

		Assert.assertEquals(0, outputFile.length());
	}

	@Test
	public void simpleSort() throws Exception{
		ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(StandardCharsets.UTF_8)
			.comparator(DEFAULT_COMPARATOR)
			.build();

		sorter.sort(inputFile, options, outputFile);

		Assert.assertTrue(outputFile.length() == 27);
	}

	@Test
	public void sortDistinctFileResult() throws Exception{
		ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(StandardCharsets.UTF_8)
			.comparator(DEFAULT_COMPARATOR)
			.build();

		sorter.sort(inputFile, options, outputFile);

		Assert.assertTrue(outputFile.length() == 27);
	}

	@Test
	public void sortDistinctFileDistinctResult() throws Exception{
		ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(StandardCharsets.UTF_8)
			.comparator(DEFAULT_COMPARATOR)
			.removeDuplicates(true)
			.build();

		sorter.sort(inputFile, options, outputFile);

		Assert.assertTrue(outputFile.length() == 21);
		Assert.assertEquals("a\r\nc\r\ne\r\ng\r\ni\r\nj\r\nk\r\n", new String(Files.readAllBytes(outputFile.toPath())));
	}

	@Test
	public void skipHeader() throws Exception{
		ExternalSorterOptions options = ExternalSorterOptions.builder()
			.charset(StandardCharsets.UTF_8)
			.comparator(DEFAULT_COMPARATOR)
			.skipHeaderLines(2)
			.build();

		sorter.sort(inputFile, options, outputFile);

		Assert.assertTrue(outputFile.length() == 27);
		Assert.assertEquals("g\r\na\r\na\r\nc\r\ne\r\ni\r\ni\r\nj\r\nk\r\n", new String(Files.readAllBytes(outputFile.toPath())));
	}

}
