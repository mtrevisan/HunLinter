package unit731.hunspeller.parsers.thesaurus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


@Getter
public class ThesaurusDictionary{

	private static final Pattern REGEX_LF = PatternService.pattern(StringUtils.LF);
	private static final Matcher REGEX_POS = PatternService.matcher("\\([^)]+\\)");


	@JsonProperty
	private final List<ThesaurusEntry> synonyms = new ArrayList<>();

	@JsonIgnore
	private boolean modified;


	public boolean add(String partOfSpeech, List<String> meanings){
		boolean result = false;
		for(String meaning : meanings){
			StringJoiner sj = new StringJoiner(ThesaurusEntry.PIPE);
			sj.add(partOfSpeech);
			meanings.stream()
				.filter(m -> !m.equals(meaning))
				.forEachOrdered(sj::add);
			String mm = sj.toString();

			String mean = PatternService.replaceAll(meaning, REGEX_POS, StringUtils.EMPTY);
			ThesaurusEntry foundSynonym = findByMeaning(mean);

			MeaningEntry entry = new MeaningEntry(mm);
			if(foundSynonym != null)
				//add to meanings if synonym does exists
				foundSynonym.getMeanings().add(entry);
			else
				//add to list if synonym does not exists
				result = synonyms.add(new ThesaurusEntry(mean, Arrays.asList(entry)));
		}

		modified = true;

		return result;
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
		clear();

		this.synonyms.addAll(dictionary.synonyms);
	}

	public void clear(){
		synonyms.clear();

		modified = true;
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
			String[] lines = PatternService.split(text, REGEX_LF);
			meanings.clear();
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
			.filter(s -> !allItems.add(s))
			.collect(Collectors.toList());
	}

}
