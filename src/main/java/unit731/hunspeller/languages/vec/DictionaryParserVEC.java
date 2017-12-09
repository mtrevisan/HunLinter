package unit731.hunspeller.languages.vec;

import java.io.File;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.AffixEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.RuleProductionEntry;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.hyphenation.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;


public class DictionaryParserVEC extends DictionaryParser{

	private static final String VANISHING_EL = "ƚ";
	private static final String START_TAGS = "/[^\\t\\n]*";

	private static final String ADJECTIVE_FIRST_CLASS_RULE = "B0";
	private static final String ADJECTIVE_SECOND_CLASS_RULE = "C0";
	private static final String ADJECTIVE_THIRD_CLASS_RULE = "D0";
	private static final String PLURAL_NOUN_MASCULINE_RULE = "T0";
	private static final String VARIANT_TRANSFORMATIONS_RULE = "T2";
	private static final String METAPHONESIS_RULE = "mf";
	private static final String DIMINUTIVE_ETO_RULE_NON_VANISHING_EL = "&0";
	private static final String DIMINUTIVE_ETO_RULE_VANISHING_EL = "&1";
	private static final String DIMINUTIVE_EL_RULE_NON_VANISHING_EL = "[0";
	private static final String DIMINUTIVE_EL_RULE_VANISHING_EL = "[1";
	private static final String AUGMENTATIVE_OTO_RULE_NON_VANISHING_EL = "(0";
	private static final String AUGMENTATIVE_OTO_RULE_VANISHING_EL = "(1";
	private static final String AUGMENTATIVE_ON_RULE_NON_VANISHING_EL = ")0";
	private static final String AUGMENTATIVE_ON_RULE_VANISHING_EL = ")1";
	private static final String PEJORATIVE_ATO_RULE = "§0";
	private static final String PEJORATIVE_ATHO_RULE_NON_VANISHING_EL = "<0";
	private static final String PEJORATIVE_ATHO_RULE_VANISHING_EL = "<1";
	private static final String NORTHERN_PLURAL_ACCENTED_RULE = "U0";
	private static final String COLLECTIVE_NOUNS_RULE = "Y0";
	private static final String FINAL_SONORIZATION_RULE = "I0";

	private static final Matcher MISMATCHED_VARIANTS = PatternService.matcher("ƚ[^ŧđ]*[ŧđ]|[ŧđ][^ƚ]*ƚ");
	private static final Matcher NON_VANISHING_EL = PatternService.matcher("(^|[aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ-])l([aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ-]|$)");
	private static final Matcher VANISHING_EL_NEAR_CONSONANT = PatternService.matcher("[^aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ-]ƚ|ƚ[^aàeèéiíoòóuúAÀEÈÉIÍOÒÓUÚʼ]");

	private static final Matcher L_BETWEEN_VOWELS = PatternService.matcher("l i l$");
	private static final Matcher D_BETWEEN_VOWELS = PatternService.matcher("d[ou]ra? [ou]ra?\\/[^ ]+ \\[aei\\]d[ou]ra?$");
	private static final Matcher NHIV = PatternService.matcher("[cijɉñ]i[aàeèéiíoòóuú]");
	private static final Matcher CIUI = PatternService.matcher("ciuí$");

	private static final Pattern REGEX_PATTERN_HYPHEN_MINUS = PatternService.pattern(HyphenationParser.HYPHEN_MINUS);

	private static final String NON_VANISHING_L = "(^l|[aeiouàèéíòóú]l)[aeiouàèéíòóú][^ƚ/]*" + START_TAGS;
	private static final String NON_VANISHING_L_NOT_ENDING_IN_A = "(^l|[aeiouàèéíòóú]l)[aeiouàèéíòóú][^ƚ/]*[^a]" + START_TAGS;
	private static final String VANISHING_L = "ƚ[^/]+" + START_TAGS;
	private static final String VANISHING_L_NOT_ENDING_IN_A = "ƚ[^/]*[^a]" + START_TAGS;

