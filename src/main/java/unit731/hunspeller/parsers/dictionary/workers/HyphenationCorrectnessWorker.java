package unit731.hunspeller.parsers.dictionary.workers;

import java.io.IOException;
import java.text.MessageFormat;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import unit731.hunspeller.languages.RulesLoader;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;


public class HyphenationCorrectnessWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Hyphenation correctness checking";

	private static final String SLASH = "/";
	private static final String ASTERISK = "*";

	private static final String POS_NUMERAL_LATIN = "numeral_latin";
	private static final String POS_UNIT_OF_MEASURE = "unit_of_measure";

	private static final MessageFormat WORD_IS_NOT_SYLLABABLE = new MessageFormat("Word {0} ({1}) is not syllabable");


	public HyphenationCorrectnessWorker(String language, DictionaryParser dicParser, HyphenatorInterface hyphenator, WordGenerator wordGenerator)
			throws IOException{
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(hyphenator);

		RulesLoader rulesLoader = new RulesLoader(language, null);

		BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			List<Production> productions = wordGenerator.applyAffixRules(line);

			productions.forEach(production -> {
				String word = production.getWord();
				if(word.length() > 1 && !production.hasPartOfSpeech(POS_NUMERAL_LATIN) && !production.hasPartOfSpeech(POS_UNIT_OF_MEASURE)
						&& !rulesLoader.containsUnsyllabableWords(word)){
					Hyphenation hyphenation = hyphenator.hyphenate(word);
					if(hyphenation.hasErrors()){
						String message = WORD_IS_NOT_SYLLABABLE.format(new Object[]{word,
							hyphenation.formatHyphenation(new StringJoiner(SLASH), syllabe -> ASTERISK + syllabe + ASTERISK), row});
						StringBuffer sb = new StringBuffer(message);
						if(production.hasProductionRules())
							sb.append(" (via ").append(production.getRulesSequence()).append(")");
						sb.append(", line ").append(row);
						throw new IllegalArgumentException(sb.toString());
					}
				}
			});
		};
		WorkerData data = WorkerData.createParallelPreventExceptionRelaunch(WORKER_NAME, dicParser);
		createReadWorker(data, lineProcessor);
	}

}
