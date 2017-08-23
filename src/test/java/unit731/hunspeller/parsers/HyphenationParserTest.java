package unit731.hunspeller.parsers;

import java.util.Arrays;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.collections.trie.Trie;
import unit731.hunspeller.resources.Hyphenation;
import unit731.hunspeller.resources.HyphenationOptions;


public class HyphenationParserTest{

	@Test
	public void noHyphenationDueToLeftMin(){
		Trie<String> patterns = new Trie<>();
		patterns.add("abc", "a1bc");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(2)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("abc"), hyphenation.getSyllabes());
	}

	@Test
	public void base(){
		Trie<String> patterns = new Trie<>();
		String rule = "a1bc";
		patterns.add(getKeyFromData(rule), rule);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("a", "bc"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithRemovalBeforeHyphen(){
		Trie<String> patterns = new Trie<>();
		String rule = "aa1tje/=,2,1";
		patterns.add(getKeyFromData(rule), rule);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("du", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("omaatje");

		Assert.assertEquals(Arrays.asList("oma", "tje"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithIndexes(){
		Trie<String> patterns = new Trie<>();
		String rule = "1-/-=,1,1";
		patterns.add(getKeyFromData(rule), rule);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("ab-cd");

		Assert.assertEquals(Arrays.asList("ab-", "-cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithoutIndexes(){
		Trie<String> patterns = new Trie<>();
		String rule = "1-/-=";
		patterns.add(getKeyFromData(rule), rule);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("ab-cd");

		Assert.assertEquals(Arrays.asList("ab-", "-cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedNonWordInitial(){
		Trie<String> patterns = new Trie<>();
		String rule = "eigh1teen/ht=t,4,2";
		patterns.add(getKeyFromData(rule), rule);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("en-US", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("eighteen");

		Assert.assertEquals(Arrays.asList("eight", "teen"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWordInitial(){
		Trie<String> patterns = new Trie<>();
		String rule = ".schif1fahrt/ff=f,5,2";
		patterns.add(getKeyFromData(rule), rule);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("de", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("schiffahrt");

		Assert.assertEquals(Arrays.asList("schiff", "fahrt"), hyphenation.getSyllabes());
	}

	private String getKeyFromData(String rule){
		return rule.replaceAll("\\d|/.+$", StringUtils.EMPTY);
	}

}