	private static final Matcher CAN_HAVE_METAPHONESIS = PatternService.matcher("([eo]([dđkƚñstŧxv]o|nt[eo]|[lnr])|o[lr][kd]o|orse|exe)$");
	private static final Matcher HAS_PLURAL = PatternService.matcher("[^i]" + START_TAGS + "T0|[^aie]" + START_TAGS + "B0|[^ieo]" + START_TAGS
		+ "C0|[^aio]" + START_TAGS + ADJECTIVE_THIRD_CLASS_RULE);
	private static final Matcher MISSING_PLURAL_AFTER_N_OR_L = PatternService.matcher("^[^ƚ]*[eaouèàòéóú][ln]\\/[^ZUu\\t]+\\t");
	private static final Matcher ENDS_IN_MAN = PatternService.matcher("man\\/");

	private static final Map<Matcher, String> MISMATCH_CHECKS = new HashMap<>();
	private static final String CANNOT_USE_RULE_WITH_VANISHING_EL = "Cannot use {0} rule with vanishing el, use {1}";
	private static final String CANNOT_USE_RULE_WITH_NON_VANISHING_EL = "Cannot use {0} rule with non-vanishing el, use {1}";
	static{
		MISMATCH_CHECKS.put(PatternService.matcher(VANISHING_L + "r0"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_VANISHING_EL, "r0", "r1"));
		MISMATCH_CHECKS.put(PatternService.matcher(NON_VANISHING_L + "r1"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_NON_VANISHING_EL, "r1", "r0"));
		MISMATCH_CHECKS.put(PatternService.matcher(VANISHING_L + "s1"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_VANISHING_EL, "s1", "s2"));
		MISMATCH_CHECKS.put(PatternService.matcher(NON_VANISHING_L + "s2"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_NON_VANISHING_EL, "s2", "s1"));
		MISMATCH_CHECKS.put(PatternService.matcher(VANISHING_L + "W0"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_VANISHING_EL, "W0", "W1"));
		MISMATCH_CHECKS.put(PatternService.matcher(NON_VANISHING_L + "W1"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_NON_VANISHING_EL, "W1", "W0"));
		MISMATCH_CHECKS.put(PatternService.matcher(VANISHING_L_NOT_ENDING_IN_A + DIMINUTIVE_ETO_RULE_NON_VANISHING_EL),
			MessageFormat.format(CANNOT_USE_RULE_WITH_VANISHING_EL, "&0", "&1"));
		MISMATCH_CHECKS.put(PatternService.matcher(NON_VANISHING_L_NOT_ENDING_IN_A + DIMINUTIVE_ETO_RULE_VANISHING_EL),
			MessageFormat.format(CANNOT_USE_RULE_WITH_NON_VANISHING_EL, "&1", "&0"));
		MISMATCH_CHECKS.put(PatternService.matcher(VANISHING_L_NOT_ENDING_IN_A + "\\[0"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_VANISHING_EL, "[0", "[1"));
		MISMATCH_CHECKS.put(PatternService.matcher(NON_VANISHING_L_NOT_ENDING_IN_A + "\\[1"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_NON_VANISHING_EL, "[1", "[0"));
		MISMATCH_CHECKS.put(PatternService.matcher(VANISHING_L_NOT_ENDING_IN_A + "\\(0"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_VANISHING_EL, "(0", "(1"));
		MISMATCH_CHECKS.put(PatternService.matcher(NON_VANISHING_L_NOT_ENDING_IN_A + "\\(1"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_NON_VANISHING_EL, "(1", "(0"));
		MISMATCH_CHECKS.put(PatternService.matcher(VANISHING_L_NOT_ENDING_IN_A + "\\)0"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_VANISHING_EL, ")0", ")1"));
		MISMATCH_CHECKS.put(PatternService.matcher(NON_VANISHING_L_NOT_ENDING_IN_A + "\\)1"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_NON_VANISHING_EL, ")1", ")0"));
		MISMATCH_CHECKS.put(PatternService.matcher(VANISHING_L_NOT_ENDING_IN_A + "<0"),
			MessageFormat.format(CANNOT_USE_RULE_WITH_VANISHING_EL, "<0", "<1"));
		MISMATCH_CHECKS.put(PatternService.matcher("[đŧ][^/]*[^a]" + START_TAGS + "<1"), "Cannot use <1 rule with đ or ŧ, use <0");
	}

