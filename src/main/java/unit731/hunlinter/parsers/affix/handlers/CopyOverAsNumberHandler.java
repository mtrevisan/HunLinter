package unit731.hunlinter.parsers.affix.handlers;

import java.text.MessageFormat;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.affix.ParsingContext;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.workers.exceptions.LinterException;


public class CopyOverAsNumberHandler implements Handler{

	private static final MessageFormat BAD_FIRST_PARAMETER = new MessageFormat("Error reading line ''{0}'': The first parameter is not a number");


	@Override
	public int parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixOption, List<String>> getData){
		if(!NumberUtils.isCreatable(context.getFirstParameter()))
			throw new LinterException(BAD_FIRST_PARAMETER.format(new Object[]{context}));

		addData.accept(context.getRuleType(), Integer.parseInt(context.getAllButFirstParameter()));

		return Integer.parseInt(context.getFirstParameter());
	}

}
