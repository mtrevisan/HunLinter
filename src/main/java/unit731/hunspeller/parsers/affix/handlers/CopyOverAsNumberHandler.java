package unit731.hunspeller.parsers.affix.handlers;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;


public class CopyOverAsNumberHandler implements Handler{

	@Override
	public void parse(ParsingContext context, FlagParsingStrategy strategy, BiConsumer<String, Object> addData,
			Function<AffixTag, List<String>> getData){
		if(!NumberUtils.isCreatable(context.getFirstParameter()))
			throw new IllegalArgumentException("Error reading line \"" + context + "\": The first parameter is not a number");

		addData.accept(context.getRuleType(), Integer.parseInt(context.getAllButFirstParameter()));
	}
	
}
