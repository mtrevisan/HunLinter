package unit731.hunspeller.parsers.hyphenation;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.collections.trie.Trie;
import unit731.hunspeller.services.PatternService;


public class HyphenationParserTest{

	private static final Matcher REGEX_CLEANER = Pattern.compile("\\d|/.+$").matcher(StringUtils.EMPTY);


	@Test
	public void noHyphenationDueToLeftMin(){
		Trie<String> patterns = new Trie<>();
		patterns.add("abc", "a1bc");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(2)
			.rightMin(0)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("abc"), hyphenation.getSyllabes());
	}

	@Test
	public void noHyphenationDueToRightMin(){
		Trie<String> patterns = new Trie<>();
		patterns.add("abc", "ab1c");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(0)
			.rightMin(2)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("abc"), hyphenation.getSyllabes());
	}

	@Test
	public void hyphenationOkLeftMin(){
		Trie<String> patterns = new Trie<>();
		addRule(patterns, "a1bc");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(0)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("a", "bc"), hyphenation.getSyllabes());
	}

	@Test
	public void hyphenationOkRightMin(){
		Trie<String> patterns = new Trie<>();
		addRule(patterns, "ab1c");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(0)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("ab", "c"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithRemovalBeforeHyphen(){
		Trie<String> patterns = new Trie<>();
		addRule(patterns, "aa1tje/=,2,1");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("du", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("omaatje");

		Assert.assertEquals(Arrays.asList("oma", "tje"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithIndexes(){
		Trie<String> patterns = new Trie<>();
		addRule(patterns, "1-/-=,1,1");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("ab-cd");

		Assert.assertEquals(Arrays.asList("ab-", "-cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithoutIndexes(){
		Trie<String> patterns = new Trie<>();
		addRule(patterns, "1-/-=");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("ab-cd");

		Assert.assertEquals(Arrays.asList("ab-", "-cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedAfterBreak(){
		Trie<String> patterns = new Trie<>();
		addRule(patterns, "-1/-=-");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("ab-cd");

		Assert.assertEquals(Arrays.asList("ab-", "-cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedAfterBreak2(){
		Trie<String> patterns = new Trie<>();
		addRule(patterns, "1k");
		addRule(patterns, "-1/-=-");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("kuko-fu");

		Assert.assertEquals(Arrays.asList("ku", "ko-", "-fu"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedNonWordInitial(){
		Trie<String> patterns = new Trie<>();
		addRule(patterns, "eigh1teen/ht=t,4,2");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("en", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("eighteen");

		Assert.assertEquals(Arrays.asList("eight", "teen"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWordInitial(){
		Trie<String> patterns = new Trie<>();
		addRule(patterns, ".schif1fahrt/ff=f,5,2");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("de", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("schiffahrt");

		Assert.assertEquals(Arrays.asList("schiff", "fahrt"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedBase(){
		Trie<String> patterns = new Trie<>();
		addRule(patterns, "c1k/k=k");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("de", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("Zucker");

		Assert.assertEquals(Arrays.asList("Zuk", "ker"), hyphenation.getSyllabes());
	}

	@Test
	public void competingRules(){
		Trie<String> patterns = new Trie<>();
		addRule(patterns, "ab1c");
		addRule(patterns, "2c");
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", patterns, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("abc"), hyphenation.getSyllabes());
	}


	private void addRule(Trie<String> patterns, String rule){
		patterns.add(getKeyFromData(rule), rule);
	}

	private String getKeyFromData(String rule){
		return PatternService.replaceAll(rule, REGEX_CLEANER, StringUtils.EMPTY);
	}

}
