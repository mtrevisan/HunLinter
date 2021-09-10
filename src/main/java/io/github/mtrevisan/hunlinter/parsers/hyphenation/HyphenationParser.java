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
package io.github.mtrevisan.hunlinter.parsers.hyphenation;

import io.github.mtrevisan.hunlinter.datastructures.ahocorasicktrie.AhoCorasickTrie;
import io.github.mtrevisan.hunlinter.datastructures.ahocorasicktrie.AhoCorasickTrieBuilder;
import io.github.mtrevisan.hunlinter.languages.Orthography;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Implements Franklin Mark Liang's hyphenation algorithm with Petr Soijka's non-standard hyphenation extension.
 *
 * @see <a href="https://tug.org/docs/liang/liang-thesis.pdf">Liang's thesis</a>
 * @see <a href="http://hunspell.sourceforge.net/tb87nemeth.pdf">László Németh's paper</a>
 * @see <a href="https://android.googlesource.com/platform/external/hyphenation/+/ics-mr0">László Németh's non-standard readme</a>
 * @see <a href="https://github.com/hunspell/hyphen">C source code</a>
 * @see <a href="https://wiki.openoffice.org/wiki/Documentation/SL/Using_TeX_hyphenation_patterns_in_OpenOffice.org">Using TeX hyphenation patterns in OpenOffice.org</a>
 */
public class HyphenationParser{

	private static final String MORE_THAN_TWO_LEVELS = "Cannot have more than two levels";
	private static final String DUPLICATED_CUSTOM_HYPHENATION = "Custom hyphenation `{}` is already present";
	private static final String DUPLICATED_HYPHENATION = "Duplicate found: `{}`";
	private static final String INVALID_RULE = "Rule `{}` has an invalid format";
	private static final String INVALID_HYPHENATION_POINT = "Rule `{}` has no hyphenation point(s)";
	private static final String INVALID_HYPHENATION_POINT_NEAR_DOT = "Rule `{}` is invalid, the hyphenation point should not be adjacent to a dot";
	private static final String MORE_HYPHENATION_DOTS = "Augmented rule `{}` has not exactly one hyphenation point";
	private static final String AUGMENTED_RULE_INDEX_NOT_LESS_THAN = "Augmented rule `{}` has the index number not less than the hyphenation point";
	private static final String AUGMENTED_RULE_LENGTH_NOT_LESS_THAN = "Augmented rule `{}` has the length number not less than the hyphenation point";
	private static final String AUGMENTED_RULE_LENGTH_EXCEEDS = "Augmented rule `{}` has the length number that exceeds the length of the rule";
	private static final String DUPLICATED_RULE = "Pattern `{}` already present as `{}`";

	private static final String NEXT_LEVEL = "NEXTLEVEL";

	//Hyphens from the wikipedia article: https://en.wikipedia.org/wiki/Hyphen#Unicode
//	private static final String HYPHEN = "‐";
//	private static final String HYPHEN_MINUS = "-";
	public static final String MINUS_SIGN = "-";
	public static final char EQUALS_SIGN = '=';
	public static final String SOFT_HYPHEN = "\u00AD";
	public static final String EN_DASH = "–";
//	private static final char EM_DASH = '—';
	public static final String APOSTROPHE = "'";
//	public static final char RIGHT_SINGLE_QUOTATION_MASK = '‘';
	/**
	 * https://en.wikipedia.org/wiki/Modifier_letter_apostrophe
	 * https://en.wikipedia.org/wiki/Quotation_mark
	 */
	public static final char MODIFIER_LETTER_APOSTROPHE = 'ʼ';

	public static final String BREAK_CHARACTER = SOFT_HYPHEN;

	private static final String ONE = "1";
	public static final String WORD_BOUNDARY = ".";
	public static final char AUGMENTED_RULE = '/';

	private static final String COMMA = ",";

