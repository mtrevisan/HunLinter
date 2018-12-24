package unit731.hunspeller.parsers.affix.handlers;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;


public class AliasesHandler implements Handler{

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

			List<String> aliases = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = extractLine(br);

				String[] parts = StringUtils.split(line);

				checkValidity(parts, context);

				aliases.add(parts[1]);
			}

			addData.accept(context.getRuleType(), aliases);
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private void checkValidity(String[] parts, ParsingContext context) throws IllegalArgumentException{
		if(parts.length != 2)
			throw new IllegalArgumentException("Error reading line \"" + context
				+ ": Bad number of entries, it must be <tag> <flag/morphological field>");
		if(!context.getRuleType().equals(parts[0]))
			throw new IllegalArgumentException("Error reading line \"" + context
				+ ": Bad tag, it must be " + context.getRuleType());
	}

	private String extractLine(BufferedReader br) throws IOException, EOFException{
		String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading Dictionary file");

		return DictionaryParser.cleanLine(line);
	}
	
}
