package unit731.hunspeller.languages.vec;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.AffixEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.RuleProductionEntry;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.hyphenation.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.PatternService;


public class DictionaryParserVEC extends DictionaryParser{

	private static final String VANISHING_EL = "ƚ";

	private static final Matcher MISMATCHED_VARIANTS = PatternService.matcher("ƚ[^ŧđ]*[ŧđ]|[ŧđ][^ƚ]*ƚ");
	private static final Matcher MULTIPLE_ACCENTS = PatternService.matcher("([^àèéíòóú]*[àèéíòóú]){2,}");

	private static final Matcher L_BETWEEN_VOWELS = PatternService.matcher("l i l$");
	private static final Matcher D_BETWEEN_VOWELS = PatternService.matcher("d[ou]ra? [ou]ra?\\/[^ ]+ \\[aei\\]d[ou]ra?$");
	private static final Matcher NHIV = PatternService.matcher("[cijɉñ]i[aàeèéiíoòóuú]");
	private static final Matcher CIUI = PatternService.matcher("ciuí$");

	private static final Pattern REGEX_PATTERN_HYPHEN_MINUS = PatternService.pattern(HyphenationParser.HYPHEN_MINUS);

	private static final String NON_VANISHING_L = "(^l|[aeiouàèéíòóú]l)[aeiouàèéíòóú][^ƚ/]*/[^\\t\\n]*";
	private static final String NON_VANISHING_L_NOT_ENDING_IN_A = "(^l|[aeiouàèéíòóú]l)[aeiouàèéíòóú][^ƚ/]*[^a]/[^\\t\\n]*";
	private static final String VANISHING_L = "ƚ[^/]+/[^\\t\\n]*";
	private static final String VANISHING_L_NOT_ENDING_IN_A = "ƚ[^/]*[^a]/[^\\t\\n]*";

	private static final Matcher CAN_HAVE_METAPHONESIS = PatternService.matcher("[eo]([kƚñstxv]o|nt[eo]|[lnr])/");
	private static final Matcher HAS_METAPHONESIS = PatternService.matcher("/[^\\t\\n]*mf");
	private static final Matcher HAS_PLURAL = PatternService.matcher("[^i]/[^\\t\\n]*T0|[^aie]/[^\\t\\n]*B0|[^ieo]/[^\\t\\n]*C0|[^aio]/[^\\t\\n]*D0");
	private static final Matcher MISSING_PLURAL_AFTER_N_OR_L = PatternService.matcher("^[^ƚ]*[eaouèàòéóú][ln]\\/[^ZUu\\t]+\\t");
	private static final Matcher ENDS_IN_MAN = PatternService.matcher("man\\/");

