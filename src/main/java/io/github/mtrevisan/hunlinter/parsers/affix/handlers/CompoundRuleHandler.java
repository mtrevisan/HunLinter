/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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

import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.ParsingContext;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.services.eventbus.EventBusService;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterWarning;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.EOFException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;


public class CompoundRuleHandler implements Handler{

	private static final String COMPOUND_RULE_EXPECTED = "Expected a compound rule entry, found something else{} in parent flag `{}`; counter should be {}";
	private static final String MISMATCHED_COMPOUND_RULE_TYPE = "Error reading line `{}`: mismatched compound rule type (expected {})";
	private static final String DUPLICATED_LINE = "Error reading line `{}`: duplicated line";
	private static final String BAD_FIRST_PARAMETER = "Error reading line `{}`: the first parameter is not a number";
	private static final String BAD_NUMBER_OF_ENTRIES = "Error reading line `{}`: bad number of entries, `{}` must be a positive integer less or equal than " + Short.MAX_VALUE;
	private static final String EMPTY_COMPOUND_RULE_TYPE = "Error reading line `{}`: compound rule type cannot be empty";
	private static final String BAD_FORMAT = "Error reading line `{}`: compound rule is bad formatted";


	@Override
	public final int parse(final ParsingContext context, final AffixData affixData) throws EOFException{
		final FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();

		final int numEntries = checkValidity(context);
		final Scanner scanner = context.getScanner();
		final Set<String> compoundRules = new HashSet<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			ParserHelper.assertNotEOF(scanner);

			final String line = scanner.nextLine();
			if(line == null || !line.startsWith(AffixOption.COMPOUND_RULE.getCode()))
				throw new LinterException(COMPOUND_RULE_EXPECTED, (line != null && !line.isEmpty()? ": `" + line + "`": StringUtils.EMPTY),
					AffixOption.COMPOUND_RULE.getCode(), i);

			final String[] lineParts = StringUtils.split(line);

			final AffixOption option = AffixOption.createFromCode(lineParts[0]);
			if(option != AffixOption.COMPOUND_RULE)
				throw new LinterException(MISMATCHED_COMPOUND_RULE_TYPE, line, AffixOption.COMPOUND_RULE);

			final String rule = lineParts[1];

			checkRuleValidity(rule, line, strategy);

			final boolean inserted = compoundRules.add(rule);
			if(!inserted)
				EventBusService.publish(new LinterWarning(DUPLICATED_LINE, line)
					.withIndex(context.getIndex() + i));
		}

		affixData.addData(AffixOption.COMPOUND_RULE.getCode(), compoundRules);

		return numEntries;
	}

	private static int checkValidity(final ParsingContext context){
		if(!NumberUtils.isCreatable(context.getFirstParameter()))
			throw new LinterException(BAD_FIRST_PARAMETER, context);
		final int numEntries = Integer.parseInt(context.getFirstParameter());
		if(numEntries <= 0 || numEntries > Short.MAX_VALUE)
			throw new LinterException(BAD_NUMBER_OF_ENTRIES, context, context.getFirstParameter());

		return numEntries;
	}

	private static void checkRuleValidity(final String rule, final String line, final FlagParsingStrategy strategy){
		if(StringUtils.isBlank(rule))
			throw new LinterException(EMPTY_COMPOUND_RULE_TYPE, line);
		final String[] compounds = strategy.extractCompoundRule(rule);
		if(compounds.length == 0)
			throw new LinterException(BAD_FORMAT, line);
	}

}
