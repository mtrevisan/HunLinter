package unit731.hunspeller.parsers.hyphenation;

import unit731.hunspeller.parsers.hyphenation.dtos.HyphenationInterface;
import unit731.hunspeller.parsers.hyphenation.dtos.CompoundHyphenation;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import static unit731.hunspeller.parsers.hyphenation.HyphenationParser.SOFT_HYPHEN;
import unit731.hunspeller.services.PatternService;


@AllArgsConstructor
public abstract class AbstractHyphenator{

	protected static final Matcher MATCHER_WORD_BOUNDARIES = PatternService.matcher(Pattern.quote(HyphenationParser.WORD_BOUNDARY));


	@NonNull
	protected final HyphenationParser hypParser;


	/**
	 * Performs hyphenation
	 * NOTE: Calling the method {@link Orthography.#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @return the hyphenation object(s)
	 */
	public HyphenationInterface hyphenate(String word){
		hypParser.acquireLock();

		try{
			return hyphenate(word, hypParser.getPatterns());
		}
		finally{
			hypParser.releaseLock();
		}
	}

	/**
	 * Performs hyphenation including an additional rule
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @param addedRule	Rule to add to the set of rules that will generate the hyphenation
	 * @param level	The level to add the rule to
	 * @return the hyphenation object
	 * @throws CloneNotSupportedException	If the radix tree does not support the {@code Cloneable} interface
	 */
	public HyphenationInterface hyphenate(String word, String addedRule, HyphenationParser.Level level) throws CloneNotSupportedException{
		hypParser.acquireLock();

		try{
			String key = HyphenationParser.getKeyFromData(addedRule);
			Map<HyphenationParser.Level, RadixTree<String, String>> patterns = hypParser.getPatterns();
			HyphenationInterface hyph = null;
			if(!patterns.get(level).containsKey(key)){
				patterns.get(level).put(key, addedRule);

				hyph = hyphenate(word, patterns);

				patterns.get(level).remove(key);
			}
			return hyph;
		}
		finally{
			hypParser.releaseLock();
		}
	}

	/**
	 * Performs hyphenation
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @param patterns	The radix tree containing the patterns
	 * @return the hyphenation object
	 */
	public HyphenationInterface hyphenate(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns){
		//apply first level hyphenation
		HyphenationInterface result = hyphenate(word, patterns, HyphenationParser.Level.FIRST, SOFT_HYPHEN, false);

		//if there is a second level
		if(!patterns.get(HyphenationParser.Level.SECOND).isEmpty()){
			//apply second level hyphenation for the word parts
			List<HyphenationInterface> subHyphenations = result.getSyllabes().stream()
				.map(compound -> hyphenate(compound, patterns, HyphenationParser.Level.SECOND, SOFT_HYPHEN, true))
				.collect(Collectors.toList());
			result = CompoundHyphenation.build(subHyphenations);
		}

		return result;
	}

	/**
	 * Performs hyphenation
	 * NOTE: Calling the method {@link Orthography#correctOrthography(String)} may be necessary
	 *
	 * @param word	String to hyphenate
	 * @param patterns	The radix tree containing the patterns
	 * @param level	Level at which to hyphenate
	 * @param breakCharacter	Character to add to mark breakpoints
	 * @param isCompound	Whether the word is part of a compounded word
	 * @return the hyphenation object
	 */
	protected abstract HyphenationInterface hyphenate(String word, Map<HyphenationParser.Level, RadixTree<String, String>> patterns, HyphenationParser.Level level, String breakCharacter,
		boolean isCompound);

}
