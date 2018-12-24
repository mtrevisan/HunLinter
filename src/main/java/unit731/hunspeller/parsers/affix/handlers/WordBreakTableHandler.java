package unit731.hunspeller.parsers.affix.handlers;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;


public class WordBreakTableHandler implements Handler{

	private static final String DOUBLE_MINUS_SIGN = HyphenationParser.MINUS_SIGN + HyphenationParser.MINUS_SIGN;


	@Override
	public void parse(ParsingContext context, FlagParsingStrategy strategy, BiConsumer<String, Object> addData,
			Function<AffixTag, List<String>> getData){
		try{
			BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context + "\": The first parameter is not a number");
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be a positive integer");

			Set<String> wordBreakCharacters = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				if(line == null)
					throw new EOFException("Unexpected EOF while reading Dictionary file");

				line = DictionaryParser.cleanLine(line);

				String[] lineParts = StringUtils.split(line);
				AffixTag tag = AffixTag.createFromCode(lineParts[0]);
				if(tag != AffixTag.BREAK)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": mismatched type (expected "
						+ AffixTag.BREAK + ")");
				String breakCharacter = lineParts[1];
				if(StringUtils.isBlank(breakCharacter))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": break character cannot be empty");
				if(DOUBLE_MINUS_SIGN.equals(breakCharacter))
					breakCharacter = HyphenationParser.EN_DASH;

				boolean inserted = wordBreakCharacters.add(breakCharacter);
				if(!inserted)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": duplicated line");
			}

			addData.accept(AffixTag.BREAK.getCode(), wordBreakCharacters);
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}
	
}
