package unit731.hunspeller.parsers.dictionary.workers;

import java.text.MessageFormat;

import unit731.hunspeller.languages.BaseBuilder;
import unit731.hunspeller.languages.Orthography;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import unit731.hunspeller.languages.RulesLoader;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.hyphenation.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.HyphenatorInterface;


public class HyphenationCorrectnessWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Hyphenation correctness checking";

	private static final String SLASH = "/";
	private static final String ASTERISK = "*";

	private static final String POS_NUMERAL_LATIN = "numeral_latin";
	private static final String POS_UNIT_OF_MEASURE = "unit_of_measure";

	private static final MessageFormat WORD_IS_NOT_SYLLABABLE = new MessageFormat("Word {0} ({1}) is not syllabable");


	public HyphenationCorrectnessWorker(final String language, final DictionaryParser dicParser, final HyphenatorInterface hyphenator,
			final WordGenerator wordGenerator){
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(hyphenator);

		final Orthography orthography = BaseBuilder.getOrthography(language);
		final RulesLoader rulesLoader = new RulesLoader(language, null);

		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final List<Production> productions = wordGenerator.applyAffixRules(line);

			productions.forEach(production -> {
				final String word = production.getWord();
				if(word.length() > 1 && !production.hasPartOfSpeech(POS_NUMERAL_LATIN) && !production.hasPartOfSpeech(POS_UNIT_OF_MEASURE)
						&& !rulesLoader.containsUnsyllabableWords(word)){
					final Hyphenation hyphenation = hyphenator.hyphenate(word);
					final List<String> syllabes = hyphenation.getSyllabes();
					if(orthography.hasSyllabationErrors(syllabes)){
						final String message = WORD_IS_NOT_SYLLABABLE.format(new Object[]{word,
							orthography.formatHyphenation(syllabes, new StringJoiner(SLASH), syllabe -> ASTERISK + syllabe + ASTERISK), row});
						final StringBuffer sb = new StringBuffer(message);
						if(production.hasProductionRules())
							sb.append(" (via ").append(production.getRulesSequence()).append(")");
						sb.append(", line ").append(row);
						throw new IllegalArgumentException(sb.toString());
					}
				}
			});
		};
		final WorkerData data = WorkerData.createParallelPreventExceptionRelaunch(WORKER_NAME, dicParser);
		createReadWorker(data, lineProcessor);
	}

	@Override
	public String getWorkerName(){
		return WORKER_NAME;
	}

}
