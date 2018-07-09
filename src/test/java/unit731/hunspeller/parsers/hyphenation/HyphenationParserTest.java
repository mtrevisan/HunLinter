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
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		patterns1stLevel.put("abc", "a1bc");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(2)
			.rightMin(0)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("abc"), hyphenation.getSyllabes());
	}

	@Test
	public void noHyphenationDueToRightMin(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		patterns1stLevel.put("abc", "ab1c");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(0)
			.rightMin(2)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("abc"), hyphenation.getSyllabes());
	}

	@Test
	public void hyphenationOkLeftMin(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "a1bc");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(0)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("a", "bc"), hyphenation.getSyllabes());
	}

	@Test
	public void hyphenationOkRightMin(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "ab1c");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(0)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("ab", "c"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithRemovalBeforeHyphen(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "aa1tje/=,2,1");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("du", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("omaatje");

		Assert.assertEquals(Arrays.asList("oma", "tje"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithIndexes(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "1–/–=,1,1");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("ab–cd");

		Assert.assertEquals(Arrays.asList("ab–", "–cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWithoutIndexes(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "1–/–=");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("ab–cd");

		Assert.assertEquals(Arrays.asList("ab–", "–cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedAfterBreak(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "–1/–=–");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("ab–cd");

		Assert.assertEquals(Arrays.asList("ab–", "–cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedAfterBreakWithRuleOverlap(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "–3/–=–");
		addRule(patterns1stLevel, "1c");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("ab–cd");

		Assert.assertEquals(Arrays.asList("ab–", "–cd"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedAfterBreak2(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "1k");
		addRule(patterns1stLevel, "–1/–=–");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("kuko–fu");

		Assert.assertEquals(Arrays.asList("ku", "ko–", "–fu"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedNonWordInitial(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "eigh1teen/ht=t,4,2");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("en", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("eighteen");

		Assert.assertEquals(Arrays.asList("eight", "teen"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedWordInitial(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, ".schif1fahrt/ff=f,5,2");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("de", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("schiffahrt");

		Assert.assertEquals(Arrays.asList("schiff", "fahrt"), hyphenation.getSyllabes());
	}

	@Test
	public void augmentedBase(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "c1k/k=k");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("de", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("Zucker");

		Assert.assertEquals(Arrays.asList("Zuk", "ker"), hyphenation.getSyllabes());
	}

	@Test
	public void competingRules(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "ab1c");
		addRule(patterns1stLevel, "2c");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("abc");

		Assert.assertEquals(Arrays.asList("abc"), hyphenation.getSyllabes());
	}

//	@Test
//	public void ahoCorasick(){
//		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
//		addRule(patterns1stLevel, ".s2");
//		addRule(patterns1stLevel, "1v");
//		addRule(patterns1stLevel, "2nd");
//		addRule(patterns1stLevel, "1d");
//		addRule(patterns1stLevel, "2lm");
//		addRule(patterns1stLevel, "1m");
//		addRule(patterns1stLevel, "2nt");
//		addRule(patterns1stLevel, "1t");
//		addRule(patterns1stLevel, "1n");
//		addRule(patterns1stLevel, "1d");
//		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
//		allPatterns.put(HyphenationParser.Level.COMPOUND, patterns1stLevel);
//		HyphenationOptions options = HyphenationOptions.builder()
//			.leftMin(1)
//			.build();
//		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);
//
//		String word = "savendolmento";
//		HyphenationInterface hyphenation = parser.hyphenate(word);
//
//		Assert.assertEquals(Arrays.asList(null, ".s2", "1v", null, "2nd", "1d", null, "2lm", "1m", null, "2nt", "1t", null), hyphenation.getRules());
//		Assert.assertEquals(Arrays.asList("sa", "ven", "dol", "men", "to"), hyphenation.getSyllabes());
//
//		patterns1stLevel.prepare();
//		hyphenation = parser.hyphenate(word);
//
//		Assert.assertEquals(Arrays.asList(null, ".s2", "1v", null, "2nd", "1d", null, "2lm", "1m", null, "2nt", "1t", null), hyphenation.getRules());
//		Assert.assertEquals(Arrays.asList("sa", "ven", "dol", "men", "to"), hyphenation.getSyllabes());
//
//		hyphenation = parser.hyphenate2(word);
//
//		Assert.assertEquals(Arrays.asList(null, ".s2", "1v", null, "2nd", "1d", null, "2lm", "1m", null, "2nt", "1t", null), hyphenation.getRules());
//		Assert.assertEquals(Arrays.asList("sa", "ven", "dol", "men", "to"), hyphenation.getSyllabes());
//	}


	/** German pre-reform hyphenation: Schiffahrt -> Schiff-fahrt */
	@Test
	public void germanPreReform(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "f1f");
		addRule(patterns1stLevel, "if3fa/ff=f,2,2");
		addRule(patterns1stLevel, "tenerif5fa");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("de", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("schiffen");

		Assert.assertEquals(Arrays.asList("schif", "fen"), hyphenation.getSyllabes());

		hyphenation = parser.hyphenate("schiffahrt");

		Assert.assertEquals(Arrays.asList("schiff", "fahrt"), hyphenation.getSyllabes());

		hyphenation = parser.hyphenate("teneriffa");

		Assert.assertEquals(Arrays.asList("tenerif", "fa"), hyphenation.getSyllabes());
	}

	/** Hungarian simplified double 2-character consonants: ssz -> sz-sz, nny -> ny-ny */
	@Test
	public void hungarianSimplifiedDoubleConsonants(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "s1sz/sz=sz,1,3");
		addRule(patterns1stLevel, "n1ny/ny=ny,1,3");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("hu", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("asszonnyal");

		Assert.assertEquals(Arrays.asList("asz", "szony", "nyal"), hyphenation.getSyllabes());
	}

	/** Dutch: omaatje -> oma-tje */
	@Test
	public void dutch1(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "aa1tje./=,2,1");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("nl", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("omaatje");

		Assert.assertEquals(Arrays.asList("oma", "tje"), hyphenation.getSyllabes());
	}

	/** Dutch: omaatje -> oma-tje */
	@Test
	public void dutch2(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "aa1tje./a=tje,1,5");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("nl", null, allPatterns, null, options);

		HyphenationInterface hyphenation = parser.hyphenate("omaatje");

		Assert.assertEquals(Arrays.asList("oma", "tje"), hyphenation.getSyllabes());
	}


	private void addRule(RadixTree<String, String> patterns, String rule){
		patterns.put(getKeyFromData(rule), rule);
	}

	private String getKeyFromData(String rule){
		return PatternService.replaceAll(rule, REGEX_CLEANER, StringUtils.EMPTY);
	}

}
