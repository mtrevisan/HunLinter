package unit731.hunlinter.parsers.affix.handlers;

import unit731.hunlinter.parsers.affix.ParsingContext;
import unit731.hunlinter.parsers.affix.RelationTable;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.AffixOption;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;


public class RelationTableHandler implements Handler{

	private final AffixOption affixOption;


	public RelationTableHandler(final AffixOption affixOption){
		this.affixOption = affixOption;
	}

	@Override
	public int parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixOption, List<String>> getData){
		final RelationTable table = new RelationTable(affixOption);
		table.parse(context);

		addData.accept(affixOption.getCode(), table);

		return Integer.parseInt(context.getFirstParameter());
	}

}
