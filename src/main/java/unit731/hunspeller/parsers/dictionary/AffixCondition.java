package unit731.hunspeller.parsers.dictionary;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


public class AffixCondition{

	private static final Pattern PATTERN_CONDITION_SPLITTER = PatternService.pattern("(?<!\\[\\^?)(?![^\\[]*\\])");


	private final String[] condition;


	public AffixCondition(String condition, AffixEntry.Type affixType){
		this.condition = PatternService.split(condition, PATTERN_CONDITION_SPLITTER);

		if(affixType == AffixEntry.Type.SUFFIX)
			//invert condition
			Collections.reverse(Arrays.asList(this.condition));
	}

	public boolean match(String word, AffixEntry.Type type){
		boolean match = false;

		int size = word.length();
		//if the length of the condition is greater than the length of the word then the rule cannot be applied
		if(condition.length <= size){
			match = true;

			int idxWord = (type == AffixEntry.Type.PREFIX? 0: size - 1);
			for(String conditionPart : condition){
				if(idxWord < 0 || idxWord >= size){
					match = false;
					break;
				}

				char firstChar = conditionPart.charAt(0);
				if(firstChar != '.'){
					if(firstChar == '['){
						boolean negatedGroup = (conditionPart.charAt(1) == '^');
						conditionPart = conditionPart.substring(1 + (negatedGroup? 1: 0), conditionPart.length() - 1);
						match = (negatedGroup ^ StringUtils.contains(conditionPart, word.charAt(idxWord)));
					}
					else
						match = (word.charAt(idxWord) == firstChar);

					if(!match)
						break;
				}

				idxWord += (type == AffixEntry.Type.PREFIX? 1: -1);
			}
		}

		return match;
	}

}
