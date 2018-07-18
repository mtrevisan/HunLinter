package unit731.hunspeller.parsers.hyphenation;

import unit731.hunspeller.parsers.hyphenation.hyphenators.Hyphenator;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;
import unit731.hunspeller.parsers.hyphenation.valueobjects.HyphenationOptions;
import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationInterface;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(2)
			.rightMin(0)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		check(parser, "abc", "abc");
	}

	@Test
	public void noHyphenationDueToRightMin(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		patterns1stLevel.put("abc", "ab1c");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(0)
			.rightMin(2)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		check(parser, "abc", "abc");
	}

	@Test
	public void hyphenationOkLeftMin(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "a1bc");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(0)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		check(parser, "abc", "a", "bc");
	}

	@Test
	public void hyphenationOkRightMin(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "ab1c");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(0)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		check(parser, "abc", "ab", "c");
	}

	@Test
	public void augmentedWithRemovalBeforeHyphen(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "aa1tje/=,2,1");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("du", allPatterns, null, options);

		check(parser, "omaatje", "oma", "tje");
	}

	@Test
	public void augmentedWithIndexes(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "1–/–=,1,1");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedWithoutIndexes(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "1–/–=");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedAfterBreak(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "–1/–=–");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedAfterBreakWithRuleOverlap(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "–3/–=–");
		addRule(patterns1stLevel, "1c");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		check(parser, "ab–cd", "ab–", "–cd");
	}

	@Test
	public void augmentedAfterBreak2(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "1k");
		addRule(patterns1stLevel, "–1/–=–");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		check(parser, "kuko–fu", "ku", "ko–", "–fu");
	}

	@Test
	public void augmentedNonWordInitial(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "eigh1teen/ht=t,4,2");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("en", allPatterns, null, options);

		check(parser, "eighteen", "eight", "teen");
	}

	@Test
	public void augmentedWordInitial(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, ".schif1fahrt/ff=f,5,2");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("de", allPatterns, null, options);

		check(parser, "schiffahrt", "schiff", "fahrt");
	}

	@Test
	public void augmentedBase(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "c1k/k=k");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("de", allPatterns, null, options);

		check(parser, "Zucker", "Zuk", "ker");
	}

	@Test
	public void customHyphenation(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		Map<HyphenationParser.Level, Map<String, String>> custom = new HashMap<>();
		Map<String, String> custom1stLevel = new HashMap<>();
		custom1stLevel.put("abcd", "ab=cd");
		custom.put(HyphenationParser.Level.FIRST, custom1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, custom, options);

		check(parser, "abcd", "ab", "cd");
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
		HyphenationParser parser = new HyphenationParser("vec", allPatterns, null, options);

		check(parser, "abc", "abc");
	}


	/** German pre-reform hyphenation: Schiffahrt -> Schiff-fahrt */
	@Test
	public void germanPreReform(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "f1f");
		addRule(patterns1stLevel, "if3fa/ff=f,2,2");
		addRule(patterns1stLevel, "tenerif5fa");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("de", allPatterns, null, options);

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
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("hu", allPatterns, null, options);

		check(parser, "asszonnyal", "asz", "szony", "nyal");
	}

	/** Dutch: omaatje -> oma-tje */
	@Test
	public void dutch1(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "aa1tje./=,2,1");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("nl", allPatterns, null, options);

		check(parser, "omaatje", "oma", "tje");
	}

	/** Dutch: omaatje -> oma-tje */
	@Test
	public void dutch2(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "aa1tje./a=tje,1,5");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("nl", allPatterns, null, options);

		check(parser, "omaatje", "oma", "tje");
	}

	@Test
	public void french(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "xé1ém/á=a,2,2");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("fr", allPatterns, null, options);

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
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, options);

		check(parser, "paral·lel", "paral", "lel");
		check(parser, "omaatje", "oma", "tje");
		check(parser, "cafeetje", "café", "tje");
		check(parser, "schiffahrt", "schiff", "fahrt");
		check(parser, "drucker", "druk", "ker");
		check(parser, "briddzsel", "bridzs", "dzsel");
		check(parser, "asszonnyal", "asz", "szony", "nyal");
		check(parser, "tillata", "till", "lata");
	}

	@Test
	public void englishCompound1(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "motor1cycle");
		RadixTree<String, String> patterns2ndLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns2ndLevel, ".mo1tor.");
		addRule(patterns2ndLevel, ".cy1cle.");
		//check independency of the 1st and 2nd hyphenation levels
		addRule(patterns2ndLevel, ".motor2cycle.");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.SECOND, patterns2ndLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftCompoundMin(2)
			.rightCompoundMin(3)
			.build();
		HyphenationParser parser = new HyphenationParser("en", allPatterns, null, options);

		check(parser, "motorcycle", "mo", "tor", "cy", "cle");
	}

	@Test
	public void englishCompound2(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "motor1cycle");
		RadixTree<String, String> patterns2ndLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns2ndLevel, ".mo1tor.");
		addRule(patterns2ndLevel, ".cy1cle.");
		//check independency of the 1st and 2nd hyphenation levels
		addRule(patterns2ndLevel, ".motor2cycle.");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.SECOND, patterns2ndLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftCompoundMin(3)
			.rightCompoundMin(4)
			.build();
		HyphenationParser parser = new HyphenationParser("en", allPatterns, null, options);

		check(parser, "motorcycle", "motor", "cycle");
	}

	@Test
	public void compound2(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "szony1fő");
		addRule(patterns1stLevel, "ök1assz");
		RadixTree<String, String> patterns2ndLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns2ndLevel, ".as1szony./sz=,2,1");
		addRule(patterns2ndLevel, ".fő1nök.");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.SECOND, patterns2ndLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("hu", allPatterns, null, options);

		check(parser, "főnökasszony", "fő", "nök", "asz", "szony");
		check(parser, "asszonyfőnök", "asz", "szony", "fő", "nök");
	}

	/**
	 * Norwegian: non-standard hyphenation at compound boundary (kilowattime -> kilowatt-time)
	 * and recursive compound hyphenation (kilowatt->kilo-watt)
	 */
	@Test
	public void compound3(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "wat1time/tt=t,3,2");
		addRule(patterns1stLevel, ".kilo1watt");
		RadixTree<String, String> patterns2ndLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns2ndLevel, ".ki1lo.");
		addRule(patterns2ndLevel, ".ti1me.");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.SECOND, patterns2ndLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("no", allPatterns, null, options);

		check(parser, "kilowattime", "ki", "lo", "watt", "ti", "me");
	}

	@Test
	public void compound5(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, ".post1");
		RadixTree<String, String> patterns2ndLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns2ndLevel, "e1");
		addRule(patterns2ndLevel, "a1");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.SECOND, patterns2ndLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.leftCompoundMin(1)
			.rightCompoundMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, options);

		check(parser, "postea", "post", "e", "a");
	}

	@Test
	public void compound6(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "1que.");
		RadixTree<String, String> patterns2ndLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns2ndLevel, "e1");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.SECOND, patterns2ndLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.leftCompoundMin(1)
			.rightCompoundMin(1)
			.build();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, options);

		check(parser, "meaque", "me", "a", "que");
	}

	@Test
	public void noHyphen1(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "1_1");
		addRule(patterns1stLevel, "1" + HyphenationParser.MINUS_SIGN + "1");
		addRule(patterns1stLevel, "1" + HyphenationParser.APOSTROPHE + "1");
		addRule(patterns1stLevel, "1" + HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK + "1");
		RadixTree<String, String> patterns2ndLevel = RadixTree.createTree(new StringSequencer());
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.SECOND, patterns2ndLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.leftCompoundMin(1)
			.rightCompoundMin(1)
			.noHyphen(new HashSet<>(Arrays.asList("^_", "_$", "-", HyphenationParser.APOSTROPHE, HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK)))
			.build();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, options);

		check(parser, "_foobara'foobarb-foo_barc\u2019foobard_", "_foobara'foobarb-foo", "_", "barc\u2019foobard_");
	}

	@Test
	public void noHyphen2(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "1_1");
		addRule(patterns1stLevel, "1" + HyphenationParser.MINUS_SIGN + "1");
		addRule(patterns1stLevel, "1" + HyphenationParser.APOSTROPHE + "1");
		addRule(patterns1stLevel, "1" + HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK + "1");
		RadixTree<String, String> patterns2ndLevel = RadixTree.createTree(new StringSequencer());
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		allPatterns.put(HyphenationParser.Level.SECOND, patterns2ndLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(1)
			.rightMin(1)
			.leftCompoundMin(1)
			.rightCompoundMin(1)
			.noHyphen(new HashSet<>(Arrays.asList("-", HyphenationParser.APOSTROPHE, HyphenationParser.RIGHT_SINGLE_QUOTATION_MARK, "=")))
			.build();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, options);

		check(parser, "=foobara'foobarb-foo_barc\u2019foobard=", "=foobara'foobarb-foo", "_", "barc\u2019foobard=");
	}

	/** Unicode ligature hyphenation (ffi -> f=fi) */
	@Test
	public void ligature(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "ﬃ1/f=ﬁ,1,1");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, options);

		check(parser, "maﬃa", "maf", "ﬁa");
		check(parser, "maﬃaﬃa", "maf", "ﬁaf", "ﬁa");
	}

		@Test
	public void settings(){
		RadixTree<String, String> patterns1stLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns1stLevel, "ő1");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.FIRST, patterns1stLevel);
		HyphenationOptions options = HyphenationOptions.builder()
			.leftMin(2)
			.rightMin(2)
			.build();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, options);

		check(parser, "őőőőőőő", "őő", "ő", "ő", "ő", "őő");
	}

	@Test
	public void unicode(){
		RadixTree<String, String> patterns2ndLevel = RadixTree.createTree(new StringSequencer());
		addRule(patterns2ndLevel, "l·1l/l=l,1,3");
		addRule(patterns2ndLevel, "e1ë/e=e,1,2");
		addRule(patterns2ndLevel, "a1atje./a=t,1,3");
		addRule(patterns2ndLevel, "e1etje./é=tje,1,5");
		addRule(patterns2ndLevel, "eigh1teen/t=t,5,1");
		addRule(patterns2ndLevel, ".schif1fahrt/ff=f,5,2");
		addRule(patterns2ndLevel, "c1k/k=k,1,2");
		addRule(patterns2ndLevel, "1ΐ/=ί,1,1");
		addRule(patterns2ndLevel, "d1dzsel./dzs=dzs,1,4");
		addRule(patterns2ndLevel, ".as3szon/sz=sz,2,3");
		addRule(patterns2ndLevel, "n1nyal./ny=ny,1,3");
		addRule(patterns2ndLevel, "bus1s/ss=s,3,2");
		addRule(patterns2ndLevel, "7-/=-,1,1");
		addRule(patterns2ndLevel, ".til1låta./ll=l,3,2");
		Map<HyphenationParser.Level, RadixTree<String, String>> allPatterns = new HashMap<>();
		allPatterns.put(HyphenationParser.Level.SECOND, patterns2ndLevel);
		HyphenationOptions options = HyphenationOptions.createEmpty();
		HyphenationParser parser = new HyphenationParser("xx", allPatterns, null, options);

		check(parser, "paral·lel", "paral", "lel");
		check(parser, "reëel", "re", "eel");
		check(parser, "omaatje", "oma", "tje");
		check(parser, "cafeetje", "café", "tje");
		check(parser, "eighteen", "eight", "teen");
		check(parser, "drucker", "druk", "ker");
		check(parser, "schiffahrt", "schiff", "fahrt");
		check(parser, "Μαΐου", "Μα", "ίου");
		check(parser, "asszonnyal", "asz", "szony", "nyal");
		check(parser, "briddzsel", "bridzs", "dzsel");
		check(parser, "bussjåfør", "buss", "sjåfør");
		check(parser, "100-sekundowy", "100", "-sekundowy");
		check(parser, "tillåta", "till", "låta");
	}


	private void addRule(RadixTree<String, String> patterns, String rule){
		patterns.put(getKeyFromData(rule), rule);
	}

	private String getKeyFromData(String rule){
		return PatternService.replaceAll(rule, REGEX_CLEANER, StringUtils.EMPTY);
	}

	private void check(HyphenationParser parser, String word, String ... hyphs){
		HyphenatorInterface hyphenator = new Hyphenator(parser);
		HyphenationInterface hyphenation = hyphenator.hyphenate(word);

		Assert.assertEquals(Arrays.asList(hyphs), hyphenation.getSyllabes());
	}

}
