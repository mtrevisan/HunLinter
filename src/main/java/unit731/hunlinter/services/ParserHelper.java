package unit731.hunlinter.services;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.regex.Pattern;


public class ParserHelper{

	public static final Pattern PATTERN_COMMENT = PatternHelper.pattern("^\\s*[#\\/].*$");


	public static boolean isComment(final String line){
		return PatternHelper.find(line, PATTERN_COMMENT);
	}

	public static String extractLine(final BufferedReader br) throws IOException{
		final String line = br.readLine();
		if(line == null)
			throw new EOFException("Unexpected EOF while reading file");

		return cleanLine(line);
	}

	/**
	 * Removes comment lines and then cleans up blank lines and trailing whitespace.
	 *
	 * @param line	The line to be cleaned
	 * @return	The cleaned line (without comments or spaces at the beginning or at the end)
	 */
	public static String cleanLine(String line){
		//remove comments
		line = PatternHelper.clear(line, PATTERN_COMMENT);
		//trim the entire string
		line = StringUtils.strip(line);
		return line;
	}

}