	private static final String ESCAPE_SEQUENCE = "^^";
	private static final Pattern PATTERN_ESCAPED_UNICODE = RegexHelper.pattern("(\\^{2}[a-fA-F0-9]{2})|.");
	private static final Pattern PATTERN_VALID_RULE = RegexHelper.pattern("^\\.?[^.]+\\.?$");
	private static final Pattern PATTERN_VALID_RULE_BREAK_POINTS = RegexHelper.pattern("[\\d]");
	private static final Pattern PATTERN_INVALID_RULE_START = RegexHelper.pattern("^\\.[\\d]");
	private static final Pattern PATTERN_INVALID_RULE_END = RegexHelper.pattern("[\\d]\\.$");
	private static final Pattern PATTERN_AUGMENTED_RULE_HYPHEN_INDEX = RegexHelper.pattern("[13579]");

	public static final int PARAM_RULE = 1;
	public static final int PARAM_ADD_BEFORE = 2;
	public static final int PARAM_HYPHEN = 3;
	public static final int PARAM_ADD_AFTER = 4;
	public static final int PARAM_START = 5;
	public static final int PARAM_CUT = 6;
	public static final Pattern PATTERN_AUGMENTED_RULE = RegexHelper.pattern("^(?<rule>[^/]+)/(?<addBefore>[^=_]*?)(?:=|(?<hyphen>.)_)(?<addAfter>[^,]*)(?:,(?<start>\\d+),(?<cut>\\d+))?$");
	public static final Pattern PATTERN_POINTS_AND_NUMBERS = RegexHelper.pattern("[.\\d]");
	public static final Pattern PATTERN_WORD_INITIAL = RegexHelper.pattern("^" + Pattern.quote(WORD_BOUNDARY));

	public static final Pattern PATTERN_WORD_BOUNDARIES = RegexHelper.pattern(Pattern.quote(WORD_BOUNDARY));

	private static final Pattern PATTERN_KEY = RegexHelper.pattern("[\\d=]|/.+$");
	private static final Pattern PATTERN_HYPHENATION_POINT = RegexHelper.pattern("[^13579]|/.+$");

	public static final Pattern PATTERN_REDUCE = RegexHelper.pattern("/.+$");

	private static final char[] NEW_LINE = {'\n'};

	public enum Level{NON_COMPOUND, COMPOUND}


	private static final Map<Level, Set<String>> REDUCED_PATTERNS = new EnumMap<>(Level.class);
	static{
		for(final Level level : Level.values())
			REDUCED_PATTERNS.put(level, new HashSet<>(0));
	}

	private final Comparator<String> comparator;

	private boolean secondLevelPresent;
	private Pattern patternNoHyphen;
	private final Map<Level, Map<String, String>> rules = new EnumMap<>(Level.class);
	private final Map<Level, AhoCorasickTrie<String>> patterns = new EnumMap<>(Level.class);
	private final Map<Level, Map<String, String>> customHyphenations = new EnumMap<>(Level.class);
	private HyphenationOptionsParser options;


	public HyphenationParser(final Comparator<String> comparator){
		Objects.requireNonNull(comparator, "Comparator cannot be null");

		this.comparator = comparator;

		for(final Level level : Level.values()){
			rules.put(level, new HashMap<>(0));
			customHyphenations.put(level, new HashMap<>(0));
		}
		options = new HyphenationOptionsParser();
	}

	HyphenationParser(final Comparator<String> comparator, final Map<Level, AhoCorasickTrie<String>> patterns,
			Map<Level, Map<String, String>> customHyphenations, final HyphenationOptionsParser options){
		Objects.requireNonNull(patterns, "Patterns cannot be null");
		Objects.requireNonNull(comparator, "Comparator cannot be null");

		this.comparator = comparator;

		secondLevelPresent = patterns.containsKey(Level.COMPOUND);
		for(final Level level : Level.values())
			this.patterns.put(level, patterns.get(level));
		customHyphenations = Optional.ofNullable(customHyphenations)
			.orElse(new EnumMap<>(Level.class));
		for(final Level level : Level.values()){
			final Map<String, String> ch = customHyphenations.getOrDefault(level, new HashMap<>(0));
			this.customHyphenations.put(level, ch);
		}
		this.options = (options != null? options: new HyphenationOptionsParser());
	}

	public final boolean isSecondLevelPresent(){
		return secondLevelPresent;
	}

	public final Pattern getPatternNoHyphen(){
		return patternNoHyphen;
	}

	public final Map<Level, AhoCorasickTrie<String>> getPatterns(){
		return patterns;
	}

