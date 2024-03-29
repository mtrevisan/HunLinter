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
package io.github.mtrevisan.hunlinter.parsers.vos;

import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;
import io.github.mtrevisan.hunlinter.parsers.enums.MorphologicalTag;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.eventbus.EventBusService;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterWarning;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AffixEntry{

	private static final String AFFIX_EXPECTED = "Expected an affix entry, found something else{} in parent flag `{}`";
	private static final String WRONG_FORMAT = "Cannot parse affix line `{}`";
	private static final String WRONG_REMOVING_APPENDING_FORMAT = "Same removal and addition parts: `{}`";
	private static final String WRONG_TYPE = "Wrong rule type, expected `{}`, got `{}`: {}";
	private static final String WRONG_FLAG = "Wrong rule flag, expected `{}`, got `{}`: {}";
	private static final String WRONG_CONDITION_END = "Condition part doesn't ends with removal part: `{}`";
	private static final String WRONG_CONDITION_START = "Condition part doesn't starts with removal part: `{}`";
	private static final String POS_PRESENT = "Part-of-Speech detected: `{}`";
	private static final String CHARACTERS_IN_COMMON = "Characters in common between removed and added part: `{}`";
	private static final String CANNOT_FULL_STRIP = "Cannot strip full word `{}` without the FULLSTRIP option";

	private static final int PARAM_CONDITION = 1;
	private static final int PARAM_CONTINUATION_CLASSES = 2;
	private static final Pattern PATTERN_LINE = RegexHelper.pattern("^(?<condition>[^\\s]+?)(?:(?<!\\\\)\\/(?<continuationClasses>[^\\s]+))?$");

	private static final MorphologicalTag[] INFLECTIONAL_AFFIXES = {MorphologicalTag.INFLECTIONAL_SUFFIX, MorphologicalTag.INFLECTIONAL_PREFIX};
	private static final MorphologicalTag[] DERIVATIONAL_AFFIXES = {MorphologicalTag.DERIVATIONAL_SUFFIX, MorphologicalTag.DERIVATIONAL_PREFIX};

	private static final String TAB = "\t";
	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";

	private static final String DOT = ".";
	private static final String ZERO = "0";


	private RuleEntry parent;

	/** string to strip. */
	private final String removing;
	/** string to append. */
	private final String appending;
	final List<String> continuationFlags;
	/** condition that must be met before the affix can be applied. */
	private final String condition;
	final List<String> morphologicalFields;


	public AffixEntry(final String line, final int index, final AffixType parentType, final String parentFlag,
			final FlagParsingStrategy strategy, final List<String> aliasesFlag, final List<String> aliasesMorphologicalField){
		Objects.requireNonNull(line, "Line cannot be null");
		Objects.requireNonNull(strategy, "Strategy cannot be null");

		//remove comments at the end of the line
		final int commentIndex = line.indexOf(ParserHelper.COMMENT_MARK_SHARP);
		final String cleanedLine = (commentIndex >= 0? line.substring(0, commentIndex).trim(): line);

		final String[] lineParts = StringUtils.split(cleanedLine, null, 6);
		if(lineParts.length < 4 || lineParts.length > 6)
			throw new LinterException(AFFIX_EXPECTED, (lineParts.length > 0? ": `" + line + "`": StringUtils.EMPTY), parentFlag);

		final AffixType type = AffixType.createFromCode(lineParts[0]);
		final String flag = lineParts[1];
		final String removal = StringUtils.replace(lineParts[2], SLASH_ESCAPED, SLASH);
		final Matcher m = RegexHelper.matcher(lineParts[3], PATTERN_LINE);
		if(!m.find())
			throw new LinterException(WRONG_FORMAT, line);

		final String addition = StringUtils.replace(m.group(PARAM_CONDITION), SLASH_ESCAPED, SLASH);
		final String continuationClasses = m.group(PARAM_CONTINUATION_CLASSES);
		condition = (lineParts.length > 4? StringUtils.replace(lineParts[4], SLASH_ESCAPED, SLASH): DOT);
		morphologicalFields = (lineParts.length > 5
			? new ArrayList<>(Arrays.asList(StringUtils.split(expandAliases(lineParts[5], aliasesMorphologicalField))))
			: null);

		final String[] classes = strategy.parseFlags((continuationClasses != null? expandAliases(continuationClasses, aliasesFlag): null));
		continuationFlags = (classes != null && classes.length > 0? new ArrayList<>(Arrays.asList(classes)): null);
		removing = (!ZERO.equals(removal)? removal: StringUtils.EMPTY);
		appending = (!ZERO.equals(addition)? addition: StringUtils.EMPTY);

		checkValidity(parentType, type, parentFlag, flag, removal, line, index);
	}

	public final AffixEntry setParent(final RuleEntry parent){
		Objects.requireNonNull(parent, "Parent cannot be null");

		this.parent = parent;

		return this;
	}

	private void checkValidity(final AffixType parentType, final AffixType type, final String parentFlag, final String flag,
			final String removal, final String line, final int index){
		if(parentType != type)
			throw new LinterException(WRONG_TYPE, parentType, type, line);
		if(parentFlag != null && !parentFlag.equals(flag))
			throw new LinterException(WRONG_FLAG, parentFlag, flag, line);
		if(!removing.isEmpty()){
			if(removing.equals(appending))
				throw new LinterException(WRONG_REMOVING_APPENDING_FORMAT, line);
			if(parentType == AffixType.SUFFIX){
				if(!condition.endsWith(removal))
					throw new LinterException(WRONG_CONDITION_END, line);
				if(appending.length() > 1 && removal.charAt(0) == appending.charAt(0))
					EventBusService.publish(new LinterWarning(CHARACTERS_IN_COMMON, line)
						.withIndex(index));
			}
			else{
				if(!condition.startsWith(removal))
					throw new LinterException(WRONG_CONDITION_START, line);
				if(appending.length() > 1 && StringHelper.lastChar(removal) == StringHelper.lastChar(appending))
					EventBusService.publish(new LinterWarning(CHARACTERS_IN_COMMON, line)
						.withIndex(index));
			}
		}
	}

	public final String getAppending(){
		return appending;
	}

	private static String expandAliases(final String part, final List<String> aliases){
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)? aliases.get(Integer.parseInt(part) - 1): part);
	}

	public final boolean hasContinuationFlags(){
		return (continuationFlags != null && !continuationFlags.isEmpty());
	}

	public final boolean hasContinuationFlag(final String flag){
		return (hasContinuationFlags() && flag != null && continuationFlags.contains(flag));
	}

	public final List<String> combineContinuationFlags(final Collection<String> otherContinuationFlags){
		final Set<String> flags = new HashSet<>();
		if(otherContinuationFlags != null && !otherContinuationFlags.isEmpty())
			flags.addAll(otherContinuationFlags);
		if(continuationFlags != null)
			flags.addAll(continuationFlags);
		return new ArrayList<>(flags);
	}

	//FIXME is this documentation updated/true?
	/**
	 *
	 * Derivational Suffix: stemming doesn't remove derivational suffixes (morphological generation depends on the order of the
	 * 	suffix fields)
	 * Inflectional Suffix: all inflectional suffixes are removed by stemming (morphological generation depends on the order of the
	 * 	suffix fields)
	 * Terminal Suffix: inflectional suffix fields removed by additional (not terminal) suffixes, useful for zero morphemes and
	 * 	affixes removed by splitting rules
	 *
	 * @param dicEntry	The dictionary entry to combine from
	 * @return	The list of new morphological fields
	 */
	public final List<String> combineMorphologicalFields(final DictionaryEntry dicEntry){
		final List<String> baseMorphFields = (dicEntry.morphologicalFields != null? new ArrayList<>(dicEntry.morphologicalFields):
			new ArrayList<>(0));
		final List<String> ruleMorphFields = (morphologicalFields != null? morphologicalFields: new ArrayList<>(0));

		//NOTE: Part-of-Speech is NOT overwritten, both in simple application of an affix rule and of a compound rule
		final boolean containsInflectionalAffix = containsAffixes(ruleMorphFields, INFLECTIONAL_AFFIXES);
//		final boolean containsTerminalAffixes = containsAffixes(ruleMorphFields, MorphologicalTag.TERMINAL_SUFFIX,
//			MorphologicalTag.TERMINAL_PREFIX);
		final boolean containsDerivationalAffixes = containsAffixes(ruleMorphFields, DERIVATIONAL_AFFIXES);
		//remove inflectional and terminal suffixes
//		baseMorphFields = removeIf(baseMorphFields, field ->
//			containsInflectionalAffix && (MorphologicalTag.INFLECTIONAL_SUFFIX.isSupertypeOf(field) || MorphologicalTag.INFLECTIONAL_PREFIX.isSupertypeOf(field))
//			|| !containsTerminalAffixes && (MorphologicalTag.TERMINAL_SUFFIX.isSupertypeOf(field) || MorphologicalTag.TERMINAL_PREFIX.isSupertypeOf(field)));
		final Iterator<String> itr = baseMorphFields.iterator();
		while(itr.hasNext()){
			final String field = itr.next();
			final boolean filter = (containsInflectionalAffix && (MorphologicalTag.INFLECTIONAL_SUFFIX.isSupertypeOf(field)
					|| MorphologicalTag.INFLECTIONAL_PREFIX.isSupertypeOf(field))
				|| containsDerivationalAffixes && (MorphologicalTag.TERMINAL_SUFFIX.isSupertypeOf(field)
					|| MorphologicalTag.TERMINAL_PREFIX.isSupertypeOf(field)));
			if(filter)
				itr.remove();
		}

		//add morphological fields from the applied affix
		baseMorphFields.addAll((parent.getType() == AffixType.SUFFIX? baseMorphFields.size(): 0), ruleMorphFields);
		return baseMorphFields;
	}

	private static boolean containsAffixes(final List<String> amf, final MorphologicalTag[] tags){
		for(final MorphologicalTag tag : tags)
			for(final String elem : amf)
				if(tag.isSupertypeOf(elem))
					return true;
		return false;
	}

	public static List<String> extractMorphologicalFields(final List<DictionaryEntry> compoundEntries){
		int size = 0;
		for(int i = 0; i < compoundEntries.size(); i ++)
			size += compoundEntries.get(i).morphologicalFields.size() + 1;

		final List<String> mf = new ArrayList<>(size);
		for(int i = 0; i < compoundEntries.size(); i ++){
			final DictionaryEntry compoundEntry = compoundEntries.get(i);
			final String compound = compoundEntry.getWord();
			mf.add(MorphologicalTag.PART.attachValue(compound));
			for(final String cemf : compoundEntry.morphologicalFields)
				mf.add(cemf);
		}
		return mf;
	}

	public final void validate(){
		final List<String> filteredFields = getMorphologicalFields(MorphologicalTag.PART_OF_SPEECH);
		if(!filteredFields.isEmpty())
			throw new LinterException(POS_PRESENT, String.join(", ", filteredFields));
	}

	public final AffixType getType(){
		return parent.getType();
	}

	public final String getFlag(){
		return parent.getFlag();
	}

	public List<String> getMorphologicalFields(final MorphologicalTag morphologicalTag){
		final List<String> collector = new ArrayList<>(morphologicalFields != null? morphologicalFields.size(): 0);
		if(morphologicalFields != null){
			final String tag = morphologicalTag.getCode();
			final int purgeTag = tag.length();
			for(final String mf : morphologicalFields)
				if(mf.startsWith(tag))
					collector.add(mf.substring(purgeTag));
		}
		return collector;
	}

	public final boolean canApplyTo(final String word){
		if(condition.length() == 1 && condition.charAt(0) == '.')
			return true;

		return (parent.getType() == AffixType.PREFIX
			? canApplyToPrefix(word)
			: canApplyToSuffix(word));
	}

	@SuppressWarnings("IfStatementMissingBreakInLoop")
	private boolean canApplyToPrefix(final String word){
		if(word.startsWith(condition))
			return true;

		final char[] wrd = word.toCharArray();
		final char[] cond = condition.toCharArray();
		int i, j;
		for(i = 0, j = 0; i < wrd.length && j < cond.length; i ++, j ++){
			if(cond[j] == '['){
				//search inside group
				final boolean neg = (cond[j + 1] == '^');
				boolean in = false;
				do{
					j ++;
					if(!in && wrd[i] == cond[j])
						in = true;
				}while(j < cond.length - 1 && cond[j] != ']');
				//cope with negation inside group
				if(neg == in || j == cond.length - 1 && cond[j] != ']')
					break;
			}
			else if(cond[j] != wrd[i])
				break;
		}
		return (j >= cond.length);
	}

	@SuppressWarnings("IfStatementMissingBreakInLoop")
	private boolean canApplyToSuffix(final String word){
		if(word.endsWith(condition))
			return true;

		final char[] wrd = word.toCharArray();
		final char[] cond = condition.toCharArray();
		int i, j;
		for(i = wrd.length - 1, j = cond.length - 1; i >= 0 && j >= 0; i --, j --){
			if(cond[j] == ']'){
				//search inside group
				boolean in = false;
				do{
					j --;
					if(!in && wrd[i] == cond[j])
						in = true;
				}while(j > 0 && cond[j] != '[');
				if(j == 0 && cond[j] != '[')
					break;

				//cope with negation inside group
				final boolean neg = (cond[j + 1] == '^');
				if(neg == in)
					break;
			}
			else if(cond[j] != wrd[i])
				break;
		}
		return (j < 0);
	}

	public final boolean canInverseApplyTo(final String word){
		return (parent.getType() == AffixType.SUFFIX? word.endsWith(appending): word.startsWith(appending));
	}

	public final boolean isFullstripRule(final String word){
		return (word.length() == removing.length());
	}

	public final String applyRule(final String word, final boolean isFullstrip){
		if(!isFullstrip && isFullstripRule(word))
			throw new LinterException(CANNOT_FULL_STRIP, word);

		return (parent.getType() == AffixType.SUFFIX
			? word.substring(0, word.length() - removing.length()) + appending
			: appending + word.substring(removing.length()));
	}

	//NOTE: {#canInverseApplyTo} should be called to verify applicability
	public final String undoRule(final String word){
		return (parent.getType() == AffixType.SUFFIX
			? word.substring(0, word.length() - appending.length()) + removing
			: removing + word.substring(appending.length()));
	}

	public final String toString(final FlagParsingStrategy strategy){
		Objects.requireNonNull(strategy, "Strategy cannot be null");

		final StringBuilder sb = new StringBuilder();
		if(continuationFlags != null && !continuationFlags.isEmpty())
			sb.append(SLASH)
				.append(strategy.joinFlags(continuationFlags));
		if(morphologicalFields != null && !morphologicalFields.isEmpty())
			sb.append(TAB)
				.append(StringUtils.join(morphologicalFields, StringUtils.SPACE));
		return sb.toString();
	}

	@Override
	public final String toString(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(parent.getType().getOption().getCode())
			.add(parent.getFlag())
			.add(removing.isEmpty()? ZERO: removing)
			.add((appending.isEmpty()? ZERO: appending)
				+ (continuationFlags != null && !continuationFlags.isEmpty()
					? SLASH + String.join(StringUtils.EMPTY, continuationFlags)
					: StringUtils.EMPTY))
			.add(condition);
		if(morphologicalFields != null && !morphologicalFields.isEmpty())
			sj.add(String.join(StringUtils.SPACE, morphologicalFields));
		return sj.toString();
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final AffixEntry rhs = (AffixEntry)obj;
		return (Objects.equals(parent, rhs.parent)
			&& removing.equals(rhs.removing)
			&& appending.equals(rhs.appending)
			&& Objects.equals(continuationFlags, rhs.continuationFlags)
			&& condition.equals(rhs.condition)
			&& morphologicalFields.equals(rhs.morphologicalFields));
	}

	@Override
	public final int hashCode(){
		int result = (parent == null? 0: parent.hashCode());
		result = 31 * result + (removing == null? 0: removing.hashCode());
		result = 31 * result + (appending == null? 0: appending.hashCode());
		result = 31 * result + (continuationFlags == null? 0: continuationFlags.hashCode());
		result = 31 * result + (condition == null? 0: condition.hashCode());
		result = 31 * result + (morphologicalFields == null? 0: morphologicalFields.hashCode());
		return result;
	}

}
