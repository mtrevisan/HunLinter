/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.parsers.vos;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.datastructures.FixedArray;
import unit731.hunlinter.datastructures.SimpleDynamicArray;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.services.ParserHelper;
import unit731.hunlinter.services.RegexHelper;
import unit731.hunlinter.services.eventbus.EventBusService;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.workers.exceptions.LinterWarning;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static unit731.hunlinter.services.system.LoopHelper.match;
import static unit731.hunlinter.services.system.LoopHelper.removeIf;


public class AffixEntry{

	private static final MessageFormat AFFIX_EXPECTED = new MessageFormat("Expected an affix entry, found something else{0} in parent flag ''{1}''");
	private static final MessageFormat WRONG_FORMAT = new MessageFormat("Cannot parse affix line ''{0}''");
	private static final MessageFormat WRONG_TYPE = new MessageFormat("Wrong rule type, expected ''{0}'', got ''{1}'': {2}");
	private static final MessageFormat WRONG_FLAG = new MessageFormat("Wrong rule flag, expected ''{0}'', got ''{1}'': {2}");
	private static final MessageFormat WRONG_CONDITION_END = new MessageFormat("Condition part doesn''t ends with removal part: ''{0}''");
	private static final MessageFormat WRONG_CONDITION_START = new MessageFormat("Condition part doesn''t starts with removal part: ''{0}''");
	private static final MessageFormat POS_PRESENT = new MessageFormat("Part-of-Speech detected: ''{0}''");
	private static final MessageFormat CHARACTERS_IN_COMMON = new MessageFormat("Characters in common between removed and added part: ''{0}''");
	private static final MessageFormat CANNOT_FULL_STRIP = new MessageFormat("Cannot strip full word ''{0}'' without the FULLSTRIP option");

	private static final int PARAM_CONDITION = 1;
	private static final int PARAM_CONTINUATION_CLASSES = 2;
	private static final Pattern PATTERN_LINE = RegexHelper.pattern("^(?<condition>[^\\s]+?)(?:(?<!\\\\)\\/(?<continuationClasses>[^\\s]+))?$");

	private static final String TAB = "\t";
	private static final String SLASH = "/";
	private static final String SLASH_ESCAPED = "\\/";

	private static final String DOT = ".";
	private static final String ZERO = "0";


	private RuleEntry parent;

	/** string to strip */
	private final String removing;
	/** string to append */
	private final String appending;
	final String[] continuationFlags;
	/** condition that must be met before the affix can be applied */
	private final String condition;
	final String[] morphologicalFields;


	public AffixEntry(final String line, final int index, final AffixType parentType, final String parentFlag, final FlagParsingStrategy strategy,
			final List<String> aliasesFlag, final List<String> aliasesMorphologicalField){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		//remove comments at the end of the line
		final int commentIndex = line.indexOf(ParserHelper.COMMENT_MARK_SHARP);
		final String cleanedLine = (commentIndex >= 0? line.substring(0, commentIndex).trim(): line);

		final String[] lineParts = StringUtils.split(cleanedLine, null, 6);
		if(lineParts.length < 4 || lineParts.length > 6)
			throw new LinterException(AFFIX_EXPECTED.format(new Object[]{(lineParts.length > 0? ": '" + line + "'": StringUtils.EMPTY), parentFlag}));

		final AffixType type = AffixType.createFromCode(lineParts[0]);
		final String flag = lineParts[1];
		final String removal = StringUtils.replace(lineParts[2], SLASH_ESCAPED, SLASH);
		final Matcher m = RegexHelper.matcher(lineParts[3], PATTERN_LINE);
		if(!m.find())
			throw new LinterException(WRONG_FORMAT.format(new Object[]{line}));
		final String addition = StringUtils.replace(m.group(PARAM_CONDITION), SLASH_ESCAPED, SLASH);
		final String continuationClasses = m.group(PARAM_CONTINUATION_CLASSES);
		condition = (lineParts.length > 4? StringUtils.replace(lineParts[4], SLASH_ESCAPED, SLASH): DOT);
		morphologicalFields = (lineParts.length > 5? StringUtils.split(expandAliases(lineParts[5], aliasesMorphologicalField)): null);

		final String[] classes = strategy.parseFlags((continuationClasses != null? expandAliases(continuationClasses, aliasesFlag): null));
		continuationFlags = (classes != null && classes.length > 0? classes: null);
		removing = (!ZERO.equals(removal)? removal: StringUtils.EMPTY);
		appending = (!ZERO.equals(addition)? addition: StringUtils.EMPTY);

		checkValidity(parentType, type, parentFlag, flag, removal, line, index);
	}

	public void setParent(final RuleEntry parent){
		Objects.requireNonNull(parent);

		this.parent = parent;
	}

