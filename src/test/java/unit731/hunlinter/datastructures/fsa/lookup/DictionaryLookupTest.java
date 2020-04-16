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
import java.nio.charset.StandardCharsets;
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

	@Test
	void prefixDictionaries() throws IOException{
		final URL url = getClass().getResource("/services/fsa/lookup/prefix.dict");
		Dictionary d = Dictionary.read(url);
		final DictionaryLookup s = new DictionaryLookup(d);

		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzeczypospolitej", d));
		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzecząpospolitą", d));

		//this word is not in the dictionary
		assertNoStemFor(s, "martygalski", d);
	}

	@Test
	void inputConversion() throws IOException{
		final URL url = getClass().getResource("/services/fsa/lookup/prefix.dict");
		Dictionary d = Dictionary.read(url);
		final DictionaryLookup s = new DictionaryLookup(d);

		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzecz\\apospolit\\a", d));

		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "krowa\\apospolit\\a", d));
	}

	@Test
	void infixDictionaries() throws IOException{
		final URL url = getClass().getResource("/services/fsa/lookup/infix.dict");
		Dictionary d = Dictionary.read(url);
		final DictionaryLookup s = new DictionaryLookup(d);

		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzeczypospolitej", d));
		Assertions.assertArrayEquals(new String[]{"Rzeczycki", "adj:pl:nom:m"}, stem(s, "Rzeczyccy", d));
		Assertions.assertArrayEquals(new String[]{"Rzeczpospolita", "subst:irreg"}, stem(s, "Rzecząpospolitą", d));
		//this word is not in the dictionary
		assertNoStemFor(s, "martygalski", d);
		//this word uses characters that are outside of the encoding range of the dictionary
		assertNoStemFor(s, "Rzeczyckiõh", d);
	}

	@Test
	void wordDataIterator() throws IOException{
		final URL url = getClass().getResource("/services/fsa/lookup/infix.dict");
		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));

		final Set<String> entries = new HashSet<>();
		for(WordData wd : s)
			entries.add(wd.getWord() + " " + wd.getStem() + " " + wd.getTag());

		//make sure a sample of the entries is present
		Assertions.assertEquals(new HashSet<>(List.of("Rzekunia Rzekuń subst:sg:gen:m", "Rzeczkowskie Rzeczkowski adj:sg:nom.acc.voc:n+adj:pl:acc.nom.voc:f.n", "Rzecząpospolitą Rzeczpospolita subst:irreg", "Rzeczypospolita Rzeczpospolita subst:irreg", "Rzeczypospolitych Rzeczpospolita subst:irreg", "Rzeczyckiej Rzeczycki adj:sg:gen.dat.loc:f")), entries);
	}

//	@Test
//	void wordDataCloning() throws IOException{
//		final URL url = getClass().getResource("/services/fsa/lookup/infix.dict");
//		final DictionaryLookup s = new DictionaryLookup(Dictionary.read(url));
//
//		List<WordData> words = new ArrayList<>();
//		for(WordData wd : s){
//			WordData clone = new WordData();
//			clone.setWord(wd.getWord());
//			clone.setStem(wd.getStem());
//			clone.setTag(wd.getTag());
//			words.add(clone);
//		}
//
//		//reiterate and verify that we have the same entries
//		final DictionaryLookup s2 = new DictionaryLookup(Dictionary.read(url));
//		int i = 0;
//		for(WordData wd : s2){
//			WordData clone = words.get(i ++);
//			Assertions.assertArrayEquals(clone.getStem(), wd.getStem());
//			Assertions.assertArrayEquals(clone.getTag(), wd.getTag());
//			Assertions.assertEquals(clone.getWord(), wd.getWord());
//		}
//
//		//check collections contract
//		final Set<WordData> entries = new HashSet<>();
//		try{
//			entries.add(words.get(0));
//			Assertions.fail();
//		}
//		catch(RuntimeException e){
//			//expected
//			System.out.println();
//		}
//	}

	@Test
	void multibyteEncodingUTF8() throws IOException{
		final URL url = getClass().getResource("/services/fsa/lookup/diacritics-utf8.dict");
		Dictionary d = Dictionary.read(url);
		final DictionaryLookup s = new DictionaryLookup(d);

		Assertions.assertArrayEquals(new String[]{"merge", "001"}, stem(s, "mergeam", d));
		Assertions.assertArrayEquals(new String[]{"merge", "002"}, stem(s, "merseserăm", d));
	}

	@Test
	void synthesis() throws IOException{
		final URL url = getClass().getResource("/services/fsa/lookup/synth.dict");
		Dictionary d = Dictionary.read(url);
		final DictionaryLookup s = new DictionaryLookup(d);

		Assertions.assertArrayEquals(new String[]{"miała", null}, stem(s, "mieć|verb:praet:sg:ter:f:?perf", d));
		Assertions.assertArrayEquals(new String[]{"a", null}, stem(s, "a|conj", d));
		Assertions.assertArrayEquals(new String[]{}, stem(s, "dziecko|subst:sg:dat:n", d));
		//this word is not in the dictionary
		assertNoStemFor(s, "martygalski", d);
	}

//	@Test
//	void inputWithSeparators() throws IOException{
//		final URL url = getClass().getResource("/services/fsa/lookup/separators.dict");
//		Dictionary d = Dictionary.read(url);
//		final DictionaryLookup s = new DictionaryLookup(d);
//
//		//attemp to reconstruct input sequences using WordData iterator
//		List<String> sequences = new ArrayList<>();
//		for(WordData wd : s)
//			sequences.add(wd.getWord() + " " + toString(wd.getStem(), d) + " " + toString(wd.getTag(), d));
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

	@Test
	void separatorInLookupTerm() throws IOException{
		FSA fsa = FSA.read(getClass().getResourceAsStream("/services/fsa/lookup/separator-in-lookup.fsa"));

		DictionaryMetadata metadata = new DictionaryMetadataBuilder()
			.separator('+')
			.encoding("iso8859-1")
			.encoder(EncoderType.INFIX)
			.build();

		final DictionaryLookup s = new DictionaryLookup(new Dictionary(fsa, metadata));
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
			() -> s.lookup("l+A"));
		Assertions.assertEquals("No valid input can contain the separator: l+A", exception.getMessage());
	}


	private static void assertNoStemFor(DictionaryLookup s, String word, Dictionary d){
		Assertions.assertArrayEquals(new String[]{}, stem(s, word, d));
	}

	private static String[] stem(DictionaryLookup s, String word, Dictionary d){
		List<String> result = new ArrayList<>();
		for(WordData wd : s.lookup(word)){
			result.add(toString(wd.getStem(), d));
			result.add(toString(wd.getTag(), d));
		}
		return result.toArray(String[]::new);
	}

	private static String toString(byte[] array, Dictionary d){
		return (array != null? new String(array,d.metadata.getCharset()): null);
	}

}
