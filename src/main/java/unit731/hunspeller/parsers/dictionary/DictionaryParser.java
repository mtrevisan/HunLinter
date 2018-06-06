package unit731.hunspeller.parsers.dictionary;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;
import unit731.hunspeller.services.externalsorter.ExternalSorter;


@Slf4j
@Getter
public class DictionaryParser{

	private static final Matcher REGEX_COMMENT = PatternService.matcher("^\\s*[#\\/].*$");
	private static final Matcher REGEX_PARENTHESIS = PatternService.matcher("\\([^)]+\\)");

	private static final Matcher REGEX_FILTER_EMPTY = PatternService.matcher("^\\(.+?\\)\\|?|^\\||\\|$");
	private static final Matcher REGEX_FILTER_OR = PatternService.matcher("\\|{2,}");

	public static final NumberFormat COUNTER_FORMATTER = NumberFormat.getInstance(Locale.US);
	public static final DecimalFormat PERCENT_FORMATTER = new DecimalFormat("0.#####%", DecimalFormatSymbols.getInstance(Locale.US));


	private final File dicFile;
	protected final WordGenerator wordGenerator;
	private final Charset charset;
	private final String language;
	@Setter
	protected HyphenationParser hyphenationParser;
	private final ExternalSorter sorter = new ExternalSorter();

	private final NavigableMap<Integer, Integer> boundaries = new TreeMap<>();


	public DictionaryParser(File dicFile, WordGenerator wordGenerator, Charset charset){
		Objects.requireNonNull(dicFile);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(charset);

		this.dicFile = dicFile;
		this.wordGenerator = wordGenerator;
		this.charset = charset;
		String filename = dicFile.getName();
		language = filename.substring(0, filename.indexOf(".dic"));
	}


	//correctness worker:
	public void checkProduction(RuleProductionEntry production, FlagParsingStrategy strategy) throws IllegalArgumentException{}


	//minimal pairs worker:
	public boolean isConsonant(char chr){
		return true;
	}

	public boolean shouldBeProcessedForMinimalPair(RuleProductionEntry production){
		return true;
	}


	//sorter worker
	public final Map.Entry<Integer, Integer> getBoundary(int lineIndex){
		return Optional.ofNullable(boundaries.floorEntry(lineIndex))
			.filter(e -> lineIndex <= e.getValue())
			.orElse(null);
	}

	public final int getBoundaryIndex(int lineIndex) throws IOException{
		calculateDictionaryBoundaries();

		return searchBoundary(lineIndex)
			.map(e -> boundaries.headMap(lineIndex, true).size() - 1)
			.orElse(-1);
	}

	public final int getNextBoundaryIndex(int lineIndex) throws IOException{
		return Optional.ofNullable(boundaries.higherEntry(lineIndex))
			.map(Map.Entry::getKey)
			.orElse(-1);
	}

	public final int getPreviousBoundaryIndex(int lineIndex) throws IOException{
		return Optional.ofNullable(boundaries.lowerEntry(lineIndex))
			.map(Map.Entry::getKey)
			.orElse(-1);
	}

	public final boolean isInBoundary(int lineIndex) throws IOException{
		return searchBoundary(lineIndex)
			.isPresent();
	}

	private Optional<Map.Entry<Integer, Integer>> searchBoundary(int lineIndex) throws IOException{
		return Optional.ofNullable(boundaries.floorEntry(lineIndex))
			.filter(e -> lineIndex <= e.getValue());
	}

	public final void calculateDictionaryBoundaries() throws IOException{
		if(boundaries.isEmpty()){
			int lineIndex = 0;
			try(BufferedReader br = Files.newBufferedReader(dicFile.toPath(), charset)){
				String prevLine = null;
				String line;
				int startSection = -1;
				boolean needSorting = false;
				Comparator<String> comparator = ComparatorBuilder.getComparator(language);
				while(Objects.nonNull(line = br.readLine())){
					if(isComment(line) || StringUtils.isBlank(line)){
						if(startSection >= 0){
							//filter out single word that doesn't need to be sorted
							if(lineIndex - startSection > 2 && needSorting)
								boundaries.put(startSection, lineIndex - 1);
							prevLine = null;
							startSection = -1;
							needSorting = false;
						}
					}
					else{
						if(startSection < 0)
							startSection = lineIndex;

						if(!needSorting && StringUtils.isNotBlank(prevLine))
							needSorting = (comparator.compare(line, prevLine) < 0);
						prevLine = line;
					}

					lineIndex ++;
				}
				//filter out single word that doesn't need to be sorted
				if(startSection >= 0 && lineIndex - startSection > 2 && needSorting)
					boundaries.put(startSection, lineIndex - 1);
			}
		}
	}


	private boolean isComment(String line){
		return PatternService.find(line, REGEX_COMMENT);
	}

	public String prepareTextForFilter(String text){
		text = StringUtils.strip(text);
		text = PatternService.clear(text, REGEX_FILTER_EMPTY);
		text = PatternService.replaceAll(text, REGEX_FILTER_OR, "|");
		text = PatternService.replaceAll(text, REGEX_PARENTHESIS, StringUtils.EMPTY);
		return "(?iu)(" + text + ")";
	}

	public final void clear(){
		if(Objects.nonNull(boundaries))
			boundaries.clear();
	}

	/**
	 * Removes comment lines and then cleans up blank lines and trailing whitespace.
	 * 
	 * @param line	The line to be cleaned
	 * @return	The cleaned line (withou comments or spaces at the beginning or at the end)
	 */
	public final String cleanLine(String line){
		//remove comments
		line = PatternService.clear(line, REGEX_COMMENT);
		//trim the entire string
		line = StringUtils.strip(line);
		return line;
	}

	//https://stackoverflow.com/questions/526037/how-to-open-user-system-preferred-editor-for-given-file
	public static void openFileWithChoosenEditor(File file) throws InterruptedException, IOException{
		ProcessBuilder builder = null;
		if(SystemUtils.IS_OS_WINDOWS)
			builder = new ProcessBuilder("rundll32.exe", "shell32.dll,OpenAs_RunDLL", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_LINUX)
			builder = new ProcessBuilder("edit", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_MAC)
			builder = new ProcessBuilder("open", file.getAbsolutePath());

		if(Objects.nonNull(builder)){
			builder.redirectErrorStream();
			builder.redirectOutput();
			Process process = builder.start();
			process.waitFor();
		}
		else
			log.warn("Cannot open file {}, OS not recognized ({})", file.getName(), SystemUtils.OS_NAME);
	}

}
