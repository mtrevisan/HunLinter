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
package unit731.hunlinter.datastructures.fsa.stemming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


class DictionaryMetadataTest{

	@Test
	void wrongSeparator(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
			() -> DictionaryMetadata.read(getClass().getResourceAsStream("/services/fsa/stemming/wrong-separator.info")));
		Assertions.assertEquals("Attribute fsa.dict.separator must be a single character: \\t+", exception.getMessage());
	}

	@Test
	void escapeSeparator() throws IOException{
		DictionaryMetadata m = DictionaryMetadata.read(getClass().getResourceAsStream("/services/fsa/stemming/escape-separator.info"));

		Assertions.assertEquals((byte)'\t', m.getSeparator());
	}

	@Test
	void unicodeSeparator() throws IOException{
		DictionaryMetadata m = DictionaryMetadata.read(getClass().getResourceAsStream("/services/fsa/stemming/unicode-separator.info"));

		Assertions.assertEquals((byte)'\t', m.getSeparator());
	}

	@Test
	void writeMetadata() throws IOException{
		for(EncoderType encoder : EncoderType.values()){
			for(Charset charset : Arrays.asList(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1, StandardCharsets.US_ASCII)){
				StringWriter sw = new StringWriter();
				new DictionaryMetadataBuilder()
					.encoding(charset)
					.encoder(encoder)
					.separator('|')
					.build()
					.write(sw);

				ByteArrayInputStream is = new ByteArrayInputStream(sw.toString().getBytes(StandardCharsets.UTF_8));
				DictionaryMetadata other = DictionaryMetadata.read(is);

				Assertions.assertEquals((byte)'|', other.getSeparator());
				Assertions.assertEquals(charset, other.getCharset());
				Assertions.assertEquals(encoder, other.getSequenceEncoderType());
			}
		}
	}

}
