/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.datastructures.fsa.stemming;

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
