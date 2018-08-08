package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.awt.Frame;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.Getter;
import unit731.hunspeller.DictionaryStatisticsDialog;
import unit731.hunspeller.languages.CorrectnessChecker;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.WordGenerator;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.valueobjects.Production;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;


public class StatisticsWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Statistics";

	@Getter
	private final boolean performHyphenationStatistics;

	private final DictionaryStatistics dicStatistics;


	public StatisticsWorker(AffixParser affParser, DictionaryParser dicParser, AbstractHyphenator hyphenator, WordGenerator wordGenerator,
			CorrectnessChecker checker, boolean performHyphenationStatistics, Frame parent){
		Objects.requireNonNull(dicParser);
		Objects.requireNonNull(hyphenator);
		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(checker);
		Objects.requireNonNull(parent);

		this.performHyphenationStatistics = performHyphenationStatistics;

		dicStatistics = new DictionaryStatistics(affParser.getLanguage(), affParser.getCharset(), checker);


		BiConsumer<String, Integer> lineaReader = (line, row) -> {
			List<Production> productions = wordGenerator.applyRules(line);

			for(Production production : productions){
				//collect statistics
				String word = production.getWord();
				if(performHyphenationStatistics){
					List<String> subwords = hyphenator.splitIntoCompounds(word);
					if(subwords.isEmpty())
						dicStatistics.addData(word);
					else
						for(String subword : subwords){
							Hyphenation hyph = hyphenator.hyphenate(dicStatistics.getOrthography().markDefaultStress(subword));
							dicStatistics.addData(word, hyph);
						}
				}
				else
					dicStatistics.addData(word);
			}
		};
		Runnable done = () -> {
			if(!isCancelled()){
				//show statistics window
				DictionaryStatisticsDialog dialog = new DictionaryStatisticsDialog(dicStatistics, parent);
				dialog.setLocationRelativeTo(parent);
				dialog.setVisible(true);
			}
		};
		createWorker(WORKER_NAME, dicParser, lineaReader, done);
	}

	@Override
	public void execute(){
		dicStatistics.clear();

		super.execute();
	}

}
