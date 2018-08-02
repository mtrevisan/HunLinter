package unit731.hunspeller.parsers.dictionary.workers;

import java.util.Arrays;
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
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.services.regexgenerator.HunspellRegexWordGenerator;


@Slf4j
public class CompoundRulesWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Compound rules extraction";

	private static final String PIPE = "|";
	private static final String LEFT_PARENTHESIS_DOUBLE = "((";
	private static final String RIGHT_PARENTHESIS_DOUBLE = "))";
	private static final String[] PARENTHESIS_DOUBLE = new String[]{LEFT_PARENTHESIS_DOUBLE, RIGHT_PARENTHESIS_DOUBLE};
	private static final String LEFT_PARENTHESIS = "(";
	private static final String RIGHT_PARENTHESIS = ")";
	private static final String[] PARENTHESIS = new String[]{LEFT_PARENTHESIS, RIGHT_PARENTHESIS};

	private Map<String, String> rules;

	private String compoundRule;
	private final long limit;
	private BiConsumer<List<String>, Long> fnDeferring;


	public CompoundRulesWorker(AffixParser affParser, DictionaryParser dicParser, WordGenerator wordGenerator, long limit){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(wordGenerator);
		if(limit <= 0)
			throw new IllegalArgumentException("Limit canno be non-positive");

		this.limit = limit;

		Map<String, Set<String>> compounds = new HashMap<>();
		BiConsumer<String, Integer> body = (line, row) -> {
			//collect words belonging to a compound rule
			List<RuleProductionEntry> productions = wordGenerator.applyRules(line);
			for(RuleProductionEntry production : productions){
				Map<String, Set<String>> c = Arrays.stream(production.getContinuationFlags())
					.filter(affParser::isManagedByCompoundRule)
					.collect(Collectors.groupingBy(flag -> flag, Collectors.mapping(x -> production.getWord(), Collectors.toSet())));

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
		createWorker(WORKER_NAME, dicParser, body, done);
	}

	private void extract(){
		//compose compound rule
		String expandedCompoundRule = StringUtils.replaceEach(compoundRule, rules.keySet().toArray(new String[rules.size()]),
			rules.values().toArray(new String[rules.size()]));
		//FIXME recognize if each rule has been replaced
		if(!expandedCompoundRule.equals(compoundRule))
			expandedCompoundRule = StringUtils.replaceEach(expandedCompoundRule, PARENTHESIS_DOUBLE, PARENTHESIS);

		HunspellRegexWordGenerator regexWordGenerator = new HunspellRegexWordGenerator(expandedCompoundRule);
		long wordCount = regexWordGenerator.wordCount();
		log.info(Backbone.MARKER_APPLICATION, "Total compounds: {}", (wordCount == HunspellRegexWordGenerator.INFINITY? '\u221E': wordCount));
		//generate all the words that matches the given regex
		long wordPrintedCount = (wordCount == HunspellRegexWordGenerator.INFINITY? limit: Math.min(wordCount, limit));
		List<String> words = regexWordGenerator.generateAll(wordPrintedCount);

		fnDeferring.accept(words, wordCount);
	}

	public void extractCompounds(String compoundRule, BiConsumer<List<String>, Long> fnDeferring){
		this.compoundRule = compoundRule;
		this.fnDeferring = fnDeferring;
		clear();

		super.execute();
	}

	@Override
	public void execute(){
		throw new UnsupportedOperationException("Invalid call to execute, call extractCompounds instead");
	}

	public void clear(){
		if(rules != null)
			rules.clear();
	}

}
