package unit731.hunlinter.parsers.affix;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.StringHelper;


public class ParsingContext{

	private final String line;
	private final String[] definitionParts;
	private final BufferedReader reader;


	public ParsingContext(final String line, final BufferedReader br){
		Objects.requireNonNull(line);
		Objects.requireNonNull(br);

		this.line = line;
		definitionParts = StringUtils.split(line);
		reader = br;
	}

	public BufferedReader getReader(){
		return reader;
	}

	public String getRuleType(){
		return definitionParts[0];
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
		return StringUtils.join(Arrays.asList(definitionParts).subList(1, definitionParts.length), StringUtils.SPACE);
	}

	@Override
	public String toString(){
		return line;
	}

}
