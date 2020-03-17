package unit731.hunlinter.parsers.thesaurus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.system.LoopHelper;


public class ThesaurusDictionary{

	public static final Pattern PATTERN_SYNONYM_USE = PatternHelper.pattern("\\s*\\([^)]+\\)");

	private static final String LIST_SEPARATOR = ", ";
	private static final String PART_OF_SPEECH_START = "(";
	private static final String PART_OF_SPEECH_END = ")";


	private final Map<String, ThesaurusEntry> dictionary;


	public ThesaurusDictionary(final String language){
		final Comparator<String> comparator = BaseBuilder.getComparator(language);
		//sort the definitions in language-specific order
		dictionary = new TreeMap<>(comparator);
	}

	public boolean add(final ThesaurusEntry entry){
		return (dictionary.put(entry.getDefinition(), entry) == null);
	}

	public boolean add(final String[] partOfSpeeches, final String[] synonyms){
		final StringJoiner sj = new StringJoiner(LIST_SEPARATOR, PART_OF_SPEECH_START, PART_OF_SPEECH_END);
		LoopHelper.forEach(partOfSpeeches, sj::add);
		final String wholePartOfSpeeches = sj.toString();
		final List<String> uniqueSynonyms = new ArrayList<>();
		final Set<String> uniqueValues = new HashSet<>();
		LoopHelper.forEach(synonyms, synonym -> {
			final String s = synonym.toLowerCase(Locale.ROOT);
			if(uniqueValues.add(s))
				uniqueSynonyms.add(s);
		});

		boolean result = false;
		for(String currentDefinition : uniqueSynonyms){
			final SynonymsEntry synonymsEntry = extractPartOfSpeechAndSynonyms(wholePartOfSpeeches, uniqueSynonyms,
				currentDefinition);

			currentDefinition = removeSynonymUse(currentDefinition);
			final ThesaurusEntry foundDefinition = dictionary.get(currentDefinition);
			if(foundDefinition != null)
				//add definition and synonyms if definition does exists
				foundDefinition.addSynonym(synonymsEntry);
			else{
				//add to list if definition doesn't exists
				final ThesaurusEntry entry = ThesaurusEntry.createFromDefinitionAndSynonyms(currentDefinition, synonymsEntry);
				dictionary.put(currentDefinition, entry);

				result = true;
			}
		}

		return result;
	}

	private SynonymsEntry extractPartOfSpeechAndSynonyms(final String partOfSpeeches, final List<String> synonyms,
			final String definition){
		final StringJoiner sj = new StringJoiner(ThesaurusEntry.PIPE);
		sj.add(partOfSpeeches);
		LoopHelper.applyIf(synonyms,
			synonym -> !synonym.equals(definition),
			sj::add);
		return new SynonymsEntry(sj.toString());
	}

	/* Find if there is a duplicate with the same definition and same part–of–speech */
	public boolean contains(final String definition, final String[] partOfSpeeches, final String synonym){
		final ThesaurusEntry def = dictionary.get(definition);
		return (def != null && def.containsPartOfSpeechesAndSynonym(partOfSpeeches, synonym));
	}

	/* Find if there is a duplicate with the same part–of–speech and same synonyms */
	public boolean contains(final String[] partOfSpeeches, final String[] synonyms){
		final List<String> pos = (partOfSpeeches != null? Arrays.asList(partOfSpeeches): null);
		final List<String> syns = Arrays.asList(synonyms);
		return (LoopHelper.match(dictionary.values(), entry -> entry.contains(pos, syns)) != null);
	}

	//FIXME? remove only one entry?
	public void deleteDefinition(final String definition, final String synonyms){
		//recover all words (definition and synonyms) from given definition
		final ThesaurusEntry entryToBeDeleted = dictionary.get(definition);
		final Set<String> definitions = entryToBeDeleted.getSynonymsSet();
		definitions.add(definition);

		//remove all
		dictionary.entrySet()
			.removeIf(entry -> definitions.contains(entry.getKey()));

//		//recover definition and synonyms pairs (to be deleted)
//		final String[] synonymsByDefinition = StringUtils.splitByWholeSeparator(synonyms, ThesaurusTableModel.TAG_NEW_LINE);
//		final List<SynonymsEntry> entries = Arrays.stream(synonymsByDefinition)
//			.map(syns -> new SynonymsEntry(GUIUtils.removeHTMLCode(syns) + ThesaurusEntry.PIPE + definition))
//			.collect(Collectors.toList());
//
//		//delete each occurrence of the definition-synonyms pair
//		dictionary.entrySet()
//			.removeIf(entry -> {
//				final String def = entry.getKey();
//				final List<SynonymsEntry> syns = entry.getValue().getSynonyms().stream()
//					.map(s -> new SynonymsEntry(s + ThesaurusEntry.PIPE + def))
//					.collect(Collectors.toList());
//				return entries.stream()
//					.anyMatch(e -> syns.stream().anyMatch(s -> e.isSame(s)));
//			});
	}

	public List<ThesaurusEntry> getSynonymsDictionary(){
		return new ArrayList<>(dictionary.values());
	}

	public List<ThesaurusEntry> getSortedSynonyms(){
		final List<ThesaurusEntry> synonyms = new ArrayList<>(dictionary.values());
		//need to sort the definitions in natural order
		synonyms.sort((entry1, entry2) -> Comparator.<String>naturalOrder().compare(entry1.getDefinition(), entry2.getDefinition()));
		return synonyms;
	}

	public void clear(){
		dictionary.clear();
	}

	public int size(){
		return dictionary.size();
	}

	/* Find all the entries that have part–of–speech and synonyms contained into the given ones */
	public List<ThesaurusEntry> extractDuplicates(final String[] partOfSpeeches, final String[] synonyms){
		final List<String> pos = Arrays.asList(partOfSpeeches);
		final List<String> syns = Arrays.asList(synonyms);
		final List<ThesaurusEntry> list = new ArrayList<>();
		LoopHelper.applyIf(dictionary.values(),
			entry -> entry.intersects(pos, syns),
			list::add);
		return list;
	}

	public static String removeSynonymUse(final String synonym){
		return PatternHelper.replaceAll(synonym, PATTERN_SYNONYM_USE, StringUtils.EMPTY);
	}

}
