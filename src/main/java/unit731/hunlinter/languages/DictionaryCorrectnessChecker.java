package unit731.hunlinter.languages;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Set;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.parsers.hyphenation.HyphenatorInterface;
import unit731.hunlinter.services.eventbus.EventBusService;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.workers.exceptions.LinterWarning;


public class DictionaryCorrectnessChecker{

	private static final MessageFormat INVALID_CIRCUMFIX_FLAG = new MessageFormat("{0} cannot have a circumfix flag ({1})");
	private static final MessageFormat NON_AFFIX_ENTRY_CONTAINS_FORBID_COMPOUND_FLAG = new MessageFormat("Non-affix entry contains {0}");
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

	public void checkCircumfix(final DictionaryEntry dicEntry){
		final String circumfixFlag = affixData.getCircumfixFlag();
		if(circumfixFlag != null && dicEntry.hasContinuationFlag(circumfixFlag))
			throw new LinterException(INVALID_CIRCUMFIX_FLAG.format(new Object[]{dicEntry.getWord(), circumfixFlag}));
	}

	//used by the correctness worker after calling {@link #loadRules()}:
	public void checkInflection(final Inflection inflection, final int index){
		final String forbidCompoundFlag = affixData.getForbidCompoundFlag();
		if(forbidCompoundFlag != null && !inflection.hasInflectionRules() && inflection.hasContinuationFlag(forbidCompoundFlag))
			throw new LinterException(NON_AFFIX_ENTRY_CONTAINS_FORBID_COMPOUND_FLAG.format(new Object[]{
				AffixOption.FORBID_COMPOUND_FLAG.getCode()}));

		if(rulesLoader.isMorphologicalFieldsCheck())
			morphologicalFieldCheck(inflection, index);

		incompatibilityCheck(inflection);
	}

	private void morphologicalFieldCheck(final Inflection inflection, final int index){
		if(!inflection.hasMorphologicalFields())
			EventBusService.publish(new LinterWarning(NO_MORPHOLOGICAL_FIELD.format(new Object[]{inflection.getWord()}), IndexDataPair.of(index, null)));

		inflection.forEachMorphologicalField(morphologicalField -> {
			if(morphologicalField.length() < 4)
				EventBusService.publish(new LinterWarning(INVALID_MORPHOLOGICAL_FIELD_PREFIX.format(new Object[]{inflection.getWord(), morphologicalField}),
					IndexDataPair.of(index, null)));

			final MorphologicalTag key = MorphologicalTag.createFromCode(morphologicalField);
			if(!rulesLoader.containsDataField(key))
				EventBusService.publish(new LinterWarning(UNKNOWN_MORPHOLOGICAL_FIELD_PREFIX.format(new Object[]{inflection.getWord(), morphologicalField}),
					IndexDataPair.of(index, null)));
			final Set<String> morphologicalFieldTypes = rulesLoader.getDataField(key);
			if(morphologicalFieldTypes != null && !morphologicalFieldTypes.contains(morphologicalField))
				EventBusService.publish(new LinterWarning(UNKNOWN_MORPHOLOGICAL_FIELD_VALUE.format(new Object[]{inflection.getWord(), morphologicalField}),
					IndexDataPair.of(index, null)));
		});
	}

	private void incompatibilityCheck(final Inflection inflection){
		rulesLoader.letterToFlagIncompatibilityCheck(inflection);

		rulesLoader.flagToFlagIncompatibilityCheck(inflection);
	}

	//used by the correctness worker:
	protected void checkCompoundInflection(final String subword, final int subwordIndex, final Inflection inflection){}

	//used by the minimal pairs worker:
	public boolean isConsonant(final char chr){
		return true;
	}

	//used by the minimal pairs worker:
	public boolean shouldBeProcessedForMinimalPair(final Inflection inflection){
		return true;
	}

}
