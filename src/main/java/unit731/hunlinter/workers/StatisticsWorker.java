package unit731.hunlinter.workers;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import java.awt.Frame;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import unit731.hunlinter.DictionaryStatisticsDialog;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.dictionary.DictionaryStatistics;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;
import unit731.hunlinter.parsers.hyphenation.HyphenatorInterface;


public class StatisticsWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsWorker.class);

	public static final String WORKER_NAME = "Collecting statistics";

	private final DictionaryStatistics dicStatistics;
	private final HyphenatorInterface hyphenator;
	private final Orthography orthography;


	public StatisticsWorker(final AffixParser affParser, final DictionaryParser dicParser, final HyphenatorInterface hyphenator,
			final WordGenerator wordGenerator, final Frame parent){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

		Objects.requireNonNull(affParser);
		Objects.requireNonNull(wordGenerator);


		final AffixData affixData = affParser.getAffixData();
		final String language = affixData.getLanguage();
		dicStatistics = new DictionaryStatistics(language, affixData.getCharset());
		this.hyphenator = hyphenator;
		orthography = BaseBuilder.getOrthography(language);

		final BiConsumer<Integer, String> lineProcessor = (row, line) -> {
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(line, affixData);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			for(final Production production : productions){
				//collect statistics
				final String word = production.getWord();
				final List<String> subwords = (hyphenator != null? hyphenator.splitIntoCompounds(word): Collections.emptyList());
				if(subwords.isEmpty())
					dicStatistics.addData(word);
				else
					for(final String subword : subwords){
						final Hyphenation hyph = hyphenator.hyphenate(orthography.markDefaultStress(subword));
						dicStatistics.addData(word, hyph);
					}
			}
		};
		final Runnable completed = () -> {
			dicStatistics.close();

			//show statistics window
			final DictionaryStatisticsDialog dialog = new DictionaryStatisticsDialog(dicStatistics, parent);
			GUIUtils.addCancelByEscapeKey(dialog);
			dialog.setLocationRelativeTo(parent);
			dialog.setVisible(true);
		};
		final Consumer<Exception> cancelled = exception -> dicStatistics.close();

		getWorkerData()
			.withDataCompletedCallback(completed)
			.withDataCancelledCallback(cancelled);

		final Function<Void, List<Pair<Integer, String>>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/2)");

			return readLines();
		};
		final Function<List<Pair<Integer, String>>, Void> step2 = lines -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Execute " + workerData.getWorkerName() + " (step 2/2)");

			executeReadProcess(lineProcessor, lines);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

	public boolean isPerformingHyphenationStatistics(){
		return (hyphenator != null);
	}

}
