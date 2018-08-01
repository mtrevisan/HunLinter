package unit731.hunspeller.parsers.dictionary.workers;

import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryReadBase;
import java.awt.Frame;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.Getter;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.DictionaryStatisticsDialog;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;
import unit731.hunspeller.parsers.hyphenation.hyphenators.AbstractHyphenator;


public class StatisticsWorker extends WorkerDictionaryReadBase{

	public static final String WORKER_NAME = "Statistics";

	@Getter
	private final boolean performHyphenationStatistics;

	private final DictionaryStatistics dicStatistics;


	public StatisticsWorker(Backbone backbone, boolean performHyphenationStatistics, Frame parent){
		Objects.requireNonNull(backbone);
		Objects.requireNonNull(backbone.getAffParser());
		Objects.requireNonNull(backbone.getDicParser());
		Objects.requireNonNull(backbone.getHyphenator());
		Objects.requireNonNull(parent);

		AffixParser affParser = backbone.getAffParser();
		DictionaryParser dicParser = backbone.getDicParser();
		AbstractHyphenator hyphenator = backbone.getHyphenator();
		this.performHyphenationStatistics = performHyphenationStatistics;

		dicStatistics = new DictionaryStatistics(affParser, dicParser);


		BiConsumer<String, Integer> body = (line, row) -> {
			List<RuleProductionEntry> productions = backbone.applyRules(line);

			for(RuleProductionEntry production : productions){
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
		createWorker(WORKER_NAME, dicParser, body, done);
	}

	@Override
	public void execute(){
		dicStatistics.clear();

		super.execute();
	}

}
