/**
 * Copyright (c) 2019-2026 Mauro Trevisan
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

import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


public class RulesReducer3{

	private final Comparator<String> comparator;


	public RulesReducer3(final AffixData affixData){
		Objects.requireNonNull(affixData, "Affix data cannot be null");

		comparator = BaseBuilder.getComparator(affixData.getLanguage());
	}


	/**
	 * Reduces the provided collection of line entries into the absolute minimal set of Hunspell subrules.
	 *
	 * @param entries	The expanded dictionary entries to minimize.
	 * @return	A list of minimized LineEntry instances representing the optimal rule set.
	 */
	public List<LineEntry> reduceRules(final Collection<LineEntry> entries){
		if(entries == null || entries.isEmpty())
			return Collections.emptyList();

		final Map<String, List<LineEntry>> groupedByTransform = entries.stream()
			.collect(Collectors.groupingBy(e -> e.removal + "->" + e.addition));

		final List<LineEntry> finalizedList = new ArrayList<>();

		for(final Map.Entry<String, List<LineEntry>> group : groupedByTransform.entrySet()){
			final List<LineEntry> groupEntries = group.getValue();
			final LineEntry template = groupEntries.getFirst();
			final String removal = template.removal;

			final Set<String> internalWords = groupEntries.stream()
				.flatMap(e -> e.from.stream())
				.collect(Collectors.toSet());

			final Set<String> externalWords = entries.stream()
				.filter(e -> !e.removal.equals(removal) || !e.addition.equals(template.addition))
				.flatMap(e -> e.from.stream())
				.collect(Collectors.toSet());

			// If there are no external collisions on the base removal suffix
			if(externalWords.stream().noneMatch(w -> w.endsWith(removal))){
				final LineEntry reduced = new LineEntry(removal, template.addition, removal.isEmpty() ? "." : removal,
					internalWords);
				finalizedList.add(reduced);
				continue;
			}

			// Split internal words by the character immediately preceding the removal string
			final Map<String, Set<String>> tailGroups = new HashMap<>();
			for(final String word : internalWords){
				final int idx = word.length() - removal.length() - 1;
				final String tail = (idx >= 0? String.valueOf(word.charAt(idx)): StringUtils.EMPTY) + removal;
				tailGroups.computeIfAbsent(tail, k -> new HashSet<>()).add(word);
			}

			final Set<String> remainingWords = new HashSet<>(internalWords);
			for(final Map.Entry<String, Set<String>> tailEntry : tailGroups.entrySet()){
				final String tail = tailEntry.getKey();
				final Set<String> wordsInTail = tailEntry.getValue();

				// Check if this specific tail (e.g., "ƚo") collides with external words
				final List<String> collisions = externalWords.stream()
					.filter(w -> w.endsWith(tail))
					.toList();

				if(!collisions.isEmpty()){
					// Extract forbidden characters preceding this tail from external colliding words
					final Set<Character> forbiddenChars = new HashSet<>();
					boolean exactMatch = false;
					for(final String ext : collisions){
						final int idx = ext.length() - tail.length() - 1;
						if(idx >= 0)
							forbiddenChars.add(ext.charAt(idx));
						else
							exactMatch = true;
					}

					// Verify which internal characters are using this tail
					final Set<Character> internalChars = wordsInTail.stream()
						.map(w -> w.charAt(w.length() - tail.length() - 1))
						.collect(Collectors.toSet());

					forbiddenChars.removeAll(internalChars);

					if(!exactMatch && !forbiddenChars.isEmpty()){
						final LineEntry negatedRule = new LineEntry(removal, template.addition,
							RegexHelper.makeNotGroup(forbiddenChars, comparator) + tail, new HashSet<>(wordsInTail));
						finalizedList.add(negatedRule);
						remainingWords.removeAll(wordsInTail);
					}
				}
			}

			// For remaining words that do not have specific collisions or use other branches (e.g., "ro")
			if(!remainingWords.isEmpty()){
				final Set<Character> remainingPrefixes = remainingWords.stream()
					.map(w -> w.length() - removal.length() - 1 >= 0
						? w.charAt(w.length() - removal.length() - 1)
						: null)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

				// Collect external prefixes belonging only to layers with a matching or shorter removal length hierarchy
				final Set<Character> externalPrefixes = new HashSet<>();
				boolean exactExternalMatch = false;
				final Set<String> hierarchicallyRelevantExternalWords = entries.stream()
					.filter(e -> !e.removal.equals(removal) || !e.addition.equals(template.addition))
					.filter(e -> e.removal.length() <= removal.length() || e.removal.endsWith(removal))
					.flatMap(e -> e.from.stream())
					.collect(Collectors.toSet());
				for(final String ext : hierarchicallyRelevantExternalWords){
					if(ext.endsWith(removal)){
						final int idx = ext.length() - removal.length() - 1;
						if(idx >= 0)
							externalPrefixes.add(ext.charAt(idx));
						else
							exactExternalMatch = true;
					}
				}

				externalPrefixes.removeAll(remainingPrefixes);

				// Generate both inclusive and exclusive representations
				final String positiveGroup = (remainingPrefixes.isEmpty()
					? StringUtils.EMPTY
					: RegexHelper.makeGroup(remainingPrefixes, comparator)) + removal;
				final String negativeGroup = (exactExternalMatch || externalPrefixes.isEmpty()
					? StringUtils.EMPTY
					: RegexHelper.makeNotGroup(externalPrefixes, comparator)) + removal;
				// Calculate condition lengths using proper Hunspell rules metrics where [^a] = 1 and [rn] = 2
				final int positiveLen = RegexHelper.conditionLength(positiveGroup);
				final int negativeLen = (negativeGroup.isEmpty() ? Integer.MAX_VALUE : RegexHelper.conditionLength(negativeGroup));
				// Determine optimal condition based on Hunspell metrics and character class density
				final String optimalCondition;
				if(positiveLen < negativeLen)
					optimalCondition = positiveGroup;
				else if(negativeLen < positiveLen)
					optimalCondition = negativeGroup;
				else{
					// Tie-breaker when Hunspell lengths are identical (e.g., [dg]e vs [^mtv]e, or ro vs [^ƚ]o)
					// Prefer the negative class only if it's strictly leaner or equal in character density
					optimalCondition = (externalPrefixes.size() <= remainingPrefixes.size())
						? negativeGroup
						: positiveGroup;
				}

				final LineEntry genericRule = new LineEntry(removal, template.addition, optimalCondition, remainingWords);
				finalizedList.add(genericRule);
			}
		}

		finalizedList.sort((r1, r2) -> Integer.compare(RegexHelper.conditionLength(r2.condition),
			RegexHelper.conditionLength(r1.condition)));
		return finalizedList;
	}

}
