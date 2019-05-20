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


	public ConversionTableHandler(final AffixTag affixTag){
		this.affixTag = affixTag;
	}

	@Override
	public void parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixTag, List<String>> getData){
		final ConversionTable table = new ConversionTable(affixTag);
		table.parseConversionTable(context);

		addData.accept(affixTag.getCode(), table);
	}
	
}
