package unit731.hunlinter.services;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;


public class ParserHelper{

	public static final char COMMENT_MARK_SHARP = '#';
	public static final char COMMENT_MARK_SLASH = '/';
	public static final char COMMENT_MARK_PERCENT = '%';


	public static boolean isComment(String line, final char... comment){
		line = line.trim();
		return (line.isEmpty() || StringUtils.indexOfAny(line, comment) == 0);
	}

	public static String extractLine(final BufferedReader br) throws IOException{
		final String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading file");

		return (isComment(line)? StringUtils.EMPTY: line);
	}

}
