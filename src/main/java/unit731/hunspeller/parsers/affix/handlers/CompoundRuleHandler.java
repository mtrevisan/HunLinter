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


public class CompoundRuleHandler implements Handler{

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

			Set<String> compoundRules = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = extractLine(br);

				String[] lineParts = StringUtils.split(line);
				AffixTag tag = AffixTag.createFromCode(lineParts[0]);
				if(tag != AffixTag.COMPOUND_RULE)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i
						+ ": mismatched compound rule type (expected " + AffixTag.COMPOUND_RULE + ")");
				String rule = lineParts[1];
				if(StringUtils.isBlank(rule))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i
						+ ": compound rule type cannot be empty");
				String[] compounds = strategy.extractCompoundRule(rule);
				if(compounds.length == 0)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i
						+ ": compound rule is bad formatted");

				boolean inserted = compoundRules.add(rule);
				if(!inserted)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i
						+ ": duplicated line");
			}

			addData.accept(AffixTag.COMPOUND_RULE.getCode(), compoundRules);
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private String extractLine(BufferedReader br) throws IOException, EOFException{
		String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading Dictionary file");
	
		return DictionaryParser.cleanLine(line);
	}
	
}
