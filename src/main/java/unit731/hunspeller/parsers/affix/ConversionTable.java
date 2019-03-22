package unit731.hunspeller.parsers.affix;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;


public class ConversionTable{

	@FunctionalInterface
	public interface ConversionFunction{
		void convert(String word, Pair<String, String> entry, List<String> conversions);
	}

	private static final Map<String, ConversionFunction> CONVERSION_TABLE_ADD_METHODS = new HashMap<>();
	static{
		CONVERSION_TABLE_ADD_METHODS.put("  ", ConversionTable::convertInside);
		CONVERSION_TABLE_ADD_METHODS.put("^ ", ConversionTable::convertStartsWith);
		CONVERSION_TABLE_ADD_METHODS.put(" $", ConversionTable::convertEndsWith);
		CONVERSION_TABLE_ADD_METHODS.put("^$", ConversionTable::convertWhole);
	}


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

	/**
	 * NOTE: returns the original word if no conversion has been applied!
	 * 
	 * @param word	Word to be converted
	 * @return	The conversion
	 */
	public String applySingleConversionTable(String word){
		List<String> conversions = applyConversionTable(word);
		if(conversions.size() > 1)
			throw new IllegalArgumentException("Cannot convert word " + word + ", too much appliable rules");

		return (!conversions.isEmpty()? conversions.get(0): word);
	}

	/**
	 * NOTE: does not include the original word!
	 * 
	 * @param word	Word to be converted
	 * @return	The list of conversions
	 */
	public List<String> applyConversionTable(String word){
		List<String> conversions = new ArrayList<>();
		if(table != null)
			for(Pair<String, String> entry : table){
				String reducedKey = reduceKey(entry.getKey());
				ConversionFunction fun = CONVERSION_TABLE_ADD_METHODS.get(reducedKey);
				fun.convert(word, entry, conversions);
			}
		return conversions;
	}

	private String reduceKey(String key){
		return (isStarting(key)? "^": " ") + (isEnding(key)? "$": " ");
	}

	private static void convertInside(String word, Pair<String, String> entry, List<String> conversions){
		String key = entry.getKey();
		//FIXME also combinations of more than one REP are possible? or mixed REP substitutions?
		if(word.contains(key)){
			//search every occurence of the pattern in the word
			int idx = -1;
			StringBuffer sb = new StringBuffer();
			while((idx = word.indexOf(key, idx + 1)) >= 0){
				sb.setLength(0);
				sb.append(word);
				sb.replace(idx, idx + key.length(), entry.getValue());
				conversions.add(sb.toString());
			}
		}
	}

	private static void convertStartsWith(String word, Pair<String, String> entry, List<String> conversions){
		String key = entry.getKey();
		String strippedKey = key.substring(1);
		if(word.startsWith(strippedKey))
			conversions.add(entry.getValue() + word.substring(key.length() - 1));
	}

	private static void convertEndsWith(String word, Pair<String, String> entry, List<String> conversions){
		String key = entry.getKey();
		int keyLength = key.length() - 1;
		String strippedKey = key.substring(0, keyLength);
		if(word.endsWith(strippedKey))
			conversions.add(word.substring(0, word.length() - keyLength) + entry.getValue());
	}

	private static void convertWhole(String word, Pair<String, String> entry, List<String> conversions){
		String key = entry.getKey();
		String strippedKey = key.substring(1, key.length() - 1);
		if(word.equals(strippedKey))
			conversions.add(entry.getValue());
	}

	private boolean isStarting(String key){
		return (key.charAt(0) == '^');
	}

	private boolean isEnding(String key){
		return (key.charAt(key.length() - 1) == '$');
	}

	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append("[affixTag=").append(affixTag).append(',');
		sb.append("table=").append(table).append(']');
		return sb.toString();
	}

}
