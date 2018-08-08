package unit731.hunspeller.parsers.dictionary.workers;

import java.util.HashMap;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.services.regexgenerator.HunspellRegexWordGenerator;


@Slf4j
public class CompoundRulesWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Compound rules extraction";

	private static final String PIPE = "|";
	private static final String LEFT_PARENTHESIS = "(";
	private static final String RIGHT_PARENTHESIS = ")";


	private final WordGenerator wordGenerator;
	private final long limit;

	private Map<String, String> rules;

	private String compoundRule;
	private BiConsumer<List<String>, Long> fnDeferring;


	public CompoundRulesWorker(AffixParser affParser, DictionaryParser dicParser, WordGenerator wordGenerator, long limit){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		if(limit <= 0 && limit != HunspellRegexWordGenerator.INFINITY)
			throw new IllegalArgumentException("Limit cannot be non-positive");

		this.wordGenerator = wordGenerator;
		this.limit = limit;

		int compoundMinimumLength = affParser.getCompoundMinimumLength();
		Map<String, Set<String>> compounds = new HashMap<>();
		BiConsumer<String, Integer> lineaReader = (line, row) -> {
			//collect words belonging to a compound rule
			List<Production> productions = wordGenerator.applyRules(line);
			for(Production production : productions)
				if(production.getWord().length() >= compoundMinimumLength){
					Map<String, Set<String>> c = production.collectFlagsFromCompound(affParser);
					for(Map.Entry<String, Set<String>> entry: c.entrySet()){
						String affix = entry.getKey();
						Set<String> prods = entry.getValue();

						Set<String> sub = compounds.get(affix);
						if(sub == null)
							compounds.put(affix, prods);
						else
							sub.addAll(prods);
					}
				}
		};
		Runnable done = () -> {
			if(!isCancelled()){
				//extract values for the given compound rule
				rules = compounds.entrySet().stream()
					.filter(entry -> affParser.isManagedByCompoundRule(entry.getKey()))
					.collect(Collectors.toMap(entry -> entry.getKey(), entry -> LEFT_PARENTHESIS + StringUtils.join(entry.getValue(), PIPE) + RIGHT_PARENTHESIS));

				if(!rules.isEmpty())
					extract();
			}
		};
		createWorker(WORKER_NAME, dicParser, lineaReader, done);
	}

	private void extract(){
		//compose compound rule
		FlagParsingStrategy strategy = wordGenerator.getFlagParsingStrategy();
		List<String> compoundRuleComponents = strategy.extractCompoundRule(compoundRule);
		StringBuilder expandedCompoundRule = new StringBuilder();
		for(String component : compoundRuleComponents){
			String flag = strategy.cleanCompoundRuleComponent(component);
			String expandedComponent = rules.get(flag);
			if(expandedComponent == null)
				log.info(Backbone.MARKER_APPLICATION, "Missing word(s) for rule {} in compound rule {}", flag, compoundRule);
			else{
				char lastChar = component.charAt(component.length() - 1);
				if(lastChar == '*' || lastChar == '?')
					expandedComponent += lastChar;

				if(expandedComponent.equals(component))
					log.info(Backbone.MARKER_APPLICATION, "Missing word(s) for rule {} in compound rule {}", flag, compoundRule);
				else
					expandedCompoundRule.append(expandedComponent);
			}
		}

		HunspellRegexWordGenerator regexWordGenerator = new HunspellRegexWordGenerator(expandedCompoundRule.toString());
		long wordCount = regexWordGenerator.wordCount();
		//generate all the words that matches the given regex
		long wordPrintedCount = (wordCount == HunspellRegexWordGenerator.INFINITY? limit: Math.min(wordCount, limit));
		List<String> words = regexWordGenerator.generateAll(wordPrintedCount);

		fnDeferring.accept(words, wordCount);
	}

	public void extractCompounds(String compoundRule, BiConsumer<List<String>, Long> fnDeferring){
		clear();

		this.compoundRule = compoundRule;
		this.fnDeferring = fnDeferring;

		super.execute();
	}

	@Override
	public void execute(){
		throw new UnsupportedOperationException("Invalid call to execute, call extractCompounds(String, BiConsumer<List<String>, Long>) instead");
	}

	public void clear(){
		if(rules != null)
			rules.clear();
		compoundRule = null;
		fnDeferring = null;
	}

}
