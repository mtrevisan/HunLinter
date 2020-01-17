package unit731.hunlinter.parsers.thesaurus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.JavaHelper;
import unit731.hunlinter.services.PatternHelper;


public class ThesaurusDictionary{

	public static final Pattern PATTERN_SYNONYM_USE = PatternHelper.pattern("\\s*\\([^)]+\\)");

	private static final String LIST_SEPARATOR = ", ";
	private static final String PART_OF_SPEECH_START = "(";
	private static final String PART_OF_SPEECH_END = ")";


	private final Map<String, ThesaurusEntry> dictionary = new HashMap<>();


	public boolean add(final ThesaurusEntry entry){
		boolean result = false;
		final String definition = entry.getDefinition();
		if(!dictionary.containsKey(definition)){
			dictionary.put(definition, entry);

			result = true;
		}
		return result;
	}

	public boolean add(final String[] partOfSpeeches, final String[] synonyms){
		boolean result = false;
		final String wholePartOfSpeeches = Arrays.stream(partOfSpeeches)
			.collect(Collectors.joining(LIST_SEPARATOR, PART_OF_SPEECH_START, PART_OF_SPEECH_END));
		for(String currentDefinition : synonyms){
			final SynonymsEntry synonymsEntry = extractPartOfSpeechAndSynonyms(wholePartOfSpeeches, synonyms, currentDefinition);

			currentDefinition = removeSynonymUse(currentDefinition);
			final ThesaurusEntry foundDefinition = dictionary.get(currentDefinition);
			if(foundDefinition != null)
				//add definition and synonyms if definition does exists
				foundDefinition.addSynonym(synonymsEntry);
			else{
				//add to list if definition does not exists
				final ThesaurusEntry entry = ThesaurusEntry.createFromDefinitionAndSynonyms(currentDefinition, synonymsEntry);
				dictionary.put(currentDefinition, entry);

				result = true;
			}
		}

		return result;
	}

	private SynonymsEntry extractPartOfSpeechAndSynonyms(final String partOfSpeeches, final String[] synonyms, final String definition){
		final StringJoiner sj = new StringJoiner(ThesaurusEntry.PIPE);
		sj.add(partOfSpeeches);
		JavaHelper.nullableToStream(synonyms)
			.filter(synonym -> !synonym.equals(definition))
			.forEachOrdered(sj::add);
		return new SynonymsEntry(sj.toString());
	}

	/** Find if there is a duplicate with the same part of speech and same synonyms */
	public boolean contains(final String[] partOfSpeeches, final String[] synonyms){
		final String[] syns = cleanSynonymsFromUse(synonyms);
		return dictionary.values().stream()
			.anyMatch(entry -> ArrayUtils.contains(syns, entry.getDefinition()) && entry.contains(partOfSpeeches, syns));
	}

	public void deleteDefinition(final String definition){
		//recover all words (definition and synonyms) from given definition
		final ThesaurusEntry entryToBeDeleted = dictionary.get(definition);
		final Set<String> definitions = entryToBeDeleted.getSynonyms();
		definitions.add(definition);

		//remove all
		dictionary.entrySet()
			.removeIf(entry -> definitions.contains(entry.getKey()));
	}

	public List<ThesaurusEntry> getSortedSynonyms(){
		final List<ThesaurusEntry> synonyms = new ArrayList<>(dictionary.values());
		//sort the synonyms
		synonyms.sort(Comparator.naturalOrder());
		return synonyms;
	}

	public void clear(){
		dictionary.clear();
	}

	public int size(){
		return dictionary.size();
	}

	/** Find all the entries that have part of speech and synonyms contained into the given ones */
	public List<ThesaurusEntry> extractDuplicates(final String[] partOfSpeeches, final String[] synonyms){
		final String[] syns = cleanSynonymsFromUse(synonyms);
		return dictionary.values().stream()
			.filter(entry -> ArrayUtils.contains(syns, entry.getDefinition()) && entry.contains(partOfSpeeches, syns))
			.collect(Collectors.toList());
	}

	private String[] cleanSynonymsFromUse(final String[] synonyms){
		return JavaHelper.nullableToStream(synonyms)
			.map(this::removeSynonymUse)
			.toArray(String[]::new);
	}

	private String removeSynonymUse(final String synonym){
		return PatternHelper.replaceAll(synonym, PATTERN_SYNONYM_USE, StringUtils.EMPTY);
	}

}
