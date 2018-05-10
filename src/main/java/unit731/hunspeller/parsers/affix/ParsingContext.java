package unit731.hunspeller.parsers.affix;

import unit731.hunspeller.parsers.dictionary.AffixEntry;
import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


public class ParsingContext{

	private static final Pattern REGEX_PATTERN_SEPARATOR = PatternService.pattern("[\\s\\t]+");


	private final String line;
	private final String[] definitionParts;
	@Getter private final BufferedReader reader;


	public ParsingContext(String line, BufferedReader br){
		Objects.requireNonNull(line);
		Objects.requireNonNull(br);

		this.line = line;
		definitionParts = PatternService.split(line, REGEX_PATTERN_SEPARATOR);
		reader = br;
	}

	public String getRuleType(){
		return definitionParts[0];
	}

	public boolean isSuffix(){
		return AffixEntry.Type.SUFFIX.getFlag().equals(getRuleType());
	}

	public String getFirstParameter(){
		return definitionParts[1];
	}

	public String getSecondParameter(){
		return definitionParts[2];
	}

	public String getThirdParameter(){
		return definitionParts[3];
	}

	public String getAllButFirstParameter(){
		return String.join(StringUtils.SPACE, Arrays.asList(definitionParts).subList(1, definitionParts.length));
	}

	public int getRuleLength(){
		String part = getFirstParameter();
		return (StringUtils.isNumeric(part)? Integer.parseInt(part): 0);
	}

	@Override
	public String toString(){
		return line;
	}

}
