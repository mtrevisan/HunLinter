package unit731.hunlinter.parsers.affix.handlers;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.affix.ConversionTable;
import unit731.hunlinter.parsers.affix.ParsingContext;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;


public class ConversionTableHandler implements Handler{

	private final AffixOption affixOption;


	public ConversionTableHandler(final AffixOption affixOption){
		this.affixOption = affixOption;
	}

	@Override
	public void parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixOption, List<String>> getData){
		final ConversionTable table = new ConversionTable(affixOption);
		table.parseConversionTable(context);

		addData.accept(affixOption.getCode(), table);
	}

}
