package io.github.mtrevisan.hunlinter.parsers.dictionary;

import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.services.RegexHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


public class RulesReducer2{

	private final Comparator<String> comparator;


	public RulesReducer2(final AffixData affixData){
		Objects.requireNonNull(affixData, "Affix data cannot be null");

		comparator = BaseBuilder.getComparator(affixData.getLanguage());
	}


	/**
	 * Reduces the provided collection of line entries into the absolute minimal set of Hunspell subrules.
	 * The algorithm initializes conditions to their absolute minimum (removal string) and expands them
	 * only when vertical collisions with other rules occur.
	 *
	 * @param entries The expanded dictionary entries to minimize.
	 * @return A list of minimized LineEntry instances representing the optimal rule set.
	 */
	public List<LineEntry> reduceRules(final Collection<LineEntry> entries){
		if(entries == null || entries.isEmpty())
			return Collections.emptyList();

		// 1. Group initial entries by their transformation operations (removal and addition)
		final Map<String, List<LineEntry>> groupedByTransform = entries.stream()
			.collect(Collectors.groupingBy(e -> e.removal + "->" + e.addition));

		// 2. Initialize each rule with its absolute minimal valid condition (the removal string itself)
		final List<LineEntry> rawRules = new ArrayList<>();
		for(Map.Entry<String, List<LineEntry>> group : groupedByTransform.entrySet()){
			final List<LineEntry> groupEntries = group.getValue();
			final List<String> sourceWords = groupEntries.stream()
				.flatMap(e -> e.from.stream())
				.distinct()
				.toList();

			// The condition cannot be shorter than removal. Initialize it exactly to removal.
			final LineEntry template = groupEntries.getFirst();
			final LineEntry newRule = new LineEntry(template.removal, template.addition, template.removal,
				new HashSet<>(sourceWords));
			rawRules.add(newRule);
		}

		// 3. Resolve vertical overlaps: generic rules must exclude starting branches of specific rules
		// Sort by raw condition length descending so that deeply specific rules are evaluated first
		rawRules.sort((r1, r2) -> Integer.compare(r2.condition.length(), r1.condition.length()));

		for(int i = 0; i < rawRules.size(); i++){
			final LineEntry genericRule = rawRules.get(i);
			final Set<Character> conflictingChars = new HashSet<>();

			// Compare with all other rules that are more specific (longer or more restrictive conditions)
			for(int j = 0; j < i; j++){
				final LineEntry specificRule = rawRules.get(j);

				// Clean up specific condition to extract the raw trailing characters (ignoring existing regex brackets)
				String cleanSpecificCond = stripRegexGroups(specificRule.condition);
				String cleanGenericCond = stripRegexGroups(genericRule.condition);

				// Check if the specific condition physically overlaps/ends with the generic condition
				if(cleanSpecificCond.endsWith(cleanGenericCond) && !cleanSpecificCond.equals(cleanGenericCond)){

					// Verify if any word from the specific rule would be falsely intercepted by this generic rule
					boolean causesCollision = specificRule.from.stream()
						.anyMatch(word -> word.endsWith(cleanGenericCond) && !genericRule.from.contains(word));

					if(causesCollision){
						// Extract the character immediately preceding the generic suffix match inside the specific rule
						int charPos = cleanSpecificCond.length() - cleanGenericCond.length() - 1;
						if(charPos >= 0){
							conflictingChars.add(cleanSpecificCond.charAt(charPos));
						}
					}
				}
			}

			// If conflicts are detected, evaluate whether an inclusion or exclusion class is shorter
			if(!conflictingChars.isEmpty()){
				// Determine all valid boundary characters present in the 'from' set for this generic rule
				final String cleanGeneric = stripRegexGroups(genericRule.condition);
				final Set<Character> validPrefixChars = genericRule.from.stream()
					.filter(word -> word.endsWith(cleanGeneric) && word.length() > cleanGeneric.length())
					.map(word -> word.charAt(word.length() - cleanGeneric.length() - 1))
					.collect(Collectors.toSet());

				// Compare token lengths: "[abc]" vs "[^def]"
				if(!validPrefixChars.isEmpty() && validPrefixChars.size() <= conflictingChars.size())
					// Inclusion class is shorter or equal (e.g., [dg] instead of [^mtv])
					genericRule.condition = RegexHelper.makeGroup(validPrefixChars, comparator) + cleanGeneric;
				else{
					// Exclusion class is shorter
					final String sortedExclusions = conflictingChars.stream()
						.sorted()
						.map(String::valueOf)
						.collect(Collectors.joining());
					genericRule.condition = "[^" + sortedExclusions + "]" + genericRule.condition;
				}
			}
			else if(genericRule.condition.isEmpty())
				genericRule.condition = ".";
		}

		// 5. Aggregate parallel rules that differ only by a single positive boundary character class
		return aggregateHorizontalConditions(rawRules);
	}

