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
package io.github.mtrevisan.hunlinter.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


class StringHelperTest{

	private static class AllCodepointsIterator{
		//see http://unicode.org/glossary/
		private static final int MAX = 0x0010_FFFF;
		private static final int SURROGATE_FIRST = 0x0000_D800;
		private static final int SURROGATE_LAST = 0x0000_DFFF;

		private int codepoint = 0;


		public boolean hasNext(){
			return (codepoint < MAX);
		}

		public int next(){
			int ret = codepoint;
			codepoint = next(codepoint);
			return ret;
		}

		private int next(int codepoint){
			while(codepoint ++ < MAX){
				if(codepoint == SURROGATE_FIRST)
					codepoint = SURROGATE_LAST + 1;
				if(!Character.isDefined(codepoint))
					continue;

				return codepoint;
			}
			return MAX;
		}
	}


	@Test
	void rawBytesLength(){
		Charset charset = StandardCharsets.UTF_8;
		AllCodepointsIterator iterator = new AllCodepointsIterator();
		while(iterator.hasNext()){
			String test = new String(Character.toChars(iterator.next()));
			Assertions.assertEquals(test.getBytes(charset).length, StringHelper.rawBytesLength(test));
		}
	}

}
