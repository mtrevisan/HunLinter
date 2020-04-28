package unit731.hunlinter.parsers.affix.handlers;

import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.ParsingContext;


public class CopyOverHandler implements Handler{

	@Override
	public int parse(final ParsingContext context, final AffixData affixData){
		affixData.addData(context.getRuleType(), context.getAllButFirstParameter());

		return 0;
	}

}
