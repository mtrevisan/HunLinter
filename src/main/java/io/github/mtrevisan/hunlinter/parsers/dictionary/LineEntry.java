/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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

import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.RegexSequencer;
import io.github.mtrevisan.hunlinter.services.log.ShortPrefixNotNullToStringStyle;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;


public class LineEntry implements Serializable{

	@Serial
	private static final long serialVersionUID = 8374397415767767436L;

	private static final String CANNOT_EXTRACT_GROUP = "Cannot extract group from [{}] at index {} from last because of the presence of the word `{}` that is too short";

	private static final Pattern SPLITTER_ADDITION = RegexHelper.pattern("(?=[/\\t])");

	private static final String PATTERN_END_OF_WORD = "$";
	private static final String TAB = "\t";
	private static final String DOT = ".";
	private static final String ZERO = "0";


	final Set<String> from;

	String removal;
	final Set<String> addition;
	String condition;


	public static LineEntry createFrom(final LineEntry entry, final String condition){
		final Set<String> words = entry.extractFromEndingWith(condition);
		return createFromWithWords(entry, condition, words);
	}

	public static LineEntry createFromWithWords(final LineEntry entry, final String condition,
			final Collection<String> words){
		return new LineEntry(entry.removal, entry.addition, condition, words);
	}

	LineEntry(final String removal, final String addition, final String condition, final String word){
		this(removal, new HashSet<>(Arrays.asList(addition)), condition, word);
	}

