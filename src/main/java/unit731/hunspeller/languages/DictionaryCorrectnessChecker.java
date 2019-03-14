package unit731.hunspeller.languages;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;


public class DictionaryCorrectnessChecker{

	protected static final String SYLLABE_SEPARATOR = "/";
	protected static final String NON_SYLLABE_MARK = "*";

	private static final MessageFormat WORD_HAS_NOT_MORPHOLOGICAL_FIELD = new MessageFormat("{0} does not have any morphological fields");
	private static final MessageFormat WORD_HAS_INVALID_MORPHOLOGICAL_FIELD_PREFIX = new MessageFormat("{0} has an invalid morphological field prefix: {1}");
	private static final MessageFormat WORD_HAS_UNKNOWN_MORPHOLOGICAL_FIELD_PREFIX = new MessageFormat("{0} has an unknown morphological field prefix: {1}");
	private static final MessageFormat WORD_HAS_UNKNOWN_MORPHOLOGICAL_FIELD_VALUE = new MessageFormat("{0} has an unknown morphological field value: {1}");


	protected AffixData affixData;
	protected final HyphenatorInterface hyphenator;
	protected Orthography orthography;

	protected RulesLoader rulesLoader;


	public DictionaryCorrectnessChecker(AffixData affixData, HyphenatorInterface hyphenator){
		Objects.requireNonNull(affixData);

		this.affixData = affixData;
		this.hyphenator = hyphenator;
	}

	public void loadRules() throws IOException{
		rulesLoader = new RulesLoader(affixData.getLanguage(), affixData.getFlagParsingStrategy());
	}

	public HyphenatorInterface getHyphenator(){
		return hyphenator;
	}

	//used by the correctness worker:
	public void checkProduction(Production production) throws IllegalArgumentException{
		String forbidCompoundFlag = affixData.getForbidCompoundFlag();
		if(forbidCompoundFlag != null && !production.hasProductionRules() && production.hasContinuationFlag(forbidCompoundFlag)){
			IllegalArgumentException e = new IllegalArgumentException("Non-affix entry contains " + AffixTag.FORBID_COMPOUND_FLAG.getCode());
			manageException(e, production);
		}

		if(rulesLoader.isMorphologicalFieldsCheck()){
			try{
				morphologicalFieldCheck(production);
			}
			catch(IllegalArgumentException e){
				manageException(e, production);
			}
		}

		try{
			incompatibilityCheck(production);
		}
		catch(IllegalArgumentException e){
			manageException(e, production);
		}

		if(hyphenator != null){
			List<String> splittedWords = hyphenator.splitIntoCompounds(production.getWord());
			int size = splittedWords.size();
			int i = 1;
			for(String subword : splittedWords){
				try{
					checkCompoundProduction(subword, i - size, production);
				}
				catch(IllegalArgumentException e){
					manageException(e, production);
				}

				i ++;
			}
		}
	}

	protected void manageException(IllegalArgumentException e, Production production) throws IllegalArgumentException{
		StringBuilder sb = new StringBuilder(e.getMessage());
		if(production.hasProductionRules())
			sb.append(" (via ").append(production.getRulesSequence()).append(")");
		throw new IllegalArgumentException(sb.toString());
	}

	private void morphologicalFieldCheck(Production production) throws IllegalArgumentException{
		if(!production.hasMorphologicalFields())
			throw new IllegalArgumentException(WORD_HAS_NOT_MORPHOLOGICAL_FIELD.format(new Object[]{production.getWord()}));

		production.forEachMorphologicalField(morphologicalField -> {
			if(morphologicalField.length() < 4)
				throw new IllegalArgumentException(WORD_HAS_INVALID_MORPHOLOGICAL_FIELD_PREFIX.format(new Object[]{production.getWord(),
					morphologicalField}));

			String key = morphologicalField.substring(0, 3);
			if(!rulesLoader.containsDataField(key))
				throw new IllegalArgumentException(WORD_HAS_UNKNOWN_MORPHOLOGICAL_FIELD_PREFIX.format(new Object[]{production.getWord(),
					morphologicalField}));
			Set<String> morphologicalFieldTypes = rulesLoader.getDataField(key);
			if(morphologicalFieldTypes != null && !morphologicalFieldTypes.contains(morphologicalField.substring(3)))
				throw new IllegalArgumentException(WORD_HAS_UNKNOWN_MORPHOLOGICAL_FIELD_VALUE.format(new Object[]{production.getWord(),
					morphologicalField}));
		});
	}

	private void incompatibilityCheck(Production production) throws IllegalArgumentException{
		rulesLoader.letterToFlagIncompatibilityCheck(production);

		rulesLoader.flagToFlagIncompatibilityCheck(production);
	}

	//used by the correctness worker:
	protected void checkCompoundProduction(String subword, int subwordIndex, Production production) throws IllegalArgumentException{}

	//used by the minimal pairs worker:
	public boolean isConsonant(char chr){
		return true;
	}

	//used by the minimal pairs worker:
	public boolean shouldBeProcessedForMinimalPair(Production production){
		return true;
	}

}
