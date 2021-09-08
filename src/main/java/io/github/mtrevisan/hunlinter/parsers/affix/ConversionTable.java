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
package io.github.mtrevisan.hunlinter.parsers.affix;

import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.StringJoiner;


public class ConversionTable{

	private static final String BAD_FIRST_PARAMETER = "Error reading line `{}`: the first parameter is not a number";
	private static final String BAD_NUMBER_OF_ENTRIES = "Error reading line `{}`: bad number of entries, `{}` must be a positive integer less or equal than " + Short.MAX_VALUE;
	private static final String WRONG_FORMAT = "Error reading line `{}`: bad number of entries, it must be '<option> <pattern-from> <pattern-to>'";
	private static final String BAD_OPTION = "Error reading line `{}`: bad option, it must be {}";


	private static final String KEY_INSIDE = reduceKey(" ");
	private static final String KEY_STARTS_WITH = reduceKey("^");
	private static final String KEY_ENDS_WITH = reduceKey("$");
	private static final String KEY_WHOLE = reduceKey("^$");
	private static final String ZERO = "0";


	private final AffixOption affixOption;
	private Map<String, List<Pair<String, String>>> table;


	public ConversionTable(final AffixOption affixOption){
		this.affixOption = affixOption;
	}

	public final void parse(final ParsingContext context){
		try{
			final Scanner scanner = context.getScanner();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new LinterException(BAD_FIRST_PARAMETER, context);
			final int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0 || numEntries > Short.MAX_VALUE)
				throw new LinterException(BAD_NUMBER_OF_ENTRIES, context, context.getFirstParameter());

			table = new HashMap<>(4);
			for(int i = 0; i < numEntries; i ++){
				ParserHelper.assertNotEOF(scanner);

				final String line = scanner.nextLine();
				final String[] parts = StringUtils.split(line);

				checkValidity(parts, context);

				final String key = reduceKey(parts[1]);
				table.computeIfAbsent(key, k -> new ArrayList<>(1))
					.add(Pair.of(parts[1], parts[2]));
			}

			//sort substitutions by length
			for(final List<Pair<String, String>> list : table.values())
				list.sort(Comparator.comparingInt((Pair<String, String> e) -> e.getKey().length()).reversed());
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private void checkValidity(final String[] parts, final ParsingContext context){
		if(parts.length != 3)
			throw new LinterException(WRONG_FORMAT, context);
		if(!affixOption.is(parts[0]))
			throw new LinterException(BAD_OPTION, context, affixOption.getCode());
	}

	/**
	 * NOTE: returns the original word if no conversion has been applied!
	 *
	 * @param word	Word to be converted
	 * @return	The list of conversions
	 */
	public final String applyConversionTable(final String word){
		String convertedWord = word;
		if(table != null){
			final String inputWord = StringUtils.replace(word, StringUtils.SPACE, "_");

			int maxInputLength = 0;

			//whole
			List<Pair<String, String>> list = table.getOrDefault(KEY_WHOLE, Collections.emptyList());
			for(final Pair<String, String> entry : list){
				final String key = entry.getKey();
				if(inputWord.equals(key.substring(1, key.length() - 1))){
					maxInputLength = key.length() - 2;
					convertedWord = ZERO.equals(entry.getValue())? StringUtils.EMPTY: entry.getValue();
					break;
				}
			}

			//starts with
			list = table.getOrDefault(KEY_STARTS_WITH, Collections.emptyList());
			for(final Pair<String, String> entry : list){
				final String key = entry.getKey();
				final int keyLength = key.length() - 1;
				if(keyLength > maxInputLength && inputWord.startsWith(key.substring(1))){
					maxInputLength = keyLength;
					final String value = (ZERO.equals(entry.getValue())? StringUtils.EMPTY: entry.getValue());
					convertedWord = value + inputWord.substring(key.length() - 1);
					break;
				}
			}

			//ends with
			list = table.getOrDefault(KEY_ENDS_WITH, Collections.emptyList());
			for(final Pair<String, String> entry : list){
				final String key = entry.getKey();
				final int keyLength = key.length() - 1;
				if(keyLength > maxInputLength && inputWord.endsWith(key.substring(0, keyLength))){
					maxInputLength = keyLength;
					final String value = (ZERO.equals(entry.getValue())? StringUtils.EMPTY: entry.getValue());
					convertedWord = inputWord.substring(0, inputWord.length() - keyLength) + value;
					break;
				}
			}

			//inside
			list = table.getOrDefault(KEY_INSIDE, Collections.emptyList());
			for(final Pair<String, String> entry : list){
				final String key = entry.getKey();
				final int keyLength = key.length();
				if(keyLength > maxInputLength && inputWord.contains(key)){
					final String value = (ZERO.equals(entry.getValue())? StringUtils.EMPTY: entry.getValue());

					//search every occurrence of the pattern in the word
					convertedWord = RegExUtils.replaceAll(inputWord, key, value);
					break;
				}
			}

			if(convertedWord != null)
				convertedWord = StringUtils.replace(convertedWord, "_", StringUtils.SPACE);
		}

		return convertedWord;
	}

	private static String reduceKey(final CharSequence key){
		return (isStarting(key)? "^": " ") + (isEnding(key)? "$": " ");
	}

	private static boolean isStarting(final CharSequence key){
		return (key.charAt(0) == '^');
	}

	private static boolean isEnding(final CharSequence key){
		return (key.charAt(key.length() - 1) == '$');
	}

	public final String extractAsList(){
		final StringJoiner sj = new StringJoiner(", ");
		for(final List<Pair<String, String>> pairs : table.values())
			for(final Pair<String, String> entry : pairs)
				sj.add(entry.getKey() + StringUtils.SPACE + entry.getValue());
		return sj.toString();
	}

	@Override
	public final String toString(){
		return "[affixOption=" + affixOption + ',' + "table=" + table + ']';
	}

}
