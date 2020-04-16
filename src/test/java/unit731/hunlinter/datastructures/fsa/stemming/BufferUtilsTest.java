package unit731.hunlinter.datastructures.fsa.stemming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;


class BufferUtilsTest{

	@Test
	void testSharedPrefix(){
		Assertions.assertEquals(4, BufferUtils.sharedPrefixLength(toByteArray("abcdef"), toByteArray("abcd__")));
		Assertions.assertEquals(0, BufferUtils.sharedPrefixLength(toByteArray(""), toByteArray("_")));
		Assertions.assertEquals(2, BufferUtils.sharedPrefixLength(toByteArray("cd"), toByteArray("cd")));
	}

	private static byte[] toByteArray(String arg){
		byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
		Assertions.assertEquals(arg.length(), bytes.length);
		return bytes;
	}

}