	private void checkValidity(final AffixType parentType, final AffixType type, final String parentFlag, final String flag,
			final String removal, final String line, final int index){
		if(parentType != type)
			throw new LinterException(WRONG_TYPE.format(new Object[]{parentType, type, line}));
		if(!parentFlag.equals(flag))
			throw new LinterException(WRONG_FLAG.format(new Object[]{parentFlag, flag, line}));
		if(!removing.isEmpty()){
			if(parentType == AffixType.SUFFIX){
				if(!condition.endsWith(removal))
					throw new LinterException(WRONG_CONDITION_END.format(new Object[]{line}));
				if(appending.length() > 1 && removal.charAt(0) == appending.charAt(0))
					EventBusService.publish(new LinterWarning(CHARACTERS_IN_COMMON.format(new Object[]{line}), IndexDataPair.of(index, null)));
			}
			else{
				if(!condition.startsWith(removal))
					throw new LinterException(WRONG_CONDITION_START.format(new Object[]{line}));
				if(appending.length() > 1 && removal.charAt(removal.length() - 1) == appending.charAt(appending.length() - 1))
					EventBusService.publish(new LinterWarning(CHARACTERS_IN_COMMON.format(new Object[]{line}), IndexDataPair.of(index, null)));
			}
		}
	}

	public String getAppending(){
		return appending;
	}

	private String expandAliases(final String part, final List<String> aliases){
		return (aliases != null && !aliases.isEmpty() && NumberUtils.isCreatable(part)? aliases.get(Integer.parseInt(part) - 1): part);
	}

	public boolean hasContinuationFlags(){
		return (continuationFlags != null && continuationFlags.length > 0);
	}

	public boolean hasContinuationFlag(final String flag){
		return (hasContinuationFlags() && flag != null && Arrays.binarySearch(continuationFlags, flag) >= 0);
	}