	private static final Set<List<String>> ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS = new HashSet<>();
	private static final String WORD_WITH_RULE_CANNOT_HAVE = "Word with rule {0} cannot have rule {1}";
	static{
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(Arrays.asList(DIMINUTIVE_ETO_RULE_NON_VANISHING_EL, DIMINUTIVE_ETO_RULE_VANISHING_EL,
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "B0", "&0 or &1")));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(Arrays.asList(DIMINUTIVE_EL_RULE_NON_VANISHING_EL, DIMINUTIVE_EL_RULE_VANISHING_EL,
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "B0", "[0 or [1")));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(Arrays.asList(AUGMENTATIVE_OTO_RULE_NON_VANISHING_EL, AUGMENTATIVE_OTO_RULE_VANISHING_EL,
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "B0", "(0 or (1")));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(Arrays.asList(AUGMENTATIVE_ON_RULE_NON_VANISHING_EL, AUGMENTATIVE_ON_RULE_VANISHING_EL,
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "B0", ")0 or )1")));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(Arrays.asList(PEJORATIVE_ATO_RULE,
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "B0", "§0")));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(Arrays.asList(COLLECTIVE_NOUNS_RULE,
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "B0", "Y0")));
		ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS.add(Arrays.asList(PEJORATIVE_ATHO_RULE_NON_VANISHING_EL, PEJORATIVE_ATHO_RULE_VANISHING_EL,
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "B0", "<0 or <1")));
	}

	//also V0/)0 - &0/&1
	private static final Set<List<String>> VARIANT_TRANSFORMATION_MISMATCH_CHECKS = new HashSet<>();
	static{
		VARIANT_TRANSFORMATION_MISMATCH_CHECKS.add(Arrays.asList("V0",
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "T2", "V0")));
		VARIANT_TRANSFORMATION_MISMATCH_CHECKS.add(Arrays.asList("v0",
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "T2", "v0")));
		VARIANT_TRANSFORMATION_MISMATCH_CHECKS.add(Arrays.asList("T1",
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "T2", "T1")));
		VARIANT_TRANSFORMATION_MISMATCH_CHECKS.add(Arrays.asList(DIMINUTIVE_ETO_RULE_NON_VANISHING_EL, DIMINUTIVE_ETO_RULE_VANISHING_EL,
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "T2", "&0 or &1")));
		VARIANT_TRANSFORMATION_MISMATCH_CHECKS.add(Arrays.asList(AUGMENTATIVE_ON_RULE_NON_VANISHING_EL, AUGMENTATIVE_ON_RULE_VANISHING_EL,
			MessageFormat.format(WORD_WITH_RULE_CANNOT_HAVE, "T2", ")0 or )1")));
	}

	private static final String POS_PROPER_NOUN = "proper_noun";
	private static final String POS_VERB = "verb";
	private static final String POS_ARTICLE = "article";
	private static final String POS_PRONOUN = "pronoun";
	private static final String POS_ADVERB = "adverb";
	private static final String POS_NUMERAL_LATIN = "numeral_latin";
	private static final String POS_INTERJECTION = "interjection";
	private static final String POS_UNIT_OF_MEASURE = "unit_of_measure";

	private static final Set<String> PART_OF_SPEECH = new HashSet<>(Arrays.asList("noun", POS_PROPER_NOUN, POS_VERB, "adjective",
		"adjective_possessive", "adjective_demonstrative", "adjective_identificative", "adjective_interrogative", "quantifier", POS_NUMERAL_LATIN,
		"numeral_cardenal", "numeral_ordenal", "numeral_collective", "numeral_fractional", "numeral_multiplicative", POS_ARTICLE, POS_PRONOUN,
		"preposition", POS_ADVERB, "conjunction", "prefix", POS_INTERJECTION, POS_UNIT_OF_MEASURE));
	private static final Set<String> INFLECTIONAL_SUFFIX = new HashSet<>(Arrays.asList("singular_masculine", "singular_femenine", "plural",
		"plural_masculine", "plural_femenine", "procomplementar", "interrogative", "second_singular", "second_plural"));
	private static final Set<String> TERMINAL_SUFFIX = new HashSet<>(Arrays.asList("indicative_present", "indicative_imperfect",
		"indicative_future", "subjunctive_present", "subjunctive_imperfect", "conditional_present", "imperative_present", "infinitive_simple",
		"gerund_simple", "participle_active", "participle_passive", "participle_perfect", "participle_perfect_strong", "participle_imperfect"));

	private static final Map<String, Set<String>> DATA_FIELDS = new HashMap<>();
	static{
		DATA_FIELDS.put(WordGenerator.TAG_PART_OF_SPEECH, PART_OF_SPEECH);
		DATA_FIELDS.put(WordGenerator.TAG_INFLECTIONAL_SUFFIX, INFLECTIONAL_SUFFIX);
		DATA_FIELDS.put(WordGenerator.TAG_TERMINAL_SUFFIX, TERMINAL_SUFFIX);
		DATA_FIELDS.put(WordGenerator.TAG_STEM, null);
		DATA_FIELDS.put(WordGenerator.TAG_ALLOMORPH, null);
	}

	private static final List<String> UNSYLLABABLE_INTERJECTIONS = Arrays.asList("brr", "ehh", "mh", "ohh", "ssh");


	private final Orthography orthography = OrthographyVEC.getInstance();


	public DictionaryParserVEC(File dicFile, WordGenerator wordGenerator, Charset charset){
		super(dicFile, wordGenerator, charset);
	}

	@Override
	protected void checkProduction(RuleProductionEntry production, FlagParsingStrategy strategy) throws IllegalArgumentException{
		try{
			if(!production.hasDataFields())
				throw new IllegalArgumentException("Line does not contains any data fields");

			dataFieldCheck(production);

			vanishingElCheck(production);

			incompatibilityCheck(production);

			String derivedWord = production.getWord();

			String derivedWordWithoutDataFields = derivedWord + strategy.joinRuleFlags(production.getRuleFlags());
			if(production.hasRuleFlags() && !production.isPartOfSpeech(POS_VERB) && !production.isPartOfSpeech(POS_ADVERB)){
				metaphonesisCheck(production, derivedWordWithoutDataFields);

				northernPluralCheck(production, derivedWordWithoutDataFields);
			}

			mismatchCheck(derivedWordWithoutDataFields);

			finalSonorizationCheck(production);

			String[] splittedWords = PatternService.split(derivedWord, REGEX_PATTERN_HYPHEN_MINUS);
			for(String subword : splittedWords){
				accentCheck(subword, production);

				ciuiCheck(subword, production);
			}

			syllabationCheck(production, derivedWord);
		}
		catch(IllegalArgumentException e){
			String message = e.getMessage();
			String rulesSequence = production.getRulesSequence();
			if(rulesSequence.length() > 0)
				message += " (via " + rulesSequence + ")";
			throw new IllegalArgumentException(message);
		}
	}

	private void dataFieldCheck(RuleProductionEntry production) throws IllegalArgumentException{
		String[] dataFields = production.getDataFields();
		if(dataFields != null)
			for(String dataField : dataFields){
				if(dataField.length() < 4)
					throw new IllegalArgumentException("Word " + production.getWord() + " has an invalid Data Field prefix: " + dataField);

				String dataFieldPrefix = dataField.substring(0, 3);
				if(!DATA_FIELDS.containsKey(dataFieldPrefix))
					throw new IllegalArgumentException("Word " + production.getWord() + " has an unknown Data Field prefix: " + dataField);

				Set<String> dataFieldTypes = DATA_FIELDS.get(dataFieldPrefix);
				if(dataFieldTypes != null && !dataFieldTypes.contains(dataField.substring(3)))
					throw new IllegalArgumentException("Word " + production.getWord() + " has an unknown Data Field value: " + dataField);
			}
	}

	private void vanishingElCheck(RuleProductionEntry production) throws IllegalArgumentException{
		String derivedWord = production.getWord();
		if(derivedWord.contains(VANISHING_EL) && PatternService.find(derivedWord, NON_VANISHING_EL))
			throw new IllegalArgumentException("Word with a vanishing el cannot contain non vanishing el, " + derivedWord);
		if(PatternService.find(derivedWord, MISMATCHED_VARIANTS))
			throw new IllegalArgumentException("Word with a vanishing el cannot contain characters from another variant, " + derivedWord);
		if(PatternService.find(derivedWord, VANISHING_EL_NEAR_CONSONANT))
			throw new IllegalArgumentException("Word with a vanishing el near a consonant, " + derivedWord);
		if(derivedWord.contains(VANISHING_EL) && production.containsRuleFlag(NORTHERN_PLURAL_ACCENTED_RULE))
			throw new IllegalArgumentException("Word with a vanishing el cannot contain rule U0, " + derivedWord);
	}

	private void incompatibilityCheck(RuleProductionEntry production) throws IllegalArgumentException{
		commonIncompatibilityCheck(production, ADJECTIVE_FIRST_CLASS_RULE, ADJECTIVE_FIRST_CLASS_MISMATCH_CHECKS);
		commonIncompatibilityCheck(production, VARIANT_TRANSFORMATIONS_RULE, VARIANT_TRANSFORMATION_MISMATCH_CHECKS);
	}

	private void commonIncompatibilityCheck(RuleProductionEntry production, String ruleFlag, Set<List<String>> checks)
			throws IllegalArgumentException{
		if(production.containsRuleFlag(ruleFlag))
			for(List<String> key : checks){
				int size = key.size() - 1;
				for(int i = 0; i < size; i ++)
					if(production.containsRuleFlag(key.get(i)))
						throw new IllegalArgumentException(key.get(size) + " for word " + production.getWord());
			}
	}

	private void metaphonesisCheck(RuleProductionEntry production, String line) throws IllegalArgumentException{
		if(!production.isPartOfSpeech(POS_PROPER_NOUN) && !production.isPartOfSpeech(POS_ARTICLE)){
			boolean canHaveMetaphonesis = PatternService.find(production.getWord(), CAN_HAVE_METAPHONESIS);
			boolean hasMetaphonesisFlag = production.containsRuleFlag(METAPHONESIS_RULE);
			if(canHaveMetaphonesis ^ hasMetaphonesisFlag){
				boolean hasPluralFlag = PatternService.find(line, HAS_PLURAL);
				if(canHaveMetaphonesis && hasPluralFlag)
					throw new IllegalArgumentException("Metaphonesis missing for word " + line + ", add mf");
				else if(!canHaveMetaphonesis && !hasPluralFlag)
					throw new IllegalArgumentException("Metaphonesis not needed for word " + line + ", remove mf");
			}
		}
	}

	private void northernPluralCheck(RuleProductionEntry production, String line) throws IllegalArgumentException{
		if(!production.isPartOfSpeech(POS_ARTICLE) && !production.isPartOfSpeech(POS_PRONOUN)
				&& !PatternService.find(line, ENDS_IN_MAN)
				&& PatternService.find(line, MISSING_PLURAL_AFTER_N_OR_L))
			throw new IllegalArgumentException("Plural missing after n or l for word " + line + ", add "
				+ (Word.isStressed(PatternService.clear(line, PatternService.matcher(START_TAGS)))? "u0": "U0"));
	}

	private void mismatchCheck(String line) throws IllegalArgumentException{
		Set<Matcher> keys = MISMATCH_CHECKS.keySet();
		for(Matcher key : keys)
			if(PatternService.find(line, key))
				throw new IllegalArgumentException(MISMATCH_CHECKS.get(key) + " for word " + line);
	}

	private void finalSonorizationCheck(RuleProductionEntry production) throws IllegalArgumentException{
		String word = production.getWord();
		List<AffixEntry> appliedRules = production.getAppliedRules();
		if(word.length() > 2 && (appliedRules == null || appliedRules.size() < 2) && production.hasProductionRule(AffixEntry.TYPE.SUFFIX)
				&& !production.hasProductionRule("G0") && !production.hasProductionRule("E0")
				&& !production.hasProductionRule("G1") && !production.hasProductionRule("E1")
				&& !word.contains(VANISHING_EL)
				&& !production.isPartOfSpeech(POS_PROPER_NOUN) && !production.isPartOfSpeech(POS_ARTICLE) && !production.isPartOfSpeech(POS_VERB)
				&& !production.hasProductionRule(ADJECTIVE_FIRST_CLASS_RULE)&& !production.hasProductionRule(PLURAL_NOUN_MASCULINE_RULE)
				&& !production.hasProductionRule(ADJECTIVE_THIRD_CLASS_RULE) && !production.hasProductionRule(ADJECTIVE_SECOND_CLASS_RULE)
				&& !production.hasProductionRule(VARIANT_TRANSFORMATIONS_RULE)){
			DictionaryEntry entry = new DictionaryEntry(production, FINAL_SONORIZATION_RULE, wordGenerator.getFlagParsingStrategy());
			List<RuleProductionEntry> productions = Collections.<RuleProductionEntry>emptyList();
			try{
				productions = wordGenerator.applyRules(entry);
			}
			catch(IllegalArgumentException e){
				//no productions result from the application of the rule
			}
			int numberOfProductions = productions.size();

			boolean hasRule = production.containsRuleFlag(FINAL_SONORIZATION_RULE);
			if(hasRule && numberOfProductions == 0)
				throw new IllegalArgumentException("Superfluous rule for word " + production.getWord() + ", remove " + FINAL_SONORIZATION_RULE);
			else if(!hasRule && numberOfProductions > 1)
				throw new IllegalArgumentException("Missing rule for word " + production.getWord() + ", add " + FINAL_SONORIZATION_RULE);
		}
	}

	private void accentCheck(String subword, RuleProductionEntry production) throws IllegalArgumentException{
		if(Word.hasMultipleAccents(subword))
			throw new IllegalArgumentException("Word " + production.getWord() + " cannot have multiple accents");

		if(Word.isStressed(subword) && !subword.equals(Word.unmarkDefaultStress(subword))){
			boolean elBetweenVowelsRemoval = false;
			List<AffixEntry> appliedRules = production.getAppliedRules();
			for(AffixEntry appliedRule : appliedRules)
				if(PatternService.find(appliedRule.toString(), L_BETWEEN_VOWELS)){
					elBetweenVowelsRemoval = true;
					break;
				}
			if(!elBetweenVowelsRemoval)
				throw new IllegalArgumentException("Word " + production.getWord() + " cannot have an accent here");
		}
	}

	private void ciuiCheck(String subword, RuleProductionEntry production) throws IllegalArgumentException{
		if(!production.isPartOfSpeech(POS_NUMERAL_LATIN) && PatternService.find(subword, NHIV) && !PatternService.find(subword, CIUI)){
			boolean dBetweenVowelsRemoval = false;
			List<AffixEntry> appliedRules = production.getAppliedRules();
			for(AffixEntry appliedRule : appliedRules)
				if(PatternService.find(appliedRule.toString(), D_BETWEEN_VOWELS)){
					dBetweenVowelsRemoval = true;
					break;
				}
			if(!dBetweenVowelsRemoval)
				throw new IllegalArgumentException("Word " + production.getWord() + " cannot have [cijɉñ]iV");
		}
	}

	private void syllabationCheck(RuleProductionEntry production, String derivedWord) throws IllegalArgumentException{
		if(!production.isPartOfSpeech(POS_VERB) && !production.isPartOfSpeech(POS_NUMERAL_LATIN) && !production.isPartOfSpeech(POS_UNIT_OF_MEASURE)){
			derivedWord = derivedWord.toLowerCase(Locale.ROOT);
			if(!UNSYLLABABLE_INTERJECTIONS.contains(derivedWord)){
				String correctedDerivedWord = hyphenationParser.correctOrthography(derivedWord);
				if(!correctedDerivedWord.equals(derivedWord))
					throw new IllegalArgumentException("Word " + derivedWord + " is mispelled (should be " + correctedDerivedWord + ")");
			}

			if(hyphenationParser != null && derivedWord.length() > 1 && !derivedWord.contains(HyphenationParser.HYPHEN_MINUS)
					&& !production.isPartOfSpeech(POS_NUMERAL_LATIN)
					&& !production.isPartOfSpeech(POS_UNIT_OF_MEASURE)
					&& (!production.isPartOfSpeech(POS_INTERJECTION) || !UNSYLLABABLE_INTERJECTIONS.contains(derivedWord))){
				Hyphenation hyphenation = hyphenationParser.hyphenate(derivedWord);
				if(hyphenation.hasErrors())
					throw new IllegalArgumentException("Word " + String.join(HyphenationParser.HYPHEN, hyphenation.getSyllabes())
						+ " is not syllabable");
			}
		}
	}

	@Override
	public String prepareTextForFilter(String text){
		text = super.prepareTextForFilter(text);

		return orthography.correctOrthography(text);
	}

}
