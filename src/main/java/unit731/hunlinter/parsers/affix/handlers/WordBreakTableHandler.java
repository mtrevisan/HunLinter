package unit731.hunlinter.parsers.affix.handlers;

import java.io.EOFException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.affix.ParsingContext;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.services.ParserHelper;
import unit731.hunlinter.workers.exceptions.LinterException;


public class WordBreakTableHandler implements Handler{

	private static final MessageFormat BAD_FIRST_PARAMETER = new MessageFormat("Error reading line ''{0}'': the first parameter is not a number");
	private static final MessageFormat BAD_NUMBER_OF_ENTRIES = new MessageFormat("Error reading line ''{0}'': bad number of entries, ''{1}'' must be a positive integer");
	private static final MessageFormat MISMATCHED_TYPE = new MessageFormat("Error reading line ''{0}'': mismatched type (expected {1})");
	private static final MessageFormat EMPTY_BREAK_CHARACTER = new MessageFormat("Error reading line ''{0}'': break character cannot be empty");
	private static final MessageFormat DUPLICATED_LINE = new MessageFormat("Error reading line ''{0}'': duplicated line");

	private static final String DOUBLE_MINUS_SIGN = HyphenationParser.MINUS_SIGN + HyphenationParser.MINUS_SIGN;


	@Override
	public int parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixOption, List<String>> getData){
		try{
			final Scanner scanner = context.getScanner();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new LinterException(BAD_FIRST_PARAMETER.format(new Object[]{context}));
			final int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new LinterException(BAD_NUMBER_OF_ENTRIES.format(new Object[]{context, context.getFirstParameter()}));

			final Set<String> wordBreakCharacters = readCharacters(scanner, numEntries);

			addData.accept(AffixOption.WORD_BREAK_CHARACTERS.getCode(), wordBreakCharacters);

			return numEntries;
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private Set<String> readCharacters(final Scanner scanner, final int numEntries) throws EOFException{
		final Set<String> wordBreakCharacters = new HashSet<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			ParserHelper.assertNotEOF(scanner);

			final String line = scanner.nextLine();
			final String[] lineParts = StringUtils.split(line);

			final AffixOption option = AffixOption.createFromCode(lineParts[0]);
			if(option != AffixOption.WORD_BREAK_CHARACTERS)
				throw new LinterException(MISMATCHED_TYPE.format(new Object[]{line, AffixOption.WORD_BREAK_CHARACTERS}));

			final String breakCharacter = (DOUBLE_MINUS_SIGN.equals(lineParts[1])? HyphenationParser.EN_DASH: lineParts[1]);
			if(StringUtils.isBlank(breakCharacter))
				throw new LinterException(EMPTY_BREAK_CHARACTER.format(new Object[]{line}));

			final boolean inserted = wordBreakCharacters.add(breakCharacter);
			if(!inserted)
				throw new LinterException(DUPLICATED_LINE.format(new Object[]{line}));
		}
		return wordBreakCharacters;
	}

}
