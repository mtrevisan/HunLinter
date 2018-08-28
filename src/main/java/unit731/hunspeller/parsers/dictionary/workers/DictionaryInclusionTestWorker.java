package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.collections.bloomfilter.BloomFilterInterface;
import unit731.hunspeller.collections.bloomfilter.ScalableInMemoryBloomFilter;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;
import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


@Slf4j
public class DictionaryInclusionTestWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Dictionary inclusion test";

	private final BloomFilterInterface<String> dictionary;


	public DictionaryInclusionTestWorker(DictionaryParser dicParser, WordGenerator wordGenerator, CorrectnessChecker checker, ReadWriteLockable lockable){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(lockable);

		dictionary = new ScalableInMemoryBloomFilter<>(BitArrayBuilder.Type.JAVA,
			checker.getExpectedNumberOfElements(), checker.getFalsePositiveProbability(), checker.getGrowRatioWhenFull());
		dictionary.setCharset(dicParser.getCharset());

		BiConsumer<String, Integer> lineReader = (line, row) -> {
			List<Production> productions = wordGenerator.applyRules(line);

			productions.forEach(production -> dictionary.add(production.getWord()));
		};
		Runnable done = () -> {
			if(!isCancelled()){
				int totalUniqueProductions = dictionary.getAddedElements();
				double falsePositiveProbability = dictionary.getTrueFalsePositiveProbability();
				int falsePositiveCount = (int)Math.ceil(totalUniqueProductions * falsePositiveProbability);
				log.info(Backbone.MARKER_APPLICATION, "Total unique productions: {} ± {} ({})",
					DictionaryParser.COUNTER_FORMATTER.format(totalUniqueProductions), DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability),
					falsePositiveCount);
			}
		};
		createWorker(WORKER_NAME, dicParser, lineReader, done, lockable);
	}

	@Override
	public void execute(){
		dictionary.clear();

		super.execute();
	}

	public boolean isInDictionary(String word){
		return dictionary.contains(word);
	}

}
