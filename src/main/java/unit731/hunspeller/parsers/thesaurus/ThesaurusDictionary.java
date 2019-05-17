package unit731.hunspeller.parsers.thesaurus;

import unit731.hunspeller.parsers.thesaurus.dtos.ThesaurusEntry;
import unit731.hunspeller.parsers.thesaurus.dtos.MeaningEntry;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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


	public List<ThesaurusEntry> getSynonyms(){
		return synonyms;
	}

	public boolean isModified(){
		return modified;
	}

	public boolean add(String partOfSpeech, List<String> meanings){
		boolean result = false;
		for(String meaning : meanings){
			MeaningEntry entry = extractPartOfSpeechAndMeanings(partOfSpeech, meanings, meaning);

			String mean = PatternHelper.replaceAll(meaning, PATTERN_PART_OF_SPEECH, StringUtils.EMPTY);
			ThesaurusEntry foundSynonym = findByMeaning(mean);
			if(foundSynonym != null)
				//add to meanings if synonym does exists
				foundSynonym.getMeanings()
					.add(entry);
			else{
				//add to list if synonym does not exists
				List<MeaningEntry> entries = new ArrayList<>();
				entries.add(entry);
				result = synonyms.add(new ThesaurusEntry(mean, entries));
			}
		}

		modified = true;

		return result;
	}

	private MeaningEntry extractPartOfSpeechAndMeanings(String partOfSpeech, List<String> meanings, String meaning){
		StringJoiner sj = new StringJoiner(ThesaurusEntry.PIPE);
		sj.add(partOfSpeech);
		meanings.stream()
			.filter(m -> !m.equals(meaning))
			.forEachOrdered(sj::add);
		return new MeaningEntry(sj.toString());
	}

	public boolean add(ThesaurusEntry entry){
		boolean result = synonyms.add(entry);

		modified = true;

		return result;
	}

	public ThesaurusEntry remove(int index){
		ThesaurusEntry previousValue = synonyms.remove(index);

		modified = true;

		return previousValue;
	}

	public void restore(ThesaurusDictionary dictionary){
		clear(true);

		this.synonyms.addAll(dictionary.synonyms);
	}

	public void clear(boolean setModifiedFlag){
		if(!synonyms.isEmpty()){
			synonyms.clear();

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
		synonyms.sort((s0, s1) -> s0.compareTo(s1));
	}

	public void setMeanings(int index, List<MeaningEntry> meanings, String text){
		if(StringUtils.isNotBlank(text)){
			meanings.clear();
			String[] lines = StringUtils.split(text, StringUtils.LF);
			for(String line : lines)
				meanings.add(new MeaningEntry(line));
		}

		synonyms.get(index).setMeanings(meanings);

		modified = true;
	}

	private ThesaurusEntry findByMeaning(String meaning){
		ThesaurusEntry foundSynonym = null;
		for(ThesaurusEntry synonym : synonyms)
			if(synonym.getSynonym().equals(meaning)){
				foundSynonym = synonym;
				break;
			}
		return foundSynonym;
	}

	public List<String> extractDuplicates(){
		Set<String> allItems = new HashSet<>();
		return synonyms.stream()
			.map(ThesaurusEntry::getSynonym)
			.filter(Predicate.not(allItems::add))
			.collect(Collectors.toList());
	}

}
