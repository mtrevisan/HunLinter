package unit731.hunspeller.parsers.dictionary.workers;

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

	private final BloomFilterInterface<String> bloomFilter;


	public WordCountWorker(Backbone backbone){
		Objects.requireNonNull(backbone);

		bloomFilter = new ScalableInMemoryBloomFilter<>(BitArrayBuilder.Type.FAST, backbone.getExpectedNumberOfDictionaryElements(), backbone.getFalsePositiveDictionaryProbability(), backbone.getGrowRatioWhenDictionaryFull());
		bloomFilter.setCharset(backbone.getCharset());


		BiConsumer<String, Integer> body = (line, lineIndex) -> {
			List<RuleProductionEntry> productions = backbone.applyRules(line);
			for(RuleProductionEntry production : productions)
				bloomFilter.add(production.getWord());
		};
		Runnable done = () -> {
			int totalProductions = bloomFilter.getAddedElements();
			double falsePositiveProbability = bloomFilter.getTrueFalsePositiveProbability();
			int falsePositiveCount = (int)Math.ceil(totalProductions * falsePositiveProbability);
			log.info(Backbone.MARKER_APPLICATION, "Total unique productions: {} ± {} ({})",
				DictionaryParser.COUNTER_FORMATTER.format(totalProductions), DictionaryParser.PERCENT_FORMATTER.format(falsePositiveProbability),
				falsePositiveCount);
		};
		createWorker(backbone, body, done);
	}

	@Override
	public void execute(){
		bloomFilter.clear();

		super.execute();
	}

}
