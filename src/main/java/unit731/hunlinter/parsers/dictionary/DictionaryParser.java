package unit731.hunlinter.parsers.dictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.ParserHelper;
import unit731.hunlinter.services.externalsorter.ExternalSorter;


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
	private final ExternalSorter sorter = new ExternalSorter();
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

	public ExternalSorter getSorter(){
		return sorter;
	}

	//sorter worker
	public final Map.Entry<Integer, Integer> getBoundary(final int lineIndex){
		return Optional.ofNullable(boundaries.floorEntry(lineIndex))
			.filter(e -> lineIndex <= e.getValue())
			.orElse(null);
	}

	public final int getBoundaryIndex(final int lineIndex){
		if(boundaries.isEmpty())
			calculateDictionaryBoundaries();

		return searchBoundary(lineIndex)
			.map(e -> boundaries.headMap(lineIndex, true).size() - 1)
			.orElse(-1);
	}

	private void calculateDictionaryBoundaries(){
		try(final BufferedReader br = Files.newBufferedReader(dicFile.toPath(), charset)){
			//skip line count
			FileHelper.readCount(br.readLine());
			int lineIndex = 1;

			String prevLine = null;
			String line;
			int startSection = -1;
			boolean needSorting = false;
			while((line = br.readLine()) != null){
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
					prevLine = null;
					startSection = -1;
					needSorting = false;
				}

				lineIndex ++;
			}
			//filter out single word that doesn't need to be sorted
			if(startSection >= 0 && lineIndex - startSection > 2 && needSorting)
				boundaries.put(startSection, lineIndex - 1);
		}
		catch(final IOException e){
			LOGGER.error(null, e);
		}
	}

	public final int getNextBoundaryIndex(final int lineIndex){
		return Optional.ofNullable(boundaries.higherEntry(lineIndex))
			.map(Map.Entry::getKey)
			.orElse(-1);
	}

	public final int getPreviousBoundaryIndex(final int lineIndex){
		return Optional.ofNullable(boundaries.lowerEntry(lineIndex))
			.map(Map.Entry::getKey)
			.orElse(-1);
	}

	public final boolean isInBoundary(final int lineIndex){
		return searchBoundary(lineIndex)
			.isPresent();
	}

	private Optional<Map.Entry<Integer, Integer>> searchBoundary(final int lineIndex){
		return Optional.ofNullable(boundaries.floorEntry(lineIndex))
			.filter(e -> lineIndex <= e.getValue());
	}


	public final void clear(){
		boundaries.clear();
	}

}
