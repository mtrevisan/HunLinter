package unit731.hunlinter.datastructures.fsa.stemming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


class SequenceEncodersTest{

	@Test
	void encodeSamples(){
		for(final EncoderType encoderType : EncoderType.values()){
			SequenceEncoderInterface coder = encoderType.get();

			assertRoundtripEncode("", "", coder);

			assertRoundtripEncode("abc", "ab", coder);
			assertRoundtripEncode("abc", "abx", coder);
			assertRoundtripEncode("ab", "abc", coder);
			assertRoundtripEncode("xabc", "abc", coder);
			assertRoundtripEncode("axbc", "abc", coder);
			assertRoundtripEncode("axybc", "abc", coder);
			assertRoundtripEncode("axybc", "abc", coder);
			assertRoundtripEncode("azbc", "abcxy", coder);

			assertRoundtripEncode("Niemcami", "Niemiec", coder);
			assertRoundtripEncode("Niemiec", "Niemcami", coder);
		}
	}

	private void assertRoundtripEncode(String srcString, String dstString, final SequenceEncoderInterface coder){
		byte[] source = srcString.getBytes(StandardCharsets.UTF_8);
		byte[] target = dstString.getBytes(StandardCharsets.UTF_8);

		byte[] encoded = coder.encode(source, target);
		byte[] decoded = coder.decode(source, encoded);

		if(!Arrays.equals(decoded, target)){
			System.out.println("src: " + new String(source, StandardCharsets.UTF_8));
			System.out.println("dst: " + new String(target, StandardCharsets.UTF_8));
			System.out.println("enc: " + new String(encoded, StandardCharsets.UTF_8));
			System.out.println("dec: " + new String(decoded, StandardCharsets.UTF_8));
			Assertions.fail("Mismatch.");
		}
	}

}
