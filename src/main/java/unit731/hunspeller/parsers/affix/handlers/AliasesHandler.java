package unit731.hunspeller.parsers.affix.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.enums.AffixOption;
import unit731.hunspeller.parsers.affix.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ParserHelper;


public class AliasesHandler implements Handler{

	private static final MessageFormat BAD_FIRST_PARAMETER = new MessageFormat("Error reading line ''{0}'': The first parameter is not a number");
	private static final MessageFormat BAD_NUMBER_OF_ENTRIES = new MessageFormat("Error reading line ''{0}'': Bad number of entries, ''{1}'' must be a positive integer");
	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Error reading line ''{0}'': Bad number of entries, it must be '<option> <flag/morphological field>'");
	private static final MessageFormat BAD_OPTION = new MessageFormat("Error reading line ''{0}'': Bad option, it must be {1}");


	@Override
	public void parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixOption, List<String>> getData){
		try{
			final BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException(BAD_FIRST_PARAMETER.format(new Object[]{context}));
			final int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException(BAD_NUMBER_OF_ENTRIES.format(new Object[]{context, context.getFirstParameter()}));

			final List<String> aliases = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				final String line = ParserHelper.extractLine(br);

				final String[] parts = StringUtils.split(line);

				checkValidity(parts, context);

				aliases.add(parts[1]);
			}

			addData.accept(context.getRuleType(), aliases);
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private void checkValidity(final String[] parts, final ParsingContext context) throws IllegalArgumentException{
		if(parts.length != 2)
			throw new IllegalArgumentException(WRONG_FORMAT.format(new Object[]{context}));
		if(!context.getRuleType().equals(parts[0]))
			throw new IllegalArgumentException(BAD_OPTION.format(new Object[]{context, context.getRuleType()}));
	}

}
