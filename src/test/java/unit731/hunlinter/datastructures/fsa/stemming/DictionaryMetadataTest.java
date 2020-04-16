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
	void testWrongSeparator(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
			() -> DictionaryMetadata.read(getClass().getResourceAsStream("/services/fsa/stemming/wrong-separator.info")));
		Assertions.assertEquals("Attribute fsa.dict.separator must be a single character: \\t+", exception.getMessage());
	}

	@Test
	void testEscapeSeparator() throws IOException{
		DictionaryMetadata m = DictionaryMetadata.read(getClass().getResourceAsStream("/services/fsa/stemming/escape-separator.info"));

		Assertions.assertEquals((byte)'\t', m.getSeparator());
	}

	@Test
	void testUnicodeSeparator() throws IOException{
		DictionaryMetadata m = DictionaryMetadata.read(getClass().getResourceAsStream("/services/fsa/stemming/unicode-separator.info"));

		Assertions.assertEquals((byte)'\t', m.getSeparator());
	}

	@Test
	void testWriteMetadata() throws IOException{
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
