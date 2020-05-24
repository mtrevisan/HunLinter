package unit731.hunlinter.parsers.affix.handlers;

import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.affix.ConversionTable;
import unit731.hunlinter.parsers.affix.ParsingContext;


public class ConversionTableHandler implements Handler{

	private final AffixOption affixOption;


	public ConversionTableHandler(final AffixOption affixOption){
		this.affixOption = affixOption;
	}

	@Override
	public int parse(final ParsingContext context, final AffixData affixData){
		final ConversionTable table = new ConversionTable(affixOption);
		table.parse(context);

		affixData.addData(affixOption.getCode(), table);

		return Integer.parseInt(context.getFirstParameter());
	}

}