	/**
	 * Helper method to strip regex characters like [^abc] or [abc] to read the raw suffix context.
	 */
	private String stripRegexGroups(final String condition){
		if(condition.startsWith("[^")){
			int closeIdx = condition.indexOf(']');
			return closeIdx != -1 ? condition.substring(closeIdx + 1) : condition;
		}
		if(condition.startsWith("[")){
			int closeIdx = condition.indexOf(']');
			return closeIdx != -1 ? condition.substring(closeIdx + 1) : condition;
		}
		return condition;
	}

	/**
	 * Finds the longest matching trailing string across a list of words.
	 */
	private String extractMaxCommonSuffix(final List<String> words){
		if(words == null || words.isEmpty()){
			return "";
		}

		String base = words.getFirst();
		for(int i = 1; i < words.size(); i++){
			final String target = words.get(i);
			int matchCount = 0;
			while(matchCount < base.length() && matchCount < target.length() &&
				base.charAt(base.length() - 1 - matchCount) == target.charAt(target.length() - 1 - matchCount)){
				matchCount++;
			}
			base = base.substring(base.length() - matchCount);
			if(base.isEmpty()){
				break;
			}
		}
		return base;
	}

	/**
	 * Compresses parallel rules that contain matching conversions and identical suffix tails
	 * by creating positive character classes (e.g. [nr] instead of separate rules).
	 */
	private List<LineEntry> aggregateHorizontalConditions(final List<LineEntry> rules){
		final Map<String, List<LineEntry>> transformGroups = rules.stream()
			.collect(Collectors.groupingBy(r -> r.removal + "->" + r.addition));

		final List<LineEntry> finalizedList = new ArrayList<>();

		for(List<LineEntry> subGroup : transformGroups.values()){
			// Group rules based on the shared context remaining after trimming the first character
			final Map<String, List<LineEntry>> contextGroups = subGroup.stream()
				.collect(Collectors.groupingBy(r -> isolateConditionTail(r.condition)));

			for(Map.Entry<String, List<LineEntry>> contextEntry : contextGroups.entrySet()){
				final List<LineEntry> occurrences = contextEntry.getValue();

				// Only merge positive singular entries; do not alter negative set configurations ([^...])
				if(occurrences.size() > 1 && !occurrences.getFirst().condition.startsWith("[^")){
					final Set<Character> boundaryChars = new HashSet<>();
					for(LineEntry entry : occurrences)
						if(!entry.condition.isEmpty()){
							boundaryChars.add(entry.condition.charAt(0));
						}
					final String collectionStr = boundaryChars.stream()
						.sorted()
						.map(String::valueOf)
						.collect(Collectors.joining());
					final LineEntry masterRule = occurrences.getFirst();

					final LineEntry compressedRule = new LineEntry(masterRule.removal, masterRule.addition,
						"[" + collectionStr + "]" + contextEntry.getKey(),
						occurrences.stream().flatMap(o -> o.from.stream()).collect(Collectors.toSet()));
					finalizedList.add(compressedRule);
				}
				else{
					finalizedList.addAll(occurrences);
				}
			}
		}

		// Keep rules prioritized by condition length for top-down processing compliance in Hunspell
		finalizedList.sort((r1, r2) -> Integer.compare(r2.condition.length(), r1.condition.length()));
		return finalizedList;
	}

	/**
	 * Extracts the invariant structural suffix tail of a condition regex pattern.
	 */
	private String isolateConditionTail(final String condition){
		if(condition.startsWith("[^")){
			return condition;
		}

		if(condition.startsWith("[")){
			int closingIdx = condition.indexOf(']');
			return closingIdx != -1 ? condition.substring(closingIdx + 1) : condition;
		}
		return condition.length() > 1 ? condition.substring(1) : "";
	}

}
