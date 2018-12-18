package unit731.hunspeller.parsers.dictionary.workers;

import java.util.HashSet;
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
						newAffixEntry = composeLine(AffixEntry.Type.SUFFIX, flag, removal, addition, condition);
					}
					else{
						//TODO
						newAffixEntry = StringUtils.EMPTY;
					}
					newAffixEntries.add(newAffixEntry);
				});
			}
		};
		Runnable completed = () -> {
			newAffixEntries.forEach(System.out::println);
		};
		createReadParallelWorkerPreventExceptionRelaunch(WORKER_NAME, dicParser, lineProcessor, completed, null, lockable);
	}

	public static void main(String[] args){
		String flag = "&0";
		String word = "mano";
		String producedWord = "maneta";
		boolean isSuffix = true;

		int lastCommonLetter;
		for(lastCommonLetter = 0; lastCommonLetter < Math.min(word.length(), producedWord.length()); lastCommonLetter ++)
			if(word.charAt(lastCommonLetter) != producedWord.charAt(lastCommonLetter))
				break;
		String removal = (lastCommonLetter < word.length()? word.substring(lastCommonLetter): AffixEntry.ZERO);
		String addition = producedWord.substring(lastCommonLetter);
		String condition = (lastCommonLetter < word.length()? removal: AffixEntry.DOT);
		String newAffixEntry = composeLine((isSuffix? AffixEntry.Type.SUFFIX: AffixEntry.Type.PREFIX), flag, removal, addition, condition);

		System.out.println(newAffixEntry);
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
