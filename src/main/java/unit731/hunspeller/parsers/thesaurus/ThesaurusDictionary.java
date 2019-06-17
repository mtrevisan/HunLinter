package unit731.hunspeller.parsers.thesaurus;

import unit731.hunspeller.parsers.thesaurus.dtos.ThesaurusEntry;
import unit731.hunspeller.parsers.thesaurus.dtos.MeaningEntry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
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


	@JsonProperty
	private final List<ThesaurusEntry> synonyms = new ArrayList<>();

	@JsonIgnore
	private boolean modified;

	//internal variable for fast inclusion test
	private final Map<String, ThesaurusEntry> dictionary = new HashMap<>();


	public List<ThesaurusEntry> getSynonyms(){
		return synonyms;
	}

	public boolean isModified(){
		return modified;
	}

	public boolean add(final String partOfSpeech, final List<String> meanings){
		boolean result = false;
		for(final String meaning : meanings){
			final MeaningEntry meaningEntry = extractPartOfSpeechAndMeanings(partOfSpeech, meanings, meaning);

			final String mean = PatternHelper.replaceAll(meaning, PATTERN_PART_OF_SPEECH, StringUtils.EMPTY);
			final ThesaurusEntry foundSynonym = findByMeaning(mean);
			if(foundSynonym != null)
				//add to meanings if synonym does exists
				foundSynonym.getMeanings()
					.add(meaningEntry);
			else{
				//add to list if synonym does not exists
				final List<MeaningEntry> entries = new ArrayList<>();
				entries.add(meaningEntry);
				final ThesaurusEntry entry = new ThesaurusEntry(mean, entries);
				result = synonyms.add(entry);
				if(result)
					dictionary.put(entry.getSynonym(), entry);
			}
		}

		modified = true;

		return result;
	}

	private MeaningEntry extractPartOfSpeechAndMeanings(final String partOfSpeech, final List<String> meanings, final String meaning){
		final StringJoiner sj = new StringJoiner(ThesaurusEntry.PIPE);
		sj.add(partOfSpeech);
		meanings.stream()
			.filter(m -> !m.equals(meaning))
			.forEachOrdered(sj::add);
		return new MeaningEntry(sj.toString());
	}

	public boolean add(final ThesaurusEntry entry){
		boolean result = false;
		final String synonym = entry.getSynonym();
		if(!dictionary.containsKey(synonym)){
			result = synonyms.add(entry);
			if(result)
				dictionary.put(synonym, entry);

			modified = true;
		}
		return result;
	}

	public ThesaurusEntry remove(final int index){
		final ThesaurusEntry previousValue = synonyms.remove(index);
		dictionary.remove(previousValue.getSynonym());

		modified = true;

		return previousValue;
	}

	public void restore(final ThesaurusDictionary dictionary){
		clear(true);

		this.synonyms.addAll(dictionary.synonyms);

		this.dictionary.clear();
		dictionary.synonyms
			.forEach(syn -> this.dictionary.put(syn.getSynonym(), syn));
	}

	public void clear(final boolean setModifiedFlag){
		if(!synonyms.isEmpty()){
			synonyms.clear();
			dictionary.clear();

			if(setModifiedFlag)
				modified = true;
		}
	}

	public void resetModified(){
		modified = false;
	}

	public int size(){
		return synonyms.size();
	}

	public void sort(){
		synonyms.sort(Comparator.naturalOrder());
	}

	public void setMeanings(final int index, final List<MeaningEntry> meanings, final String text){
		if(StringUtils.isNotBlank(text)){
			meanings.clear();
			final String[] lines = StringUtils.split(text, StringUtils.LF);
			for(final String line : lines)
				meanings.add(new MeaningEntry(line));
		}

		synonyms.get(index).setMeanings(meanings);

		modified = true;
	}

	private ThesaurusEntry findByMeaning(final String meaning){
		return dictionary.get(meaning);
	}

	public List<String> extractDuplicates(){
		final Set<String> allItems = new HashSet<>();
		return synonyms.stream()
			.map(ThesaurusEntry::getSynonym)
			.filter(Predicate.not(allItems::add))
			.collect(Collectors.toList());
	}

}
