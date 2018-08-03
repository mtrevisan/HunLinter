package unit731.hunspeller.parsers.affix.strategies;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;


/** Abstraction of the process of parsing flags taken from the affix and dic files */
public interface FlagParsingStrategy{

	@AllArgsConstructor
	@Getter
	static enum Type{
		ASCII(null, new ASCIIParsingStrategy()),
		UTF_8("UTF-8", new UTF8ParsingStrategy()),
		LONG("long", new DoubleASCIIParsingStrategy()),
		NUMERIC("num", new NumericalParsingStrategy());

		private final String code;
		private final FlagParsingStrategy stategy;

		public static Type toEnum(String flag){
			Type type = ASCII;
			if(!StringUtils.isBlank(flag)){
				type = null;
				for(Type t : values())
					if(flag.equals(t.getCode())){
						type = t;
						break;
					}
			}
			return type;
		}
	};


	/**
	 * Parses the given String into multiple flags
	 *
	 * @param textFlags	String to parse into flags
	 * @return Parsed flags
	 */
	String[] parseFlags(String textFlags);


	/**
	 * Compose the given array of String into one flag stream
	 *
	 * @param textFlags	Array of String to compose into flags
	 * @return Composed flags
	 */
	String joinFlags(String[] textFlags);

	default String[] removeDuplicates(String[] continuationFlags){
		Set<String> set = new HashSet<>(Arrays.asList(continuationFlags));
		return set.toArray(new String[set.size()]);
	}

	/**
	 * Extract each rule from a compound rule ("a*bc?" into ["a*", "b", "c?"])
	 *
	 * @param compoundRule	String to parse into flags
	 * @return Parsed flags
	 */
	List<String> extractCompoundRule(String compoundRule);

	default List<String> cleanCompoundRuleComponents(List<String> components){
		return components.stream()
			.map(this::cleanCompoundRuleComponent)
			.collect(Collectors.toList());
	}

	default String cleanCompoundRuleComponent(String component){
		int firstCharIndex = 0;
		int lastCharIndex = component.length() - 1;
		char chr = component.charAt(lastCharIndex);
		if(chr == '*' || chr == '?')
			lastCharIndex --;
		if(component.charAt(firstCharIndex) == '(' && component.charAt(lastCharIndex) == ')')
			firstCharIndex ++;
		return component.substring(firstCharIndex, lastCharIndex + 1);
	}

}
