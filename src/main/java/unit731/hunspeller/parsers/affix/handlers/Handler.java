package unit731.hunspeller.parsers.affix.handlers;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import unit731.hunspeller.parsers.enums.AffixTag;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;


public interface Handler{
	
	void parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
		final Function<AffixTag, List<String>> getData);
	
}
