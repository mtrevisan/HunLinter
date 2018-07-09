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

		check(parser, "abc", "abc");
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

		check(parser, "abc", "abc");
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

		check(parser, "abc", "a", "bc");
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

		check(parser, "abc", "ab", "c");
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

		check(parser, "omaatje", "oma", "tje");
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

		check(parser, "ab–cd", "ab–", "–cd");
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

		check(parser, "ab–cd", "ab–", "–cd");
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

		check(parser, "ab–cd", "ab–", "–cd");
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

		check(parser, "ab–cd", "ab–", "–cd");
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

		check(parser, "kuko–fu", "ku", "ko–", "–fu");
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

		check(parser, "eighteen", "eight", "teen");
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

		check(parser, "schiffahrt", "schiff", "fahrt");
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

		check(parser, "Zucker", "Zuk", "ker");
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

		check(parser, "abc", "abc");
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

		check(parser, "schiffen", "schif", "fen");
		check(parser, "schiffahrt", "schiff", "fahrt");
		check(parser, "teneriffa", "tenerif", "fa");
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

		check(parser, "asszonnyal", "asz", "szony", "nyal");
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

		check(parser, "omaatje", "oma", "tje");
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

		check(parser, "omaatje", "oma", "tje");
	}

	@Test
	public void french(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "xé1ém/á=a,2,2");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("fr", null, allPatterns, null, options);

		check(parser, "exéémple", "exá", "ample");
		check(parser, "exéémplxééme", "exá", "amplxá", "ame");
	}

	@Test
	public void baseAlt(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "l·1l/l=l,1,3");
		addRule(patterns1stLevel, "a1atje./a=t,1,3");
		addRule(patterns1stLevel, "e1etje./é=tje,1,5");
		addRule(patterns1stLevel, ".schif1fahrt/ff=f,5,2");
		addRule(patterns1stLevel, "c1k/k=k,1,2");
		addRule(patterns1stLevel, "d1dzsel./dzs=dzs,1,4");
		addRule(patterns1stLevel, ".as3szon/sz=sz,2,3");
		addRule(patterns1stLevel, "n1nyal./ny=ny,1,3");
		addRule(patterns1stLevel, ".til1lata./ll=l,3,2");
		patterns1stLevel.prepare();
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("xx", null, allPatterns, null, options);

		check(parser, "paral·lel", "paral", "lel");
		check(parser, "omaatje", "oma", "tje");
		check(parser, "cafeetje", "café", "tje");
		check(parser, "schiffahrt", "schiff", "fahrt");
		check(parser, "drucker", "druk", "ker");
		check(parser, "briddzsel", "bridzs", "dzsel");
		check(parser, "asszonnyal", "asz", "szony", "nyal");
		check(parser, "tillata", "till", "lata");
	}


	private void addRule(RadixTree<String, String> patterns, String rule){
		patterns.put(getKeyFromData(rule), rule);
	}

	private String getKeyFromData(String rule){
		return PatternService.replaceAll(rule, REGEX_CLEANER, StringUtils.EMPTY);
	}

	private void check(HyphenationParser parser, String word, String ... hyphs){
		HyphenationInterface hyphenation = parser.hyphenate(word);

		Assert.assertEquals(Arrays.asList(hyphs), hyphenation.getSyllabes());
	}

}
