package unit731.hunspeller.parsers.hyphenation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.collections.radixtree.sequencers.StringSequencer;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import unit731.hunspeller.services.PatternService;


public class HyphenationParserTest{

	private static final Matcher REGEX_CLEANER = PatternService.matcher("\\d|/.+$");


	@Test
	public void noHyphenationDueToLeftMin(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		patternsLevelCompound.put("abc", "a1bc");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(2)
			.rightMin(0)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("abc"), hyphenation.getSyllabes());
	}

	@Test
	public void noHyphenationDueToRightMin(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		patternsLevelCompound.put("abc", "ab1c");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(0)
			.rightMin(2)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("abc"), hyphenation.getSyllabes());
	}

	@Test
	public void hyphenationOkLeftMin(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, "a1bc");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(0)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("a", "bc"), hyphenation.getSyllabes());
	}

	@Test
	public void hyphenationOkRightMin(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, "ab1c");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(0)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("ab", "c"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithRemovalBeforeHyphen(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, "aa1tje/=,2,1");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("du", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("omaatje");

		Assert.assertEquals(Arrays.asList("oma", "tje"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithIndexes(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, "1-/-=,1,1");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("ab-cd");

		Assert.assertEquals(Arrays.asList("ab-", "-cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithoutIndexes(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, "1-/-=");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("ab-cd");

		Assert.assertEquals(Arrays.asList("ab-", "-cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedAfterBreak(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, "-1/-=-");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("ab-cd");

		Assert.assertEquals(Arrays.asList("ab-", "-cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedAfterBreak2(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, "1k");
		addRule(patternsLevelCompound, "-1/-=-");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("kuko-fu");

		Assert.assertEquals(Arrays.asList("ku", "ko-", "-fu"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedNonWordInitial(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, "eigh1teen/ht=t,4,2");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("en", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("eighteen");

		Assert.assertEquals(Arrays.asList("eight", "teen"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWordInitial(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, ".schif1fahrt/ff=f,5,2");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("de", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("schiffahrt");

		Assert.assertEquals(Arrays.asList("schiff", "fahrt"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedBase(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, "c1k/k=k");
		patternsLevelCompound.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("de", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("Zucker");

		Assert.assertEquals(Arrays.asList("Zuk", "ker"), hyphenation.getSyllabes());
	}

	@Test
	public void competingRules(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, "ab1c");
		addRule(patternsLevelCompound, "2c");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		Hyphenation hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("abc"), hyphenation.getSyllabes());
	}

	@Test
	public void ahoCorasick(){
		RadixTree<String, String> patternsLevelCompound = RadixTree.createTree(new StringSequencer());
		addRule(patternsLevelCompound, ".s2");
		addRule(patternsLevelCompound, "1v");
		addRule(patternsLevelCompound, "2nd");
		addRule(patternsLevelCompound, "1d");
		addRule(patternsLevelCompound, "2lm");
		addRule(patternsLevelCompound, "1m");
		addRule(patternsLevelCompound, "2nt");
		addRule(patternsLevelCompound, "1t");
		addRule(patternsLevelCompound, "1n");
		addRule(patternsLevelCompound, "1d");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.COMPOUND, patternsLevelCompound);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		String word = "savéndolmento";
		Hyphenation hyphenation = parser.hyphenate(word);

//savéndolmento	.s2 1v 2nd 1d 2lm 1m 2nt 1t
//						.s2 1n 2nd 1l 2lm 1n 2nt
		Assert.assertEquals(Arrays.asList("sa", "vén", "dol", "men", "to"), hyphenation.getSyllabes());

		patternsLevelCompound.prepare();
		hyphenation = parser.hyphenate(word);

		Assert.assertEquals(Arrays.asList("sa", "vén", "dol", "men", "to"), hyphenation.getSyllabes());

		hyphenation = parser.hyphenate2(word);

		Assert.assertEquals(Arrays.asList("sa", "vén", "dol", "men", "to"), hyphenation.getSyllabes());
	}


	private void addRule(RadixTree<String, String> patterns, String rule){
		patterns.put(getKeyFromData(rule), rule);
	}

	private String getKeyFromData(String rule){
		return PatternService.replaceAll(rule, REGEX_CLEANER, StringUtils.EMPTY);
	}

}
