package unit731.hunspeller.parsers.dictionary.dtos;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.Getter;
import unit731.hunspeller.parsers.dictionary.valueobjects.Frequency;


/**
 * @see <a href="https://home.ubalt.edu/ntsbarsh/Business-stat/otherapplets/PoissonTest.htm">Goodness-of-Fit for Poisson</a>
 */
@Getter
public class DictionaryStatistics{

	private static final DecimalFormat PERCENT_FORMATTER = (DecimalFormat)NumberFormat.getInstance(Locale.US);
	static{
		DecimalFormatSymbols symbols = PERCENT_FORMATTER.getDecimalFormatSymbols();
		symbols.setGroupingSeparator(' ');
		PERCENT_FORMATTER.setDecimalFormatSymbols(symbols);
		PERCENT_FORMATTER.setMinimumFractionDigits(1);
		PERCENT_FORMATTER.setMaximumFractionDigits(1);
	}


	private int totalProductions;
//	private final DescriptiveStatistics lengthsStatistics = new DescriptiveStatistics();
//	private final DescriptiveStatistics syllabesStatistics = new DescriptiveStatistics();
	private final Frequency<Integer> lengthsFrequencies = new Frequency();
	private final Frequency<Integer> syllabeLengthsFrequencies = new Frequency();
	private final Frequency<String> syllabesFrequencies = new Frequency();


	public void addLengthAndSyllabeLength(int length, int syllabes){
//		lengthsStatistics.addValue(length);
//		syllabesStatistics.addValue(syllabes);

		lengthsFrequencies.addValue(length);
		syllabeLengthsFrequencies.addValue(syllabes);
		totalProductions ++;
	}

	public void addSyllabes(List<String> syllabes){
		for(String syllabe : syllabes)
			syllabesFrequencies.addValue(syllabe);
	}

	public long getTotalProductions(){
		return totalProductions;
	}

	public List<String> getMostCommonSyllabes(int size){
		int i = 0;
		List<String> response = new ArrayList<>(size);
		Set<String> values = syllabesFrequencies.getMostCommonValues();
		for(String value : values){
			response.add(value + " (" + PERCENT_FORMATTER.format(syllabesFrequencies.getPercentOf(value) * 100) + "%)");
			if(++ i == size)
				break;
		}
		return response;
//		return syllabesFrequencies.getMode().stream()
//			.limit(size)
//			.map(String.class::cast)
//			.collect(Collectors.toList());
	}

	public void clear(){
		lengthsFrequencies.clear();
		syllabeLengthsFrequencies.clear();
	}

}
