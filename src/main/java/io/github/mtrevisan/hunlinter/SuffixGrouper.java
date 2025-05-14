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
		final Path inputFile = Paths.get(SuffixGrouper.class.getClassLoader().getResource("list.txt").toURI());
		final Path outputFile = inputFile.getParent()
			.resolve("words_by_suffix.txt");

		// Legge tutte le righe del file
		final List<String> words = Files.readAllLines(inputFile);
		final Set<String> wordSet = new HashSet<>();
		for(final String word : words)
			if(!word.isEmpty())
				wordSet.add(word);

		//costruzione della mappa dei suffissi
		final Map<String, Set<String>> suffixMap = new HashMap<>();
		for(final String word : words){
			for(int i = 6; i < word.length(); i++){
				final String suffix = word.substring(word.length() - i);
				//aggiunge solo se il suffisso è anche una parola valida nella lista
				if(wordSet.contains(suffix))
					suffixMap.computeIfAbsent(suffix, k -> new HashSet<>()).add(word);
			}
		}

		//filtra solo i suffissi condivisi da almeno 2 parole
		final List<Map.Entry<String, Set<String>>> validSuffixes = new ArrayList<>();
		for(final Map.Entry<String, Set<String>> entry : suffixMap.entrySet())
			if(entry.getValue().size() > 1)
				validSuffixes.add(entry);

		//ordina per lunghezza del suffisso (decrescente), poi per numero di parole (decrescente)
		validSuffixes.sort((a, b) -> {
			final int lenDiff = b.getKey().length() - a.getKey().length();
			return (lenDiff != 0 ? lenDiff : b.getValue().size() - a.getValue().size());
		});

		//scrive l’output nel file
		try(final BufferedWriter writer = Files.newBufferedWriter(outputFile.normalize())){
			for(final Map.Entry<String, Set<String>> entry : validSuffixes){
				final String suffix = entry.getKey();
				final List<String> group = new ArrayList<>(entry.getValue());

				//ordina le parole dalla più corta alla più lunga
				group.sort(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));

				writer.write("Suffix: " + suffix + " (" + group.size() + " words)\n");

				for(final String word : group){
					int prefixLength = word.length() - suffix.length();
					String prefix = word.substring(0, prefixLength);
					writer.write(word + " (" + prefix + ")\n");
				}

				writer.write("\n");
			}
		}

		System.out.println("File creato: " + outputFile.toAbsolutePath());
	}

}
