package unit731.hunspeller.parsers.dictionary;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.languages.builders.ComparatorBuilder;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.PatternService;
import unit731.hunspeller.services.externalsorter.ExternalSorter;
import unit731.hunspeller.services.externalsorter.ExternalSorterOptions;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;
import unit731.hunspeller.languages.vec.GraphemeVEC;
import unit731.hunspeller.languages.vec.WordVEC;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.HammingDistance;


@Slf4j
public class DictionaryParser{

	private static final Matcher REGEX_COMMENT = PatternService.matcher("^\\s*[#\\/].*$");

	private static final Matcher REGEX_FILTER_EMPTY = PatternService.matcher("^\\(.+?\\)\\|?|^\\||\\|$");
	private static final Matcher REGEX_FILTER_OR = PatternService.matcher("\\|{2,}");

	private static final NumberFormat COUNTER_FORMATTER = NumberFormat.getInstance(Locale.US);
	private static final DecimalFormat PERCENT_FORMATTER = new DecimalFormat("0.#####%", DecimalFormatSymbols.getInstance(Locale.US));

	private static Charset CHARSET;


	private final File dicFile;
	private final NavigableMap<Integer, Integer> boundaries = new TreeMap<>();
	protected final WordGenerator wordGenerator;
	@Setter protected HyphenationParser hyphenationParser;
	private final ExternalSorter sorter = new ExternalSorter();


	public DictionaryParser(File dicFile, WordGenerator wordGenerator, Charset charset){
		Objects.requireNonNull(dicFile);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(charset);

		this.dicFile = dicFile;
		this.wordGenerator = wordGenerator;
		DictionaryParser.CHARSET = charset;
	}

	public void clear(){
		if(boundaries != null)
			boundaries.clear();
	}

	private String getLanguage(){
		String filename = dicFile.getName();
		return filename.substring(0, filename.indexOf(".dic"));
	}


	@AllArgsConstructor
	public static class CorrectnessWorker extends SwingWorker<Void, String>{

		private final AffixParser affParser;
		private final DictionaryParser dicParser;
		private final Resultable resultable;


		@Override
		protected Void doInBackground() throws Exception{
			try{
				publish("Opening Dictionary file for correctness checking: " + affParser.getLanguage() + ".dic");
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
					long readSoFar = line.length();
					long totalSize = dicParser.dicFile.length();
					while((line = br.readLine()) != null){
						lineIndex ++;
						readSoFar += line.length();
						line = dicParser.cleanLine(line);
						if(!line.isEmpty()){
							DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);

							try{
								List<RuleProductionEntry> productions = dicParser.wordGenerator.applyRules(dictionaryWord);

								for(RuleProductionEntry production : productions)
									dicParser.checkProduction(production, strategy);
							}
							catch(IllegalArgumentException e){
								publish(e.getMessage() + " on line " + lineIndex + ": " + dictionaryWord.toWordAndFlagString());
							}
						}

						setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
					}

					setProgress(100);
				}

				publish("Finished reading Dictionary file");
			}
			catch(IOException | IllegalArgumentException e){
				publish(e instanceof ClosedChannelException? "Correctness thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
			}
			catch(Exception e){
				String message = ExceptionService.getMessage(e, getClass());
				publish(e.getClass().getSimpleName() + ": " + message);
			}
			return null;
		}

