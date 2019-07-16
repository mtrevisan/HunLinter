package unit731.hunspeller.parsers.affix.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.enums.AffixTag;
import unit731.hunspeller.parsers.affix.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.enums.AffixType;
import unit731.hunspeller.parsers.vos.RuleEntry;
import unit731.hunspeller.parsers.vos.AffixEntry;
import unit731.hunspeller.services.ParserHelper;


public class AffixHandler implements Handler{

	@Override
	public void parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixTag, List<String>> getData){
		try{
			final boolean isSuffix = AffixType.SUFFIX.is(context.getRuleType());
			final String ruleFlag = context.getFirstParameter();
			final char combinable = context.getSecondParameter().charAt(0);
			if(!NumberUtils.isCreatable(context.getThirdParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context + "\": The third parameter is not a number");

			final List<AffixEntry> entries = readEntries(context, strategy, getData);

			addData.accept(ruleFlag, new RuleEntry(isSuffix, combinable, entries));
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private List<AffixEntry> readEntries(final ParsingContext context, final FlagParsingStrategy strategy,
			final Function<AffixTag, List<String>> getData) throws IOException, IllegalArgumentException{
		final int numEntries = Integer.parseInt(context.getThirdParameter());
		if(numEntries <= 0)
			throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be a positive integer");

		final BufferedReader br = context.getReader();
		final AffixType ruleType = AffixType.createFromCode(context.getRuleType());
		final String ruleFlag = context.getFirstParameter();

		//List<AffixEntry> prefixEntries = new ArrayList<>();
		//List<AffixEntry> suffixEntries = new ArrayList<>();
		final List<String> aliasesFlag = getData.apply(AffixTag.ALIASES_FLAG);
		final List<String> aliasesMorphologicalField = getData.apply(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);
		String line;
		final List<AffixEntry> entries = new ArrayList<>(numEntries);
		try{
			for(int i = 0; i < numEntries; i ++){
				line = ParserHelper.extractLine(br);

				final AffixEntry entry = new AffixEntry(line, strategy, aliasesFlag, aliasesMorphologicalField);

				checkValidity(entry, ruleType, ruleFlag);

				if(entries.contains(entry))
					throw new IllegalArgumentException("duplicated line");

				final boolean inserted = entries.add(entry);

//String regexToMatch = (entry.getMatch() != null? entry.getMatch().pattern().pattern().replaceFirst("^\\^", StringUtils.EMPTY).replaceFirst("\\$$", StringUtils.EMPTY): ".");
//String[] arr = RegExpTrieSequencer.extractCharacters(regexToMatch);
//List<AffixEntry> lst = new ArrayList<>();
//lst.add(entry);
//if(entry.isSuffix()){
//	ArrayUtils.reverse(arr);
//	suffixEntries.add(arr, lst);
//}
//else
//	prefixEntries.put(arr, lst);
			}
		}
		catch(final IllegalArgumentException e){
			throw new IllegalArgumentException("Reading error: " + e.getMessage());
		}
		return entries;
	}

	private void checkValidity(final AffixEntry entry, final AffixType ruleType, final String ruleFlag) throws IllegalArgumentException{
		if(entry.getType() != ruleType)
			throw new IllegalArgumentException("mismatched rule type (expected " + ruleType + ")");
		if(!ruleFlag.equals(entry.getFlag()))
			throw new IllegalArgumentException("mismatched rule flag (expected " + ruleFlag + ")");
	}
	
}
