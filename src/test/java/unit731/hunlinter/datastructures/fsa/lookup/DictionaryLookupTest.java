package unit731.hunlinter.datastructures.fsa.lookup;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.datastructures.fsa.FSA;
import unit731.hunlinter.datastructures.fsa.stemming.Dictionary;
import unit731.hunlinter.datastructures.fsa.stemming.DictionaryAttribute;
import unit731.hunlinter.datastructures.fsa.stemming.DictionaryMetadata;
import unit731.hunlinter.datastructures.fsa.stemming.DictionaryMetadataBuilder;
import unit731.hunlinter.datastructures.fsa.stemming.EncoderType;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


class DictionaryLookupTest{

	@Test
	void applyReplacements(){
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
	void removedEncoderProperties(){
		final URL url = getClass().getResource("/services/fsa/lookup/removed-props.dict");
		Throwable exception = Assertions.assertThrows(IOException.class,
			() -> new DictionaryLookup(Dictionary.read(url)));
		Assertions.assertEquals("Deprecated encoder keys in metadata. Use fsa.dict.encoder=INFIX", exception.getMessage());
	}

//	@Test
//	void prefixDictionaries() throws IOException{
//		final URL url = getClass().getResource("/services/fsa/lookup/prefix.dict");
//		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
//
//		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzeczypospolitej"));
//		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzecząpospolitą"));
//
//		// This word is not in the dictionary.
//		assertNoStemFor(s, "martygalski");
//	}

//	@Test
//	void testInputConversion() throws IOException{
//		final URL url = getClass().getResource("/services/fsa/lookup/prefix.dict");
//		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
//
//		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzecz\\apospolit\\a"));
//
//		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "krowa\\apospolit\\a"));
//	}
//
//	@Test
//	void testInfixDictionaries() throws IOException{
//		final URL url = getClass().getResource("/services/fsa/lookup/test-infix.dict");
//		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
//
//		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzeczypospolitej"));
//		Assertions.assertArrayEquals(new String[]{"Rzeczycki", "adj:pl:nom:m"}, stem(s, "Rzeczyccy"));
//		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzecząpospolitą"));
//		//this word is not in the dictionary
//		assertNoStemFor(s, "martygalski");
//		//this word uses characters that are outside of the encoding range of the dictionary
//		assertNoStemFor(s, "Rzeczyckiõh");
//	}
//
//	@Test
//	void testWordDataIterator() throws IOException{
//		final URL url = getClass().getResource("/services/fsa/lookup/test-infix.dict");
//		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
//
//		final Set<String> entries = new HashSet<>();
//		for(WordData wd : s)
//			entries.add(wd.getWord() + " " + wd.getStem() + " " + wd.getTag());
//
//		//make sure a sample of the entries is present
//		Assertions.assertEquals(new HashSet<>(List.of("Rzekunia Rzekuń subst:sg:gen:m", "Rzeczkowskie Rzeczkowski adj:sg:nom.acc.voc:n+adj:pl:acc.nom.voc:f.n", "Rzecząpospolitą Rzeczpospolita subst:irreg", "Rzeczypospolita Rzeczpospolita subst:irreg", "Rzeczypospolitych Rzeczpospolita subst:irreg", "Rzeczyckiej Rzeczycki adj:sg:gen.dat.loc:f")), entries);
//	}
//
//	@Test
//	void testWordDataCloning() throws IOException{
//		final URL url = getClass().getResource("/services/fsa/lookup/test-infix.dict");
//		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
//
//		List<WordData> words = new ArrayList<>();
//		for(WordData wd : s){
//			WordData clone = wd.clone();
//			words.add(clone);
//		}
//
//		//reiterate and verify that we have the same entries
//		final DictionaryLookup s2 = new DictionaryLookup(Dictionary.read(url));
//		int i = 0;
//		for(WordData wd : s2){
//			WordData clone = words.get(i++);
//			assertEqualSequences(clone.getStem(), wd.getStem());
//			assertEqualSequences(clone.getTag(), wd.getTag());
//			assertEqualSequences(clone.getWord(), wd.getWord());
//		}
//
//		//check collections contract
//		final Set<WordData> entries = new HashSet<>();
//		try{
//			entries.add(words.get(0));
//			Assertions.fail();
//		}
//		catch(RuntimeException e){
//			// Expected.
//		}
//	}
//
//	private void assertEqualSequences(CharSequence s1, CharSequence s2){
//		Assertions.assertEquals(s1.toString(), s2.toString());
//	}
//
//	@Test
//	void testMultibyteEncodingUTF8() throws IOException{
//		final URL url = getClass().getResource("/services/fsa/lookup/test-diacritics-utf8.dict");
//		Dictionary read = Dictionary.read(url);
//		final DictionaryLookup s = new DictionaryLookup(read);
//
//		Assertions.assertArrayEquals(new String[]{"merge", "001"}, stem(s, "mergeam"));
//		Assertions.assertArrayEquals(new String[]{"merge", "002"}, stem(s, "merseserăm"));
//	}
//
//	@Test
//	void testSynthesis() throws IOException{
//		final URL url = getClass().getResource("/services/fsa/lookup/test-synth.dict");
//		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
//
//		Assertions.assertArrayEquals(new String[]{"miała", null}, stem(s, "mieć|verb:praet:sg:ter:f:?perf"));
//		Assertions.assertArrayEquals(new String[]{"a", null}, stem(s, "a|conj"));
//		Assertions.assertArrayEquals(new String[]{}, stem(s, "dziecko|subst:sg:dat:n"));
//
//		// This word is not in the dictionary.
//		assertNoStemFor(s, "martygalski");
//	}
//
//	@Test
//	void testInputWithSeparators() throws IOException{
//		final URL url = getClass().getResource("/services/fsa/lookup/test-separators.dict");
//		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
//
//		//attemp to reconstruct input sequences using WordData iterator
//		ArrayList<String> sequences = new ArrayList<>();
//		for(WordData wd : s)
//			sequences.add("" + wd.getWord() + " " + wd.getStem() + " " + wd.getTag());
//		Collections.sort(sequences);
//
//		Assertions.assertEquals("token1 null null", sequences.get(0));
//		Assertions.assertEquals("token2 null null", sequences.get(1));
//		Assertions.assertEquals("token3 null +", sequences.get(2));
//		Assertions.assertEquals("token4 token2 null", sequences.get(3));
//		Assertions.assertEquals("token5 token2 null", sequences.get(4));
//		Assertions.assertEquals("token6 token2 +", sequences.get(5));
//		Assertions.assertEquals("token7 token2 token3+", sequences.get(6));
//		Assertions.assertEquals("token8 token2 token3++", sequences.get(7));
//	}
//
//	@Test
//	void testSeparatorInLookupTerm() throws IOException{
//		FSA fsa = FSA.read(getClass().getResourceAsStream("/services/fsa/lookup/test-separator-in-lookup.fsa"));
//
//		DictionaryMetadata metadata = new DictionaryMetadataBuilder()
//			.separator('+')
//			.encoding("iso8859-1")
//			.encoder(EncoderType.INFIX)
//			.build();
//
//		final DictionaryLookup s = new DictionaryLookup(new Dictionary(fsa, metadata));
//		Assertions.assertEquals(0, s.lookup("l+A").size());
//	}
//
//	@Test
//	void testGetSeparator() throws IOException{
//		final URL url = getClass().getResource("/services/fsa/lookup/test-separators.dict");
//		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
//		Assertions.assertEquals('+', s.getSeparator());
//	}

	private static void assertNoStemFor(DictionaryLookup s, String word){
		Assertions.assertArrayEquals(new String[]{}, stem(s, word));
	}

	private static String[] stem(DictionaryLookup s, String word){
		List<String> result = new ArrayList<>();
		for(WordData wd : s.lookup(word)){
			result.add(asString(wd.getStem()));
			result.add(asString(wd.getTag()));
		}
		return result.toArray(String[]::new);
	}

	private static String asString(CharSequence s){
		return (s != null? s.toString(): null);
	}

}
