package unit731.hunlinter.languages;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;


class WordTokenizerTest{

	private static final WordTokenizer tokenizer = new WordTokenizer();


	@Test
	void simple(){
		List<String> tokens = tokenizer.tokenize("I am here! well, there. So... to speak.");

		Assertions.assertEquals(Arrays.asList("I", " ", "am", " ", "here", "!", " ", "well", ",", " ", "there", ".", " ",
			"So", "…", " ", "to", " ", "speak", "."), tokens);
	}

	@Test
	void base64(){
		List<String> tokens = tokenizer.tokenize("QQ== YmFzZTY0 YmFzZTY0IQ==");

		Assertions.assertEquals(Arrays.asList("QQ==", " ", "YmFzZTY0", " ", "YmFzZTY0IQ=="), tokens);
	}

//	@Test
//	void semanticVersioning(){
//		List<String> tokens = tokenizer.tokenize("1.2.3 1.2.3-pre 1.2.3+build 1.2.3-pre+build v1.2.3-pre+build");
//
//		Assertions.assertEquals(Arrays.asList("1.2.3", " ", "1.2.3-pre", " ", "1.2.3+build", " ", "1.2.3-pre+build", " ", "v1.2.3-pre+build"), tokens);
//	}

//	@Test
//	void phoneNumber(){
//		List<String> tokens = tokenizer.tokenize("(999) 999 9999_(999) 999-9999_999-999-9999_+9 999-999-9999_+9 (999) 999-9999_+999 (999.9) 99-999-9999" +
//			"_999-9999");
//
//		Assertions.assertEquals(Arrays.asList("(999) 999 9999", "_", "(999) 999-9999", "_", "999-999-9999", "_", "+9 999-999-9999", "_", "+9 (999) 999-9999",
//			"_", "+999 (999.9) 99-999-9999", "_", "999-9999"), tokens);
//	}

	@Test
	void dateISO8601(){
		List<String> tokens = tokenizer.tokenize("2009-12T12:34_2009_2009-05-19_20090519_2009123_2009-05_2009-123_2009-222_2009-001_2009-W01-1_2009-W51-1"
			+ "_2009-W51_2009-W33_2009W51_2009-05-19_2009-05-19 00:00_2009-05-19 14_2009-05-19 14:31_2009-05-19 14:39:22_2009-05-19T14:39Z_2009-W21-2"
			+ "_2009-W21-2T01:22_2009-139_2009-05-19 14:39:22-06:00_2009-05-19 14:39:22+0600_2009-05-19 14:39:22-01_20090621T0545Z_2007-04-06T00:00_2007-04-05T24:00"
			+ "_2010-02-18T16:23:48.5_2010-02-18T16:23:48,444_2010-02-18T16:23:48,3-06:00_2010-02-18T16:23.4_2010-02-18T16:23,25_2010-02-18T16:23.33+0600"
			+ "_2010-02-18T16.23334444_2010-02-18T16,2283_2009-05-19 143922.500_2009-05-19 1439,55");

		Assertions.assertEquals(Arrays.asList("2009-12T12:34", "_", "2009", "_", "2009-05-19", "_", "20090519", "_", "2009123", "_", "2009-05", "_", "2009-123",
			"_", "2009-222", "_", "2009-001", "_", "2009-W01-1", "_", "2009-W51-1", "_", "2009-W51", "_", "2009-W33", "_", "2009W51", "_", "2009-05-19",
			"_", "2009-05-19 00:00", "_", "2009-05-19 14", "_", "2009-05-19 14:31", "_", "2009-05-19 14:39:22", "_", "2009-05-19T14:39Z", "_", "2009-W21-2",
			"_", "2009-W21-2T01:22", "_", "2009-139", "_", "2009-05-19 14:39:22-06:00", "_", "2009-05-19 14:39:22+0600", "_", "2009-05-19 14:39:22-01",
			"_", "20090621T0545Z", "_", "2007-04-06T00:00", "_", "2007-04-05T24:00", "_", "2010-02-18T16:23:48.5", "_", "2010-02-18T16:23:48,444",
			"_", "2010-02-18T16:23:48,3-06:00", "_", "2010-02-18T16:23.4", "_", "2010-02-18T16:23,25", "_", "2010-02-18T16:23.33+0600",
			"_", "2010-02-18T16.23334444", "_", "2010-02-18T16,2283", "_", "2009-05-19 143922.500", "_", "2009-05-19 1439,55"), tokens);
	}

	@Test
	void time(){
		List<String> tokens = tokenizer.tokenize("10:12:12 am_13:56:00_8:14:00pM");

		Assertions.assertEquals(Arrays.asList("10:12:12 am", "_", "13:56:00", "_", "8:14:00pM"), tokens);
	}

	@Test
	void email(){
		List<String> tokens = tokenizer.tokenize("Here bla@blah.com is my email.");

		Assertions.assertEquals(Arrays.asList("Here", " ", "bla@blah.com", " ", "is", " ", "my", " ", "email", "."), tokens);
	}

	@Test
	void url(){
		List<String> tokens = tokenizer.tokenize("Here http://www.bla.com is another url.");

		Assertions.assertEquals(Arrays.asList("Here", " ", "http://www.bla.com", " ", "is", " ", "another", " ", "url", "."), tokens);
	}

}
