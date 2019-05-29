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


public class LineEntry implements Serializable{

	private static final long serialVersionUID = 8374397415767767436L;

	private static final String PATTERN_END_OF_WORD = "$";

	private static final String TAB = "\t";


	final Set<String> from;

	final String removal;
	final Set<String> addition;
	String condition;


	public static LineEntry createFrom(final LineEntry entry, final String condition, final Collection<String> words){
		return new LineEntry(entry.removal, entry.addition, condition, words);
	}

	LineEntry(final String removal, final String addition, final String condition, final String word){
		this(removal, new HashSet<>(Arrays.asList(addition)), condition, word);
	}

	LineEntry(final String removal, final String addition, final String condition, final Collection<String> words){
		this(removal, new HashSet<>(Arrays.asList(addition)), condition, words);
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
			.filter(from -> PatternHelper.find(from, conditionPattern))
			.collect(Collectors.toList());
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

	public String sha(){
		return removal + TAB + mergeSet(addition) + TAB + condition;
	}

	private String mergeSet(final Set<String> set){
		return set.stream()
			.sorted()
			.collect(Collectors.joining());
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
