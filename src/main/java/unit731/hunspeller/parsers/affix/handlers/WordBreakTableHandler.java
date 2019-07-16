package unit731.hunspeller.parsers.affix.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.enums.AffixTag;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.ParserHelper;


public class WordBreakTableHandler implements Handler{

	private static final String DOUBLE_MINUS_SIGN = HyphenationParser.MINUS_SIGN + HyphenationParser.MINUS_SIGN;


	@Override
	public void parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixTag, List<String>> getData){
		try{
			final BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context + "\": The first parameter is not a number");
			final int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be a positive integer");

			final Set<String> wordBreakCharacters = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				final String line = ParserHelper.extractLine(br);

				final String[] lineParts = StringUtils.split(line);
				final AffixTag tag = AffixTag.createFromCode(lineParts[0]);
				if(tag != AffixTag.WORD_BREAK_CHARACTERS)
					throw new IllegalArgumentException("Error reading line '" + line + "' at row " + i + ": mismatched type (expected "
						+ AffixTag.WORD_BREAK_CHARACTERS + ")");

				String breakCharacter = lineParts[1];
				if(DOUBLE_MINUS_SIGN.equals(breakCharacter))
					breakCharacter = HyphenationParser.EN_DASH;

				if(StringUtils.isBlank(breakCharacter))
					throw new IllegalArgumentException("Error reading line '" + line + "' at row " + i + ": break character cannot be empty");

				final boolean inserted = wordBreakCharacters.add(breakCharacter);
				if(!inserted)
					throw new IllegalArgumentException("Error reading line '" + line + "' at row " + i + ": duplicated line");
			}

			addData.accept(AffixTag.WORD_BREAK_CHARACTERS.getCode(), wordBreakCharacters);
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

}
