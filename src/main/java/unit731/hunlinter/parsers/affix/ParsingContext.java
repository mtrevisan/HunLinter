package unit731.hunlinter.parsers.affix;

import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.ParserHelper;


public class ParsingContext{

	private final String line;
	private final int index;
	private final Scanner scanner;

	private final String[] lineParts;


	public ParsingContext(final String line, final int index, final Scanner scanner){
		Objects.requireNonNull(line);
		Objects.requireNonNull(scanner);

		this.line = line;
		this.index = index;
		this.scanner = scanner;

		lineParts = StringUtils.split(line);
	}

	public String getLine(){
		final int commentIndex = line.indexOf(ParserHelper.COMMENT_MARK_SHARP);
		return (commentIndex >= 0? line.substring(0, commentIndex).trim(): line);
	}

	public int getIndex(){
		return index;
	}

	public Scanner getScanner(){
		return scanner;
	}

	public String getRuleType(){
		return lineParts[0];
	}

	public String getFirstParameter(){
		return lineParts[1];
	}

	public String getSecondParameter(){
		return lineParts[2];
	}

	public String getThirdParameter(){
		return lineParts[3];
	}

	public String getAllButFirstParameter(){
		return StringUtils.join(Arrays.asList(lineParts).subList(1, lineParts.length), StringUtils.SPACE);
	}

	@Override
	public String toString(){
		return line;
	}

}
