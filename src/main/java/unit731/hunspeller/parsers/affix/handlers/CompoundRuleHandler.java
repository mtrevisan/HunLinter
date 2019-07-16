package unit731.hunspeller.parsers.affix.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.enums.AffixTag;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ParserHelper;


public class CompoundRuleHandler implements Handler{

	@Override
	public void parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixTag, List<String>> getData){
		try{
			checkValidity(context);

			final BufferedReader br = context.getReader();

			final int numEntries = Integer.parseInt(context.getFirstParameter());
			final Set<String> compoundRules = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				final String line = ParserHelper.extractLine(br);

				final String[] lineParts = StringUtils.split(line);

				final AffixTag tag = AffixTag.createFromCode(lineParts[0]);
				if(tag != AffixTag.COMPOUND_RULE)
					throw new IllegalArgumentException("Error reading line '" + line + "' at row " + i
						+ ": mismatched compound rule type (expected " + AffixTag.COMPOUND_RULE + ")");

				final String rule = lineParts[1];

				checkRuleValidity(rule, line, i, strategy);

				final boolean inserted = compoundRules.add(rule);
				if(!inserted)
					throw new IllegalArgumentException("Error reading line '" + line + "' at row " + i
						+ ": duplicated line");
			}

			addData.accept(AffixTag.COMPOUND_RULE.getCode(), compoundRules);
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private void checkValidity(final ParsingContext context) throws IllegalArgumentException{
		if(!NumberUtils.isCreatable(context.getFirstParameter()))
			throw new IllegalArgumentException("Error reading line '" + context + "': The first parameter is not a number");
		final int numEntries = Integer.parseInt(context.getFirstParameter());
		if(numEntries <= 0)
			throw new IllegalArgumentException("Error reading line '" + context + "': Bad number of entries, it must be a positive integer");
	}

	private void checkRuleValidity(final String rule, final String line, final int i, final FlagParsingStrategy strategy)
			throws IllegalArgumentException{
		if(StringUtils.isBlank(rule))
			throw new IllegalArgumentException("Error reading line '" + line + "' at row " + i
				+ ": compound rule type cannot be empty");
		final String[] compounds = strategy.extractCompoundRule(rule);
		if(compounds.length == 0)
			throw new IllegalArgumentException("Error reading line '" + line + "' at row " + i
				+ ": compound rule is bad formatted");
	}
	
}
