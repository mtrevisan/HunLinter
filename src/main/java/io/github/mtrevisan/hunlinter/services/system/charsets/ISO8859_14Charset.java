/**
 * Copyright (c) 2021-2022 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.services.system.charsets;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class ISO8859_14Charset extends Charset{

	@SuppressWarnings("OverlyLargePrimitiveArrayInitializer")
	private static final char[] TABLE = {
		0x00A0, 0x1E02, 0x1E03, 0x00A3, 0x010A, 0x010B, 0x1E0A, 0x00A7,
		0x1E80, 0x00A9, 0x1E82, 0x1E0B, 0x1EF2, 0x00AD, 0x00AE, 0x0178,
		0x1E1E, 0x1E1F, 0x0120, 0x0121, 0x1E40, 0x1E41, 0x00B6, 0x1E56,
		0x1E81, 0x1E57, 0x1E83, 0x1E60, 0x1EF3, 0x1E84, 0x1E85, 0x1E61,
		0x00C0, 0x00C1, 0x00C2, 0x00C3, 0x00C4, 0x00C5, 0x00C6, 0x00C7,
		0x00C8, 0x00C9, 0x00CA, 0x00CB, 0x00CC, 0x00CD, 0x00CE, 0x00CF,
		0x0174, 0x00D1, 0x00D2, 0x00D3, 0x00D4, 0x00D5, 0x00D6, 0x1E6A,
		0x00D8, 0x00D9, 0x00DA, 0x00DB, 0x00DC, 0x00DD, 0x0176, 0x00DF,
		0x00E0, 0x00E1, 0x00E2, 0x00E3, 0x00E4, 0x00E5, 0x00E6, 0x00E7,
		0x00E8, 0x00E9, 0x00EA, 0x00EB, 0x00EC, 0x00ED, 0x00EE, 0x00EF,
		0x0175, 0x00F1, 0x00F2, 0x00F3, 0x00F4, 0x00F5, 0x00F6, 0x1E6B,
		0x00F8, 0x00F9, 0x00FA, 0x00FB, 0x00FC, 0x00FD, 0x0177, 0x00FF
	};
	private static final Map<Character, Character> INVERSE_TABLE = new HashMap<>(TABLE.length);
	static{
		for(int i = 0; i < TABLE.length; i ++)
			INVERSE_TABLE.put(TABLE[i], (char)(i + 0x00A0));
	}


	protected static class ISO8859_14Decoder extends CharsetDecoder{

		protected ISO8859_14Decoder(){
			super(StandardCharsets.ISO_8859_1, 1.f, 1.f);
		}

		@Override
		protected final CoderResult decodeLoop(final ByteBuffer in, final CharBuffer out){
			while(in.hasRemaining() && out.hasRemaining()){
				final char ch = (char)(in.get() & 0x00FF);
				out.put(ch >= 0xA0? TABLE[ch - 0x00A0]: ch);
			}
			return (in.hasRemaining()? CoderResult.OVERFLOW: CoderResult.UNDERFLOW);
		}
	}

	protected static class ISO8859_14Encoder extends CharsetEncoder{

		protected ISO8859_14Encoder(){
			super(StandardCharsets.ISO_8859_1, 1.f, 1.f);
		}

		@Override
		protected final CoderResult encodeLoop(final CharBuffer in, final ByteBuffer out){
			while(in.hasRemaining() && out.hasRemaining()){
				final char ch = in.get();
				out.putChar(INVERSE_TABLE.getOrDefault(ch, ch));
			}
			return (in.hasRemaining()? CoderResult.OVERFLOW: CoderResult.UNDERFLOW);
		}
	}


	public ISO8859_14Charset(){
		super("ISO-8859-14", null);
	}

	@Override
	public final boolean contains(final Charset cs){
		return false;
	}

	@Override
	public final CharsetDecoder newDecoder(){
		return new ISO8859_14Decoder();
	}

	@Override
	public final CharsetEncoder newEncoder(){
		return new ISO8859_14Encoder();
	}

}
