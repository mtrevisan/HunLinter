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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;


class ExternalSorterTest{

	private final ExternalSorter sorter = new ExternalSorter();
	private final Comparator<String> DEFAULT_COMPARATOR = Comparator.naturalOrder();

	private File inputFile;
	private File outputFile;


	@BeforeEach
	void setUp() throws Exception{
		inputFile = new File(getClass().getClassLoader().getResource("external-sorter.txt").toURI());

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
			.removeDuplicates()
			.build();

		sorter.sort(inputFile, options, outputFile);

		Assertions.assertEquals(21, outputFile.length());
		Assertions.assertEquals("a\r\nc\r\ne\r\ng\r\ni\r\nj\r\nk\r\n", Files.readString(outputFile.toPath()));
	}

}
