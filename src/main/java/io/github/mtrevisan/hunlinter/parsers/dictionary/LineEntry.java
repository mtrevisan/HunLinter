/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.parsers.dictionary;

import io.github.mtrevisan.hunlinter.datastructures.SetHelper;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.RegexSequencer;
import io.github.mtrevisan.hunlinter.services.log.ShortPrefixNotNullToStringStyle;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;


public class LineEntry implements Serializable{

	@Serial
	private static final long serialVersionUID = 8374397415767767436L;

	private static final String CANNOT_EXTRACT_GROUP = "Cannot extract group from [{}] at index {} from last because of the presence of the word `{}` that is too short";

	private static final Pattern SPLITTER_ADDITION = RegexHelper.pattern("(?=[/\\t])");

	public static final RegexSequencer SEQUENCER_REGEXP = new RegexSequencer();

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
		final List<String> words = new ArrayList<>(parentRulesFrom.size());
		for(final LineEntry rule : parentRulesFrom)
			words.addAll(rule.extractFromEndingWith(condition));
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

		from = (words != null? new HashSet<>(words): new HashSet<>(0));
	}

	public final List<String> extractFromEndingWith(final String suffix){
		final Pattern conditionPattern = RegexHelper.pattern(suffix + PATTERN_END_OF_WORD);
		final ArrayList<String> list = new ArrayList<>(from.size());
		for(final String word : from)
			if(RegexHelper.find(word, conditionPattern))
				list.add(word);
		list.trimToSize();
		return list;
	}

	public final boolean isProductive(){
		return !from.isEmpty();
	}

	public final String anAddition(){
		return addition.iterator().next();
	}

	public final LineEntry createReverse(){
		final String reversedRemoval = StringUtils.reverse(removal);
		final Set<String> reversedAddition = new HashSet<>(addition.size());
		for(final String add : addition){
			final String[] additions = RegexHelper.split(add, SPLITTER_ADDITION);
			additions[0] = StringUtils.reverse(additions[0]);
			reversedAddition.add(StringUtils.join(additions, StringUtils.EMPTY));
		}
		final String reversedCondition = SEQUENCER_REGEXP
			.toString(SEQUENCER_REGEXP.reverse(RegexSequencer.splitSequence(condition)));
		return new LineEntry(reversedRemoval, reversedAddition, reversedCondition, Collections.emptyList());
	}

	public final boolean isContainedInto(final LineEntry rule){
		final Set<String> parentBones = extractRuleSpine(this);
		final Set<String> childBones = extractRuleSpine(rule);
		return childBones.containsAll(parentBones);
	}

	private Set<String> extractRuleSpine(final LineEntry rule){
		final Set<String> parentBones = new HashSet<>(rule.addition.size());
		for(final String add : rule.addition){
			final int lcsLength = StringHelper.longestCommonPrefix(add, rule.removal)
				.length();
			parentBones.add(rule.removal.substring(lcsLength) + TAB + add.substring(lcsLength));
		}
		return parentBones;
	}

	public final Set<Character> extractGroup(final int indexFromLast){
		return extractGroup(indexFromLast, from);
	}

	public static Set<Character> extractGroup(final int indexFromLast, final Collection<String> words){
		final Set<Character> group = new HashSet<>(words.size());
		for(final String word : words){
			final int index = word.length() - indexFromLast - 1;
			if(index < 0)
				throw new LinterException(CANNOT_EXTRACT_GROUP, StringUtils.join(words, ","), indexFromLast, word);

			group.add(word.charAt(index));
		}
		return group;
	}

	public final void expandConditionToMaxLength(final Comparator<String> comparator){
		final String lcs = StringHelper.longestCommonSuffix(from.toArray(new String[0]));
		if(lcs != null){
			final Set<Character> group = extractGroup(lcs.length());
			final int entryConditionLength = SEQUENCER_REGEXP.length(RegexSequencer.splitSequence(condition));
			if(lcs.length() + (group.isEmpty()? 0: 1) > entryConditionLength)
				condition = RegexHelper.makeGroup(group, comparator) + lcs;
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

	public final String toHunspellRule(final AffixType type, final String flag){
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
	public final String toString(){
		return new ToStringBuilder(this, ShortPrefixNotNullToStringStyle.SHORT_PREFIX_NOT_NULL_STYLE)
			.append("rem", removal)
			.append("add", addition)
			.append("cond", condition)
			.append("from", from)
			.toString();
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final LineEntry rhs = (LineEntry)obj;
		return (removal.equals(rhs.removal)
			&& addition.equals(rhs.addition)
			&& condition.equals(rhs.condition));
	}

	@Override
	public final int hashCode(){
		int result = (removal == null? 0: removal.hashCode());
		result = 31 * result + addition.hashCode();
		result = 31 * result + condition.hashCode();
		return result;
	}


	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

}
