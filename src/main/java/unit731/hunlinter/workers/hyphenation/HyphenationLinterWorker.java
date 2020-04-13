package unit731.hunlinter.workers.hyphenation;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.MessageFormat;

import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;

import unit731.hunlinter.languages.RulesLoader;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;
import unit731.hunlinter.parsers.hyphenation.HyphenatorInterface;
import unit731.hunlinter.workers.exceptions.LinterException;


public class HyphenationLinterWorker extends WorkerDictionary{

	public static final String WORKER_NAME = "Hyphenation linter";

	private static final String SLASH = "/";
	private static final String ASTERISK = "*";

	private static final String POS_NUMERAL_LATIN = MorphologicalTag.PART_OF_SPEECH.attachValue("numeral_latin");
	private static final String POS_UNIT_OF_MEASURE = MorphologicalTag.PART_OF_SPEECH.attachValue("unit_of_measure");

	private static final MessageFormat WORD_IS_NOT_SYLLABABLE = new MessageFormat("Word {0} ({1}) is not syllabable");


	public HyphenationLinterWorker(final String language, final DictionaryParser dicParser, final HyphenatorInterface hyphenator,
			final WordGenerator wordGenerator){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(hyphenator);


		final Orthography orthography = BaseBuilder.getOrthography(language);
		final RulesLoader rulesLoader = new RulesLoader(language, null);

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

			for(final Inflection inflection : inflections){
				final String word = inflection.getWord();
				if(word.length() > 1 && !inflection.hasPartOfSpeech(POS_NUMERAL_LATIN) && !inflection.hasPartOfSpeech(POS_UNIT_OF_MEASURE)
						&& !rulesLoader.containsUnsyllabableWords(word)){
					final Hyphenation hyphenation = hyphenator.hyphenate(word);
					final String[] syllabes = hyphenation.getSyllabes();
					if(orthography.hasSyllabationErrors(syllabes)){
						final String message = WORD_IS_NOT_SYLLABABLE.format(new Object[]{word,
							orthography.formatHyphenation(syllabes, new StringJoiner(SLASH), syllabe -> ASTERISK + syllabe + ASTERISK), indexData.getData()});
						final StringBuffer sb = new StringBuffer(message);
						if(inflection.hasInflectionRules())
							sb.append(" (via ").append(inflection.getRulesSequence()).append(")");
						throw new LinterException(sb.toString());
					}
				}
			}
		};

		final Function<Void, List<IndexDataPair<String>>> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			final Path dicPath = dicParser.getDicFile().toPath();
			final Charset charset = dicParser.getCharset();
			processLines(dicPath, charset, lineProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1);
	}

}