	public final Map<Level, Map<String, String>> getCustomHyphenations(){
		return customHyphenations;
	}

	public final HyphenationOptionsParser getOptions(){
		return options;
	}

	public final void setOptions(final HyphenationOptionsParser options){
		this.options = options;
	}

	/**
	 * Parse the hyphenation rules out from a .dic file.
	 *
	 * @param hypFile	The content of the hyphenation file
	 * @throws LinterException   If something is wrong while parsing the file
	 */
	public final void parse(final File hypFile){
		final Path hypPath = hypFile.toPath();
		Level level = Level.NON_COMPOUND;
		final Charset charset = FileHelper.determineCharset(hypPath);
		try(final Scanner scanner = FileHelper.createScanner(hypPath, charset)){
			String line = scanner.nextLine();
			FileHelper.readCharset(line);

			while(scanner.hasNextLine()){
				line = scanner.nextLine();
				if(ParserHelper.isHyphenationComment(line))
					continue;

				final boolean parsedLine = options.parseLine(line);
				if(parsedLine)
					continue;

				//extract next level
				if(line.equals(NEXT_LEVEL)){
					if(level == Level.COMPOUND)
						throw new LinterException(MORE_THAN_TWO_LEVELS);

					//start with non-compound level
					level = Level.COMPOUND;
					continue;
				}

				if(charset == StandardCharsets.ISO_8859_1)
					line = convertUnicode(line);

				if(isCustomRule(line))
					parseCustomRule(line, level);
				else{
					validateRule(line, level);

					parseCommonRule(line, level);
				}
			}

			if(level == Level.NON_COMPOUND)
				addDefaults(Level.NON_COMPOUND, charset);
		}
		catch(final LinterException | IOException e){
			throw new LinterException(e, e.getMessage());
		}

		//build tries
		for(final Level lev : Level.values())
			buildTrie(lev, rules.get(lev));

		secondLevelPresent = (level == Level.COMPOUND);
	}

	/** Transform escaped unicode into true unicode (ex. `^^e1` into `á`). */
	private static String convertUnicode(final CharSequence line){
		final String[] components = RegexHelper.extract(line, PATTERN_ESCAPED_UNICODE);
		for(int i = 0; i < components.length; i ++)
			if(components[i].startsWith(ESCAPE_SEQUENCE))
				components[i] = String.valueOf((char)Integer.parseInt(components[i].substring(2), 16));
		return StringUtils.join(components);
	}

	private void parseCustomRule(final String line, final Level level){
		final String key = StringHelper.removeAll(line, EQUALS_SIGN);
		if(customHyphenations.get(level).containsKey(key))
			throw new LinterException(DUPLICATED_CUSTOM_HYPHENATION, line);

		customHyphenations.get(level)
			.put(key, line);
	}

	private void parseCommonRule(final String line, final Level level){
		//remove all (breaking) numbers
		final String key = getKeyFromData(line);
		final boolean duplicatedRule = isRuleDuplicated(key, line, level);
		if(duplicatedRule)
			throw new LinterException(DUPLICATED_HYPHENATION, line);

		//insert current pattern into the radix tree
		rules.get(level)
			.put(key, line);
	}

	private void addDefaults(final Level level, final Charset charset){
		//dash and apostrophe are added by default (retro-compatibility)
		final List<String> retroCompatibilityNoHyphen = (charset == StandardCharsets.UTF_8
			? Arrays.asList(APOSTROPHE, MINUS_SIGN, String.valueOf(MODIFIER_LETTER_APOSTROPHE), EN_DASH)
			: Arrays.asList(APOSTROPHE, MINUS_SIGN));

		patternNoHyphen = RegexHelper.pattern("[" + StringUtils.join(retroCompatibilityNoHyphen) + "]");

		options.getNoHyphen()
			.addAll(retroCompatibilityNoHyphen);

		for(final String noHyphen : retroCompatibilityNoHyphen){
			final String line = ONE + noHyphen + ONE;
			if(!isRuleDuplicated(noHyphen, line, level))
				rules.get(level)
					.put(noHyphen, line);
		}
	}

	public static boolean isAugmentedRule(final CharSequence line){
		return StringUtils.contains(line, AUGMENTED_RULE);
	}

