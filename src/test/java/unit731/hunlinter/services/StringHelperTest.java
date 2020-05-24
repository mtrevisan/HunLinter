package unit731.hunlinter.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.text.StringHelper;

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