	LineEntry(final String removal, final String addition, final String condition, final Collection<String> words){
		this(removal, new HashSet<>(Arrays.asList(addition)), condition, words);
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

	final Set<String> extractFromEndingWith(final String condition){
		final Pattern conditionPattern = RegexHelper.pattern(condition + PATTERN_END_OF_WORD);
		return extractFromEndingWith(conditionPattern);
	}

	Set<String> extractFromEndingWith(final Pattern conditionPattern){
		final Set<String> list = new HashSet<>(from.size());
		for(final String word : from)
			if(RegexHelper.find(word, conditionPattern))
				list.add(word);
		return list;
	}

	public final boolean isProductive(){
		return !from.isEmpty();
	}

	public final String firstAddition(){
		return addition.iterator()
			.next();
	}

	public final LineEntry reverse(){
		final String reversedRemoval = StringUtils.reverse(removal);
		final Collection<String> reversedAddition = new HashSet<>(addition.size());
		final Collection<String> reversedFrom = new HashSet<>(from.size());
		final StringBuilder sb = new StringBuilder();
		for(final String add : addition){
			final String[] additions = RegexHelper.split(add, SPLITTER_ADDITION);
			sb.setLength(0);
			sb.append(additions[0]);
			sb.reverse();
			for(int i = 1; i < additions.length; i ++)
				sb.append(additions[i]);

			reversedAddition.add(sb.toString());
		}
		final String reversedCondition = RegexSequencer
			.toString(RegexSequencer.reverse(RegexSequencer.splitSequence(condition)));
		for(final String f : from)
			reversedFrom.add(StringUtils.reverse(f));

		removal = reversedRemoval;
		addition.clear();
		addition.addAll(reversedAddition);
		condition = reversedCondition;
		from.clear();
		from.addAll(reversedFrom);

		return this;
	}

	public final boolean isContainedInto(final LineEntry rule){
		final Set<String> parentBones = extractRuleSpine(this);
		final Set<String> childBones = extractRuleSpine(rule);
		return childBones.containsAll(parentBones);
	}

	private static Set<String> extractRuleSpine(final LineEntry rule){
		final Set<String> parentBones = new HashSet<>(rule.addition.size());
		for(final String add : rule.addition){
			final int lcsLength = StringHelper.longestCommonPrefix(add, rule.removal)
				.length();
			parentBones.add(rule.removal.substring(lcsLength) + TAB + add.substring(lcsLength));
		}
		return parentBones;
	}

//	public final char[] extractGroup(final int indexFromLast){
//		return extractGroup(indexFromLast, from);
//	}

	public final Set<Character> extractGroup(final int indexFromLast){
		return extractGroup(indexFromLast, from);
	}

//	public static char[] extractGroup(final int indexFromLast, final Collection<String> words){
//		final char[] group = new char[words.size()];
//		int i = 0;
//		for(final String word : words){
//			final int index = word.length() - indexFromLast - 1;
//			if(index < 0)
//				throw new LinterException(CANNOT_EXTRACT_GROUP, StringUtils.join(words, ","), indexFromLast, word);
//
//			group[i ++] = word.charAt(index);
//		}
//		return group;
//	}

	public static Set<Character> extractGroup(final int indexFromLast, final Collection<String> words){
		final Set<Character> group = new HashSet<>(words.size());
		for(final String word : words){
			final int index = word.length() - indexFromLast - 1;
			if(index < 0)
//				throw new LinterException(CANNOT_EXTRACT_GROUP, StringUtils.join(words, ","), indexFromLast, word);
				return Collections.emptySet();

			group.add(word.charAt(index));
		}
		return group;
	}

	public final int getMinimumFromLength(){
		int minLength = -1;
		for(final String f : from){
			final int length = f.length();
			if(length < minLength || minLength < 0){
				minLength = length;

				if(minLength == 1)
					break;
			}
		}
		return minLength;
	}

	public final void expandConditionToMaxLength(final Comparator<String> comparator){
		final String lcs = StringHelper.longestCommonSuffix(from.toArray(new String[from.size()]));
		if(lcs != null){
			final Set<Character> group = extractGroup(lcs.length());
			final int entryConditionLength = RegexSequencer.length(RegexSequencer.splitSequence(condition));
			if(lcs.length() + (group.isEmpty()? 0: 1) > entryConditionLength)
				condition = RegexHelper.makeGroup(group, comparator) + lcs;
		}
	}

	public static List<LineEntry> eliminateCollisions(final List<LineEntry> entries,
			final Comparator<String> comparator){
		final List<LineEntry> result = new ArrayList<>(entries);
		final int length = result.size();

		boolean changed;
		do{
			changed = false;

			//sort by condition length (more generic first)
			result.sort(Comparator.comparingInt(rule -> RegexHelper.conditionLength(rule.condition)));

			for(int i = 0; !changed && i < length; i ++){
				final LineEntry a = result.get(i);
				final String[] seqA = RegexSequencer.splitSequence(a.condition);

				for(int j = i + 1; !changed && j < length; j ++){
					final LineEntry b = result.get(j);
					final String[] seqB = RegexSequencer.splitSequence(b.condition);
					if(!RegexSequencer.endsWith(seqB, seqA) || a.from.equals(b.from))
						continue;

					if(seqA.length == seqB.length){
						//TODO if seqA intersect seqB is non-empty, then subdivide rules into only-A, A-B, only B
						if(seqA.length > 0){
							final Set<Character> seqA0 = extractCharacters(seqA[0]);
							final Set<Character> seqB0 = extractCharacters(seqB[0]);

							final Set<Character> onlyIntersection = new HashSet<>(seqA0);
							onlyIntersection.retainAll(seqB0);

							if(!onlyIntersection.isEmpty()){
								final Set<Character> onlyInA = new HashSet<>(seqA0);
								onlyInA.removeAll(seqB0);

								final Set<Character> onlyInB = new HashSet<>(seqB0);
								onlyInB.removeAll(seqA0);

								if(!onlyInA.isEmpty() && onlyInB.isEmpty()){
									//TODO split A into only-A, A-B
									final String onlyA = RegexHelper.makeGroup(onlyInA, comparator);
									String intersection = RegexHelper.makeGroup(onlyIntersection, comparator);

									//split rules
									final StringBuilder sb = new StringBuilder();
									seqA[0] = intersection;
									for(final String s : seqA)
										sb.append(s);
									intersection = sb.toString();
									final String intersectionRegex = ".*" + intersection + "$";

									final Set<String> intersectionWords	= new HashSet<>(0);
									Iterator<String> itr = a.from.iterator();
									while(itr.hasNext()){
										final String from = itr.next();
										if(from.matches(intersectionRegex)){
											intersectionWords.add(from);
											itr.remove();
										}
									}
									final LineEntry inters = new LineEntry(a.removal, a.addition, intersection, intersectionWords);
									seqA[0] = onlyA;
									sb.setLength(0);
									for(final String s : seqA)
										sb.append(s);
									a.condition = sb.toString();
									result.add(inters);

									changed = true;
								}
								else if(onlyInA.isEmpty() && !onlyInB.isEmpty()){
									//TODO split B into only-B, A-B
									final String onlyB = RegexHelper.makeGroup(onlyInB, comparator);
									String intersection = RegexHelper.makeGroup(onlyIntersection, comparator);

									//split rules
									final StringBuilder sb = new StringBuilder();
									seqB[0] = intersection;
									for(final String s : seqB)
										sb.append(s);
									intersection = sb.toString();
									final String intersectionRegex = ".*" + intersection + "$";

									final Set<String> intersectionWords	= new HashSet<>(0);
									Iterator<String> itr = b.from.iterator();
									while(itr.hasNext()){
										final String from = itr.next();
										if(from.matches(intersectionRegex)){
											intersectionWords.add(from);
											itr.remove();
										}
									}
									final LineEntry inters = new LineEntry(b.removal, b.addition, intersection, intersectionWords);
									seqB[0] = onlyB;
									sb.setLength(0);
									for(final String s : seqB)
										sb.append(s);
									b.condition = sb.toString();
									result.add(inters);

									changed = true;
								}
								else if(!onlyInA.isEmpty() && !onlyInB.isEmpty()){
									//split A and B into only-A, A-B, only B
									final String onlyA = RegexHelper.makeGroup(onlyInA, comparator);
									final String onlyB = RegexHelper.makeGroup(onlyInB, comparator);
									String intersection = RegexHelper.makeGroup(onlyIntersection, comparator);

									//split rules
									final StringBuilder sb = new StringBuilder();
									seqA[0] = intersection;
									for(final String s : seqA)
										sb.append(s);
									intersection = sb.toString();
									final String intersectionRegex = ".*" + intersection + "$";

									//split rules
									final Set<String> intersectionWords	= new HashSet<>(0);
									Iterator<String> itr = a.from.iterator();
									while(itr.hasNext()){
										final String from = itr.next();
										if(from.matches(intersectionRegex)){
											intersectionWords.add(from);
											itr.remove();
										}
									}
									itr = b.from.iterator();
									while(itr.hasNext()){
										final String word = itr.next();
										if(word.matches(intersectionRegex)){
											intersectionWords.add(word);
											itr.remove();
										}
									}
									final LineEntry inters = new LineEntry(a.removal, a.addition, intersection, intersectionWords);
									seqA[0] = onlyA;
									seqB[0] = onlyB;
									sb.setLength(0);
									for(final String s : seqA)
										sb.append(s);
									a.condition = sb.toString();
									sb.setLength(0);
									for(final String s : seqB)
										sb.append(s);
									b.condition = sb.toString();
									result.add(inters);

									changed = true;
								}
							}
						}
						if(!changed){
							changed = isChanged(a, seqA.length, comparator);
							changed |= isChanged(b, seqB.length, comparator);
						}
						continue;
					}
					else if(seqA.length > 0){
						//TODO split rule A
						final Set<Character> seqA0 = extractCharacters(seqA[0]);
						final Set<Character> seqB0 = b.extractGroup(seqA.length - 1);

						final Set<Character> onlyIntersection = new HashSet<>(seqA0);
						onlyIntersection.retainAll(seqB0);

						if(!onlyIntersection.isEmpty()){
							final Set<Character> onlyInA = new HashSet<>(seqA0);
							onlyInA.removeAll(seqB0);

							final Set<Character> onlyInB = new HashSet<>(seqB0);
							onlyInB.removeAll(seqA0);

							if(!onlyInA.isEmpty() && onlyInB.isEmpty()){
								//TODO split A into only-A, A-B
								final String onlyA = RegexHelper.makeGroup(onlyInA, comparator);
								String intersection = RegexHelper.makeGroup(onlyIntersection, comparator);

								//split rules
								final StringBuilder sb = new StringBuilder();
								seqA[0] = intersection;
								for(final String s : seqA)
									sb.append(s);
								intersection = sb.toString();
								final String intersectionRegex = ".*" + intersection + "$";

								final Set<String> intersectionWords	= new HashSet<>(0);
								Iterator<String> itr = a.from.iterator();
								while(itr.hasNext()){
									final String from = itr.next();
									if(from.matches(intersectionRegex)){
										intersectionWords.add(from);
										itr.remove();
									}
								}
								final LineEntry inters = new LineEntry(a.removal, a.addition, intersection, intersectionWords);
								seqA[0] = onlyA;
								sb.setLength(0);
								for(final String s : seqA)
									sb.append(s);
								a.condition = sb.toString();
								result.add(inters);

								changed = true;
							}
							else if(onlyInA.isEmpty() && !onlyInB.isEmpty()){
								//TODO split B into only-B, A-B
								final String onlyB = RegexHelper.makeGroup(onlyInB, comparator);
								String intersection = RegexHelper.makeGroup(onlyIntersection, comparator);

								//split rules
								final StringBuilder sb = new StringBuilder();
								seqB[0] = intersection;
								for(final String s : seqB)
									sb.append(s);
								intersection = sb.toString();
								final String intersectionRegex = ".*" + intersection + "$";

								final Set<String> intersectionWords	= new HashSet<>(0);
								Iterator<String> itr = b.from.iterator();
								while(itr.hasNext()){
									final String from = itr.next();
									if(from.matches(intersectionRegex)){
										intersectionWords.add(from);
										itr.remove();
									}
								}
								final LineEntry inters = new LineEntry(b.removal, b.addition, intersection, intersectionWords);
								seqB[0] = onlyB;
								sb.setLength(0);
								for(final String s : seqB)
									sb.append(s);
								b.condition = sb.toString();
								result.add(inters);

								changed = true;
							}
							else if(!onlyInA.isEmpty() && !onlyInB.isEmpty()){
								//split A and B into only-A, A-B, only B
								final String onlyA = RegexHelper.makeGroup(onlyInA, comparator);
								final String onlyB = RegexHelper.makeGroup(onlyInB, comparator);
								String intersection = RegexHelper.makeGroup(onlyIntersection, comparator);

								//split rules
								final StringBuilder sb = new StringBuilder();
								seqA[0] = intersection;
								for(final String s : seqA)
									sb.append(s);
								intersection = sb.toString();
								final String intersectionRegex = ".*" + intersection + "$";

								//split rules
								final Set<String> intersectionWords	= new HashSet<>(0);
								Iterator<String> itr = a.from.iterator();
								while(itr.hasNext()){
									final String from = itr.next();
									if(from.matches(intersectionRegex)){
										intersectionWords.add(from);
										itr.remove();
									}
								}
								itr = b.from.iterator();
								while(itr.hasNext()){
									final String word = itr.next();
									if(word.matches(intersectionRegex)){
										intersectionWords.add(word);
										itr.remove();
									}
								}
								final LineEntry inters = new LineEntry(a.removal, a.addition, intersection, intersectionWords);
								seqA[0] = onlyA;
								seqB[0] = onlyB;
								sb.setLength(0);
								for(final String s : seqA)
									sb.append(s);
								a.condition = sb.toString();
								sb.setLength(0);
								for(final String s : seqB)
									sb.append(s);
								b.condition = sb.toString();
								result.add(inters);

								changed = true;
							}
						}
					}

					if(!changed){
						final LineEntry generic = (seqA.length < seqB.length? a: b);
						final int genericSequenceLength = Math.min(seqA.length, seqB.length);
						changed = isChanged(generic, genericSequenceLength, comparator);
					}
				}
			}
		}while(changed);

		return result;
	}

	private static boolean isChanged(final LineEntry generic, final int genericSequenceLength,
			final Comparator<String> comparator){
		boolean changed = false;
		final String[] refined = inferSuffixSequence(genericSequenceLength, generic.from);
		if(refined.length > 0)
			if(!Arrays.equals(RegexSequencer.splitSequence(generic.condition), refined)){
				generic.condition = RegexHelper.makeGroup(toCharArray(refined), comparator) + generic.condition;
				changed = true;
			}
		return changed;
	}


	private static Set<Character> extractCharacters(final String str){
		final Set<Character> result = new HashSet<>();
		for(final char c : str.toCharArray())
			result.add(c);
		result.remove('[');
		result.remove(']');
		return result;
	}

	private static char[] toCharArray(final String[] strings){
		final int length = strings.length;
		final char[] result = new char[length];
		for(int i = 0; i < length; i ++)
			result[i] = strings[i].charAt(0);
		return result;
	}

	private static String[] inferSuffixSequence(final int sequenceLength, final Set<String> words){
		//find the maximum common suffix among all words
		final Set<String> preceding = new HashSet<>(0);
		for(final String word : words){
			final String[] sequence = RegexSequencer.splitSequence(word);
			if(sequence.length > sequenceLength)
				//take the element before the suffix
				preceding.add(sequence[sequence.length - sequenceLength - 1]);
			else
				//FIXME there's a word that is too short (current rule MUST BE separated from the rule set)
				return new String[0];
		}
		return preceding.toArray(new String[preceding.size()]);
	}

	public static String toHunspellHeader(final AffixType type, final String flag, final char combinableChar,
			final int size){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		return sj.add(type.getOption().getCode())
			.add(flag)
			.add(String.valueOf(combinableChar))
			.add(Integer.toString(size))
			.toString();
	}

	public final String toHunspellRule(final AffixType type, final String flag){
		String firstAddition = firstAddition();
		String morphologicalRules = StringUtils.EMPTY;
		final int idx = firstAddition.indexOf(TAB);
		if(idx >= 0){
			morphologicalRules = firstAddition.substring(idx);
			firstAddition = firstAddition.substring(0, idx);
		}
		final String line = type.getOption().getCode() + StringUtils.SPACE
			+ flag + StringUtils.SPACE
			+ (removal.isEmpty()? ZERO: removal) + StringUtils.SPACE
			+ firstAddition + StringUtils.SPACE
			+ (condition.isEmpty()? DOT: condition);
		return (idx >= 0? line + morphologicalRules: line);
	}

	@Override
	public final String toString(){
		return new ToStringBuilder(this, ShortPrefixNotNullToStringStyle.SHORT_PREFIX_NOT_NULL_STYLE)
			.append("cond", condition)
			.append("rem", removal)
			.append("add", addition)
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
	private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

}