	public static boolean isCustomRule(final String line){
		return (!isAugmentedRule(line) && StringUtils.contains(line, EQUALS_SIGN));
	}

	private boolean isRuleDuplicated(final String key, final CharSequence line, final Level level){
		boolean duplicatedRule = false;
		final String foundNodeValue = rules.get(level)
			.get(key);
		if(foundNodeValue != null){
			final String clearedLine = RegexHelper.clear(line, PATTERN_REDUCE);
			final String clearedFoundNodeValue = RegexHelper.clear(foundNodeValue, PATTERN_REDUCE);
			duplicatedRule = (clearedLine.contains(clearedFoundNodeValue) || clearedFoundNodeValue.contains(clearedLine));
		}
		return duplicatedRule;
	}

	public final void clear(){
		secondLevelPresent = false;
		patternNoHyphen = null;
		patterns.clear();
		for(final Level lev : Level.values()){
			rules.get(lev)
				.clear();
			REDUCED_PATTERNS.get(lev)
				.clear();
		}
		for(final Map<String, String> elem : customHyphenations.values())
			elem.clear();
		options.clear();
	}

	/**
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param rule   The rule to add
	 * @param level   Level to add the rule to
	 * @return The value of a rule if already in place, {@code null} if the insertion has completed successfully
	 */
	public final String addRule(final String rule, final Level level){
		validateRule(rule, level);

		final String oldRule;
		if(isCustomRule(rule)){
			final String key = StringHelper.removeAll(rule, EQUALS_SIGN);
			oldRule = customHyphenations.get(level)
				.putIfAbsent(key, rule);
		}
		else{
			final String key = getKeyFromData(rule);
			final Map<String, String> rulesByLevel = rules.get(level);
			oldRule = rulesByLevel.get(key);
			if(oldRule == null){
				rulesByLevel.put(key, rule);

				buildTrie(level, rulesByLevel);
			}
		}
		return oldRule;
	}

	/**
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param rule	The rule to remove
	 * @param level	Level to remove the rule from
	 * @return {@code true} if the removal has completed successfully
	 */
	public final boolean removeRule(final String rule, final Level level){
		final String oldRule;
		if(isCustomRule(rule)){
			final String key = StringHelper.removeAll(rule, EQUALS_SIGN);
			oldRule = customHyphenations.get(level)
				.get(key);
			if(oldRule != null)
				customHyphenations.get(level)
					.remove(key);
		}
		else{
			final String key = getKeyFromData(rule);
			final Map<String, String> rulesByLevel = rules.get(level);
			oldRule = rulesByLevel.get(key);
			if(oldRule != null)
				buildTrie(level, rulesByLevel);
		}
		return (oldRule != null);
	}

	private void buildTrie(final Level level, final Map<String, String> rulesByLevel){
		final AhoCorasickTrie<String> trie = new AhoCorasickTrieBuilder<String>()
			.build(rulesByLevel);
		patterns.put(level, trie);
	}

	/**
	 * Line must contain exactly one hyphenation point.
	 *
	 * @param rule   Rule to be validated.
	 * @param level   Level to add the rule to.
	 */
	public static void validateRule(final String rule, final Level level){
		validateBasicRules(rule);

		String cleanedRule = rule;
		final int augmentedIndex = rule.indexOf(AUGMENTED_RULE);
		if(augmentedIndex >= 0){
			cleanedRule = rule.substring(0, augmentedIndex);
			validateAugmentedRule(cleanedRule, rule);
		}

		final Set<String> reducedPatterns = REDUCED_PATTERNS.get(level);
		ensureUniqueness(reducedPatterns, cleanedRule, rule);
	}

	private static void validateBasicRules(final CharSequence rule){
		if(!RegexHelper.find(rule, PATTERN_VALID_RULE))
			throw new LinterException(INVALID_RULE, rule);
		if(!StringUtils.contains(rule, EQUALS_SIGN)){
			if(!RegexHelper.find(rule, PATTERN_VALID_RULE_BREAK_POINTS))
				throw new LinterException(INVALID_HYPHENATION_POINT, rule);
			if(RegexHelper.find(rule, PATTERN_INVALID_RULE_START) || RegexHelper.find(rule, PATTERN_INVALID_RULE_END))
				throw new LinterException(INVALID_HYPHENATION_POINT_NEAR_DOT, rule);
		}
	}

