package unit731.hunspeller.parsers.dictionary.workers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.affix.AffixTag;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryEntry;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


public class RuleReducerWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Rule reducer";


	public RuleReducerWorker(AffixParser affParser, DictionaryParser dicParser, WordGenerator wordGenerator, ReadWriteLockable lockable){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(wordGenerator);

String flag = "&0";
		RuleEntry originalRuleEntry = (RuleEntry)affParser.getData(flag);
		boolean isSuffix = originalRuleEntry.isSuffix();
		FlagParsingStrategy strategy = affParser.getFlagParsingStrategy();
		List<String> aliasesFlag = affParser.getData(AffixTag.ALIASES_FLAG);
		List<String> aliasesMorphologicalField = affParser.getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);
		Set<String> newAffixEntries = new HashSet<>();
		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLineWithAliases(line, strategy, aliasesFlag, aliasesMorphologicalField);
			dicEntry.applyConversionTable(affParser::applyInputConversionTable);

			if(dicEntry.hasContinuationFlag(flag)){
				String word = dicEntry.getWord();
				int wordLength = word.length();
				String lastLetter = word.substring(wordLength - 1);
				List<Production> productions = wordGenerator.applySingleAffixRule(word + "/" + flag);

				productions.forEach(production -> {
					String newAffixEntry;
					if(isSuffix){
						int lastCommonLetter;
						String producedWord = production.getWord();
						for(lastCommonLetter = 0; lastCommonLetter < Math.min(wordLength, producedWord.length()); lastCommonLetter ++)
							if(word.charAt(lastCommonLetter) != producedWord.charAt(lastCommonLetter))
								break;
						String removal = (lastCommonLetter < wordLength? word.substring(lastCommonLetter): AffixEntry.ZERO);
						String addition = producedWord.substring(lastCommonLetter);
						String condition = (lastCommonLetter < wordLength? removal: lastLetter);
						newAffixEntry = composeLine(removal, addition, condition);
					}
					else{
						int firstCommonLetter;
						String producedWord = production.getWord();
						for(firstCommonLetter = 0; firstCommonLetter < Math.min(wordLength, producedWord.length()); firstCommonLetter ++)
							if(word.charAt(firstCommonLetter) == producedWord.charAt(firstCommonLetter))
								break;
						String removal = (firstCommonLetter < wordLength? word.substring(0, firstCommonLetter): AffixEntry.ZERO);
						String addition = producedWord.substring(0, firstCommonLetter);
						String condition = (firstCommonLetter < wordLength? removal: lastLetter);
						newAffixEntry = composeLine(removal, addition, condition);
					}
					newAffixEntries.add(newAffixEntry);
				});
			}
		};
		Runnable completed = () -> {
			//aggregate rules
			Set<String> aggregatedAffixEntries = new HashSet<>();

			List<String> entries = new ArrayList<>(newAffixEntries);
			while(!entries.isEmpty()){
				String affixEntry = entries.get(0);
				String affixEntryCondition = affixEntry.substring(affixEntry.lastIndexOf(' ') + 1);
				entries.remove(0);

				//collect all the entries that have affixEntry as last part of the condition
				Set<String> collisions = new HashSet<>();
				int size = entries.size();
				for(int i = 0; i < size; i ++){
					String targetAffixEntry = entries.get(i);
					if(targetAffixEntry.endsWith(affixEntryCondition))
						collisions.add(targetAffixEntry);
				}

				System.out.println("collisions for " + affixEntry);
				if(!collisions.isEmpty())
					collisions.forEach(System.out::println);
				System.out.println("\n");
				//TODO
				//if every one else does not ends with affixEntry, then accept, otherwise collect all that matches and modify accordingly

//				System.out.println(affixEntry);
			}
		};
		createReadParallelWorkerPreventExceptionRelaunch(WORKER_NAME, dicParser, lineProcessor, completed, null, lockable);
	}

	public static String composeLine(String removal, String addition, String condition){
		StringBuilder sb = new StringBuilder();
		return sb.append(removal)
			.append(StringUtils.SPACE)
			.append(addition)
			.append(StringUtils.SPACE)
			.append(condition)
			.toString();
	}

	public static String composeLine(AffixEntry.Type type, String flag, String removal, String addition, String condition){
		StringBuilder sb = new StringBuilder();
		return sb.append(type.getFlag().getCode())
			.append(StringUtils.SPACE)
			.append(flag)
			.append(StringUtils.SPACE)
			.append(removal)
			.append(StringUtils.SPACE)
			.append(addition)
			.append(StringUtils.SPACE)
			.append(condition)
			.toString();
	}

}
