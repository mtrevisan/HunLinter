package unit731.hunlinter.services;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.io.EOFException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Scanner;


public class ParserHelper{

	private static final MessageFormat WRONG_FILE_FORMAT = new MessageFormat("Malformed file, the first line is not a number, was ''{0}''");

	public static final char COMMENT_MARK_SHARP = '#';
	public static final char COMMENT_MARK_SLASH = '/';
	public static final char COMMENT_MARK_PERCENT = '%';


	public static boolean isComment(String line, final char... comment){
		line = line.trim();
		return (line.isEmpty() || StringUtils.indexOfAny(line, comment) == 0);
	}

	public static String assertLinesCount(final Scanner scanner) throws IOException{
		if(!scanner.hasNextLine())
			throw new EOFException("Unexpected EOF while reading file");
		final String line = scanner.nextLine();
		if(!NumberUtils.isCreatable(line))
			throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));

		return line;
	}

	public static void assertLinesCount(final LineIterator itr) throws IOException{
		if(!itr.hasNext())
			throw new EOFException("Unexpected EOF while reading file");
		String line = itr.nextLine();
		if(!NumberUtils.isCreatable(line))
			throw new LinterException(WRONG_FILE_FORMAT.format(new Object[]{line}));
	}

}
