package unit731.hunspeller.parsers.affix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;


public class ConversionTable{

	private final List<Pair<String, String>> table;


	public ConversionTable(List<Pair<String, String>> table){
		this.table = table;
	}

	public String applyConversionTable(String word){
		if(table != null){
			//collect input patterns that matches the given word
			List<Pair<String, String>> appliablePatterns = collectInputPatterns(table, word);

			for(Pair<String, String> entry : appliablePatterns){
				String key = entry.getKey();
				String value = entry.getValue();

				if(key.charAt(0) == '^')
					word = value + word.substring(key.length() - 1);
				else if(key.charAt(key.length() - 1) == '$')
					word = word.substring(0, word.length() - key.length() + 1) + value;
				else
					word = StringUtils.replace(word, key, value);
			}
		}
		return word;
	}

	private List<Pair<String, String>> collectInputPatterns(List<Pair<String, String>> table, String word){
		List<Pair<String, String>> startPatterns = new ArrayList<>();
		List<Pair<String, String>> insidePatterns = new ArrayList<>();
		List<Pair<String, String>> endPatterns = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		for(Pair<String, String> entry : table){
			String key = entry.getKey();
			String value = entry.getValue();
			
			if(key.charAt(0) == '^'){
				key = key.substring(1);
				if(word.startsWith(key))
					startPatterns.add(Pair.of(key, value));
			}
			else if(key.charAt(key.length() - 1) == '$'){
				key = key.substring(0, key.length() - 1);
				if(word.endsWith(key)){
					sb.setLength(0);
					key = sb.append(key)
						.reverse()
						.toString();
					endPatterns.add(Pair.of(key, value));
				}
			}
			else if(word.contains(key))
				insidePatterns.add(Pair.of(key, value));
		}

		//keep only the longest input pattern
		startPatterns = keepLongestInputPattern(startPatterns, key -> {
			sb.setLength(0);
			return sb.append('^').append(key).toString();
		});
		insidePatterns = keepLongestInputPattern(insidePatterns, Function.identity());
		endPatterns = keepLongestInputPattern(endPatterns, key -> {
			sb.setLength(0);
			return sb.append(key).reverse().append('$').toString();
		});

		startPatterns.addAll(insidePatterns);
		startPatterns.addAll(endPatterns);
		return startPatterns;
	}

	private List<Pair<String, String>> keepLongestInputPattern(List<Pair<String, String>> table, Function<String, String> keyRemapper){
		List<Pair<String, String>> result = table;
		if(!table.isEmpty()){
			table.sort(Comparator.comparing(entry -> entry.getKey().length()));

			int size = table.size();
			for(int i = 0; i < size; i ++){
				Pair<String, String> entry = table.get(i);
				if(entry != null){
					String key = entry.getKey();
					for(int j = i + 1; j < size; j ++){
						Pair<String, String> entry2 = table.get(j);
						if(entry2 != null){
							String key2 = entry2.getKey();
							if(key2.startsWith(key))
								table.set(i, null);
						}
					}
				}
			}

			result = table.stream()
				.filter(Objects::nonNull)
				.map(entry -> Pair.of(keyRemapper.apply(entry.getKey()), entry.getValue()))
				.collect(Collectors.toList());
		}
		return result;
	}

}
