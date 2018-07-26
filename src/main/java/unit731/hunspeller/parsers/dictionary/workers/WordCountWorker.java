package unit731.hunspeller.parsers.dictionary.workers;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.channels.ClosedChannelException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.TimeWatch;


public class WordCountWorker extends SwingWorker<Void, String>{

	private final AffixParser affParser;
	private final DictionaryParser dicParser;
	private final Resultable resultable;

	private final BloomFilterInterface<String> bloomFilter;


	public WordCountWorker(AffixParser affParser, DictionaryParser dicParser, Resultable resultable){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(resultable);
		if(!(resultable instanceof Frame))
			throw new IllegalArgumentException("The resultable should also be a Frame");

		this.affParser = affParser;
		this.dicParser = dicParser;
		this.resultable = resultable;

		bloomFilter = new ScalableInMemoryBloomFilter<>(BitArrayBuilder.Type.FAST, dicParser.getExpectedNumberOfElements(), dicParser.getFalsePositiveProbability(), dicParser.getGrowRatioWhenFull());
		bloomFilter.setCharset(dicParser.getCharset());
	}

	@Override
	protected Void doInBackground() throws Exception{
		boolean stopped = false;
		bloomFilter.clear();
		try{
			publish("Opening Dictionary file for word count extraction: " + affParser.getLanguage() + ".dic");

			TimeWatch watch = TimeWatch.start();

			FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
			WordGenerator wordGenerator = dicParser.getWordGenerator();

			setProgress(0);
			File dicFile = dicParser.getDicFile();
			long totalSize = dicFile.length();
			try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(dicFile.toPath(), dicParser.getCharset()))){
				String line = br.readLine();
				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1)
					line = FileService.clearBOMMarker(line);
				if(!NumberUtils.isCreatable(line))
					throw new IllegalArgumentException("Dictionary file malformed, the first line is not a number");

				int lineIndex = 1;
				long readSoFar = line.length();
				while(Objects.nonNull(line = br.readLine())){
					lineIndex ++;
					readSoFar += line.length();

					line = DictionaryParser.cleanLine(line);
					if(!line.isEmpty()){
						try{
							DictionaryEntry dictionaryWord = new DictionaryEntry(line, strategy);
							List<RuleProductionEntry> productions = wordGenerator.applyRules(dictionaryWord);
							for(RuleProductionEntry production : productions)
								bloomFilter.add(production.getWord());
						}
						catch(IllegalArgumentException e){
							publish(e.getMessage() + " on line " + lineIndex + ": " + line);
						}
					}

					setProgress((int)Math.ceil((readSoFar * 100.) / totalSize));
				}
			}

			watch.stop();

			setProgress(100);

			publish("Word count extracted successfully (it takes " + watch.toStringMinuteSeconds() + ")");
		}
		catch(IOException | IllegalArgumentException e){
			stopped = true;

			publish(e instanceof ClosedChannelException? "Statistics thread interrupted": e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		catch(Exception e){
			stopped = true;

			String message = ExceptionService.getMessage(e, getClass());
			publish(e.getClass().getSimpleName() + ": " + message);
		}
		if(stopped)
			publish("Stopped reading Dictionary file");

		return null;
	}

	@Override
	protected void process(List<String> chunks){
		resultable.printResultLine(chunks);
	}

	@Override
	protected void done(){
		if(!isCancelled()){
			int totalProductions = bloomFilter.getAddedElements();
			double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
			int falsePositiveCount = (int)Math.ceil(totalProductions * falsePositiveProbability);
			publish("Total unique productions: " + DictionaryParser.COUNTER_FORMATTER.format(totalProductions) + " ± "
				+ DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability) + " (" + falsePositiveCount + ")");
		}
	}

}
