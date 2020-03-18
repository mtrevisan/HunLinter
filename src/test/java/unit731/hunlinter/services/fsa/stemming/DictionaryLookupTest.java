package unit731.hunlinter.services.fsa.stemming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.FSA;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


class DictionaryLookupTest{

	@Test
	void testApplyReplacements(){
		Map<String, String> conversion = new HashMap<>();
		conversion.put("'", "`");
		conversion.put("fi", "ﬁ");
		conversion.put("\\a", "ą");
		conversion.put("Barack", "George");
		conversion.put("_", "xx");

		Assertions.assertEquals("ﬁlut", DictionaryLookup.applyReplacements("filut", conversion));
		Assertions.assertEquals("ﬁzdrygałką", DictionaryLookup.applyReplacements("fizdrygałk\\a", conversion));
		Assertions.assertEquals("George Bush", DictionaryLookup.applyReplacements("Barack Bush", conversion));
		Assertions.assertEquals("xxxxxxxx", DictionaryLookup.applyReplacements("____", conversion));
	}

	@Test
	void testRemovedEncoderProperties(){
		final URL url = this.getClass().getResource("/services/fsa/stemming/test-removed-props.dict");
		Throwable exception = Assertions.assertThrows(IOException.class,
			() -> new DictionaryLookup(Dictionary.read(url)));
		Assertions.assertEquals("Deprecated encoder keys in metadata. Use fsa.dict.encoder=INFIX", exception.getMessage());
	}

	@Test
	void testPrefixDictionaries() throws IOException{
		final URL url = this.getClass().getResource("/services/fsa/stemming/test-prefix.dict");
		final IStemmer s = new DictionaryLookup(Dictionary.read(url));

		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzeczypospolitej"));
		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzecząpospolitą"));

		// This word is not in the dictionary.
		assertNoStemFor(s, "martygalski");
	}

