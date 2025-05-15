package io.github.mtrevisan.hunlinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class SuffixGrouper{

	public static void main(String[] args) throws IOException, URISyntaxException{
		int minSuffixLength = 5;
		final Path inputFile = Paths.get(SuffixGrouper.class.getClassLoader().getResource("list.txt").toURI());
		final Path outputFile = inputFile.getParent()
			.resolve("words_by_suffix.txt");

		//read all lines of the file
		final List<String> words = Files.readAllLines(inputFile);
		final Set<String> wordSet = new HashSet<>();
		for(final String word : words)
			if(!word.isEmpty())
				wordSet.add(word);

		//construction of the suffix map
		final Map<String, Set<String>> suffixMap = new HashMap<>();
		for(final String word : words){
			for(int i = minSuffixLength; i < word.length(); i ++){
				final String suffix = word.substring(word.length() - i);
				final int prefixLength = word.length() - suffix.length();
				final String prefix = word.substring(0, prefixLength);
				final int compoundIndex = prefix.indexOf("–");
				//adds only if the suffix is also a valid word in the list and the prefix is not a compound word
				boolean compoundedPrefix = (compoundIndex >= 0 && compoundIndex < prefix.length() - 1);
				if(wordSet.contains(suffix) && !compoundedPrefix)
					suffixMap.computeIfAbsent(suffix, k -> new HashSet<>()).add(word);
			}
		}

		//filter only suffixes shared by at least 2 words
		final List<Map.Entry<String, Set<String>>> validSuffixes = new ArrayList<>();
		for(final Map.Entry<String, Set<String>> entry : suffixMap.entrySet())
			if(entry.getValue().size() > 1)
				validSuffixes.add(entry);

		//sort by length of suffix (descending), then by number of words (descending)
		validSuffixes.sort((a, b) -> {
			final int lenDiff = b.getKey().length() - a.getKey().length();
			return (lenDiff != 0 ? lenDiff : b.getValue().size() - a.getValue().size());
		});

		//write output to file
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.normalize())){
			for(int j = 0, validSuffixesSize = validSuffixes.size(); j < validSuffixesSize; j ++){
				Map.Entry<String, Set<String>> entry = validSuffixes.get(j);
				final String suffix = entry.getKey();
				final List<String> group = new ArrayList<>(entry.getValue());

				//sort the words from shortest to longest
				group.sort(Comparator.comparingInt(String::length)
					.thenComparing(Comparator.naturalOrder()));

				writer.write("Suffix: " + suffix + " (" + group.size() + " words)\n");
				for(int i = 0, groupSize = group.size(); i < groupSize; i ++){
					String word = group.get(i);
					final int prefixLength = word.length() - suffix.length();
					String prefix = word.substring(0, prefixLength);
					writer.write(prefix + (i < groupSize - 1 ? "," : ""));
				}
				if(j < validSuffixesSize - 1)
					writer.write("\n\n");
			}
		}

		System.out.println("File created: " + outputFile.toAbsolutePath());
	}

}
