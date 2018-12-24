package unit731.hunspeller.parsers.affix.handlers;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;


public class AffixHandler implements Handler{

	@Override
	public void parse(ParsingContext context, FlagParsingStrategy strategy, BiConsumer<String, Object> addData,
			Function<AffixTag, List<String>> getData){
		try{
			AffixEntry.Type ruleType = AffixEntry.Type.createFromCode(context.getRuleType());
			BufferedReader br = context.getReader();
			boolean isSuffix = AffixEntry.Type.SUFFIX.is(context.getRuleType());
			String ruleFlag = context.getFirstParameter();
			char combineable = context.getSecondParameter().charAt(0);
			if(!NumberUtils.isCreatable(context.getThirdParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context + "\": The third parameter is not a number");
			int numEntries = Integer.parseInt(context.getThirdParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be a positive integer");

//List<AffixEntry> prefixEntries = new ArrayList<>();
//List<AffixEntry> suffixEntries = new ArrayList<>();
			List<String> aliasesFlag = getData.apply(AffixTag.ALIASES_FLAG);
			List<String> aliasesMorphologicalField = getData.apply(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);
			List<AffixEntry> entries = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				if(line == null)
					throw new EOFException("Unexpected EOF while reading Dictionary file");

				line = DictionaryParser.cleanLine(line);

				AffixEntry entry = new AffixEntry(line, strategy, aliasesFlag, aliasesMorphologicalField);
				if(entry.getType() != ruleType)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": mismatched rule type (expected "
						+ ruleType + ")");
				if(!ruleFlag.equals(entry.getFlag()))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": mismatched rule flag (expected "
						+ ruleFlag + ")");
				if(!entry.containsUniqueContinuationFlags())
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": multiple rule flags");

				boolean inserted = entries.add(entry);
				if(!inserted)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": duplicated line");

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

			addData.accept(ruleFlag, new RuleEntry(isSuffix, combineable, entries));
//addData(ruleFlag, new RuleEntry(isSuffix, combineable, entries, prefixEntries, suffixEntries));
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}
	
}
