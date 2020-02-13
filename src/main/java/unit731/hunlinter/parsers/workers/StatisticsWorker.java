package unit731.hunlinter.parsers.workers;

import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.parsers.workers.core.WorkerDictionaryBase;
import java.awt.Frame;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import unit731.hunlinter.DictionaryStatisticsDialog;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.dictionary.DictionaryStatistics;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.parsers.workers.core.WorkerDataDictionary;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;
import unit731.hunlinter.parsers.hyphenation.HyphenatorInterface;


public class StatisticsWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Collecting statistics";

	private final DictionaryStatistics dicStatistics;
	private final HyphenatorInterface hyphenator;
	private final Orthography orthography;


	public StatisticsWorker(final AffixParser affParser, final DictionaryParser dicParser, final HyphenatorInterface hyphenator,
			final WordGenerator wordGenerator, final Frame parent){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(parent);

		final AffixData affixData = affParser.getAffixData();
		final String language = affixData.getLanguage();
		dicStatistics = new DictionaryStatistics(language, affixData.getCharset());
		this.hyphenator = hyphenator;
		orthography = BaseBuilder.getOrthography(language);


		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
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
		final WorkerDataDictionary data = WorkerDataDictionary.createParallel(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		data.setCancelledCallback(cancelled);
		createReadWorker(data, lineProcessor);
	}

	public boolean isPerformHyphenationStatistics(){
		return (hyphenator != null);
	}

	@Override
	public String getWorkerName(){
		return WORKER_NAME;
	}

	@Override
	public void clear(){
		dicStatistics.clear();
	}

}
