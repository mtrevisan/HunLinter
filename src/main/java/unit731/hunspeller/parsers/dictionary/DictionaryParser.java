package unit731.hunspeller.parsers.dictionary;

import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.collections.bloomfilter.interfaces.BloomFilter;
import unit731.hunspeller.collections.intervalmap.Interval;
import unit731.hunspeller.collections.intervalmap.IntervalMap;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.PatternService;
import unit731.hunspeller.services.externalsorter.ExternalSorter;
import unit731.hunspeller.services.externalsorter.ExternalSorterOptions;


public class DictionaryParser{

	private static final Matcher REGEX_COMMENT = Pattern.compile("^\\s*[#\\/].*$").matcher(StringUtils.EMPTY);

	private static final Matcher REGEX_FILTER_EMPTY = Pattern.compile("^\\(.+?\\)\\|?|^\\||\\|$").matcher(StringUtils.EMPTY);
	private static final Matcher REGEX_FILTER_OR = Pattern.compile("\\|{2,}").matcher(StringUtils.EMPTY);

	private static final NumberFormat COUNTER_FORMATTER = NumberFormat.getInstance(Locale.US);
	private static final DecimalFormat PERCENT_FORMATTER = new DecimalFormat("0.#####%", DecimalFormatSymbols.getInstance(Locale.US));

	private static Charset CHARSET;


	private final File dicFile;
	private IntervalMap<Integer, Integer> boundaries;
	private final WordGenerator wordGenerator;
	@Setter protected HyphenationParser hyphenationParser;
	private final ExternalSorter sorter = new ExternalSorter();


	public DictionaryParser(File dicFile, WordGenerator wordGenerator, Charset charset){
		Objects.nonNull(dicFile);
		Objects.nonNull(wordGenerator);
		Objects.nonNull(charset);

		this.dicFile = dicFile;
		this.wordGenerator = wordGenerator;
		DictionaryParser.CHARSET = charset;
	}

	public void clear(){
		if(boundaries != null)
			boundaries.clear();
	}


	@AllArgsConstructor
	public static class CorrectnessWorker extends SwingWorker<Void, String>{

		private final AffixParser affParser;
		private final DictionaryParser dicParser;
		private final Resultable resultable;


		@Override
		protected Void doInBackground() throws Exception{
			try{
				publish("Opening Dictionary file: " + affParser.getLanguage() + ".dic");
				setProgress(0);

				FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

				try(BufferedReader br = Files.newBufferedReader(dicParser.dicFile.toPath(), CHARSET)){
					String line = br.readLine();
					//ignore any BOM marker on first line
					if(line.startsWith(FileService.BOM_MARKER))
						line = line.substring(1);
					if(!NumberUtils.isCreatable(line))
						throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

					int lineIndex = 1;
					long readSoFar = 0l;
					long totalSize = dicParser.dicFile.length();
					String keepCaseFlag = affParser.getKeepCaseFlag();
					while((line = br.readLine()) != null){
						lineIndex ++;
						readSoFar += line.length();
						line = dicParser.cleanLine(line);
						if(!line.isEmpty()){
							try{
								DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);

								dicParser.checkLineLanguageSpecific(line);

								List<RuleProductionEntry> subProductions = dicParser.wordGenerator.applyRules(dictionaryWord);
								List<String> ruleFlags = Arrays.stream(dictionaryWord.getRuleFlags())
									.filter(ruleFlag -> !ruleFlag.equals(keepCaseFlag))
									.collect(Collectors.toList());
								if(!ruleFlags.isEmpty() && subProductions.size() == 1)
									throw new IllegalArgumentException("Word has no productions");

								subProductions.forEach(production -> dicParser.checkProductionLanguageSpecific(dictionaryWord, production));
							}
							catch(IllegalArgumentException e){
								int tabIndex = line.indexOf('\t');
								if(tabIndex < 0)
									tabIndex = line.length();
								publish(e.getMessage() + " on line " + lineIndex + ": " + line.substring(0, tabIndex));
							}
						}

						setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
					}

					setProgress(100);
				}

				publish("Finished reading Dictionary file");
			}
			catch(IOException | IllegalArgumentException e){
				publish(e.getClass().getSimpleName() + ": " + e.getMessage());
			}
			return null;
		}

