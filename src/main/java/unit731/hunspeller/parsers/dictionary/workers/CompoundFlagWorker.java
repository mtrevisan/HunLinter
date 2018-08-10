package unit731.hunspeller.parsers.dictionary.workers;

import java.util.ArrayList;
import java.util.HashMap;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;


@Slf4j
public class CompoundFlagWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Compound flag extraction";


	private final Map<String, String> inputs = new HashMap<>();


	public CompoundFlagWorker(AffixParser affParser, DictionaryParser dicParser, WordGenerator wordGenerator, BiConsumer<Production, Integer> productionReader, Runnable done){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);

		BiConsumer<String, Integer> lineReader = (line, row) -> {
			List<Production> productions = wordGenerator.applyRules(line);
			for(Production production : productions)
				productionReader.accept(production, row);
		};
		createWorker(WORKER_NAME, dicParser, lineReader, done, affParser);
	}

	@Override
	public void execute(){
		inputs.clear();

		super.execute();
	}

//	public CompoundFlagWorker(AffixParser affParser, DictionaryParser dicParser, WordGenerator wordGenerator, long limit){
//		Objects.requireNonNull(affParser);
//		Objects.requireNonNull(dicParser);
//		Objects.requireNonNull(wordGenerator);
//		if(limit <= 0 && limit != HunspellRegexWordGenerator.INFINITY)
//			throw new IllegalArgumentException("Limit cannot be non-positive");
//
//		this.affParser = affParser;
//		this.limit = limit;
//
//		int compoundMinimumLength = affParser.getCompoundMinimumLength();
//		Set<String> originators = new HashSet<>();
//		BiConsumer<String, Integer> lineReader = (line, row) -> {
//			//collect words that has the compound flag as continuation flag
//			List<Production> productions = wordGenerator.applyRules(line);
//			for(Production production : productions)
//				if(production.getWord().length() >= compoundMinimumLength && production.containsContinuationFlag(compoundFlag))
//					originators.add(production.getWord());
//		};
//		Runnable done = () -> {
//			if(!isCancelled())
//				if(!originators.isEmpty())
//					extract();
//		};
//		createWorker(WORKER_NAME, dicParser, lineReader, done, affParser);
//	}

	private void extract(){
		//TODO
		//generate all the words that matches the given regex
//		List<String> words = regexWordGenerator.generateAll(limit);

		//remove compounds with triples if forbidden
//		if(affParser.isForbidTriplesInCompound()){
//			//TODO
//		}

//		fnDeferring.accept(words, wordTrueCount);
	}

//	public void execute(String compoundFlag, BiConsumer<List<String>, Long> fnDeferring){
//		clear();
//
//		this.compoundFlag = compoundFlag;
//		this.fnDeferring = fnDeferring;
//
//		super.execute();
//	}
//
//	@Override
//	public void execute(){
//		throw new UnsupportedOperationException("Invalid call to execute, call execute(String, BiConsumer<List<String>, Long>) instead");
//	}
//
//	public void clear(){
//		if(rules != null)
//			rules.clear();
//		compoundFlag = null;
//		fnDeferring = null;
//	}

}
