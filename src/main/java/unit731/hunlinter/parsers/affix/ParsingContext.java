package unit731.hunlinter.parsers.affix;

import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;


public class ParsingContext{

	private final String line;
	private final String[] definitionParts;
	private final Scanner scanner;


	public ParsingContext(final String line, final Scanner scanner){
		Objects.requireNonNull(line);
		Objects.requireNonNull(scanner);

		this.line = line;
		definitionParts = StringUtils.split(line);
		this.scanner = scanner;
	}

	public Scanner getScanner(){
		return scanner;
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
