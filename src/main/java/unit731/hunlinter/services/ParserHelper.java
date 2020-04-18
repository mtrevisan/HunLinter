package unit731.hunlinter.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.io.EOFException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Scanner;


public class ParserHelper{

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Malformed file, the first line is not a number, was ''{0}''");

	public static final char COMMENT_MARK_SHARP = '#';
	public static final char COMMENT_MARK_SLASH = '/';
	public static final char COMMENT_MARK_PERCENT = '%';


	private ParserHelper(){}

	public static boolean isComment(final String line, final char... comment){
		return (StringUtils.isBlank(line) || StringUtils.indexOfAny(line, comment) == 0);
	}

	public static void assertNotEOF(final Scanner scanner) throws EOFException{
		if(!scanner.hasNextLine())
			throw new EOFException("Unexpected EOF while reading file");
	}

	public static void assertLinesCount(final List<String> lines) throws IOException{
		if(lines.isEmpty())
			throw new EOFException("Unexpected EOF while reading file");
		String line = lines.get(0);
		if(!NumberUtils.isCreatable(line))
			throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));
	}

	public static String assertLinesCount(final Scanner scanner){
		final String line = scanner.nextLine();
		if(!NumberUtils.isCreatable(line))
			throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));

		return line;
	}

}
