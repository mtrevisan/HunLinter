package unit731.hunspeller.parsers.affix.handlers;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;


public class CopyOverHandler implements Handler{

	@Override
	public void parse(ParsingContext context, FlagParsingStrategy strategy, BiConsumer<String, Object> addData,
			Function<AffixTag, List<String>> getData){
		addData.accept(context.getRuleType(), context.getAllButFirstParameter());
	}
	
}
