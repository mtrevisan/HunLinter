package unit731.hunspeller.parsers.vos;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunspeller.parsers.enums.AffixType;
import unit731.hunspeller.services.PatternHelper;


public class AffixCondition{

	private static final Pattern PATTERN_CONDITION_SPLITTER = PatternHelper.pattern("(?<!\\[\\^?)(?![^\\[]*\\])");


	private final String[] condition;


	public AffixCondition(final String condition, final AffixType affixType){
		this.condition = PatternHelper.split(condition, PATTERN_CONDITION_SPLITTER);

		if(affixType == AffixType.SUFFIX)
			//invert condition
			Collections.reverse(Arrays.asList(this.condition));
	}

	public boolean match(final String word, final AffixType type){
		boolean match = false;

		final int size = word.length();
		//if the length of the condition is greater than the length of the word then the rule cannot be applied
		if(condition.length <= size){
			match = true;

			int idxWord = (type == AffixType.PREFIX? 0: size - 1);
			for(String conditionPart : condition){
				if(idxWord < 0 || idxWord >= size){
					match = false;
					break;
				}

				final char firstChar = conditionPart.charAt(0);
				if(firstChar != '.'){
					if(firstChar == '['){
						final boolean negatedGroup = (conditionPart.charAt(1) == '^');
						//extract inside of group
						conditionPart = conditionPart.substring(1 + (negatedGroup? 1: 0), conditionPart.length() - 1);
						match = (negatedGroup ^ conditionPart.indexOf(word.charAt(idxWord)) >= 0);
					}
					else
						match = (word.charAt(idxWord) == firstChar);

					if(!match)
						break;
				}

				idxWord += (type == AffixType.PREFIX? 1: -1);
			}
		}

		return match;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final AffixCondition rhs = (AffixCondition)obj;
		return new EqualsBuilder()
			.append(condition, rhs.condition)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(condition)
			.toHashCode();
	}

}
