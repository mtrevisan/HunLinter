package unit731.hunlinter.parsers.affix.handlers;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.affix.ParsingContext;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;


public interface Handler{

	int parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
		final Function<AffixOption, List<String>> getData);

}
