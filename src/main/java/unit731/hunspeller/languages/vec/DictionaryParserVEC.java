package unit731.hunspeller.languages.vec;

import java.io.File;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.DictionaryParser;
import unit731.hunspeller.resources.AffixEntry;
import unit731.hunspeller.resources.DictionaryEntry;
import unit731.hunspeller.resources.RuleProductionEntry;
import unit731.hunspeller.services.WordGenerator;


public class DictionaryParserVEC extends DictionaryParser{

	private static final Matcher MISMATCHED_VARIANTS = Pattern.compile("ƚ.*[ŧđ]|[ŧđ].*ƚ").matcher(StringUtils.EMPTY);
	private static final Matcher MULTIPLE_ACCENTS = Pattern.compile("([^àèéíòóú]*[àèéíòóú]){2,}").matcher(StringUtils.EMPTY);

	private static final Matcher L_BETWEEN_VOWELS = Pattern.compile("l i l$").matcher(StringUtils.EMPTY);
	private static final Matcher D_BETWEEN_VOWELS = Pattern.compile("d[ou]ra? [ou]ra?\\/[^ ]+ \\[aei\\]d[ou]ra?$").matcher(StringUtils.EMPTY);
	private static final Matcher NHIV = Pattern.compile("[cijɉñ]i[aàeèéiíoòóuú]").matcher(StringUtils.EMPTY);
	private static final Matcher CIUI = Pattern.compile("ciuí$").matcher(StringUtils.EMPTY);
	
	private static final Matcher VANISHING_L_AND_NON_VANISHING_DIMINUTIVE = Pattern.compile("ƚ[^/]+[^a]/[^\\t\\n]*&0").matcher(StringUtils.EMPTY);
	private static final Matcher NON_VANISHING_L_AND_VANISHING_DIMINUTIVE = Pattern.compile("^[^ƚ/]+[^a]/[^\\t\\n]*&1").matcher(StringUtils.EMPTY);
	private static final Matcher MISSING_PLURAL_AFTER_N_OR_L = Pattern.compile("^[^ƚ]*[eaouèàòéóú][ln]\\/[^ZUu\\t]+\\t").matcher(StringUtils.EMPTY);
	private static final Matcher ENDS_IN_MAN = Pattern.compile("man\\/").matcher(StringUtils.EMPTY);

	private static final String TAB = "\t";
	private static final String VERB = "po:verb";
	private static final String ARTICLE = "po:article";
	private static final String ADVERB = "po:adverb";


	public DictionaryParserVEC(File dicFile, WordGenerator wordGenerator, Charset charset){
		super(dicFile, wordGenerator, charset);
	}

	@Override
	protected void checkLineLanguageSpecific(String line) throws IllegalArgumentException{
		if(!line.contains(TAB))
			throw new IllegalArgumentException("Line does not contains data fields");
		if(VANISHING_L_AND_NON_VANISHING_DIMINUTIVE.reset(line).find())
			throw new IllegalArgumentException("Cannot use &0 rule with vanishing el, use &1");
		if(NON_VANISHING_L_AND_VANISHING_DIMINUTIVE.reset(line).find())
			throw new IllegalArgumentException("Cannot use &1 rule with non-vanishing el, use &0");
		if(!line.contains(VERB) && !line.contains(ARTICLE) && !line.contains(ADVERB) && !ENDS_IN_MAN.reset(line).find() && MISSING_PLURAL_AFTER_N_OR_L.reset(line).find())
			throw new IllegalArgumentException("Plural missing after n or l, add u0 or U0");
	}

	@Override
	protected void checkProductionLanguageSpecific(DictionaryEntry dicEntry, RuleProductionEntry production) throws IllegalArgumentException{
		String derivedWord = production.getWord();
		if(MISMATCHED_VARIANTS.reset(derivedWord).find())
			throw new IllegalArgumentException("Word with a vanishing el cannot contain characters from another variant: " + derivedWord);

		String[] splittedWords = derivedWord.split("-");
		for(String subword : splittedWords){
			if(MULTIPLE_ACCENTS.reset(subword).find())
				throw new IllegalArgumentException("Word cannot have multiple accents: " + derivedWord);
			if(Word.isStressed(subword) && !subword.equals(Word.unmarkDefaultStress(subword))){
				boolean elBetweenVowelsRemoval = production.getRules().stream()
					.map(AffixEntry::toString)
					.map(L_BETWEEN_VOWELS::reset)
					.anyMatch(Matcher::find);
				if(!elBetweenVowelsRemoval)
					throw new IllegalArgumentException("Word cannot have an accent here: " + derivedWord);
			}
			if(!dicEntry.containsDataField("po:numeral_latin") && NHIV.reset(subword).find() && !CIUI.reset(subword).find()){
				boolean dBetweenVowelsRemoval = production.getRules().stream()
					.map(AffixEntry::toString)
					.map(D_BETWEEN_VOWELS::reset)
					.anyMatch(Matcher::find);
				if(!dBetweenVowelsRemoval)
					throw new IllegalArgumentException("Word cannot have [cijɉñ]iV: " + derivedWord);
			}
		}
	}

	@Override
	public String prepareTextForFilter(String text){
		text = super.prepareTextForFilter(text);

		text = StringUtils.replaceAll(text, "ì", "í");
		text = StringUtils.replaceAll(text, "i/", "í");
		text = StringUtils.replaceAll(text, "o/", "ó");
		text = StringUtils.replaceAll(text, "ù", "ú");
		text = StringUtils.replaceAll(text, "u/", "ú");
		text = StringUtils.replaceAll(text, "dh", "(dh|đ)");
		text = StringUtils.replaceAll(text, "jh", "(jh|ɉ)");
		text = StringUtils.replaceAll(text, "lh", "(lh|ƚ)");
		text = StringUtils.replaceAll(text, "nh", "(nh|ñ)");
		text = StringUtils.replaceAll(text, "th", "(th|ŧ)");

		return text;
	}

}
