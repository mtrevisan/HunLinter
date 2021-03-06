/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.parsers.dictionary;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.services.ParserHelper;
import unit731.hunlinter.services.system.FileHelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Scanner;
import java.util.TreeMap;


public class DictionaryParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryParser.class);

	//thin space
	public static final char COUNTER_GROUPING_SEPARATOR = '\u2009';
	public static final DecimalFormat COUNTER_FORMATTER = (DecimalFormat)NumberFormat.getInstance(Locale.ROOT);
	static{
		final DecimalFormatSymbols symbols = COUNTER_FORMATTER.getDecimalFormatSymbols();
		symbols.setGroupingSeparator(COUNTER_GROUPING_SEPARATOR);
		COUNTER_FORMATTER.setDecimalFormatSymbols(symbols);
	}
	public static final DecimalFormat PERCENT_FORMATTER = new DecimalFormat("0.#####%", DecimalFormatSymbols.getInstance(Locale.ROOT));
	public static final DecimalFormat SHORT_PERCENT_FORMATTER = new DecimalFormat("0.#%", DecimalFormatSymbols.getInstance(Locale.ROOT));
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US);
	public static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");


	private final File dicFile;
	private final Charset charset;

	private final Comparator<String> comparator;
	private final NavigableMap<Integer, Integer> boundaries = new TreeMap<>();


	public DictionaryParser(final File dicFile, final String language, final Charset charset){
		Objects.requireNonNull(dicFile, "Dictionary file cannot be null");
		Objects.requireNonNull(charset, "Charser cannot be null");

		this.dicFile = dicFile;
		this.charset = charset;

		comparator = BaseBuilder.getComparator(language);
	}

	public File getDicFile(){
		return dicFile;
	}

	public Charset getCharset(){
		return charset;
	}

	public Comparator<String> getComparator(){
		return comparator;
	}

	public synchronized Map.Entry<Integer, Integer> getBoundary(final int lineIndex){
		final Map.Entry<Integer, Integer> entry = boundaries.floorEntry(lineIndex);
		return (entry != null && lineIndex <= entry.getValue()? entry: null);
	}

	public synchronized boolean removeBoundary(final int boundaryIndex){
		return (boundaries.remove(boundaryIndex) != null);
	}

	public synchronized int getBoundaryIndex(final int lineIndex){
		if(boundaries.isEmpty())
			calculateDictionaryBoundaries();

		final Map.Entry<Integer, Integer> entry = searchBoundary(lineIndex);
		return (entry != null? boundaries.headMap(lineIndex, true).size() - 1: -1);
	}

	private void calculateDictionaryBoundaries(){
		boundaries.clear();
		try(final Scanner scanner = FileHelper.createScanner(dicFile.toPath(), charset)){
			ParserHelper.assertLinesCount(scanner);

			String prevLine = null;
			int startSection = -1;
			boolean needSorting = false;
			int lineIndex = 1;
			while(scanner.hasNextLine()){
				final String line = scanner.nextLine();
				if(!ParserHelper.isComment(line, ParserHelper.COMMENT_MARK_SHARP, ParserHelper.COMMENT_MARK_SLASH)){
					if(startSection < 0)
						startSection = lineIndex;

					if(!needSorting && StringUtils.isNotBlank(prevLine))
						needSorting = (comparator.compare(line, prevLine) < 0);
					prevLine = line;
				}
				else if(startSection >= 0){
					//filter out single word that doesn't need to be sorted
					if(lineIndex - startSection > 2 && needSorting)
						boundaries.put(startSection, lineIndex - 1);

					//reset for next section
					prevLine = null;
					startSection = -1;
					needSorting = false;
				}

				lineIndex ++;
			}
			//add last section if needed (filter out single word that doesn't need to be sorted)
			if(startSection >= 0 && lineIndex - startSection > 2 && needSorting)
				boundaries.put(startSection, lineIndex - 1);
		}
		catch(final IOException e){
			LOGGER.error(null, e);
		}
	}

	public synchronized int getNextBoundaryIndex(final int lineIndex){
		final Map.Entry<Integer, Integer> entry = boundaries.higherEntry(lineIndex);
		return (entry != null? entry.getKey(): -1);
	}

	public synchronized int getPreviousBoundaryIndex(final int lineIndex){
		final Map.Entry<Integer, Integer> entry = boundaries.lowerEntry(lineIndex);
		return (entry != null? entry.getKey(): -1);
	}

	public synchronized boolean isInBoundary(final int lineIndex){
		return (searchBoundary(lineIndex) != null);
	}

	private Map.Entry<Integer, Integer> searchBoundary(final int lineIndex){
		final Map.Entry<Integer, Integer> entry = boundaries.floorEntry(lineIndex);
		return (entry != null && lineIndex <= entry.getValue()? entry: null);
	}


	public void clear(){
		clearBoundaries();
	}

	public synchronized void clearBoundaries(){
		boundaries.clear();
	}

}