		@Override
		protected void process(List<String> chunks){
			resultable.printResultLine(chunks);
		}
	};

	protected void checkProduction(RuleProductionEntry production, FlagParsingStrategy strategy) throws IllegalArgumentException{}


	@AllArgsConstructor
	public static class DuplicatesWorker extends SwingWorker<Void, String>{

		private final AffixParser affParser;
		private final DictionaryParser dicParser;
		private final File outputFile;
		private final Resultable resultable;


		@Override
		protected Void doInBackground() throws Exception{
			try{
				publish("Opening Dictionary file for duplications extraction: " + affParser.getLanguage() + ".dic (pass 1/3)");

				BloomFilterInterface<String> duplicatesBloomFilter = collectDuplicates();

				List<Duplicate> duplicates = extractDuplicates(duplicatesBloomFilter);

				writeDuplicates(duplicates);

				publish("Duplicates extracted successfully");

				if(!duplicates.isEmpty())
					openFileWithChoosenEditor(outputFile);
			}
			catch(IOException | IllegalArgumentException e){
				publish(e instanceof ClosedChannelException? "Duplicates thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
			}
			catch(Exception e){
				String message = ExceptionService.getMessage(e, getClass());
				publish(e.getClass().getSimpleName() + ": " + message);
			}
			return null;
		}

		private BloomFilterInterface<String> collectDuplicates() throws IOException{
			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

			BitArrayBuilder.Type bloomFilterType = BitArrayBuilder.Type.FAST;
			BloomFilterInterface<String> duplicatesBloomFilter = new ScalableInMemoryBloomFilter<>(bloomFilterType, 500_000, 0.000_000_2, 2.);
			duplicatesBloomFilter.setCharset(CHARSET);

			setProgress(0);
			try(BufferedReader br = Files.newBufferedReader(dicParser.dicFile.toPath(), CHARSET)){
				String line = br.readLine();
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				BloomFilterInterface<String> bloomFilter = new ScalableInMemoryBloomFilter<>(bloomFilterType, 10_000_000, 0.000_000_01, 1.3);
				bloomFilter.setCharset(CHARSET);

				int lineIndex = 1;
				long readSoFar = line.length();
				long totalSize = dicParser.dicFile.length();
				while((line = br.readLine()) != null){
					lineIndex ++;
					readSoFar += line.length();
					line = dicParser.cleanLine(line);
					if(!line.isEmpty()){
						DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);

						try{
							List<RuleProductionEntry> productions = dicParser.wordGenerator.applyRules(dictionaryWord);

							for(RuleProductionEntry production : productions){
								String text = production.toStringWithSignificantDataFields();
								if(!bloomFilter.add(text))
									duplicatesBloomFilter.add(text);
							}
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage() + " on line " + lineIndex + ": " + dictionaryWord.toWordAndFlagString());
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
			}

			return duplicatesBloomFilter;
		}

		private List<Duplicate> extractDuplicates(BloomFilterInterface<String> duplicatesBloomFilter) throws IOException{
			List<Duplicate> result = new ArrayList<>();

			if(duplicatesBloomFilter.getAddedElements() > 0){
				publish("Start extracting duplicates (pass 2/3)");
				setProgress(0);

				FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();

				setProgress(0);
				try(BufferedReader br = Files.newBufferedReader(dicParser.dicFile.toPath(), CHARSET)){
					String line = br.readLine();

					int lineIndex = 1;
					long readSoFar = line.length();
					long totalSize = dicParser.dicFile.length();
					while((line = br.readLine()) != null){
						lineIndex ++;
						readSoFar += line.length();
						line = dicParser.cleanLine(line);
						if(!line.isEmpty()){
							try{
								DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
								List<RuleProductionEntry> productions = dicParser.wordGenerator.applyRules(dictionaryWord);
								for(RuleProductionEntry production : productions){
									String text = production.toStringWithSignificantDataFields();
									if(duplicatesBloomFilter.contains(text))
										result.add(new Duplicate(production, dictionaryWord, lineIndex));
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

				duplicatesBloomFilter.close();
				duplicatesBloomFilter.clear();

				Comparator<String> comparator = ComparatorBuilder.getComparator(affParser.getLanguage());
				Collections.sort(result, (d1, d2) -> comparator.compare(d1.getProduction().getWord(), d2.getProduction().getWord()));
			}
			else
				publish("No duplicates found, skip remaining passes");

			return result;
		}

		private void writeDuplicates(List<Duplicate> duplicates) throws IOException{
			int totalSize = duplicates.size();
			if(totalSize > 0){
				publish("Write results to file (pass 3/3)");
				setProgress(0);

				int writtenSoFar = 0;
				List<List<Duplicate>> mergedDuplicates = mergeDuplicates(duplicates);
				setProgress((int)(100. / (totalSize + 1)));
				try(BufferedWriter writer = Files.newBufferedWriter(outputFile.toPath(), CHARSET)){
					for(List<Duplicate> entries : mergedDuplicates){
						writer.write(entries.get(0).getProduction().getWord());
						writer.write(": ");
						writer.write(entries.stream()
							.map(duplicate -> duplicate.getDictionaryWord().getWord() + " (" + duplicate.getLineIndex()
								+ (duplicate.getProduction().hasProductionRules()? " via " + duplicate.getProduction().getRulesSequence():
									StringUtils.EMPTY) + ")")
							.collect(Collectors.joining(", ")));
						writer.newLine();

						writtenSoFar ++;
						setProgress((int)((writtenSoFar * 100.) / (totalSize + 1)));
					}
					setProgress(100);

					publish("File written: " + outputFile.getAbsolutePath());
				}
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

		private final DictionaryParser dicParser;
		private final int lineIndex;
		private final Resultable resultable;


		@Override
		protected Void doInBackground() throws Exception{
			try{
				publish("Sorting file " + dicParser.dicFile.getName());
				setProgress(0);

				//extract boundaries from the file (from comment to comment, or blank line)
				dicParser.calculateDictionaryBoundaries();

				setProgress(20);

				Map.Entry<Integer, Integer> boundary = getBoundary(lineIndex);
				if(boundary != null){
					//split dictionary isolating the sorted section
					List<File> chunks = splitDictionary(boundary);

					setProgress(40);

					//sort the chosen section
					File sortSection = chunks.get(1);
					ExternalSorterOptions options = ExternalSorterOptions.builder()
						.charset(CHARSET)
						.comparator(ComparatorBuilder.getComparator(dicParser.getLanguage()))
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
			}
			catch(IOException | IllegalArgumentException e){
				publish(e instanceof ClosedChannelException? "Duplicates thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
			}
			catch(Exception e){
				String message = ExceptionService.getMessage(e, getClass());
				publish(e.getClass().getSimpleName() + ": " + message);
			}
			return null;
		}

		private Map.Entry<Integer, Integer> getBoundary(int lineIndex){
			return Optional.ofNullable(dicParser.boundaries.floorEntry(lineIndex))
				.filter(e -> lineIndex <= e.getValue())
				.orElse(null);
		}

		private List<File> splitDictionary(Map.Entry<Integer, Integer> boundary) throws IOException{
			int index = 0;
			List<File> files = new ArrayList<>();
			File file = File.createTempFile("split", ".out");
			try(BufferedReader br = Files.newBufferedReader(dicParser.dicFile.toPath(), CHARSET)){
				BufferedWriter writer = Files.newBufferedWriter(file.toPath(), CHARSET);
				String line;
				while((line = br.readLine()) != null){
					if(index == boundary.getKey() || index == boundary.getValue() + 1){
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

	public int getBoundaryIndex(int lineIndex) throws IOException{
		calculateDictionaryBoundaries();

		return searchBoundary(lineIndex)
			.map(e -> boundaries.headMap(lineIndex, true).size() - 1)
			.orElse(-1);
	}

	public int getNextBoundaryIndex(int lineIndex) throws IOException{
		return Optional.ofNullable(boundaries.higherEntry(lineIndex))
			.map(Map.Entry::getKey)
			.orElse(-1);
	}

	public int getPreviousBoundaryIndex(int lineIndex) throws IOException{
		return Optional.ofNullable(boundaries.lowerEntry(lineIndex))
			.map(Map.Entry::getKey)
			.orElse(-1);
	}

	public boolean isInBoundary(int lineIndex) throws IOException{
		return searchBoundary(lineIndex)
			.isPresent();
	}

	private Optional<Map.Entry<Integer, Integer>> searchBoundary(int lineIndex) throws IOException{
		return Optional.ofNullable(boundaries.floorEntry(lineIndex))
			.filter(e -> lineIndex <= e.getValue());
	}

	private void calculateDictionaryBoundaries() throws IOException{
		if(boundaries.isEmpty()){
			int lineIndex = 0;
			try(BufferedReader br = Files.newBufferedReader(dicFile.toPath(), CHARSET)){
				String prevLine = null;
				String line;
				int startSection = -1;
				boolean needSorting = false;
				Comparator<String> comparator = ComparatorBuilder.getComparator(getLanguage());
				while((line = br.readLine()) != null){
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


	@AllArgsConstructor
	public static class WordlistWorker extends SwingWorker<Void, String>{

		private final AffixParser affParser;
		private final DictionaryParser dicParser;
		private final File outputFile;
		private final Resultable resultable;


		@Override
		protected Void doInBackground() throws Exception{
			try{
				publish("Opening Dictionary file for wordlist extraction: " + affParser.getLanguage() + ".dic");
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
					long readSoFar = line.length();
					long totalSize = dicParser.dicFile.length();
					while((line = br.readLine()) != null){
						lineIndex ++;
						readSoFar += line.length();

						line = dicParser.cleanLine(line);
						if(!line.isEmpty()){
							DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
							try{
								List<RuleProductionEntry> productions = dicParser.wordGenerator.applyRules(dictionaryWord);

								for(RuleProductionEntry production : productions){
									writer.write(production.getWord());
									writer.newLine();
								}
							}
							catch(IllegalArgumentException e){
								publish(e.getMessage() + " on line " + lineIndex + ": " + dictionaryWord.toWordAndFlagString());
							}
						}

						setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
					}

					setProgress(100);

					publish("File written: " + outputFile.getAbsolutePath());

					publish("Wordlist extracted successfully");

					openFileWithChoosenEditor(outputFile);
				}
			}
			catch(IOException | IllegalArgumentException e){
				publish(e instanceof ClosedChannelException? "Wodlist thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
			}
			catch(Exception e){
				String message = ExceptionService.getMessage(e, getClass());
				publish(e.getClass().getSimpleName() + ": " + message);
			}
			return null;
		}

		@Override
		protected void process(List<String> chunks){
			resultable.printResultLine(chunks);
		}
	};


	@AllArgsConstructor
	public static class MinimalPairsWorker extends SwingWorker<Void, String>{

		private static final int MINIMAL_PAIR_LENGTH = 3;


		private final AffixParser affParser;
		private final DictionaryParser dicParser;
		private final File outputFile;
		private final Resultable resultable;


		@Override
		protected Void doInBackground() throws Exception{
			try{
				publish("Opening Dictionary file for minimal pairs extraction: " + affParser.getLanguage() + ".dic (pass 1/2)");
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
					long readSoFar = line.length();
					long totalSize = dicParser.dicFile.length();
					while((line = br.readLine()) != null){
						lineIndex ++;
						readSoFar += line.length();

						line = dicParser.cleanLine(line);
						if(!line.isEmpty()){
							DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
							try{
								List<RuleProductionEntry> productions = dicParser.wordGenerator.applyRules(dictionaryWord);

								for(RuleProductionEntry production : productions){
									String word = GraphemeVEC.handleJHJWPhonemes(production.getWord());
									writer.write(word);
									writer.newLine();
								}
							}
							catch(IllegalArgumentException e){
								publish(e.getMessage() + " on line " + lineIndex + ": " + dictionaryWord.toWordAndFlagString());
							}
						}

						setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
					}

					publish("File written: " + outputFile.getAbsolutePath());
				}


				//sort file by length first and by alphabet after:
				//TODO by length first
				ExternalSorterOptions options = ExternalSorterOptions.builder()
					.charset(CHARSET)
					.comparator(ComparatorBuilder.COMPARATOR_LENGTH.thenComparing(ComparatorBuilder.getComparator(dicParser.getLanguage())))
					.useZip(true)
					.removeDuplicates(true)
					.build();
				dicParser.sorter.sort(outputFile, options, outputFile);

				setProgress(100);

				publish("File sorted");


				publish("Start extracting minimal pairs (pass 2/2)");
				setProgress(0);

				int totalPairs = 0;
				Path temporaryPath = Files.createTempFile(outputFile.getName(), ".tmp");
				try(
						BufferedReader sourceBR = Files.newBufferedReader(outputFile.toPath(), CHARSET);
						BufferedWriter destinationWriter = Files.newBufferedWriter(temporaryPath, CHARSET);
						){
					String sourceLine;
					long readSoFarSource = 0;
					long totalSizeSource = outputFile.length();
					while((sourceLine = sourceBR.readLine()) != null){
						readSoFarSource += sourceLine.length();

						if(sourceLine.length() >= MINIMAL_PAIR_LENGTH){
							sourceBR.mark((int)(totalSizeSource - readSoFarSource));

							try{
								String sourceLineLowercase = sourceLine.toLowerCase(Locale.ROOT);

								String line2;
								while((line2 = sourceBR.readLine()) != null){
									String line2Lowercase = line2.toLowerCase(Locale.ROOT);

									//calculate distance
									int distance = HammingDistance.getDistance(sourceLineLowercase, line2Lowercase);
									if(distance == 1){
										Pair<Character, Character> difference = HammingDistance.findFirstDifference(sourceLineLowercase, line2Lowercase);
										char left = difference.getLeft();
										char right = difference.getRight();
										if(WordVEC.CONSONANTS.indexOf(left) >= 0 && WordVEC.CONSONANTS.indexOf(right) >= 0 && !GraphemeVEC.isSameGrapheme(left, right)){
											destinationWriter.write(left + ">" + right + ": " + GraphemeVEC.rollbackJHJWPhonemes(sourceLine + ", " + line2));
											destinationWriter.newLine();
System.out.println(left + ">" + right + ": " + GraphemeVEC.rollbackJHJWPhonemes(sourceLine + ", " + line2));
										}
									}
								}
							}
							catch(IllegalArgumentException e){
								//length varied, consider another line for minimal pair search
							}

							sourceBR.reset();
						}

						setProgress((int)((readSoFarSource * 100.) / totalSizeSource));
					}

					setProgress(100);

					publish("Total minimal pairs: " + COUNTER_FORMATTER.format(totalPairs));
				}

				Files.move(temporaryPath, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

				publish("Minimal pairs extracted successfully");

				openFileWithChoosenEditor(outputFile);
			}
			catch(IOException | IllegalArgumentException e){
				publish(e instanceof ClosedChannelException? "Minimal pairs thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
			}
			catch(Exception e){
				String message = ExceptionService.getMessage(e, getClass());
				publish(e.getClass().getSimpleName() + ": " + message);
			}
			return null;
		}

		@Override
		protected void process(List<String> chunks){
			resultable.printResultLine(chunks);
		}
	};


	public String prepareTextForFilter(String text){
		text = StringUtils.strip(text);
		text = PatternService.clear(text, REGEX_FILTER_EMPTY);
		text = PatternService.replaceAll(text, REGEX_FILTER_OR, "|");
		return "(?iu)(" + text + ")";
	}


	private boolean isComment(String line){
		return PatternService.find(line, REGEX_COMMENT);
	}

	/** Removes comment lines and then cleans up blank lines and trailing whitespace. */
	private String cleanLine(String line){
		//remove comments
		line = PatternService.clear(line, REGEX_COMMENT);
		//trim the entire string
		line = StringUtils.strip(line);
		return line;
	}

	//https://stackoverflow.com/questions/526037/how-to-open-user-system-preferred-editor-for-given-file
	private static void openFileWithChoosenEditor(File file) throws InterruptedException, IOException{
		ProcessBuilder builder = null;
		if(SystemUtils.IS_OS_WINDOWS)
			builder = new ProcessBuilder("rundll32.exe", "shell32.dll,OpenAs_RunDLL", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_LINUX)
			builder = new ProcessBuilder("edit", file.getAbsolutePath());
		else if(SystemUtils.IS_OS_MAC)
			builder = new ProcessBuilder("open", file.getAbsolutePath());

		if(builder != null){
			builder.redirectErrorStream();
			builder.redirectOutput();
			Process process = builder.start();
			process.waitFor();
		}
		else
			log.warn("Cannot open file " + file.getName() + ", OS not recognized (" + SystemUtils.OS_NAME + ")");
	}

}
