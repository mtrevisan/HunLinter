/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.services.system;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Properties;


/**
 * Properties were by default read as ISO-8859-1 characters.
 * <p>
 * However, in the last 10 years most builds use UTF-8. Since this is in general a global setting, it is very awkward to use ISO-8859-1.
 * </p>
 * <p>
 * This class always writes UTF-8. However, it will try to read UTF-8 first. If this fails, it will try ISO-8859-1, and the last attempt
 * is the platform default.
 * </p>
 */
public class PropertiesUTF8 extends Properties{

	@Serial
	private static final long serialVersionUID = -7320018209057349063L;

	/** Characters used to write comment lines in a property file. */
	private static final String COMMENT = "#!";
	/** Possible Separator between key and value of a property in a property file. */
	private static final String KEY_VALUE_SEPARATORS = "=: \t\r\n\f";


	public PropertiesUTF8(){}

	public PropertiesUTF8(final Properties p){
		super(p);
	}

	/**
	 * Reads a property list (key and element pairs) from the input stream.
	 * <p>The stream is assumed to be using the UTF-8 character encoding or compatible.</p>
	 * <p>Characters can be written with their unicode escape sequence.</p>
	 *
	 * @param inStream	The input stream.
	 * @throws IOException	If an error occurred when reading from the input stream.
	 * @throws IllegalArgumentException	If the input stream contains a malformed Unicode escape sequence.
	 */
	@Override
	public void load(final InputStream inStream) throws IOException{
		try(final BufferedReader in = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8))){
			String line;
			while((line = in.readLine()) != null){
				line = removeWhiteSpaces(line);
				if(!line.isEmpty() && COMMENT.indexOf(line.charAt(0)) < 0){
					//removes the beginning separators
					String property = line;
					//reads the whole property if it is on multiple lines
					while(continueLine(line)){
						property = property.substring(0, property.length() - 1);
						line = in.readLine();
						property += line;
					}

					if(!property.isEmpty()){
						int endOfKey = 0;
						//calculates the ending index of the key
						final int l = property.length();
						while(endOfKey < l && (KEY_VALUE_SEPARATORS.indexOf(property.charAt(endOfKey)) < 0))
							endOfKey ++;
						String key = property.substring(0, endOfKey);
						String value = property.substring(endOfKey + 1);

						key = loadConversion(key);
						value = loadConversion(removeWhiteSpaces(value));

						put(key, value);
					}
				}
			}
		}
	}

	/**
	 * A simple method to remove white spaces at the beginning of a String.
	 *
	 * @param line	The String to treat
	 * @return	The same String without white spaces at the beginning
	 */
	public static String removeWhiteSpaces(final String line){
		int index = 0;
		while(index < line.length() && KEY_VALUE_SEPARATORS.indexOf(line.charAt(index)) != -1)
			index ++;
		return line.substring(index);
	}

	/**
	 * Indicates whether the property continues on the next line or not.
	 *
	 * @param line	The beginning of the property that might be continued on the next line.
	 * @return	Whether the property continues on the following line.
	 */
	private boolean continueLine(final CharSequence line){
		return (line != null && !line.isEmpty() && line.charAt(line.length() - 1) == '\\');
	}

	/**
	 * Replaces all characters preceded by a '\' with the corresponding special character and converts unicode escape sequences to
	 * their value.
	 *
	 * @param line	The String to treat.
	 * @return	The converted line.
	 */
	private String loadConversion(final CharSequence line){
		final StringBuilder sb = new StringBuilder(line.length());
		//replace all the "\." substrings with their corresponding escaped characters
		for(int index = 0; index < line.length(); index ++){
			char currentChar = line.charAt(index);
			if(currentChar == '\\'){
				index ++;
				currentChar = line.charAt(index);
				switch(currentChar){
					case 't':
						currentChar = '\t';
						break;
					case 'r':
						currentChar = '\r';
						break;
					case 'n':
						currentChar = '\n';
						break;
					case 'f':
						currentChar = '\f';
						break;
					case 'u':
						index ++;
						//read the xxxx
						int value = 0;
						for(int i = 0; i < 4; i ++){
							currentChar = line.charAt(index ++);
							value = switch(currentChar){
								case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> (value << 4) + currentChar - '0';
								case 'a', 'b', 'c', 'd', 'e', 'f' -> (value << 4) + 10 + currentChar - 'a';
								case 'A', 'B', 'C', 'D', 'E', 'F' -> (value << 4) + 10 + currentChar - 'A';
								default -> throw new IllegalArgumentException("Malformed \\uxxxx encoding.");
							};
						}
						//index must point on the last character of the escaped sequence to avoid missing the next character
						index --;
						currentChar = (char) value;
					default:
						break;
				}
			}
			sb.append(currentChar);
		}

		return sb.toString();
	}


	//we do not want that a thread could modify this instance while storing it, hence the synchronization
	@Override
	public void store(final OutputStream out, final String header) throws IOException{
		try(final BufferedWriter output = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))){
			if(header != null){
				output.write('#' + header);
				output.newLine();
			}
			output.write('#' + ZonedDateTime.now().toString());
			output.newLine();
			for(final Object k : keySet()){
				final String key = storeConversion((CharSequence)k);
				final String value = storeConversion((CharSequence) get(key));

				output.write(key + '=' + value);
				output.newLine();
			}
			output.flush();
		}
	}

	/**
	 * Replaces special characters with their '2-chars' representation.
	 * <p>For example, '\n' becomes '\\' followed by 'n'.</p>
	 *
	 * @param line	The String to treat.
	 * @return	The resulting String.
	 */
	private String storeConversion(final CharSequence line){
		final int length = line.length();
		final StringBuilder sb = new StringBuilder(length * 2);
		for(int i = 0; i < length; i ++){
			final char currentChar = line.charAt(i);
			switch(currentChar){
				case '\\' -> {
					sb.append('\\');
					sb.append('\\');
				}
				case '\t' -> {
					sb.append('\\');
					sb.append('t');
				}
				case '\n' -> {
					sb.append('\\');
					sb.append('n');
				}
				case '\r' -> {
					sb.append('\\');
					sb.append('r');
				}
				case '\f' -> {
					sb.append('\\');
					sb.append('f');
				}
				default -> sb.append(currentChar);
			}
		}
		return sb.toString();
	}


	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

}