		@Override
		protected void process(List<String> chunks){
			resultable.printResultLine(chunks);
		}
	};

	protected void checkLineLanguageSpecific(String line) throws IllegalArgumentException{}

	protected void checkProductionLanguageSpecific(DictionaryEntry dicEntry, RuleProductionEntry production) throws IllegalArgumentException{}


	@AllArgsConstructor
	public static class DuplicatesWorker extends SwingWorker<Void, String>{

		private final AffixParser affParser;
		private final DictionaryParser dicParser;
		private final File outputFile;
		private final Resultable resultable;


		@Override
		protected Void doInBackground() throws Exception{
			publish("Opening Dictionary file: " + affParser.getLanguage() + ".dic");
			setProgress(0);

			//collect duplicates
			BloomFilter<String> duplicatesBloomFilter = collectDuplicates();
			setProgress(25);

			//extract duplicates
			List<Duplicate> duplicates = extractDuplicates(duplicatesBloomFilter);
			setProgress(50);

			//write duplicates to file
			writeDuplicates(duplicates, outputFile);
			setProgress(75);

			publish("Duplicates extracted successfully");
			setProgress(100);
			return null;
		}

		private BloomFilter<String> collectDuplicates() throws IOException{
			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

			BloomFilter<String> duplicatesBloomFilter = new ScalableInMemoryBloomFilter<>(500_000, 0.000_000_2, 2.);
			duplicatesBloomFilter.setCharset(CHARSET);

			try(BufferedReader br = Files.newBufferedReader(dicParser.dicFile.toPath(), CHARSET)){
				String line = br.readLine();
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				BloomFilter<String> bloomFilter = new ScalableInMemoryBloomFilter<>(10_000_000, 0.000_000_01, 1.3);
				bloomFilter.setCharset(CHARSET);

				int lineIndex = 1;
				long readSoFar = 0l;
				long totalSize = dicParser.dicFile.length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.length();
					line = dicParser.cleanLine(line);
					if(!line.isEmpty()){
						try{
							DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
							List<RuleProductionEntry> subProductions = dicParser.wordGenerator.applyRules(dictionaryWord);

							for(RuleProductionEntry sub : subProductions){
								String text = sub.toStringWithSignificantDataFields();
								if(!bloomFilter.add(text))
									duplicatesBloomFilter.add(text);
							}
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage() + " on line " + lineIndex + ": " + line);
						}
					}

					setProgress((int)((readSoFar * 100.) / totalSize));
				}

				setProgress(100);

				int totalProductions = bloomFilter.getAddedElements();
				double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
				publish("Total productions: " + COUNTER_FORMATTER.format(totalProductions));
				publish("False positive probability is " + PERCENT_FORMATTER.format(falsePositiveProbability * 100.)
					+ " (overall duplicates ≲ " + (int)Math.ceil(totalProductions * falsePositiveProbability) + ")");

				bloomFilter.close();
				bloomFilter.clear();
				duplicatesBloomFilter.close();
			}

			return duplicatesBloomFilter;
		}

		private List<Duplicate> extractDuplicates(BloomFilter<String> duplicatesBloomFilter) throws IOException{
			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

			List<Duplicate> result = new ArrayList<>();

			publish("Start extracting duplicates");

			try(BufferedReader br = Files.newBufferedReader(dicParser.dicFile.toPath(), CHARSET)){
				String line = br.readLine();

				int lineIndex = 1;
				long readSoFar = 0l;
				long totalSize = dicParser.dicFile.length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.length();
					line = dicParser.cleanLine(line);
					if(!line.isEmpty()){
						try{
							DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
							List<RuleProductionEntry> subProductions = dicParser.wordGenerator.applyRules(dictionaryWord);
							for(RuleProductionEntry sub : subProductions){
								String text = sub.toStringWithSignificantDataFields();
								if(duplicatesBloomFilter.contains(text))
									result.add(new Duplicate(sub, dictionaryWord, lineIndex));
							}
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage());
						}
					}

					setProgress((int)((readSoFar * 100.) / totalSize));
				}
				setProgress(100);

				int totalDuplicates = duplicatesBloomFilter.getAddedElements();
				double falsePositiveProbability = duplicatesBloomFilter.getTrueFalsePositiveProbability();
				publish("Total duplicates: " + COUNTER_FORMATTER.format(totalDuplicates));
				publish("False positive probability is " + PERCENT_FORMATTER.format(falsePositiveProbability * 100.)
					+ " (overall duplicates ≲ " + (int)Math.ceil(totalDuplicates * falsePositiveProbability) + ")");
			}

			duplicatesBloomFilter.clear();

			Comparator<String> comparator = ComparatorBuilder.getComparator(affParser.getLanguage());
			Collections.sort(result, (d1, d2) -> comparator.compare(d1.getProduction().getWord(), d2.getProduction().getWord()));

			return result;
		}

		private void writeDuplicates(List<Duplicate> duplicates, File outputFile) throws IOException{
			publish("Write results to file");

			long writtenSoFar = 0l;
			long totalSize = duplicates.size();
			List<List<Duplicate>> mergedDuplicates = mergeDuplicates(duplicates);
			try(BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), CHARSET)){
				for(List<Duplicate> entries : mergedDuplicates){
					writer.write(entries.get(0).getProduction().getWord());
					writer.write(": ");
					writer.write(entries.stream()
						.map(duplicate -> duplicate.getDictionaryWord().getWord() + " (" + duplicate.getLineIndex() + ")")
						.collect(Collectors.joining(", ")));
					writer.newLine();

					writtenSoFar ++;
					setProgress((int)((writtenSoFar * 100.) / totalSize));
				}
				setProgress(100);

				publish("File written: " + outputFile.getAbsolutePath());
			}
		}

		private List<List<Duplicate>> mergeDuplicates(List<Duplicate> duplicates){
			Map<String, List<Duplicate>> dupls = duplicates.stream()
				.collect(Collectors.toMap(duplicate -> duplicate.getProduction().getWord(),
					duplicate -> {
						List<Duplicate> list = new ArrayList<>();
						list.add(duplicate);
						return list;
					},
					(oldValue, newValue) -> {
						oldValue.addAll(newValue);
						return oldValue;
					}));

			Comparator<String> comparator = ComparatorBuilder.getComparator(affParser.getLanguage());
			List<List<Duplicate>> result = new ArrayList<>(dupls.values());
			result.sort(Comparator.<List<Duplicate>>comparingInt(List::size).reversed()
				.thenComparing(Comparator.comparing(list -> list.get(0).getProduction().getWord(), comparator)));
			return result;
		}

		@Override
		protected void process(List<String> chunks){
			resultable.printResultLine(chunks);
		}
	};


	@AllArgsConstructor
	public static class SorterWorker extends SwingWorker<Void, String>{

		private final AffixParser affParser;
		private final DictionaryParser dicParser;
		private final int lineIndex;
		private final Resultable resultable;


		@Override
		protected Void doInBackground() throws Exception{
			publish("Sorting file " + dicParser.dicFile.getName());
			setProgress(0);

			//extract boundaries from the file (from comment to comment, or blank line)
			dicParser.calculateDictionaryBoundaries(affParser);

			setProgress(20);

			Interval<Integer> boundary = getBoundary(lineIndex);
			if(boundary != null){
				//split dictionary isolating the sorted section
				List<File> chunks = splitDictionary(boundary);

				setProgress(40);

				//sort the chosen section
				File sortSection = chunks.get(1);
				ExternalSorterOptions options = ExternalSorterOptions.builder()
					.charset(CHARSET)
					.comparator(ComparatorBuilder.getComparator(affParser.getLanguage()))
					.useZip(true)
					.removeDuplicates(true)
					.build();
				dicParser.sorter.sort(sortSection, options, sortSection);

				setProgress(60);

				//re-merge dictionary
				mergeDictionary(chunks);

				setProgress(80);

				//remove temporary files
				chunks.forEach(File::delete);

				publish("File " + dicParser.dicFile.getName() + " sorted");

				dicParser.boundaries.clear();
			}
			else
				publish("File " + dicParser.dicFile.getName() + " NOT sorted");

			setProgress(100);
			return null;
		}

		private Interval<Integer> getBoundary(int lineIndex){
			Set<Interval<Integer>> intervals = dicParser.boundaries.getContaining(lineIndex).keySet();
			return (intervals.size() == 1? (new ArrayList<>(intervals)).get(0): null);
		}

		private List<File> splitDictionary(Interval<Integer> boundary) throws IOException{
			int index = 0;
			List<File> files = new ArrayList<>();
			File file = File.createTempFile("split", ".out");
			try(BufferedReader br = Files.newBufferedReader(dicParser.dicFile.toPath(), CHARSET)){
				BufferedWriter writer = Files.newBufferedWriter(file.toPath(), CHARSET);
				String line;
				while((line = br.readLine()) != null){
					if(index == boundary.getLowerBound() || index == boundary.getUpperBound() + 1){
						writer.close();

						files.add(file);

						file = File.createTempFile("split", ".out");
						writer = Files.newBufferedWriter(file.toPath(), CHARSET);
					}

					writer.write(line);
					writer.newLine();

					index ++;
				}

				writer.close();

				files.add(file);
			}
			return files;
		}

		private void mergeDictionary(List<File> files) throws IOException{
			boolean append = false;
			for(File file : files){
				copyFile(file, append);

				append = true;
			}
		}

		private void copyFile(File inputFile, boolean append) throws IOException{
			try(
					BufferedReader br = Files.newBufferedReader(inputFile.toPath(), CHARSET);
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dicParser.dicFile, append), CHARSET));
					){
				String line;
				while((line = br.readLine()) != null){
					writer.write(line);
					writer.newLine();
				}
			}
		}

		@Override
		protected void process(List<String> chunks){
			resultable.printResultLine(chunks);
		}
	};

	public void calculateDictionaryBoundaries(AffixParser affParser) throws IOException{
		if(boundaries == null || boundaries.isEmpty()){
			boundaries = new IntervalMap<>();

			int lineIndex = 0;
			try(BufferedReader br = Files.newBufferedReader(dicFile.toPath(), CHARSET)){
				String prevLine = null;
				String line;
				int startSection = -1;
				boolean needSorting = false;
				Comparator<String> comparator = ComparatorBuilder.getComparator(affParser.getLanguage());
				while((line = br.readLine()) != null){
					if(isComment(line) || StringUtils.isBlank(line)){
						if(startSection >= 0){
							//filter out single word that doesn't need to be sorted
							if(lineIndex - startSection > 2 && needSorting)
								boundaries.put(new Interval<>(startSection, lineIndex - 1), boundaries.size());
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
					boundaries.put(new Interval<>(startSection, lineIndex - 1), boundaries.size());
			}
		}
	}

	public int getBoundaryIndex(int lineIndex){
		Collection<Integer> values = boundaries.getContaining(lineIndex).values();
		return (values.size() == 1? (new ArrayList<>(values)).get(0): -1);
	}

	public boolean isInBoundary(int lineIndex){
		Set<Interval<Integer>> intervals = boundaries.getContaining(lineIndex).keySet();
		return (intervals.size() == 1);
	}


	@AllArgsConstructor
	public static class WordlistWorker extends SwingWorker<Void, String>{

		private final AffixParser affParser;
		private final DictionaryParser dicParser;
		private final File outputFile;
		private final Resultable resultable;


		@Override
		protected Void doInBackground() throws Exception{
			publish("Opening Dictionary file: " + affParser.getLanguage() + ".dic");
			setProgress(0);

			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

			try(
					BufferedReader br = Files.newBufferedReader(dicParser.dicFile.toPath(), CHARSET);
					BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), CHARSET);
					){
				String line = br.readLine();
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				int lineIndex = 1;
				long readSoFar = 0l;
				long totalSize = dicParser.dicFile.length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.length();

					line = dicParser.cleanLine(line);
					if(!line.isEmpty()){
						try{
							DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
							List<RuleProductionEntry> subProductions = dicParser.wordGenerator.applyRules(dictionaryWord);

							for(RuleProductionEntry production : subProductions){
								writer.write(production.getWord());
								writer.newLine();
							}
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage() + " on line " + lineIndex + ": " + line);
						}
					}

					setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
				}

				setProgress(100);

				publish("Wordlist extracted successfully");
			}
			return null;
		}

		@Override
		protected void process(List<String> chunks){
			resultable.printResultLine(chunks);
		}
	};


	public String prepareTextForFilter(String text){
		text = StringUtils.trim(text);
		text = PatternService.replaceAll(text, REGEX_FILTER_EMPTY, StringUtils.EMPTY);
		text = PatternService.replaceAll(text, REGEX_FILTER_OR, "|");
		return "(?iu)(" + text + ")";
	}

	private boolean isComment(String line){
		return PatternService.find(line, REGEX_COMMENT);
	}

	/** Removes comment lines and then cleans up blank lines and trailing whitespace. */
	private String cleanLine(String line){
		//remove comments
		line = PatternService.replaceAll(line, REGEX_COMMENT, StringUtils.EMPTY);
		//trim the entire string
		line = StringUtils.strip(line);
		return line;
	}

}
