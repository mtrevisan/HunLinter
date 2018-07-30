package unit731.hunspeller.parsers.dictionary.workers;

import java.awt.Frame;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.Getter;
import unit731.hunspeller.Backbone;;
import unit731.hunspeller.DictionaryStatisticsDialog;
import unit731.hunspeller.parsers.dictionary.valueobjects.DictionaryStatistics;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;
import unit731.hunspeller.parsers.hyphenation.dtos.Hyphenation;


public class StatisticsWorker extends WorkerDictionaryReadBase{

	@Getter
	private final boolean performHyphenationStatistics;

	private final DictionaryStatistics dicStatistics;


	public StatisticsWorker(Backbone backbone, boolean performHyphenationStatistics, Frame parent){
		Objects.requireNonNull(backbone);
		Objects.requireNonNull(parent);

		this.performHyphenationStatistics = performHyphenationStatistics;

		dicStatistics = new DictionaryStatistics(backbone);


		BiConsumer<String, Integer> body = (line, lineIndex) -> {
			List<RuleProductionEntry> productions = backbone.applyRules(line);

			for(RuleProductionEntry production : productions){
				//collect statistics
				String word = production.getWord();
				if(performHyphenationStatistics){
					List<String> subwords = backbone.splitWordIntoCompounds(word);
					if(subwords.isEmpty())
						dicStatistics.addData(word);
					else
						for(String subword : subwords){
							Hyphenation hyph = backbone.hyphenate(dicStatistics.getOrthography().markDefaultStress(subword));
							dicStatistics.addData(word, hyph);
						}
				}
				else
					dicStatistics.addData(word);
			}
		};
		Runnable done = () -> {
			//show statistics window
			DictionaryStatisticsDialog dialog = new DictionaryStatisticsDialog(dicStatistics, parent);
			dialog.setLocationRelativeTo(parent);
			dialog.setVisible(true);
		};
		createWorker("Statistics", backbone, body, done);
	}

	@Override
	public void execute(){
		dicStatistics.clear();

		super.execute();
	}

}
