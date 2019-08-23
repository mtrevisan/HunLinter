package unit731.hunspeller.parsers.affix.handlers;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import unit731.hunspeller.parsers.enums.AffixOption;
import unit731.hunspeller.parsers.affix.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;


public interface Handler{
	
	void parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
		final Function<AffixOption, List<String>> getData);
	
}