	private static final Map<Matcher, String> ERROR_CHECKS = new HashMap<>();
	static{
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L + "E1"), "Cannot use E1 rule with vanishing el, use E2");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L + "E2"), "Cannot use E2 rule with non-vanishing el, use E1");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L + "G1"), "Cannot use G1 rule with vanishing el, use G2");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L + "G2"), "Cannot use G2 rule with non-vanishing el, use G1");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L + "F1"), "Cannot use F1 rule with vanishing el, use F2");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L + "F2"), "Cannot use F2 rule with non-vanishing el, use F1");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L + "P1"), "Cannot use P1 rule with vanishing el, use P2");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L + "P2"), "Cannot use P2 rule with non-vanishing el, use P1");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L + "Q1"), "Cannot use Q1 rule with vanishing el, use Q2");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L + "Q2"), "Cannot use Q2 rule with non-vanishing el, use Q1");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L + "r0"), "Cannot use r0 rule with vanishing el, use r1");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L + "r1"), "Cannot use r1 rule with non-vanishing el, use r0");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L + "s1"), "Cannot use s1 rule with vanishing el, use s2");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L + "s2"), "Cannot use s2 rule with non-vanishing el, use s1");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L + "W0"), "Cannot use W0 rule with vanishing el, use W1");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L + "W1"), "Cannot use W1 rule with non-vanishing el, use W0");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L_NOT_ENDING_IN_A + "&0"), "Cannot use &0 rule with vanishing el, use &1");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L_NOT_ENDING_IN_A + "&1"), "Cannot use &1 rule with non-vanishing el, use &0");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L_NOT_ENDING_IN_A + "\\[0"), "Cannot use [0 rule with vanishing el, use [1");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L_NOT_ENDING_IN_A + "\\[1"), "Cannot use [1 rule with non-vanishing el, use [0");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L_NOT_ENDING_IN_A + "\\(0"), "Cannot use (0 rule with vanishing el, use (1");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L_NOT_ENDING_IN_A + "\\(1"), "Cannot use (1 rule with non-vanishing el, use (0");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L_NOT_ENDING_IN_A + "\\)0"), "Cannot use )0 rule with vanishing el, use )1");
		ERROR_CHECKS.put(PatternService.matcher(NON_VANISHING_L_NOT_ENDING_IN_A + "\\)1"), "Cannot use )1 rule with non-vanishing el, use )0");
		ERROR_CHECKS.put(PatternService.matcher(VANISHING_L_NOT_ENDING_IN_A + "<0"), "Cannot use <0 rule with vanishing el, use <1");
		ERROR_CHECKS.put(PatternService.matcher("[đŧ][^/]*[^a]/[^\\t\\n]*<1"), "Cannot use <1 rule with đ or ŧ, use <0");
	}

	private static final String TAB = "\t";

	private static final String POS_NOUN = "noun";
	private static final String POS_PROPER_NOUN = "proper_noun";
	private static final String POS_VERB = "verb";
	private static final String POS_ADJECTIVE = "adjective";
	private static final String POS_ADJECTIVE_POSSESSIVE = "adjective_possessive";
	private static final String POS_ADJECTIVE_DEMONSTRATIVE = "adjective_demonstrative";
	private static final String POS_ADJECTIVE_IDENTIFICATIVE = "adjective_identificative";
	private static final String POS_ADJECTIVE_INTERROGATIVE = "adjective_interrogative";
	private static final String POS_QUANTIFIER = "quantifier";
	private static final String POS_NUMERAL_LATIN = "numeral_latin";
	private static final String POS_NUMERAL_CARDENAL = "numeral_cardenal";
	private static final String POS_NUMERAL_ORDENAL = "numeral_ordenal";
	private static final String POS_NUMERAL_COLLECTIVE = "numeral_collective";
	private static final String POS_NUMERAL_FRACTIONAL = "numeral_fractional";
	private static final String POS_NUMERAL_MULTIPLICATIVE = "numeral_multiplicative";
	private static final String POS_ARTICLE = "article";
	private static final String POS_PRONOUN = "pronoun";
	private static final String POS_PREPOSITION = "preposition";
	private static final String POS_ADVERB = "adverb";
	private static final String POS_CONJUNCTION = "conjunction";
	private static final String POS_PREFIX = "prefix";
	private static final String POS_INTERJECTION = "interjection";
	private static final String POS_UNIT_OF_MEASURE = "unit_of_measure";
	
	private static final Set<String> PART_OF_SPEECH = new HashSet<>();
	static{
		PART_OF_SPEECH.add(POS_NOUN);
		PART_OF_SPEECH.add(POS_PROPER_NOUN);
		PART_OF_SPEECH.add(POS_VERB);
		PART_OF_SPEECH.add(POS_ADJECTIVE);
		PART_OF_SPEECH.add(POS_ADJECTIVE_POSSESSIVE);
		PART_OF_SPEECH.add(POS_ADJECTIVE_DEMONSTRATIVE);
		PART_OF_SPEECH.add(POS_ADJECTIVE_IDENTIFICATIVE);
		PART_OF_SPEECH.add(POS_ADJECTIVE_INTERROGATIVE);
		PART_OF_SPEECH.add(POS_QUANTIFIER);
		PART_OF_SPEECH.add(POS_NUMERAL_LATIN);
		PART_OF_SPEECH.add(POS_NUMERAL_CARDENAL);
		PART_OF_SPEECH.add(POS_NUMERAL_ORDENAL);
		PART_OF_SPEECH.add(POS_NUMERAL_COLLECTIVE);
		PART_OF_SPEECH.add(POS_NUMERAL_FRACTIONAL);
		PART_OF_SPEECH.add(POS_NUMERAL_MULTIPLICATIVE);
		PART_OF_SPEECH.add(POS_ARTICLE);
		PART_OF_SPEECH.add(POS_PRONOUN);
		PART_OF_SPEECH.add(POS_PREPOSITION);
		PART_OF_SPEECH.add(POS_ADVERB);
		PART_OF_SPEECH.add(POS_CONJUNCTION);
		PART_OF_SPEECH.add(POS_PREFIX);
		PART_OF_SPEECH.add(POS_INTERJECTION);
		PART_OF_SPEECH.add(POS_UNIT_OF_MEASURE);
	}

	private static final Matcher REGEX_I_ACUTE = PatternService.matcher("(?:i/|ì)");
	private static final Matcher REGEX_O_ACUTE = PatternService.matcher("o/");
	private static final Matcher REGEX_U_ACUTE = PatternService.matcher("(?:u/|ù)");

	private static final Matcher REGEX_DH = PatternService.matcher("dh");
	private static final Matcher REGEX_JH = PatternService.matcher("jh");
	private static final Matcher REGEX_LH = PatternService.matcher("lh");
	private static final Matcher REGEX_NH = PatternService.matcher("nh");
	private static final Matcher REGEX_TH = PatternService.matcher("th");


	public DictionaryParserVEC(File dicFile, WordGenerator wordGenerator, Charset charset){
		super(dicFile, wordGenerator, charset);
	}

	@Override
	protected void checkLine(String line) throws IllegalArgumentException{
		if(!line.contains(TAB))
			throw new IllegalArgumentException("Line does not contains data fields");

		if(line.contains("/") && !line.contains(WordGenerator.TAG_PART_OF_SPEECH + POS_VERB)
				&& !line.contains(WordGenerator.TAG_PART_OF_SPEECH + POS_ADVERB)){
			if(!line.contains(WordGenerator.TAG_PART_OF_SPEECH + POS_PROPER_NOUN) && !line.contains(WordGenerator.TAG_PART_OF_SPEECH + POS_ARTICLE)){
				boolean canHaveMetaphonesis = PatternService.find(line, CAN_HAVE_METAPHONESIS);
				boolean hasMetaphonesisFlag = PatternService.find(line, HAS_METAPHONESIS);
				boolean hasPluralFlag = PatternService.find(line, HAS_PLURAL);
				if(canHaveMetaphonesis && !hasMetaphonesisFlag && hasPluralFlag)
					throw new IllegalArgumentException("Metaphonesis missing, add mf");
				else if(!canHaveMetaphonesis && hasMetaphonesisFlag && !hasPluralFlag)
					throw new IllegalArgumentException("Metaphonesis not needed, remove mf");
			}
			if(!line.contains(WordGenerator.TAG_PART_OF_SPEECH + POS_ARTICLE) && !line.contains(WordGenerator.TAG_PART_OF_SPEECH + POS_PRONOUN)
					&& !PatternService.find(line, ENDS_IN_MAN) && PatternService.find(line, MISSING_PLURAL_AFTER_N_OR_L))
				throw new IllegalArgumentException("Plural missing after n or l, add u0 or U0");
		}

		Set<Matcher> keys = ERROR_CHECKS.keySet();
		for(Matcher key : keys)
			if(PatternService.find(line, key))
				throw new IllegalArgumentException(ERROR_CHECKS.get(key));
	}

	@Override
	protected void checkProduction(DictionaryEntry dicEntry, RuleProductionEntry production) throws IllegalArgumentException{
		String derivedWord = production.getWord();
		if(PatternService.find(derivedWord, MISMATCHED_VARIANTS))
			throw new IllegalArgumentException("Word with a vanishing el cannot contain characters from another variant: " + derivedWord);
		if(derivedWord.contains(VANISHING_EL) && production.containsRuleFlag("U0"))
			throw new IllegalArgumentException("Word with a vanishing el cannot contain rule U0:" + derivedWord);
		if(production.containsRuleFlag("B0") && production.containsRuleFlag("&0"))
			throw new IllegalArgumentException("Word with rule B0 cannot rule &0:" + derivedWord);

		String[] dataFields = dicEntry.getDataFields();
		for(String dataField : dataFields)
			if(dataField.startsWith(WordGenerator.TAG_PART_OF_SPEECH) && !PART_OF_SPEECH.contains(dataField.substring(3)))
				throw new IllegalArgumentException("Word has an unknown Part Of Speech: " + dataField);

		if(production.getRuleFlags().length > 0 && !production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_VERB)
				&& !production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_ADVERB)){
			String derivedWordWithoutDataFields = derivedWord + dicEntry.getStrategy().joinRuleFlags(production.getRuleFlags());
			if(!production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_PROPER_NOUN)
					&& !production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_ARTICLE)){
				boolean canHaveMetaphonesis = PatternService.find(derivedWordWithoutDataFields, CAN_HAVE_METAPHONESIS);
				boolean hasMetaphonesisFlag = production.containsRuleFlag("mf");
				boolean hasPluralFlag = PatternService.find(derivedWordWithoutDataFields, HAS_PLURAL);
				if(canHaveMetaphonesis && !hasMetaphonesisFlag && hasPluralFlag)
					throw new IllegalArgumentException("Metaphonesis missing for word " + derivedWordWithoutDataFields + ", add mf");
				else if(!canHaveMetaphonesis && hasMetaphonesisFlag && !hasPluralFlag)
					throw new IllegalArgumentException("Metaphonesis not needed for word " + derivedWordWithoutDataFields + ", remove mf");
			}
			if(!production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_ARTICLE)
					&& !production.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_PRONOUN)
					&& !PatternService.find(derivedWordWithoutDataFields, ENDS_IN_MAN)
					&& PatternService.find(derivedWordWithoutDataFields, MISSING_PLURAL_AFTER_N_OR_L))
				throw new IllegalArgumentException("Plural missing after n or l for word " + derivedWordWithoutDataFields + ", add u0 or U0");
		}

		String[] splittedWords = PatternService.split(derivedWord, REGEX_PATTERN_HYPHEN_MINUS);
		for(String subword : splittedWords){
			if(PatternService.find(subword, MULTIPLE_ACCENTS))
				throw new IllegalArgumentException("Word cannot have multiple accents: " + derivedWord);
			if(Word.isStressed(subword) && !subword.equals(Word.unmarkDefaultStress(subword))){
				boolean elBetweenVowelsRemoval = production.getRules().stream()
					.map(AffixEntry::toString)
					.map(L_BETWEEN_VOWELS::reset)
					.anyMatch(Matcher::find);
				if(!elBetweenVowelsRemoval)
					throw new IllegalArgumentException("Word cannot have an accent here: " + derivedWord);
			}
			if(!dicEntry.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_NUMERAL_LATIN) && PatternService.find(subword, NHIV)
					&& !PatternService.find(subword, CIUI)){
				boolean dBetweenVowelsRemoval = production.getRules().stream()
					.map(AffixEntry::toString)
					.map(D_BETWEEN_VOWELS::reset)
					.anyMatch(Matcher::find);
				if(!dBetweenVowelsRemoval)
					throw new IllegalArgumentException("Word cannot have [cijɉñ]iV: " + derivedWord);
			}
		}

		//check syllabation
		if(hyphenationParser != null && derivedWord.length() > 1 && !derivedWord.contains(HyphenationParser.HYPHEN_MINUS)
				&& !dicEntry.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_NUMERAL_LATIN)
				&& !dicEntry.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_UNIT_OF_MEASURE)
				&& (!dicEntry.containsDataField(WordGenerator.TAG_PART_OF_SPEECH + POS_INTERJECTION) || !Arrays.asList("brr", "mh", "ssh").contains(derivedWord))){
			Hyphenation hyphenation = hyphenationParser.hyphenate(derivedWord);
			if(hyphenation.hasErrors())
				throw new IllegalArgumentException("Word is not syllabable (" + String.join(HyphenationParser.HYPHEN, hyphenation.getSyllabes())
					+ "): " + derivedWord);
		}
	}

	@Override
	public String prepareTextForFilter(String text){
		text = super.prepareTextForFilter(text);

		text = PatternService.replaceAll(text, REGEX_I_ACUTE, "í");
		text = PatternService.replaceAll(text, REGEX_O_ACUTE, "ó");
		text = PatternService.replaceAll(text, REGEX_U_ACUTE, "ú");
		text = PatternService.replaceAll(text, REGEX_DH, "(dh|đ)");
		text = PatternService.replaceAll(text, REGEX_JH, "(jh|ɉ)");
		text = PatternService.replaceAll(text, REGEX_LH, "(lh|ƚ)");
		text = PatternService.replaceAll(text, REGEX_NH, "(nh|ñ)");
		text = PatternService.replaceAll(text, REGEX_TH, "(th|ŧ)");

		return text;
	}

}
