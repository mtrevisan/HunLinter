package unit731.hunspeller.parsers.thesaurus;

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
import unit731.hunspeller.services.PatternHelper;


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
			dictionary.put(synonym.getSynonym(), synonym);
	}

	public boolean add(final String[] partOfSpeeches, final List<String> meanings){
		boolean result = false;
		for(String meaning : meanings){
			final MeaningEntry meaningEntry = extractPartOfSpeechAndMeanings(partOfSpeeches, meanings, meaning);

			meaning = PatternHelper.replaceAll(meaning, PATTERN_PART_OF_SPEECH, StringUtils.EMPTY);
			final ThesaurusEntry foundSynonym = findByMeaning(meaning);
			if(foundSynonym != null)
				//add to meanings if synonym does exists
				foundSynonym.addMeaning(meaningEntry);
			else{
				//add to list if synonym does not exists
				final List<MeaningEntry> entries = new ArrayList<>();
				entries.add(meaningEntry);
				final ThesaurusEntry entry = new ThesaurusEntry(meaning, entries);
				synonyms.add(entry);
				dictionary.put(entry.getSynonym(), entry);

				result = true;
			}
		}

		return result;
	}

	private MeaningEntry extractPartOfSpeechAndMeanings(final String[] partOfSpeeches, final List<String> meanings, final String meaning){
		final StringJoiner sj = new StringJoiner(ThesaurusEntry.PIPE);
		sj.add(Arrays.stream(partOfSpeeches).collect(Collectors.joining(LIST_SEPARATOR, PART_OF_SPEECH_START, PART_OF_SPEECH_END)));
		meanings.stream()
			.filter(m -> !m.equals(meaning))
			.forEachOrdered(sj::add);
		return new MeaningEntry(sj.toString());
	}

	public boolean add(final ThesaurusEntry entry){
		boolean result = false;
		final String synonym = entry.getSynonym();
		if(!dictionary.containsKey(synonym)){
			synonyms.add(entry);
			dictionary.put(synonym, entry);

			result = true;
		}
		return result;
	}

	public ThesaurusEntry remove(final int index){
		final ThesaurusEntry previousValue = synonyms.remove(index);
		dictionary.remove(previousValue.getSynonym());

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

	public void setMeanings(final int index, final String text){
		if(StringUtils.isNotBlank(text)){
			final String[] lines = StringUtils.split(text, StringUtils.LF);
			synonyms.get(index).
				setMeanings(lines);
		}
	}

	private ThesaurusEntry findByMeaning(final String meaning){
		return dictionary.get(meaning);
	}

	/** Extracts a list of synonyms that are duplicated */
	public List<String> extractDuplicatedSynonyms(){
		final Set<String> allItems = new HashSet<>();
		return synonyms.stream()
			.map(ThesaurusEntry::getSynonym)
			.filter(Predicate.not(allItems::add))
			.collect(Collectors.toList());
	}

}
