/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.languages;

import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.parsers.enums.MorphologicalTag;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenatorInterface;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.eventbus.EventBusService;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterWarning;

import java.util.Objects;
import java.util.Set;


public class DictionaryCorrectnessChecker{

	private static final String INVALID_CIRCUMFIX_FLAG = "{} cannot have a circumfix flag ({})";
	private static final String NON_AFFIX_ENTRY_CONTAINS_FORBID_COMPOUND_FLAG = "Non-affix entry contains {}";
	private static final String NO_MORPHOLOGICAL_FIELD = "{} doesn't have any morphological fields";
	private static final String INVALID_MORPHOLOGICAL_FIELD_PREFIX = "{} has an invalid morphological field prefix: {}";
	private static final String UNKNOWN_MORPHOLOGICAL_FIELD_PREFIX = "{} has an unknown morphological field prefix: {}";
	private static final String UNKNOWN_MORPHOLOGICAL_FIELD_VALUE = "{} has an unknown morphological field value: {}";


	protected final AffixData affixData;
	protected final HyphenatorInterface hyphenator;

	protected RulesLoader rulesLoader;


	public DictionaryCorrectnessChecker(final AffixData affixData, final HyphenatorInterface hyphenator){
		Objects.requireNonNull(affixData, "Affix data cannot be null");

		this.affixData = affixData;
		this.hyphenator = hyphenator;
	}

	@SuppressWarnings("DesignForExtension")
	public void loadRules(){
		rulesLoader = new RulesLoader(affixData.getLanguage(), affixData.getFlagParsingStrategy());
	}

	public final void checkCircumfix(final DictionaryEntry dicEntry){
		final String circumfixFlag = affixData.getCircumfixFlag();
		if(circumfixFlag != null && dicEntry.hasContinuationFlag(circumfixFlag))
			throw new LinterException(INVALID_CIRCUMFIX_FLAG, dicEntry.getWord(), circumfixFlag);
	}

	/** Used by the correctness check worker after calling {@link #loadRules()}. */
	@SuppressWarnings("DesignForExtension")
	public void checkInflection(final Inflection inflection, final int index){
		final String forbidCompoundFlag = affixData.getForbidCompoundFlag();
		if(forbidCompoundFlag != null && !inflection.hasInflectionRules() && inflection.hasContinuationFlag(forbidCompoundFlag))
			throw new LinterException(NON_AFFIX_ENTRY_CONTAINS_FORBID_COMPOUND_FLAG, AffixOption.FORBID_COMPOUND_FLAG.getCode());

		if(rulesLoader.isMorphologicalFieldsCheck())
			morphologicalFieldCheck(inflection, index);

		incompatibilityCheck(inflection);
	}

	private void morphologicalFieldCheck(final Inflection inflection, final int index){
		if(!inflection.hasMorphologicalFields())
			EventBusService.publish(new LinterWarning(NO_MORPHOLOGICAL_FIELD, inflection.getWord())
				.withIndexDataPair(IndexDataPair.of(index, null)));

		final String[] morphologicalFields = inflection.getMorphologicalFieldsAsArray();
		final int size = (morphologicalFields != null? morphologicalFields.length: 0);
		for(int i = 0; i < size; i ++){
			final String morphologicalField = morphologicalFields[i];
			if(morphologicalField.length() < 4)
				EventBusService.publish(new LinterWarning(INVALID_MORPHOLOGICAL_FIELD_PREFIX, inflection.getWord(), morphologicalField)
					.withIndexDataPair(IndexDataPair.of(index, null)));

			final MorphologicalTag key = MorphologicalTag.createFromCode(morphologicalField);
			if(!rulesLoader.containsDataField(key))
				EventBusService.publish(new LinterWarning(UNKNOWN_MORPHOLOGICAL_FIELD_PREFIX, inflection.getWord(), morphologicalField)
					.withIndexDataPair(IndexDataPair.of(index, null)));
			final Set<String> morphologicalFieldTypes = rulesLoader.getDataField(key);
			if(morphologicalFieldTypes != null && !morphologicalFieldTypes.contains(morphologicalField))
				EventBusService.publish(new LinterWarning(UNKNOWN_MORPHOLOGICAL_FIELD_VALUE, inflection.getWord(), morphologicalField)
					.withIndexDataPair(IndexDataPair.of(index, null)));
		}
	}

	private void incompatibilityCheck(final Inflection inflection){
		rulesLoader.letterToFlagIncompatibilityCheck(inflection);

		rulesLoader.flagToFlagIncompatibilityCheck(inflection);
	}

	//used by the correctness check worker:
	protected void checkCompoundInflection(final String subword, final int subwordIndex, final Inflection inflection){}

	@SuppressWarnings("DesignForExtension")
	public boolean shouldNotCheckProductiveness(final String flag){
		return false;
	}

	//used by the minimal pairs' worker:
	@SuppressWarnings("DesignForExtension")
	public boolean isConsonant(final char chr){
		return true;
	}

	//used by the minimal pairs' worker:
	@SuppressWarnings("DesignForExtension")
	public boolean shouldBeProcessedForMinimalPair(final Inflection inflection){
		return true;
	}

}
