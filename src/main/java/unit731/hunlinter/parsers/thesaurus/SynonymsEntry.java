package unit731.hunlinter.parsers.thesaurus;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunlinter.datastructures.SetHelper;
import unit731.hunlinter.workers.exceptions.LinterException;

import static unit731.hunlinter.services.system.LoopHelper.match;


public class SynonymsEntry implements Comparable<SynonymsEntry>{

	private static final String COLUMN = ":";
	private static final String COMMA = ",";

	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Wrong format for thesaurus entry: ''{0}''");
	private static final MessageFormat POS_NOT_IN_PARENTHESIS = new MessageFormat("Part-of-speech is not in parenthesis: ''{0}''");
	private static final MessageFormat NOT_ENOUGH_SYNONYMS = new MessageFormat("Not enough synonyms are supplied (at least one should be present): ''{0}''");


	private final String[] partOfSpeeches;
	private final List<String> synonyms = new ArrayList<>();


	public SynonymsEntry(final String partOfSpeechAndSynonyms){
		Objects.requireNonNull(partOfSpeechAndSynonyms);

		//all entries should be in lowercase
		final String[] components = StringUtils.split(partOfSpeechAndSynonyms.toLowerCase(Locale.ROOT),
			ThesaurusEntry.PART_OF_SPEECH_SEPARATOR, 2);
		if(components.length < 2)
			throw new LinterException(WRONG_FORMAT.format(new Object[]{partOfSpeechAndSynonyms}));

		final String partOfSpeech = components[0].trim();
		if(partOfSpeech.charAt(0) == '(' ^ partOfSpeech.charAt(partOfSpeech.length() - 1) == ')')
			throw new LinterException(POS_NOT_IN_PARENTHESIS.format(new Object[]{partOfSpeechAndSynonyms}));

		partOfSpeeches = StringUtils.split(StringUtils.removeEnd(StringUtils.removeStart(partOfSpeech, "("), ")"), ',');
		for(int i = 0; i < partOfSpeeches.length; i ++)
			partOfSpeeches[i] = partOfSpeeches[i].trim();

		final Set<String> uniqueValues = new HashSet<>();
		for(final String synonym : StringUtils.split(components[1], ThesaurusEntry.SYNONYMS_SEPARATOR)){
			final String trim = synonym.trim();
			if(StringUtils.isNotBlank(trim) && uniqueValues.add(trim))
				synonyms.add(trim);
		}
		if(synonyms.isEmpty())
			throw new LinterException(NOT_ENOUGH_SYNONYMS.format(new Object[]{partOfSpeechAndSynonyms}));
	}

	public SynonymsEntry merge(final String definition, final SynonymsEntry entry){
		final SynonymsEntry newEntry = new SynonymsEntry(toString());

		//remove intersection
		newEntry.synonyms.removeIf(entry::containsSynonym);

		//add remaining synonyms
		newEntry.synonyms.addAll(new SynonymsEntry(entry.toLine(definition)).synonyms);

		return newEntry;
	}

	public String[] getPartOfSpeeches(){
		return partOfSpeeches;
	}

	public boolean hasSamePartOfSpeeches(final String[] partOfSpeeches){
		return SetHelper.setOf(this.partOfSpeeches).equals(new HashSet<>(Arrays.asList(partOfSpeeches)));
	}

	public List<String> getSynonyms(){
		return synonyms;
	}

	public boolean containsPartOfSpeech(final List<String> partOfSpeeches){
		return !Collections.disjoint(Arrays.asList(this.partOfSpeeches), partOfSpeeches);
	}

	public boolean containsSynonym(final String synonym){
		return (match(synonyms, s -> ThesaurusDictionary.removeSynonymUse(s).equals(synonym)) != null);
	}

	public boolean contains(final List<String> partOfSpeeches, final List<String> synonyms){
		return ((partOfSpeeches == null || Arrays.asList(this.partOfSpeeches).containsAll(partOfSpeeches))
			&& this.synonyms.containsAll(synonyms));
	}

	public boolean intersects(final List<String> partOfSpeeches, final List<String> synonyms){
		return ((partOfSpeeches == null || !Collections.disjoint(Arrays.asList(this.partOfSpeeches), partOfSpeeches))
			&& !Collections.disjoint(this.synonyms, synonyms));
	}

	@Override
	public String toString(){
		return (new StringJoiner(COLUMN))
			.add(String.join(COMMA, partOfSpeeches))
			.add(StringUtils.join(synonyms, COMMA))
			.toString();
	}

	public String toLine(final String definition){
		final StringJoiner sj = new StringJoiner(", ", "(", ")");
		for(final String partOfSpeech : partOfSpeeches)
			sj.add(partOfSpeech);
		return (new StringJoiner(ThesaurusEntry.PIPE))
			.add(sj.toString())
			.add(StringUtils.join(synonyms, ThesaurusEntry.PIPE))
			.add(definition)
			.toString();
	}

	@Override
	public int compareTo(final SynonymsEntry other){
		return new CompareToBuilder()
			.append(partOfSpeeches, other.partOfSpeeches)
			.append(synonyms, other.synonyms)
			.toComparison();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final SynonymsEntry rhs = (SynonymsEntry)obj;
		return new EqualsBuilder()
			.append(partOfSpeeches, rhs.partOfSpeeches)
			.append(synonyms, rhs.synonyms)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(partOfSpeeches)
			.append(synonyms)
			.toHashCode();
	}

}
