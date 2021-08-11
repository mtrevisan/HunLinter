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
package io.github.mtrevisan.hunlinter.parsers.affix.handlers;

import io.github.mtrevisan.hunlinter.datastructures.FixedArray;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.ParsingContext;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.services.eventbus.EventBusService;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterWarning;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Scanner;


public class AffixHandler implements Handler{

	private static final MessageFormat BAD_THIRD_PARAMETER = new MessageFormat("Error reading line `{0}`: the third parameter is not a number");
	private static final MessageFormat BAD_NUMBER_OF_ENTRIES = new MessageFormat("Error reading line `{0}`: bad number of entries, `{1}` must be a positive integer");
	private static final MessageFormat DUPLICATED_LINE = new MessageFormat("Duplicated line: {0}");
	private static final MessageFormat MISMATCHED_RULE_TYPE = new MessageFormat("Mismatched rule type (expected `{0}`)");
	private static final MessageFormat MISMATCHED_RULE_FLAG = new MessageFormat("Mismatched rule flag (expected `{0}`)");


	@Override
	public int parse(final ParsingContext context, final AffixData affixData){
		try{
			final AffixType parentType = AffixType.createFromCode(context.getRuleType());
			final String ruleFlag = context.getFirstParameter();
			final char combinable = context.getSecondParameter().charAt(0);
			if(!NumberUtils.isCreatable(context.getThirdParameter()))
				throw new LinterException(BAD_THIRD_PARAMETER.format(new Object[]{context}));

			final RuleEntry parent = new RuleEntry(parentType, ruleFlag, combinable);
			final AffixEntry[] entries = readEntries(context, parent, affixData);
			parent.setEntries(entries);

			affixData.addData(ruleFlag, parent);

			return Integer.parseInt(context.getThirdParameter());
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private AffixEntry[] readEntries(final ParsingContext context, final RuleEntry parent, final AffixData affixData)
			throws IOException{
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();

		final int numEntries = Integer.parseInt(context.getThirdParameter());
		if(numEntries <= 0)
			throw new LinterException(BAD_NUMBER_OF_ENTRIES.format(new Object[]{context, context.getThirdParameter()}));

		final Scanner scanner = context.getScanner();
		final AffixType parentType = AffixType.createFromCode(context.getRuleType());
		final String parentFlag = context.getFirstParameter();

		//List<AffixEntry> prefixEntries = new ArrayList<>();
		//List<AffixEntry> suffixEntries = new ArrayList<>();
		final List<String> aliasesFlag = affixData.getData(AffixOption.ALIASES_FLAG);
		final List<String> aliasesMorphologicalField = affixData.getData(AffixOption.ALIASES_MORPHOLOGICAL_FIELD);
		String line;
		final FixedArray<AffixEntry> entries = new FixedArray<>(AffixEntry.class, numEntries);
		for(int i = 0; i < numEntries; i ++){
			ParserHelper.assertNotEOF(scanner);

			line = scanner.nextLine();
			final AffixEntry entry = new AffixEntry(line, context.getIndex() + i, parentType, parentFlag, strategy, aliasesFlag,
				aliasesMorphologicalField);
			entry.setParent(parent);
//com.carrotsearch.sizeof.RamUsageEstimator.sizeOf(entry)


			checkValidity(parentType, parentFlag, context, entry);


			if(ArrayUtils.contains(entries.data, entry))
				EventBusService.publish(new LinterWarning(DUPLICATED_LINE.format(new Object[]{entry.toString()}),
					IndexDataPair.of(context.getIndex() + i, null)));
			else
				entries.add(entry);

//String regexToMatch = (entry.getMatch() != null? entry.getMatch().pattern().pattern().replaceFirst("^\\^", StringUtils.EMPTY).replaceFirst("\\$$", StringUtils.EMPTY): ".");
//String[] arr = RegExpTrieSequencer.extractCharacters(regexToMatch);
//List<AffixEntry> lst = new ArrayList<>();
//lst.add(entry);
//if(entry.isSuffix()){
//	ArrayUtils.reverse(arr);
//	suffixEntries.add(arr, lst);
//}
//else
//	prefixEntries.put(arr, lst);
		}
		return entries.extractCopyOrNull();
	}

	private void checkValidity(final AffixType ruleType, final String ruleFlag, final ParsingContext context, final AffixEntry entry){
		final String ruleTypeCode = ruleType.getOption().getCode();
		if(!context.getRuleType().equals(ruleTypeCode))
			throw new LinterException(MISMATCHED_RULE_TYPE.format(new Object[]{ruleType}));
		if(!context.getFirstParameter().equals(ruleFlag))
			throw new LinterException(MISMATCHED_RULE_FLAG.format(new Object[]{ruleFlag}));

		entry.validate();
	}

}