	@Test
	void testInputConversion() throws IOException{
		final URL url = this.getClass().getResource("/services/fsa/stemming/test-prefix.dict");
		final IStemmer s = new DictionaryLookup(Dictionary.read(url));

		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzecz\\apospolit\\a"));

		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "krowa\\apospolit\\a"));
	}

	@Test
	void testInfixDictionaries() throws IOException{
		final URL url = this.getClass().getResource("/services/fsa/stemming/test-infix.dict");
		final IStemmer s = new DictionaryLookup(Dictionary.read(url));

		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzeczypospolitej"));
		Assertions.assertArrayEquals(new String[]{"Rzeczycki", "adj:pl:nom:m"}, stem(s, "Rzeczyccy"));
		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzecząpospolitą"));

		// This word is not in the dictionary.
		assertNoStemFor(s, "martygalski");

		// This word uses characters that are outside of the encoding range of the dictionary.
		assertNoStemFor(s, "Rzeczyckiõh");
	}

	@Test
	void testWordDataIterator() throws IOException{
		final URL url = this.getClass().getResource("/services/fsa/stemming/test-infix.dict");
		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));

		final Set<String> entries = new HashSet<>();
		for(WordData wd : s){
			entries.add(wd.getWord() + " " + wd.getStem() + " " + wd.getTag());
		}

		// Make sure a sample of the entries is present.
		Set<String> expected = new HashSet<>(Arrays.asList("Rzekunia Rzekuń subst:sg:gen:m", "Rzeczkowskie Rzeczkowski adj:sg:nom.acc.voc:n+adj:pl:acc.nom.voc:f.n", "Rzecząpospolitą Rzeczpospolita subst:irreg", "Rzeczypospolita Rzeczpospolita subst:irreg", "Rzeczypospolitych Rzeczpospolita subst:irreg", "Rzeczyckiej Rzeczycki adj:sg:gen.dat.loc:f"));
		Assertions.assertTrue(entries.containsAll(expected));
	}

	@Test
	void testWordDataCloning() throws IOException{
		final URL url = this.getClass().getResource("/services/fsa/stemming/test-infix.dict");
		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));

		List<WordData> words = new ArrayList<>();
		for(WordData wd : s){
			WordData clone = wd.clone();
			words.add(clone);
		}

		// Reiterate and verify that we have the same entries.
		final DictionaryLookup s2 = new DictionaryLookup(Dictionary.read(url));
		int i = 0;
		for(WordData wd : s2){
			WordData clone = words.get(i ++);
			assertEqualSequences(clone.getStem(), wd.getStem());
			assertEqualSequences(clone.getTag(), wd.getTag());
			assertEqualSequences(clone.getWord(), wd.getWord());
		}

		// Check collections contract.
		final HashSet<WordData> entries = new HashSet<WordData>();
		try{
			entries.add(words.get(0));
			Assertions.fail();
		}catch(RuntimeException e){
			// Expected.
		}
	}

	private void assertEqualSequences(CharSequence s1, CharSequence s2){
		Assertions.assertEquals(s1.toString(), s2.toString());
	}

	@Test
	void testMultibyteEncodingUTF8() throws IOException{
		final URL url = this.getClass().getResource("/services/fsa/stemming/test-diacritics-utf8.dict");
		Dictionary read = Dictionary.read(url);
		final IStemmer s = new DictionaryLookup(read);

		Assertions.assertArrayEquals(new String[]{"merge", "001"}, stem(s, "mergeam"));
		Assertions.assertArrayEquals(new String[]{"merge", "002"}, stem(s, "merseserăm"));
	}

	@Test
	void testSynthesis() throws IOException{
		final URL url = this.getClass().getResource("/services/fsa/stemming/test-synth.dict");
		final IStemmer s = new DictionaryLookup(Dictionary.read(url));

		Assertions.assertArrayEquals(new String[]{"miała", null}, stem(s, "mieć|verb:praet:sg:ter:f:?perf"));
		Assertions.assertArrayEquals(new String[]{"a", null}, stem(s, "a|conj"));
		Assertions.assertArrayEquals(new String[]{}, stem(s, "dziecko|subst:sg:dat:n"));

		// This word is not in the dictionary.
		assertNoStemFor(s, "martygalski");
	}

	@Test
	void testInputWithSeparators() throws IOException{
		final URL url = this.getClass().getResource("/services/fsa/stemming/test-separators.dict");
		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));

		/*
		 * Attemp to reconstruct input sequences using WordData iterator.
		 */
		ArrayList<String> sequences = new ArrayList<String>();
		for(WordData wd : s){
			sequences.add("" + wd.getWord() + " " + wd.getStem() + " " + wd.getTag());
		}
		Collections.sort(sequences);

		Assertions.assertEquals("token1 null null", sequences.get(0));
		Assertions.assertEquals("token2 null null", sequences.get(1));
		Assertions.assertEquals("token3 null +", sequences.get(2));
		Assertions.assertEquals("token4 token2 null", sequences.get(3));
		Assertions.assertEquals("token5 token2 null", sequences.get(4));
		Assertions.assertEquals("token6 token2 +", sequences.get(5));
		Assertions.assertEquals("token7 token2 token3+", sequences.get(6));
		Assertions.assertEquals("token8 token2 token3++", sequences.get(7));
	}

	@Test
	void testSeparatorInLookupTerm() throws IOException{
		FSA fsa = FSA.read(getClass().getResourceAsStream("/services/fsa/stemming/test-separator-in-lookup.fsa"));

		DictionaryMetadata metadata = new DictionaryMetadataBuilder().separator('+').encoding("iso8859-1").encoder(EncoderType.INFIX).build();

		final DictionaryLookup s = new DictionaryLookup(new Dictionary(fsa, metadata));
		Assertions.assertEquals(0, s.lookup("l+A").size());
	}

	@Test
	void testGetSeparator() throws IOException{
		final URL url = this.getClass().getResource("/services/fsa/stemming/test-separators.dict");
		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
		Assertions.assertEquals('+', s.getSeparatorChar());
	}

	private static String asString(CharSequence s){
		if(s == null)
			return null;
		return s.toString();
	}

	private static String[] stem(IStemmer s, String word){
		ArrayList<String> result = new ArrayList<String>();
		for(WordData wd : s.lookup(word)){
			result.add(asString(wd.getStem()));
			result.add(asString(wd.getTag()));
		}
		return result.toArray(new String[result.size()]);
	}

	private static void assertNoStemFor(IStemmer s, String word){
		Assertions.assertArrayEquals(new String[]{}, stem(s, word));
	}

}
