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
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;


@Slf4j
public class WordCountWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Word count";

	private final BloomFilterInterface<String> bloomFilter;


	public WordCountWorker(Backbone backbone){
		Objects.requireNonNull(backbone);
		Objects.requireNonNull(backbone.getDicParser());

		DictionaryParser dicParser = backbone.getDicParser();

		bloomFilter = new ScalableInMemoryBloomFilter<>(BitArrayBuilder.Type.FAST,
			dicParser.getExpectedNumberOfElements(), dicParser.getFalsePositiveProbability(), dicParser.getGrowRatioWhenFull());
		bloomFilter.setCharset(dicParser.getCharset());


		BiConsumer<String, Integer> body = (line, row) -> {
			List<RuleProductionEntry> productions = backbone.applyRules(line);
			for(RuleProductionEntry production : productions)
				bloomFilter.add(production.getWord());
		};
		Runnable done = () -> {
			if(!isCancelled()){
				int totalProductions = bloomFilter.getAddedElements();
				double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
				int falsePositiveCount = (int)Math.ceil(totalProductions * falsePositiveProbability);
				log.info(Backbone.MARKER_APPLICATION, "Total unique productions: {} ± {} ({})",
					DictionaryParser.COUNTER_FORMATTER.format(totalProductions), DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability),
					falsePositiveCount);
			}
		};
		createWorker(WORKER_NAME, dicParser, body, done);
	}

	@Override
	public void execute(){
		bloomFilter.clear();

		super.execute();
	}

}
