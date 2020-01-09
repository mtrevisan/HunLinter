package unit731.hunlinter.parsers.thesaurus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.PatternHelper;


public class ThesaurusDictionary{

	public static final Pattern PATTERN_PART_OF_SPEECH = PatternHelper.pattern("\\s*\\([^)]+\\)");

	private static final String LIST_SEPARATOR = ", ";
	private static final String PART_OF_SPEECH_START = "(";
	private static final String PART_OF_SPEECH_END = ")";


	private final List<ThesaurusEntry> synonyms = new ArrayList<>();

	//internal variable for fast inclusion test
	private final Map<String, ThesaurusEntry> dictionary = new HashMap<>();


	List<ThesaurusEntry> getSynonyms(){
		return synonyms;
	}

	private void setSynonyms(final List<ThesaurusEntry> synonyms){
		clear();

		this.synonyms.addAll(synonyms);

		this.dictionary.clear();
		for(final ThesaurusEntry synonym : synonyms)
			dictionary.put(synonym.getDefinition(), synonym);
	}

	public boolean add(final String[] partOfSpeeches, final List<String> synonyms){
		boolean result = false;
		for(String synonym : synonyms){
			final DefinitionSynonymsEntry definitionSynonymsEntry = extractPartOfSpeechAndSynonyms(partOfSpeeches, synonyms, synonym);

			synonym = PatternHelper.replaceAll(synonym, PATTERN_PART_OF_SPEECH, StringUtils.EMPTY);
			final ThesaurusEntry foundSynonym = findByDefinition(synonym);
			if(foundSynonym != null)
				//add to synonyms if synonym does exists
				foundSynonym.addSynonym(definitionSynonymsEntry);
			else{
				//add to list if synonym does not exists
				final List<DefinitionSynonymsEntry> entries = new ArrayList<>();
				entries.add(definitionSynonymsEntry);
				final ThesaurusEntry entry = new ThesaurusEntry(synonym, entries);
				this.synonyms.add(entry);
				dictionary.put(entry.getDefinition(), entry);

				result = true;
			}
		}

		return result;
	}

	private DefinitionSynonymsEntry extractPartOfSpeechAndSynonyms(final String[] partOfSpeeches, final List<String> synonyms, final String definition){
		final StringJoiner sj = new StringJoiner(ThesaurusEntry.PIPE);
		sj.add(Arrays.stream(partOfSpeeches).collect(Collectors.joining(LIST_SEPARATOR, PART_OF_SPEECH_START, PART_OF_SPEECH_END)));
		synonyms.stream()
			.filter(synonym -> !synonym.equals(definition))
			.forEachOrdered(sj::add);
		return new DefinitionSynonymsEntry(sj.toString());
	}

	public boolean add(final ThesaurusEntry entry){
		boolean result = false;
		final String synonym = entry.getDefinition();
		if(!dictionary.containsKey(synonym)){
			synonyms.add(entry);
			dictionary.put(synonym, entry);

			result = true;
		}
		return result;
	}

	public ThesaurusEntry remove(final int index){
		final ThesaurusEntry previousValue = synonyms.remove(index);
		dictionary.remove(previousValue.getDefinition());

		return previousValue;
	}

	public void restore(final ThesaurusDictionary dictionary){
		setSynonyms(dictionary.synonyms);
	}

	public void clear(){
		if(!synonyms.isEmpty()){
			synonyms.clear();
			dictionary.clear();
		}
	}

	public int size(){
		return synonyms.size();
	}

	public void sort(){
		synonyms.sort(Comparator.naturalOrder());
	}

	public void setSynonyms(final int index, final String synonyms){
		if(StringUtils.isNotBlank(synonyms)){
			final String[] lines = StringUtils.split(synonyms, StringUtils.LF);
			this.synonyms.get(index).
				setSynonyms(lines);
		}
	}

	private ThesaurusEntry findByDefinition(final String definition){
		return dictionary.get(definition);
	}

	/** Extracts a list of synonyms that are duplicated */
	public List<String> extractDuplicatedSynonyms(){
		final Set<String> allItems = new HashSet<>();
		return synonyms.stream()
			.map(ThesaurusEntry::getDefinition)
			.filter(Predicate.not(allItems::add))
			.collect(Collectors.toList());
	}

}
