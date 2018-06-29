package unit731.hunspeller.parsers.dictionary;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;


@Slf4j
@EqualsAndHashCode(of = "entry")
public class AffixEntry{

	private static final Pattern PATTERN_SEPARATOR = PatternService.pattern("[\\s\\t]+");
	private static final Pattern PATTERN_SLASH = PatternService.pattern("/");
	private static final Pattern PATTERN_CONDITION_SPLITTER = PatternService.pattern("(?<!\\[\\^?)(?![^\\[]*\\])");
	private static final Matcher MATCHER_ENTRY = PatternService.matcher("\t.*$");

	private static final String DOT = ".";
	private static final String ZERO = "0";


	@AllArgsConstructor
	public static enum Type{
		SUFFIX("SFX"),
		PREFIX("PFX");


		@Getter
		private final String flag;

		public static Type toEnum(String flag){
			Type[] types = Type.values();
			for(Type type : types)
				if(type.getFlag().equals(flag))
					return type;
			return null;
		}
	};


	@Getter
	private final Type type;
	/** ID used to represent the affix */
	@Getter
	private final String flag;
	@Getter
	private final String[] ruleFlags;
	/** condition that must be met before the affix can be applied */
	@Getter
	private final String[] condition;
	/** string to strip */
	private final int removeLength;
	/** string to append */
	private final String add;
	@Getter
	private final String[] dataFields;

	private final String entry;


	public AffixEntry(String line, FlagParsingStrategy strategy){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		String[] lineParts = PatternService.split(line, PATTERN_SEPARATOR, 6);
		String ruleType = lineParts[0];
		this.flag = lineParts[1];
		String removal = lineParts[2];
		String[] additionParts = PatternService.split(lineParts[3], PATTERN_SLASH);
		String addition = additionParts[0];
		String cond = (lineParts.length > 4? lineParts[4]: DOT);
		dataFields = (lineParts.length > 5? PatternService.split(lineParts[5], PATTERN_SEPARATOR): new String[0]);

		type = Type.toEnum(ruleType);
		String[] classes = strategy.parseRuleFlags((additionParts.length > 1? additionParts[1]: null));
		ruleFlags = (classes.length > 0? classes: null);
		condition = PatternService.split(cond, PATTERN_CONDITION_SPLITTER);
		if(type == AffixEntry.Type.SUFFIX){
			//invert condition
			Collections.reverse(Arrays.asList(condition));
		}
		removeLength = (!ZERO.equals(removal)? removal.length(): 0);
		add = (!ZERO.equals(addition)? addition: StringUtils.EMPTY);

		if(removeLength > 0){
			if(isSuffix()){
				if(!cond.endsWith(removal))
					throw new IllegalArgumentException("This line has the condition part that not ends with the removal part: " + line);
				if(add.length() > 1 && removal.charAt(0) == add.charAt(0))
					throw new IllegalArgumentException("This line has characters in common between removed and added part: " + line);
			}
			else{
				if(!cond.startsWith(removal))
					throw new IllegalArgumentException("This line has the condition part that not starts with the removal part: " + line);
				if(add.length() > 1 && removal.charAt(removal.length() - 1) == add.charAt(add.length() - 1))
					throw new IllegalArgumentException("This line has characters in common between removed and added part: " + line);
			}
		}

		entry = PatternService.clear(line, MATCHER_ENTRY);
	}

	public final boolean isSuffix(){
		return (type == Type.SUFFIX);
	}

	public String applyRule(String word, boolean isFullstrip) throws IllegalArgumentException{
		if(!isFullstrip && removeLength > 0 && word.length() == removeLength)
			throw new IllegalArgumentException("Cannot strip full words without the flag FULLSTRIP");

		return (isSuffix()? word.substring(0, word.length() - removeLength) + add: add + word.substring(removeLength));
	}

	@Override
	public String toString(){
		return entry;
	}

}
