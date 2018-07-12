package unit731.hunspeller.parsers.dictionary.dtos;

import lombok.Getter;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


@Getter
public class DictionaryStatistics{

	private int totalProductions;
//	private final DescriptiveStatistics lengthsStatistics = new DescriptiveStatistics();
//	private final DescriptiveStatistics syllabesStatistics = new DescriptiveStatistics();
	private final Frequency lengthsFrequencies = new Frequency();
	private final Frequency syllabesFrequencies = new Frequency();


	public void addLengthAndSyllabes(int length, int syllabes){
//		lengthsStatistics.addValue(length);
//		syllabesStatistics.addValue(syllabes);

		lengthsFrequencies.addValue(length);
		syllabesFrequencies.addValue(syllabes);
		totalProductions ++;
	}

	public long getTotalProductions(){
		return totalProductions;
	}

}
