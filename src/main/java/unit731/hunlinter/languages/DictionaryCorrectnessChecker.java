package unit731.hunlinter.languages;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Set;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.hyphenation.HyphenatorInterface;
import unit731.hunlinter.workers.exceptions.LinterException;


public class DictionaryCorrectnessChecker{

	private static final MessageFormat NON_AFFIX_ENTRY_CONTAINS_FORBID_COMPOUND_FLAG = new MessageFormat("Non–affix entry contains {0}");
	private static final MessageFormat NO_MORPHOLOGICAL_FIELD = new MessageFormat("{0} doesn''t have any morphological fields");
	private static final MessageFormat INVALID_MORPHOLOGICAL_FIELD_PREFIX = new MessageFormat("{0} has an invalid morphological field prefix: {1}");
	private static final MessageFormat UNKNOWN_MORPHOLOGICAL_FIELD_PREFIX = new MessageFormat("{0} has an unknown morphological field prefix: {1}");
	private static final MessageFormat UNKNOWN_MORPHOLOGICAL_FIELD_VALUE = new MessageFormat("{0} has an unknown morphological field value: {1}");


	protected final AffixData affixData;
	protected final HyphenatorInterface hyphenator;

	protected RulesLoader rulesLoader;


	public DictionaryCorrectnessChecker(final AffixData affixData, final HyphenatorInterface hyphenator){
		Objects.requireNonNull(affixData);

		this.affixData = affixData;
		this.hyphenator = hyphenator;
	}

	public void loadRules(){
		rulesLoader = new RulesLoader(affixData.getLanguage(), affixData.getFlagParsingStrategy());
	}

	//used by the correctness worker after calling {@link #loadRules()}:
	public void checkProduction(final Production production){
		final String forbidCompoundFlag = affixData.getForbidCompoundFlag();
		if(forbidCompoundFlag != null && !production.hasProductionRules() && production.hasContinuationFlag(forbidCompoundFlag))
			throw new LinterException(NON_AFFIX_ENTRY_CONTAINS_FORBID_COMPOUND_FLAG.format(new Object[]{
				AffixOption.FORBID_COMPOUND_FLAG.getCode()}));

		if(rulesLoader.isMorphologicalFieldsCheck())
			morphologicalFieldCheck(production);

		incompatibilityCheck(production);
	}

	private void morphologicalFieldCheck(final Production production){
		if(!production.hasMorphologicalFields())
			throw new LinterException(NO_MORPHOLOGICAL_FIELD.format(new Object[]{production.getWord()}));

		production.forEachMorphologicalField(morphologicalField -> {
			if(morphologicalField.length() < 4)
				throw new LinterException(INVALID_MORPHOLOGICAL_FIELD_PREFIX.format(new Object[]{production.getWord(),
					morphologicalField}));

			final MorphologicalTag key = MorphologicalTag.createFromCode(morphologicalField);
			if(!rulesLoader.containsDataField(key))
				throw new LinterException(UNKNOWN_MORPHOLOGICAL_FIELD_PREFIX.format(new Object[]{production.getWord(),
					morphologicalField}));
			final Set<String> morphologicalFieldTypes = rulesLoader.getDataField(key);
			if(morphologicalFieldTypes != null && !morphologicalFieldTypes.contains(morphologicalField))
				throw new LinterException(UNKNOWN_MORPHOLOGICAL_FIELD_VALUE.format(new Object[]{production.getWord(),
					morphologicalField}));
		});
	}

	private void incompatibilityCheck(final Production production){
		rulesLoader.letterToFlagIncompatibilityCheck(production);

		rulesLoader.flagToFlagIncompatibilityCheck(production);
	}

	//used by the correctness worker:
	protected void checkCompoundProduction(final String subword, final int subwordIndex, final Production production){}

	//used by the minimal pairs worker:
	public boolean isConsonant(final char chr){
		return true;
	}

	//used by the minimal pairs worker:
	public boolean shouldBeProcessedForMinimalPair(final Production production){
		return true;
	}

}
