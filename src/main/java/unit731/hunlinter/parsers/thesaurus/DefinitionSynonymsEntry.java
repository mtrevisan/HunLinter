package unit731.hunlinter.parsers.thesaurus;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunlinter.parsers.workers.exceptions.HunLintException;
import unit731.hunlinter.services.StringHelper;


public class DefinitionSynonymsEntry implements Comparable<DefinitionSynonymsEntry>{

	private static final MessageFormat POS_NOT_IN_PARENTHESIS = new MessageFormat("Part of speech is not in parenthesis: ''{0}''");
	private static final MessageFormat NOT_ENOUGH_SYNONYMS = new MessageFormat("Not enough synonyms are supplied (at least one should be present): ''{0}''");
	private static final MessageFormat AIOOB_EXCEPTION = new MessageFormat("{0} with input ''{1}''");


	private String[] partOfSpeeches;
	private List<String> synonyms;


	public DefinitionSynonymsEntry(final String partOfSpeechAndSynonyms){
		Objects.requireNonNull(partOfSpeechAndSynonyms);

		try{
			//all entries should be in lowercase
			final String[] components = StringUtils.split(partOfSpeechAndSynonyms.toLowerCase(Locale.ROOT), ThesaurusEntry.PART_OF_SPEECH_AND_SYNONYMS_SEPARATOR, 2);

			final String partOfSpeech = StringUtils.strip(components[0]);
			if(partOfSpeech.charAt(0) != '(' || partOfSpeech.charAt(partOfSpeech.length() - 1) != ')')
				throw new HunLintException(POS_NOT_IN_PARENTHESIS.format(new Object[]{partOfSpeechAndSynonyms}));

			partOfSpeeches = partOfSpeech.substring(1, partOfSpeech.length() - 1)
				.split(",\\s*");
			synonyms = Arrays.stream(StringUtils.split(components[1], ThesaurusEntry.PART_OF_SPEECH_AND_SYNONYMS_SEPARATOR))
				.map(String::trim)
				.filter(StringUtils::isNotBlank)
				.distinct()
				.collect(Collectors.toList());
			if(synonyms.isEmpty())
				throw new HunLintException(NOT_ENOUGH_SYNONYMS.format(new Object[]{partOfSpeechAndSynonyms}));
		}
		catch(final ArrayIndexOutOfBoundsException e){
			throw new HunLintException(AIOOB_EXCEPTION.format(new Object[]{e.getMessage(), partOfSpeechAndSynonyms}));
		}
	}

	public String[] getPartOfSpeeches(){
		return partOfSpeeches;
	}

	public boolean containsSynonym(final String synonym){
		return synonyms.contains(synonym);
	}

	public boolean containsAllSynonyms(final List<String> partOfSpeeches, final List<String> synonyms){
		return (Arrays.asList(this.partOfSpeeches).containsAll(partOfSpeeches) && this.synonyms.containsAll(synonyms));
	}

	@Override
	public String toString(){
		return (new StringJoiner(ThesaurusEntry.PIPE))
			.add(Arrays.stream(partOfSpeeches).collect(Collectors.joining(", ", "(", ")")))
			.add(StringUtils.join(synonyms, ThesaurusEntry.PIPE))
			.toString();
	}

	@Override
	public int compareTo(final DefinitionSynonymsEntry other){
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

		final DefinitionSynonymsEntry rhs = (DefinitionSynonymsEntry)obj;
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
