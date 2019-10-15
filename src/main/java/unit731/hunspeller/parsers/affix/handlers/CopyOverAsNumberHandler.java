package unit731.hunspeller.parsers.affix.handlers;

import java.text.MessageFormat;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.enums.AffixOption;
import unit731.hunspeller.parsers.affix.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;


public class CopyOverAsNumberHandler implements Handler{

	private static final MessageFormat BAD_FIRST_PARAMETER = new MessageFormat("Error reading line ''{0}'': The first parameter is not a number");


	@Override
	public void parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixOption, List<String>> getData){
		if(!NumberUtils.isCreatable(context.getFirstParameter()))
			throw new IllegalArgumentException(BAD_FIRST_PARAMETER.format(new Object[]{context}));

		addData.accept(context.getRuleType(), Integer.parseInt(context.getAllButFirstParameter()));
	}
	
}
