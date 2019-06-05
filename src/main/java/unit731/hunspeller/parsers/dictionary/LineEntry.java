package unit731.hunspeller.parsers.dictionary;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.SetHelper;


public class LineEntry implements Serializable{

	private static final long serialVersionUID = 8374397415767767436L;

	private static final String PATTERN_END_OF_WORD = "$";


	final Set<String> from;

	final String removal;
	final Set<String> addition;
	String condition;


	public static LineEntry createFrom(final LineEntry entry, final String condition){
		final List<String> words = entry.extractFromEndingWith(condition);
		return createFromWithWords(entry, condition, words);
	}

	public static LineEntry createFromWithRules(final LineEntry entry, final String condition, final List<LineEntry> parentRulesFrom){
		final List<String> words = parentRulesFrom.stream()
			.flatMap(rule -> rule.extractFromEndingWith(condition).stream())
			.collect(Collectors.toList());
		return createFromWithWords(entry, condition, words);
	}

	public static LineEntry createFromWithWords(final LineEntry entry, final String condition, final List<String> words){
		return new LineEntry(entry.removal, entry.addition, condition, words);
	}

	LineEntry(final String removal, final String addition, final String condition, final String word){
		this(removal, SetHelper.setOf(addition), condition, word);
	}

	LineEntry(final String removal, final String addition, final String condition, final Collection<String> words){
		this(removal, SetHelper.setOf(addition), condition, words);
	}

	LineEntry(final String removal, final Set<String> addition, final String condition, final String word){
		this(removal, addition, condition, Arrays.asList(word));
	}

	LineEntry(final String removal, final Set<String> addition, final String condition, final Collection<String> words){
		this.removal = removal;
		this.addition = addition;
		this.condition = condition;

		from = new HashSet<>();
		if(words != null)
			from.addAll(words);
	}

	public List<LineEntry> split(final AffixEntry.Type type){
		final List<LineEntry> split = new ArrayList<>();
		if(type == AffixEntry.Type.SUFFIX)
			for(final String f : from){
				final int index = f.length() - condition.length() - 1;
				if(index < 0)
					throw new IllegalArgumentException("Cannot reduce rule, should be splitted further because of '" + f + "'");

				split.add(new LineEntry(removal, addition, f.substring(index), f));
			}
		else
			for(final String f : from){
				final int index = condition.length() + 1;
				if(index == f.length())
					throw new IllegalArgumentException("Cannot reduce rule, should be splitted further because of '" + f + "'");

				split.add(new LineEntry(removal, addition, f.substring(0, index), f));
			}
		return split;
	}

	public List<String> extractFromEndingWith(String suffix){
		final Pattern conditionPattern = PatternHelper.pattern(suffix + PATTERN_END_OF_WORD);
		return from.stream()
			.filter(word -> PatternHelper.find(word, conditionPattern))
			.collect(Collectors.toList());
	}

	public boolean isProductive(){
		return !from.isEmpty();
	}

	public String anAddition(){
		return addition.iterator().next();
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
			.append("rem", removal)
			.append("add", addition)
			.append("cond", condition)
			.append("from", from)
			.toString();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(removal)
			.append(addition)
			.append(condition)
			.toHashCode();
	}

	@Override
	public boolean equals(Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final LineEntry other = (LineEntry)obj;
		return new EqualsBuilder()
			.append(removal, other.removal)
			.append(addition, other.addition)
			.append(condition, other.condition)
			.isEquals();
	}

}
