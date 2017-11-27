package unit731.hunspeller.parsers.thesaurus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


@Slf4j
@Getter
public class ThesaurusDictionary{

	private static final Pattern REGEX_PATTERN_LF = PatternService.pattern(StringUtils.LF);


	private final List<ThesaurusEntry> synonyms = new ArrayList<>();

	private boolean modified;


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
			String[] lines = PatternService.split(text, REGEX_PATTERN_LF);
			meanings.clear();
			for(String line : lines)
				meanings.add(new MeaningEntry(line));
		}

		synonyms.get(index).setMeanings(meanings);

		modified = true;
	}

	public ThesaurusEntry findByMeaning(String meaning){
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
		List<String> duplicatedSynonyms = synonyms.stream()
			.map(ThesaurusEntry::getSynonym)
			.filter(s -> !allItems.add(s))
			.collect(Collectors.toList());
		return duplicatedSynonyms;
	}

}
