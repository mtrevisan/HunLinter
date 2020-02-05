package unit731.hunlinter.parsers.affix.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.affix.ParsingContext;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.workers.exceptions.LinterException;
import unit731.hunlinter.services.ParserHelper;


public class CompoundRuleHandler implements Handler{

	private static final MessageFormat MISMATCHED_COMPOUND_RULE_TYPE = new MessageFormat("Error reading line ''{0}'': mismatched compound rule type (expected {1})");
	private static final MessageFormat DUPLICATED_LINE = new MessageFormat("Error reading line ''{0}'': duplicated line");
	private static final MessageFormat BAD_FIRST_PARAMETER = new MessageFormat("Error reading line ''{0}'': the first parameter is not a number");
	private static final MessageFormat BAD_NUMBER_OF_ENTRIES = new MessageFormat("Error reading line ''{0}'': bad number of entries, ''{1}'' must be a positive integer");
	private static final MessageFormat EMPTY_COMPOUND_RULE_TYPE = new MessageFormat("Error reading line ''{0}'': compound rule type cannot be empty");
	private static final MessageFormat BAD_FORMAT = new MessageFormat("Error reading line ''{0}'': compound rule is bad formatted");


	@Override
	public void parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixOption, List<String>> getData){
		try{
			checkValidity(context);

			final BufferedReader br = context.getReader();

			final int numEntries = Integer.parseInt(context.getFirstParameter());
			final Set<String> compoundRules = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				final String line = ParserHelper.extractLine(br);

				final String[] lineParts = StringUtils.split(line);

				final AffixOption option = AffixOption.createFromCode(lineParts[0]);
				if(option != AffixOption.COMPOUND_RULE)
					throw new LinterException(MISMATCHED_COMPOUND_RULE_TYPE.format(new Object[]{line, AffixOption.COMPOUND_RULE}));

				final String rule = lineParts[1];

				checkRuleValidity(rule, line, strategy);

				final boolean inserted = compoundRules.add(rule);
				if(!inserted)
					throw new LinterException(DUPLICATED_LINE.format(new Object[]{line}));
			}

			addData.accept(AffixOption.COMPOUND_RULE.getCode(), compoundRules);
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private void checkValidity(final ParsingContext context){
		if(!NumberUtils.isCreatable(context.getFirstParameter()))
			throw new LinterException(BAD_FIRST_PARAMETER.format(new Object[]{context}));
		final int numEntries = Integer.parseInt(context.getFirstParameter());
		if(numEntries <= 0)
			throw new LinterException(BAD_NUMBER_OF_ENTRIES.format(new Object[]{context, context.getFirstParameter()}));
	}

	private void checkRuleValidity(final String rule, final String line, final FlagParsingStrategy strategy){
		if(StringUtils.isBlank(rule))
			throw new LinterException(EMPTY_COMPOUND_RULE_TYPE.format(new Object[]{line}));
		final String[] compounds = strategy.extractCompoundRule(rule);
		if(compounds.length == 0)
			throw new LinterException(BAD_FORMAT.format(new Object[]{line}));
	}

}
