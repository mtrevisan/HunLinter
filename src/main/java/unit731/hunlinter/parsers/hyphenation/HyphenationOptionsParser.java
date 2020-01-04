package unit731.hunlinter.parsers.hyphenation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.StringHelper;


public class HyphenationOptionsParser{

	/** minimal hyphenation distance from the left word end */
	private static final String MIN_LEFT_HYPHENATION = "LEFTHYPHENMIN";
	/** minimal hyphenation distance from the right word end */
	private static final String MIN_RIGHT_HYPHENATION = "RIGHTHYPHENMIN";
	/** minimal hyphenation distance from the left compound word boundary */
	private static final String MIN_COMPOUND_LEFT_HYPHENATION = "COMPOUNDLEFTHYPHENMIN";
	/** minimal hyphenation distance from the right compound word boundary */
	private static final String MIN_COMPOUND_RIGHT_HYPHENATION = "COMPOUNDRIGHTHYPHENMIN";
	/** comma separated list of characters or character sequences with forbidden hyphenation */
	private static final String NO_HYPHEN = "NOHYPHEN";

	private static final String NO_HYPHEN_SEPARATOR = ",";


	private final HyphenationOptions nonCompoundOptions = new HyphenationOptions(2);
	private final HyphenationOptions compoundOptions = new HyphenationOptions(0);
	private final Set<String> noHyphen = new HashSet<>();


	public HyphenationOptions getNonCompoundOptions(){
		return nonCompoundOptions;
	}

	public HyphenationOptions getCompoundOptions(){
		return compoundOptions;
	}

	public Set<String> getNoHyphen(){
		return noHyphen;
	}

	public void clear(){
		nonCompoundOptions.clear();
		compoundOptions.clear();
		noHyphen.clear();
	}

	public boolean parseLine(String line){
		boolean managed = true;
		if(line.startsWith(MIN_LEFT_HYPHENATION))
			nonCompoundOptions.setLeftMin(Integer.parseInt(extractValue(line)));
		else if(line.startsWith(MIN_RIGHT_HYPHENATION))
			nonCompoundOptions.setRightMin(Integer.parseInt(extractValue(line)));
		else if(line.startsWith(MIN_COMPOUND_LEFT_HYPHENATION))
			compoundOptions.setLeftMin(Integer.parseInt(extractValue(line)));
		else if(line.startsWith(MIN_COMPOUND_RIGHT_HYPHENATION))
			compoundOptions.setRightMin(Integer.parseInt(extractValue(line)));
		else if(line.startsWith(NO_HYPHEN))
			noHyphen.addAll(Arrays.asList(StringUtils.split(extractValue(line), NO_HYPHEN_SEPARATOR)));
		else
			managed = false;
		return managed;
	}

	private String extractValue(String line){
		String[] components = StringUtils.split(line);
		return StringUtils.strip(components[1]);
	}

	public void write(BufferedWriter writer) throws IOException{
		if(nonCompoundOptions.getLeftMin() != nonCompoundOptions.getMinDefault())
			writeValue(writer, MIN_LEFT_HYPHENATION, nonCompoundOptions.getLeftMin());
		if(nonCompoundOptions.getRightMin() != nonCompoundOptions.getMinDefault())
			writeValue(writer, MIN_RIGHT_HYPHENATION, nonCompoundOptions.getRightMin());
		if(compoundOptions.getLeftMin() != compoundOptions.getMinDefault())
			writeValue(writer, MIN_COMPOUND_LEFT_HYPHENATION, compoundOptions.getLeftMin());
		if(compoundOptions.getRightMin() != compoundOptions.getMinDefault())
			writeValue(writer, MIN_COMPOUND_RIGHT_HYPHENATION, compoundOptions.getRightMin());
		if(!noHyphen.isEmpty())
			writeValue(writer, NO_HYPHEN, StringHelper.join(NO_HYPHEN_SEPARATOR, noHyphen));
	}

	private void writeValue(BufferedWriter writer, String key, int value) throws IOException{
		writeValue(writer, key, Integer.toString(value));
	}

	private void writeValue(BufferedWriter writer, String key, String value) throws IOException{
		writer.write(key);
		writer.write(StringUtils.SPACE);
		writer.write(value);
		writer.write(StringUtils.LF);
	}

}
