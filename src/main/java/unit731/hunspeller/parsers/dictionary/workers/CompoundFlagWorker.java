package unit731.hunspeller.parsers.dictionary.workers;

import java.util.ArrayList;
import java.util.HashSet;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.services.regexgenerator.HunspellRegexWordGenerator;


@Slf4j
public class CompoundFlagWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Compound flag extraction";


	private final AffixParser affParser;
	private final long limit;

	private Map<String, String> rules;

	private String compoundFlag;
	private BiConsumer<List<String>, Long> fnDeferring;


	public CompoundFlagWorker(AffixParser affParser, DictionaryParser dicParser, WordGenerator wordGenerator, long limit){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		if(limit <= 0 && limit != HunspellRegexWordGenerator.INFINITY)
			throw new IllegalArgumentException("Limit cannot be non-positive");

		this.affParser = affParser;
		this.limit = limit;

		int compoundMinimumLength = affParser.getCompoundMinimumLength();
		Set<String> originators = new HashSet<>();
		BiConsumer<String, Integer> lineReader = (line, row) -> {
			//collect words that has the compound flag as continuation flag
			List<Production> productions = wordGenerator.applyRules(line);
			for(Production production : productions)
				if(production.getWord().length() >= compoundMinimumLength && production.containsContinuationFlag(compoundFlag))
					originators.add(production.getWord());
		};
		Runnable done = () -> {
			if(!isCancelled())
				if(!originators.isEmpty())
					extract();
		};
		createWorker(WORKER_NAME, dicParser, lineReader, done, affParser);
	}

	private void extract(){
		//TODO
		//generate all the words that matches the given regex
//		List<String> words = regexWordGenerator.generateAll(limit);

		//remove compounds with triples if forbidden
		if(affParser.isForbidTriplesInCompound()){
			//TODO
		}

//		fnDeferring.accept(words, wordTrueCount);
	}

	//https://textmechanic.com/text-tools/combination-permutation-tools/combination-generator/
	public static ArrayList<String> getCombinations(String text){
		ArrayList<String> results = new ArrayList<>();
		for(int i = 0; i < text.length(); i ++){
			//record size as the list will change
			int resultsLength = results.size();
			for(int j = 0; j < resultsLength; j ++)
				results.add(text.charAt(i) + results.get(j));
			results.add(Character.toString(text.charAt(i)));
		}
		return results;
	}

	public void execute(String compoundFlag, BiConsumer<List<String>, Long> fnDeferring){
		clear();

		this.compoundFlag = compoundFlag;
		this.fnDeferring = fnDeferring;

		super.execute();
	}

	@Override
	public void execute(){
		throw new UnsupportedOperationException("Invalid call to execute, call execute(String, BiConsumer<List<String>, Long>) instead");
	}

	public void clear(){
		if(rules != null)
			rules.clear();
		compoundFlag = null;
		fnDeferring = null;
	}

}
