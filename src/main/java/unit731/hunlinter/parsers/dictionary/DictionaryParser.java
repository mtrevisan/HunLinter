package unit731.hunlinter.parsers.dictionary;

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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.services.ParserHelper;
import unit731.hunlinter.services.system.FileHelper;


public class DictionaryParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryParser.class);

	//thin space
	public static final char COUNTER_GROUPING_SEPARATOR = '\u2009';
	//figure space
//	public static final char COUNTER_GROUPING_SEPARATOR = '\u2007';
	public static final DecimalFormat COUNTER_FORMATTER = (DecimalFormat)NumberFormat.getInstance(Locale.ROOT);
	static{
		DecimalFormatSymbols symbols = COUNTER_FORMATTER.getDecimalFormatSymbols();
		symbols.setGroupingSeparator(COUNTER_GROUPING_SEPARATOR);
		COUNTER_FORMATTER.setDecimalFormatSymbols(symbols);
	}
	public static final DecimalFormat PERCENT_FORMATTER = new DecimalFormat("0.#####%", DecimalFormatSymbols.getInstance(Locale.ROOT));
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US);
	public static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy");


	private final File dicFile;
	private final Charset charset;

	private final Comparator<String> comparator;
	private final NavigableMap<Integer, Integer> boundaries = new TreeMap<>();


	public DictionaryParser(final File dicFile, final String language, final Charset charset){
		Objects.requireNonNull(dicFile);
		Objects.requireNonNull(charset);

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

	public final Map.Entry<Integer, Integer> getBoundary(final int lineIndex){
		final Map.Entry<Integer, Integer> entry = boundaries.floorEntry(lineIndex);
		return (entry != null && lineIndex <= entry.getValue()? entry: null);
	}

	public final boolean removeBoundary(final int boundaryIndex){
		return (boundaries.remove(boundaryIndex) != null);
	}

	public final int getBoundaryIndex(final int lineIndex){
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
						boundaries.put(startSection, lineIndex);

					//reset for next section
					prevLine = null;
					startSection = -1;
					needSorting = false;
				}

				lineIndex ++;
			}
			//add last section if needed (filter out single word that doesn't need to be sorted)
			if(startSection >= 0 && lineIndex - startSection > 2 && needSorting)
				boundaries.put(startSection, lineIndex);
		}
		catch(final IOException e){
			LOGGER.error(null, e);
		}
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
		boundaries.clear();
	}

}
