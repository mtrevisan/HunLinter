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
			boolean isSuffix = AffixEntry.Type.SUFFIX.is(context.getRuleType());
			String ruleFlag = context.getFirstParameter();
			char combineable = context.getSecondParameter().charAt(0);
			if(!NumberUtils.isCreatable(context.getThirdParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context + "\": The third parameter is not a number");

			List<AffixEntry> entries = readEntries(context, strategy, getData);

			addData.accept(ruleFlag, new RuleEntry(isSuffix, combineable, entries));
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private List<AffixEntry> readEntries(ParsingContext context, FlagParsingStrategy strategy, Function<AffixTag, List<String>> getData)
			throws IOException, IllegalArgumentException{
		int numEntries = Integer.parseInt(context.getThirdParameter());
		if(numEntries <= 0)
			throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be a positive integer");
		BufferedReader br = context.getReader();
		AffixEntry.Type ruleType = AffixEntry.Type.createFromCode(context.getRuleType());
		String ruleFlag = context.getFirstParameter();

		//List<AffixEntry> prefixEntries = new ArrayList<>();
		//List<AffixEntry> suffixEntries = new ArrayList<>();
		List<String> aliasesFlag = getData.apply(AffixTag.ALIASES_FLAG);
		List<String> aliasesMorphologicalField = getData.apply(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);
		int i = 0;
		String line = null;
		List<AffixEntry> entries = new ArrayList<>(numEntries);
		try{
			while(i < numEntries){
				line = extractLine(br);

				AffixEntry entry = new AffixEntry(line, strategy, aliasesFlag, aliasesMorphologicalField);

				checkValidity(entry, ruleType, ruleFlag);

				boolean inserted = entries.add(entry);
				if(!inserted)
					throw new IllegalArgumentException("duplicated line");
	
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

				i ++;
			}
		}
		catch(IllegalArgumentException e){
			throw new IllegalArgumentException("Reading error: " + e.getMessage());
		}
		return entries;
	}

	private String extractLine(BufferedReader br) throws EOFException, IOException{
		String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading Dictionary file");

		return DictionaryParser.cleanLine(line);
	}

	private void checkValidity(AffixEntry entry, AffixEntry.Type ruleType, String ruleFlag) throws IllegalArgumentException{
		if(entry.getType() != ruleType)
			throw new IllegalArgumentException("mismatched rule type (expected " + ruleType + ")");
		if(!ruleFlag.equals(entry.getFlag()))
			throw new IllegalArgumentException("mismatched rule flag (expected " + ruleFlag + ")");
	}
	
}
