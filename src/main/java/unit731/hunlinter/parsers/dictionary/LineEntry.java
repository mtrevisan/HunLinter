package unit731.hunlinter.parsers.dictionary;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import unit731.hunlinter.services.system.LoopHelper;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.RegExpSequencer;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.SetHelper;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.services.log.ShortPrefixNotNullToStringStyle;


public class LineEntry implements Serializable{

	private static final long serialVersionUID = 8374397415767767436L;

	private static final MessageFormat CANNOT_EXTRACT_GROUP = new MessageFormat("Cannot extract group from [{0}] at index {1} from last because of the presence of the word ''{2}'' that is too short");

	private static final Pattern SPLITTER_ADDITION = PatternHelper.pattern("(?=[/\\t])");

	public static final RegExpSequencer SEQUENCER_REGEXP = new RegExpSequencer();

	private static final String PATTERN_END_OF_WORD = "$";
	private static final String TAB = "\t";
	private static final String DOT = ".";


	final Set<String> from;

	final String removal;
	final Set<String> addition;
	String condition;


	public static LineEntry createFrom(final LineEntry entry, final String condition){
		final List<String> words = entry.extractFromEndingWith(condition);
		return createFromWithWords(entry, condition, words);
	}

	public static LineEntry createFromWithRules(final LineEntry entry, final String condition, final List<LineEntry> parentRulesFrom){
		final List<String> words = new ArrayList<>();
		for(final LineEntry rule : parentRulesFrom)
			for(final String s : rule.extractFromEndingWith(condition))
				words.add(s);
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
		this(removal, addition, condition, Collections.singletonList(word));
	}

	LineEntry(final String removal, final Set<String> addition, final String condition, final Collection<String> words){
		this.removal = removal;
		this.addition = addition;
		this.condition = condition;

		from = new HashSet<>();
		if(words != null)
			from.addAll(words);
	}

	public List<String> extractFromEndingWith(final String suffix){
		final Pattern conditionPattern = PatternHelper.pattern(suffix + PATTERN_END_OF_WORD);
		final List<String> list = new ArrayList<>();
		LoopHelper.applyIf(from,
			word -> PatternHelper.find(word, conditionPattern),
			list::add);
		return list;
	}

	public boolean isProductive(){
		return !from.isEmpty();
	}

	public String anAddition(){
		return addition.iterator().next();
	}

	public LineEntry createReverse(){
		final String reversedRemoval = StringUtils.reverse(removal);
		final Set<String> reversedAddition = new HashSet<>(addition.size());
		LoopHelper.forEach(addition, add -> {
			final String[] additions = PatternHelper.split(add, SPLITTER_ADDITION);
			additions[0] = StringUtils.reverse(additions[0]);
			reversedAddition.add(StringUtils.join(additions, StringUtils.EMPTY));
		});
		final String reversedCondition = LineEntry.SEQUENCER_REGEXP
			.toString(LineEntry.SEQUENCER_REGEXP.reverse(RegExpSequencer.splitSequence(condition)));
		return new LineEntry(reversedRemoval, reversedAddition, reversedCondition, Collections.emptyList());
	}

	public boolean isContainedInto(final LineEntry rule){
		final Set<String> parentBones = extractRuleSpine(this);
		final Set<String> childBones = extractRuleSpine(rule);
		return childBones.containsAll(parentBones);
	}

	private Set<String> extractRuleSpine(final LineEntry rule){
		final Set<String> parentBones = new HashSet<>();
		for(final String add : rule.addition){
			final int lcsLength = StringHelper.longestCommonPrefix(Arrays.asList(add, rule.removal))
				.length();
			parentBones.add(rule.removal.substring(lcsLength) + TAB + add.substring(lcsLength));
		}
		return parentBones;
	}

	public Set<Character> extractGroup(final int indexFromLast){
		return extractGroup(indexFromLast, from);
	}

	public static Set<Character> extractGroup(final int indexFromLast, final Set<String> words){
		final Set<Character> group = new HashSet<>();
		for(final String word : words){
			final int index = word.length() - indexFromLast - 1;
			if(index < 0)
				throw new LinterException(CANNOT_EXTRACT_GROUP.format(new Object[]{StringUtils.join(words, ","),
					indexFromLast, word}));

			group.add(word.charAt(index));
		}
		return group;
	}

	public void expandConditionToMaxLength(final Comparator<String> comparator){
		final String lcs = StringHelper.longestCommonSuffix(from);
		if(lcs != null){
			final Set<Character> group = extractGroup(lcs.length());
			final int entryConditionLength = SEQUENCER_REGEXP.length(RegExpSequencer.splitSequence(condition));
			if(lcs.length() + (group.isEmpty()? 0: 1) > entryConditionLength)
				condition = PatternHelper.makeGroup(group, comparator) + lcs;
		}
	}

	public static String toHunspellHeader(final AffixType type, final String flag, final char combinableChar, final int size){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		return sj.add(type.getOption().getCode())
			.add(flag)
			.add(String.valueOf(combinableChar))
			.add(Integer.toString(size))
			.toString();
	}

	public String toHunspellRule(final AffixType type, final String flag){
		String anAddition = anAddition();
		String morphologicalRules = StringUtils.EMPTY;
		final int idx = anAddition.indexOf(TAB);
		if(idx >= 0){
			morphologicalRules = anAddition.substring(idx);
			anAddition = anAddition.substring(0, idx);
		}
		final String line = type.getOption().getCode() + StringUtils.SPACE + flag + StringUtils.SPACE + removal + StringUtils.SPACE
			+ anAddition + StringUtils.SPACE + (condition.isEmpty()? DOT: condition);
		return (idx >= 0? line + morphologicalRules: line);
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this, ShortPrefixNotNullToStringStyle.SHORT_PREFIX_NOT_NULL_STYLE)
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