	private static void validateAugmentedRule(final CharSequence cleanedRule, final String rule){
		final int count = RegexHelper.clear(cleanedRule, PATTERN_HYPHENATION_POINT).length();
		if(count != 1)
			throw new LinterException(MORE_HYPHENATION_DOTS, rule);

		final String[] parts = StringUtils.split(rule, COMMA);
		if(parts.length > 1){
			final int index = getIndexOfBreakpoint(rule);

			final int startIndex = extractStartIndex(parts);
			if(startIndex < 0 || startIndex >= index)
				throw new LinterException(AUGMENTED_RULE_INDEX_NOT_LESS_THAN, rule);
			final int length = extractLength(parts);
			if(length < 0 || startIndex + length < index)
				throw new LinterException(AUGMENTED_RULE_LENGTH_NOT_LESS_THAN, rule);
			if(startIndex + length >= parts[0].length())
				throw new LinterException(AUGMENTED_RULE_LENGTH_EXCEEDS, rule);
		}
	}

	private static int extractStartIndex(final String[] parts) throws NumberFormatException{
		return (parts[1] != null? Integer.parseInt(parts[1]) - 1: -1);
	}

	private static int extractLength(final String[] parts) throws NumberFormatException{
		return (parts.length > 2 && parts[2] != null? Integer.parseInt(parts[2]): 0);
	}

	/**
	 * A standard and a non-standard hyphenation pattern matching the same hyphenation point must not be on the same hyphenation level
	 * (for instance, c1 and zuc1ker/k=k,3,2 are invalid, while c1 and zuc3ker/k=k,3,2 are valid extended hyphenation patterns)
	 */
	private static void ensureUniqueness(final Iterable<String> reducedPatterns, final String cleanedRule, final String rule){
		String alreadyPresentRule = null;
		for(final String pattern : reducedPatterns)
			if(pattern.contains(cleanedRule) || cleanedRule.contains(pattern)){
				alreadyPresentRule = pattern;
				break;
			}
		if(alreadyPresentRule != null)
			throw new LinterException(DUPLICATED_RULE, rule, alreadyPresentRule);
	}

	public static int getIndexOfBreakpoint(final CharSequence rule){
		return RegexHelper.indexOf(rule, PATTERN_AUGMENTED_RULE_HYPHEN_INDEX);
	}

	public final void save(final File hypFile) throws IOException{
		final Charset charset = StandardCharsets.UTF_8;
		final Path hypPath = hypFile.toPath();
		try(final BufferedWriter writer = Files.newBufferedWriter(hypPath, charset)){
			writeln(writer, charset.name());

			writer.newLine();
			options.write(writer);

			writer.newLine();
			savePatternsByLevel(writer, Level.NON_COMPOUND);

			writer.newLine();
			writeln(writer, NEXT_LEVEL);

			writer.newLine();
			savePatternsByLevel(writer, Level.COMPOUND);
		}
	}

	private void savePatternsByLevel(final BufferedWriter writer, final Level level) throws IOException{
		final List<String> patternsByLevel = new ArrayList<>(rules.get(level).values());
		patternsByLevel.sort(comparator);
		for(final String pattern : patternsByLevel)
			writeln(writer, pattern);

		//write custom hyphenations
		final List<String> customs = new ArrayList<>(customHyphenations.get(level).values());
		customs.sort(comparator);
		for(final String rule : customs)
			writeln(writer, rule);
	}

	private static void writeln(final BufferedWriter writer, final String line) throws IOException{
		writer.write(line);
		writer.write(NEW_LINE);
	}

	/**
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param rule	The rule to be checked
	 * @param level	The level to check the rule for
	 * @return	Whether the hyphenator has the given rule
	 */
	public final boolean hasRule(final String rule, final Level level){
		return (isCustomRule(rule)
			? customHyphenations.get(level).containsValue(rule)
			: rules.get(level).containsKey(getKeyFromData(rule)));
	}

	public static String getKeyFromData(final CharSequence rule){
		return RegexHelper.clear(rule, PATTERN_KEY);
	}

}
