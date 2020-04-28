package unit731.hunlinter.parsers.affix.handlers;

import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.ParsingContext;


public interface Handler{

	int parse(final ParsingContext context, final AffixData affixData);

}