	public String[] combineContinuationFlags(final String[] otherContinuationFlags){
		final int size = (otherContinuationFlags != null? otherContinuationFlags.length: 0)
			+ (continuationFlags != null? continuationFlags.length: 0);
		final FixedArray<String> flags = new FixedArray<>(String.class, size);
		if(otherContinuationFlags != null && otherContinuationFlags.length > 0)
			flags.addAll(otherContinuationFlags);
		if(continuationFlags != null)
			flags.addAllUnique(continuationFlags);
		return flags.extractCopyOrNull();
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
	public String[] combineMorphologicalFields(final DictionaryEntry dicEntry){
		String[] baseMorphFields = (dicEntry.morphologicalFields != null? dicEntry.morphologicalFields: new String[0]);
		final String[] ruleMorphFields = (morphologicalFields != null? morphologicalFields: new String[0]);

		//NOTE: Part-of-Speech is NOT overwritten, both in simple application of an affix rule and of a compound rule
		final boolean containsInflectionalAffix = containsAffixes(ruleMorphFields, MorphologicalTag.INFLECTIONAL_SUFFIX,
			MorphologicalTag.INFLECTIONAL_PREFIX);
//		final boolean containsTerminalAffixes = containsAffixes(ruleMorphFields, MorphologicalTag.TERMINAL_SUFFIX,
//			MorphologicalTag.TERMINAL_PREFIX);
		final boolean containsDerivationalAffixes = containsAffixes(ruleMorphFields, MorphologicalTag.DERIVATIONAL_SUFFIX,
			MorphologicalTag.DERIVATIONAL_PREFIX);
		//remove inflectional and terminal suffixes
//		baseMorphFields = removeIf(baseMorphFields, field ->
//			containsInflectionalAffix && (MorphologicalTag.INFLECTIONAL_SUFFIX.isSupertypeOf(field) || MorphologicalTag.INFLECTIONAL_PREFIX.isSupertypeOf(field))
//			|| !containsTerminalAffixes && (MorphologicalTag.TERMINAL_SUFFIX.isSupertypeOf(field) || MorphologicalTag.TERMINAL_PREFIX.isSupertypeOf(field)));
		baseMorphFields = removeIf(baseMorphFields, field ->
			containsInflectionalAffix && (MorphologicalTag.INFLECTIONAL_SUFFIX.isSupertypeOf(field) || MorphologicalTag.INFLECTIONAL_PREFIX.isSupertypeOf(field))
			|| containsDerivationalAffixes && (MorphologicalTag.TERMINAL_SUFFIX.isSupertypeOf(field) || MorphologicalTag.TERMINAL_PREFIX.isSupertypeOf(field)));

		//add morphological fields from the applied affix
		return (parent.getType() == AffixType.SUFFIX? ArrayUtils.addAll(baseMorphFields, ruleMorphFields): ArrayUtils.addAll(ruleMorphFields, baseMorphFields));
	}

	private boolean containsAffixes(final String[] amf, final MorphologicalTag... tags){
		return (match(tags, tag -> match(amf, tag::isSupertypeOf) != null) != null);
	}

	public static String[] extractMorphologicalFields(final DictionaryEntry[] compoundEntries){
		int size = 0;
		for(final DictionaryEntry compoundEntry : compoundEntries)
			size += compoundEntry.morphologicalFields.length + 1;

		int offset = 0;
		final String[] mf = new String[size];
		for(final DictionaryEntry compoundEntry : compoundEntries){
			final String compound = compoundEntry.getWord();
			mf[offset ++] = MorphologicalTag.PART.attachValue(compound);
			for(final String cemf : compoundEntry.morphologicalFields)
				mf[offset ++] = cemf;
		}
		return mf;
	}

	public void validate(){
		final String[] filteredFields = getMorphologicalFields(MorphologicalTag.PART_OF_SPEECH);
		if(filteredFields.length > 0)
			throw new LinterException(POS_PRESENT.format(new Object[]{String.join(", ", filteredFields)}));
	}

	public AffixType getType(){
		return parent.getType();
	}

	public String getFlag(){
		return parent.getFlag();
	}

	private String[] getMorphologicalFields(final MorphologicalTag morphologicalTag){
		final SimpleDynamicArray<String> collector = new SimpleDynamicArray<>(String.class,
			(morphologicalFields != null? morphologicalFields.length: 0));
		if(morphologicalFields != null){
			final String tag = morphologicalTag.getCode();
			final int purgeTag = tag.length();
			for(final String mf : morphologicalFields)
				if(mf.startsWith(tag))
					collector.add(mf.substring(purgeTag));
		}
		return collector.extractCopy();
	}

	public boolean canApplyTo(final String word){
		if(condition.length() == 1 && condition.charAt(0) == '.')
			return true;

		return (parent.getType() == AffixType.PREFIX?
			canApplyToPrefix(word):
			canApplyToSuffix(word));
	}

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

	public boolean canInverseApplyTo(final String word){
		return (parent.getType() == AffixType.SUFFIX? word.endsWith(appending): word.startsWith(appending));
	}

	public String applyRule(final String word, final boolean isFullstrip){
		if(!isFullstrip && word.length() == removing.length())
			throw new LinterException(CANNOT_FULL_STRIP.format(new Object[]{word}));

		return (parent.getType() == AffixType.SUFFIX?
			word.substring(0, word.length() - removing.length()) + appending:
			appending + word.substring(removing.length()));
	}

	//NOTE: {#canInverseApplyTo} should be called to verify applicability
	public String undoRule(final String word){
		return (parent.getType() == AffixType.SUFFIX?
			word.substring(0, word.length() - appending.length()) + removing:
			removing + word.substring(appending.length()));
	}

	public String toString(final FlagParsingStrategy strategy){
		Objects.requireNonNull(strategy);

		final StringBuilder sb = new StringBuilder();
		if(continuationFlags != null && continuationFlags.length > 0){
			sb.append(SLASH);
			sb.append(strategy.joinFlags(continuationFlags));
		}
		if(morphologicalFields != null && morphologicalFields.length > 0)
			sb.append(TAB).append(StringUtils.join(morphologicalFields, StringUtils.SPACE));
		return sb.toString();
	}

	@Override
	public String toString(){
		final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
		sj.add(parent.getType().getOption().getCode())
			.add(parent.getFlag())
			.add(removing.isEmpty()? ZERO: removing)
			.add((appending.isEmpty()? ZERO: appending)
				+ (continuationFlags != null && continuationFlags.length > 0? SLASH + String.join(StringUtils.EMPTY, continuationFlags): StringUtils.EMPTY))
			.add(condition);
		if(morphologicalFields != null && morphologicalFields.length > 0)
			sj.add(String.join(StringUtils.SPACE, morphologicalFields));
		return sj.toString();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final AffixEntry rhs = (AffixEntry)obj;
		final EqualsBuilder builder = new EqualsBuilder()
			.append(parent != null, rhs.parent != null)
			.append(continuationFlags, rhs.continuationFlags)
			.append(condition, rhs.condition)
			.append(removing, rhs.removing)
			.append(appending, rhs.appending)
			.append(morphologicalFields, rhs.morphologicalFields);
		if(parent != null)
			builder
				.append(parent.getType(), rhs.parent.getType())
				.append(parent.getFlag(), rhs.parent.getFlag());
		return builder.isEquals();
	}

	@Override
	public int hashCode(){
		final HashCodeBuilder builder = new HashCodeBuilder()
			.append(continuationFlags)
			.append(condition)
			.append(removing)
			.append(appending)
			.append(morphologicalFields);
		if(parent != null)
			builder
				.append(parent.getType())
				.append(parent.getFlag());
		return builder.toHashCode();
	}

}
