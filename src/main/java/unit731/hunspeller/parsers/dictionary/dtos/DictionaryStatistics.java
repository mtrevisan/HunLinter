package unit731.hunspeller.parsers.dictionary.dtos;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;


public class DictionaryStatistics{

	private final DescriptiveStatistics lengthsStatistics = new DescriptiveStatistics();
	private final DescriptiveStatistics syllabesStatistics = new DescriptiveStatistics();


	public void addLengthAndSyllabes(int length, int syllabes){
		lengthsStatistics.addValue(length);
		syllabesStatistics.addValue(syllabes);
	}

	public long getTotalProductions(){
		return lengthsStatistics.getN();
	}

}
