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
package io.github.mtrevisan.hunlinter.parsers.dictionary;

import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private boolean boundariesCalculated;
	private final NavigableMap<Integer, Integer> boundaries = new TreeMap<>();


	public DictionaryParser(final File dicFile, final String language, final Charset charset){
		Objects.requireNonNull(dicFile, "Dictionary file cannot be null");
		Objects.requireNonNull(charset, "Charset cannot be null");

		this.dicFile = dicFile;
		this.charset = charset;

		comparator = BaseBuilder.getComparator(language);
	}

	public final File getDicFile(){
		return dicFile;
	}

	public final Charset getCharset(){
		return charset;
	}

	public final Comparator<String> getComparator(){
		return comparator;
	}

	public final Map.Entry<Integer, Integer> getBoundary(final int lineIndex){
		final Map.Entry<Integer, Integer> entry = boundaries.floorEntry(lineIndex);
		return (entry != null && lineIndex <= entry.getValue()? entry: null);
	}

	public final boolean removeBoundary(final int boundaryIndex){
		return (boundaries.remove(boundaryIndex) != null);
	}

	public final int getBoundaryIndex(final int lineIndex){
		if(!boundariesCalculated)
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
				if(!ParserHelper.isDictionaryComment(line)){
					if(startSection < 0)
						startSection = lineIndex;

					if(!needSorting && prevLine != null)
						needSorting = (comparator.compare(line, prevLine) < 0);
					prevLine = line;
				}
				else if(startSection >= 0){
					//filter out possible single lines that doesn't need to be sorted
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

		boundariesCalculated = true;
	}

	public final int getNextBoundaryIndex(final int lineIndex){
		final Map.Entry<Integer, Integer> entry = boundaries.higherEntry(lineIndex);
		return (entry != null? entry.getKey(): -1);
	}

	public final int getPreviousBoundaryIndex(final int lineIndex){
		final Map.Entry<Integer, Integer> entry = boundaries.lowerEntry(lineIndex);
		return (entry != null? entry.getKey(): -1);
	}

	public final boolean isInBoundary(final int lineIndex){
		return (searchBoundary(lineIndex) != null);
	}

	private Map.Entry<Integer, Integer> searchBoundary(final int lineIndex){
		final Map.Entry<Integer, Integer> entry = boundaries.floorEntry(lineIndex);
		return (entry != null && lineIndex <= entry.getValue()? entry: null);
	}

	public final void clear(){
		clearBoundaries();
	}

	public final void clearBoundaries(){
		boundariesCalculated = false;
		boundaries.clear();
	}

}
