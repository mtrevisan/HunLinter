package unit731.hunlinter.parsers.affix.handlers;

import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.ParsingContext;
import unit731.hunlinter.parsers.affix.RelationTable;
import unit731.hunlinter.parsers.enums.AffixOption;


public class RelationTableHandler implements Handler{

	private final AffixOption affixOption;


	public RelationTableHandler(final AffixOption affixOption){
		this.affixOption = affixOption;
	}

	@Override
	public int parse(final ParsingContext context, final AffixData affixData){
		final RelationTable table = new RelationTable(affixOption);
		table.parse(context);

		affixData.addData(affixOption.getCode(), table);

		return Integer.parseInt(context.getFirstParameter());
	}

}
