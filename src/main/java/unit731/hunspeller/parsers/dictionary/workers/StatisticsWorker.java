package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import java.awt.Frame;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import unit731.hunspeller.DictionaryStatisticsDialog;
import unit731.hunspeller.gui.GUIUtils;
import unit731.hunspeller.parsers.affix.AffixData;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.generators.WordGenerator;
import unit731.hunspeller.parsers.dictionary.vos.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.vos.Production;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerData;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.hyphenators.HyphenatorInterface;


public class StatisticsWorker extends WorkerDictionaryBase{

	public static final String WORKER_NAME = "Collecting statistics";

	private final DictionaryStatistics dicStatistics;
	private final HyphenatorInterface hyphenator;


	public StatisticsWorker(final AffixParser affParser, final DictionaryParser dicParser, final HyphenatorInterface hyphenator,
			final WordGenerator wordGenerator, final Frame parent){
		Objects.requireNonNull(affParser);
		Objects.requireNonNull(hyphenator);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(parent);

		final AffixData affixData = affParser.getAffixData();
		dicStatistics = new DictionaryStatistics(affixData.getLanguage(), affixData.getCharset());
		this.hyphenator = hyphenator;


		final BiConsumer<String, Integer> lineProcessor = (line, row) -> {
			final List<Production> productions = wordGenerator.applyAffixRules(line);

			for(final Production production : productions){
				//collect statistics
				final String word = production.getWord();
				final List<String> subwords = hyphenator.splitIntoCompounds(word);
				if(subwords.isEmpty())
					dicStatistics.addData(word);
				else
					for(final String subword : subwords){
						final Hyphenation hyph = hyphenator.hyphenate(dicStatistics.getOrthography().markDefaultStress(subword));
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
		final Runnable cancelled = dicStatistics::close;
		final WorkerData data = WorkerData.createParallel(WORKER_NAME, dicParser);
		data.setCompletedCallback(completed);
		data.setCancelledCallback(cancelled);
		createReadWorker(data, lineProcessor);
	}

	public boolean isPerformHyphenationStatistics(){
		return (hyphenator != null);
	}

	@Override
	public void clear(){
		dicStatistics.clear();
	}

}
