package unit731.hunspeller.parsers.hyphenation;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class HyphenationOptions{

	/** minimal hyphenation distance from the left word end */
	private static final String MIN_LEFT_HYPHENATION = "LEFTHYPHENMIN";
	/** minimal hyphation distance from the right word end */
	private static final String MIN_RIGHT_HYPHENATION = "RIGHTHYPHENMIN";
	/** minimal hyphation distance from the left compound word boundary */
	private static final String MIN_COMPOUND_LEFT_HYPHENATION = "COMPOUNDLEFTHYPHENMIN";
	/** minimal hyphation distance from the right compound word boundary */
	private static final String MIN_COMPOUND_RIGHT_HYPHENATION = "COMPOUNDRIGHTHYPHENMIN";
	/** comma separated list of characters or character sequences with forbidden hyphenation */
	private static final String NO_HYPHEN = "NOHYPHEN";

	private static final String NO_HYPHEN_SEPARATOR = ",";

	private static final Pattern REGEX_PATTERN_SPACE = PatternService.pattern("\\s+");
	private static final Pattern REGEX_PATTERN_NO_HYPHEN_SEPARATOR = PatternService.pattern(NO_HYPHEN_SEPARATOR);


	@Builder.Default private int leftMin = 2;
	@Builder.Default private int rightMin = 2;
	private int leftCompoundMin;
	private int rightCompoundMin;
	@Builder.Default private Set<String> noHyphen = new HashSet<>();


	public static HyphenationOptions createEmpty(){
		return HyphenationOptions.builder()
			.build();
	}

	public void clear(){
		leftMin = 2;
		rightMin = 2;
		leftCompoundMin = 0;
		rightCompoundMin = 0;
		noHyphen.clear();
	}

	public boolean parseLine(String line){
		boolean managed = false;
		if(line.startsWith(MIN_LEFT_HYPHENATION)){
			leftMin = Integer.parseInt(extractValue(line));
			if(leftMin <= 0)
				leftMin = 2;
			managed = true;
		}
		else if(line.startsWith(MIN_RIGHT_HYPHENATION)){
			rightMin = Integer.parseInt(extractValue(line));
			if(rightMin <= 0)
				rightMin = 2;
			managed = true;
		}
		else if(line.startsWith(MIN_COMPOUND_LEFT_HYPHENATION)){
			leftCompoundMin = Integer.parseInt(extractValue(line));
			if(leftCompoundMin < 0)
				leftCompoundMin = 0;
			managed = true;
		}
		else if(line.startsWith(MIN_COMPOUND_RIGHT_HYPHENATION)){
			rightCompoundMin = Integer.parseInt(extractValue(line));
			if(rightCompoundMin < 0)
				rightCompoundMin = 0;
			managed = true;
		}
		else if(line.startsWith(NO_HYPHEN)){
			noHyphen.addAll(Arrays.asList(PatternService.split(extractValue(line), REGEX_PATTERN_NO_HYPHEN_SEPARATOR)));
			managed = true;
		}
		return managed;
	}

	private String extractValue(String line){
		String[] components = PatternService.split(line, REGEX_PATTERN_SPACE);
		return StringUtils.strip(components[1]);
	}

	public void write(BufferedWriter writer) throws IOException{
		if(leftMin > 0)
			writeValue(writer, MIN_LEFT_HYPHENATION, leftMin);
		if(rightMin > 0)
			writeValue(writer, MIN_RIGHT_HYPHENATION, rightMin);
		if(leftCompoundMin > 0)
			writeValue(writer, MIN_COMPOUND_LEFT_HYPHENATION, leftCompoundMin);
		if(rightCompoundMin > 0)
			writeValue(writer, MIN_COMPOUND_RIGHT_HYPHENATION, rightCompoundMin);
		if(!noHyphen.isEmpty())
			writeValue(writer, NO_HYPHEN, StringUtils.join(noHyphen, NO_HYPHEN_SEPARATOR));
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
