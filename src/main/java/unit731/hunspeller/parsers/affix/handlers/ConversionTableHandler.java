package unit731.hunspeller.parsers.affix.handlers;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.ConversionTable;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;


public class ConversionTableHandler implements Handler{

	private final AffixTag affixTag;


	public ConversionTableHandler(AffixTag affixTag){
		this.affixTag = affixTag;
	}

	@Override
	public void parse(ParsingContext context, FlagParsingStrategy strategy, BiConsumer<String, Object> addData,
			Function<AffixTag, List<String>> getData){
		ConversionTable table = new ConversionTable(affixTag);
		table.parseConversionTable(context);

		addData.accept(affixTag.getCode(), table);
	}
	
}
