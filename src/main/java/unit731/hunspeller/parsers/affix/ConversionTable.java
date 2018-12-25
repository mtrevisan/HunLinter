package unit731.hunspeller.parsers.affix;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;


public class ConversionTable{

	private final AffixTag affixTag;
	private List<Pair<String, String>> table;


	public ConversionTable(AffixTag affixTag){
		this.affixTag = affixTag;
	}

	public AffixTag getAffixTag(){
		return affixTag;
	}

	public void parseConversionTable(ParsingContext context){
		try{
			BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context
					+ "\": The first parameter is not a number");
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context
					+ ": Bad number of entries, it must be a positive integer");

			table = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = extractLine(br);

				String[] parts = StringUtils.split(line);

				checkValidity(parts, context);

				table.add(Pair.of(parts[1], StringUtils.replaceChars(parts[2], '_', ' ')));
			}
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

	private void checkValidity(String[] parts, ParsingContext context) throws IllegalArgumentException{
		if(parts.length != 3)
			throw new IllegalArgumentException("Error reading line \"" + context
				+ ": Bad number of entries, it must be <tag> <pattern-from> <pattern-to>");
		if(!affixTag.getCode().equals(parts[0]))
			throw new IllegalArgumentException("Error reading line \"" + context
				+ ": Bad tag, it must be " + affixTag.getCode());
	}

	/** NOTE: does not include the original word! */
	public List<String> applyConversionTable(String word){
		List<String> conversions = new ArrayList<>();
		if(table != null){
			for(Pair<String, String> entry : table){
				String key = entry.getKey();
				String value = entry.getValue();

				int keyLength = key.length();
				if(isStarting(key)){
					//starts with
					if(!isEnding(key)){
						if(word.startsWith(key.substring(1)))
							conversions.add(value + word.substring(keyLength - 1));
					}
					//whole
					else if(word.equals(key.substring(1, keyLength - 1)))
						conversions.add(value);
				}
				else{
					//ends with
					if(isEnding(key)){
						if(word.endsWith(key.substring(0, keyLength - 1)))
							conversions.add(word.substring(0, word.length() - keyLength + 1) + value);
					}
					//inside
					//FIXME also combinations of more than one REP are possible? or mixed REP substitutions?
					else if(word.contains(key)){
						//search every occurence of the pattern in the word
						int idx = -1;
						StringBuilder sb = new StringBuilder();
						while((idx = word.indexOf(key, idx + 1)) >= 0){
							sb.setLength(0);
							sb.append(word);
							sb.replace(idx, idx + keyLength, value);
							conversions.add(sb.toString());
						}
					}
				}
			}
		}
		return conversions;
	}

	private boolean isStarting(String key){
		return (key.charAt(0) == '^');
	}

	private boolean isEnding(String key){
		return (key.charAt(key.length() - 1) == '$');
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("[affixTag=").append(affixTag).append(',');
		sb.append("table=").append(table).append(']');
		return sb.toString();
	}

}
