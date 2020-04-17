package unit731.hunlinter.datastructures.fsa.stemming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.text.ArrayHelper;

import java.nio.charset.StandardCharsets;


class BufferUtilsTest{

	@Test
	void sharedPrefix(){
		Assertions.assertEquals(4, ArrayHelper.longestCommonPrefix(toByteArray("abcdef"), toByteArray("abcd__")));
		Assertions.assertEquals(0, ArrayHelper.longestCommonPrefix(toByteArray(""), toByteArray("_")));
		Assertions.assertEquals(2, ArrayHelper.longestCommonPrefix(toByteArray("cd"), toByteArray("cd")));
	}

	private static byte[] toByteArray(String arg){
		byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
		Assertions.assertEquals(arg.length(), bytes.length);
		return bytes;
	}

}
