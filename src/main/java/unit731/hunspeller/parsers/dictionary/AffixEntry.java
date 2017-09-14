package unit731.hunspeller.parsers.dictionary;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;


@Slf4j
@EqualsAndHashCode(of = "entry")
public class AffixEntry{

	private static final Matcher REGEX_ENTRY = Pattern.compile("\t.*$").matcher(StringUtils.EMPTY);

	private static final String SEPARATOR = "[\\s\\t]+";
	private static final String POINT = ".";
	private static final String SLASH = "/";
	private static final String ZERO = "0";

	private static final String REGEX_START_OF_LINE = "^";
	private static final String REGEX_END_OF_LINE = "$";


	@Getter
	public static enum TYPE{
		SUFFIX("SFX"),
		PREFIX("PFX");


		private final String flag;

		TYPE(String flag){
			this.flag = flag;
		}

		public static TYPE toEnum(String flag){
			TYPE[] types = TYPE.values();
			for(TYPE type : types)
				if(type.getFlag().equals(flag))
					return type;
			return null;
		}
	};


	@Getter private final TYPE type;
	//ID used to represent the affix
	@Getter private final String ruleFlag;
	@Getter private final String[] continuationClasses;
	@Getter private final Matcher match;
	//string to strip
	private final Matcher remove;
	//string to append
	private final String add;
	@Getter private final String[] dataFields;

	private final String entry;


	public AffixEntry(String line, FlagParsingStrategy strategy){
		Objects.nonNull(line);
		Objects.nonNull(strategy);

		String[] lineParts = line.split(SEPARATOR, 6);
		String ruleType = lineParts[0];
		this.ruleFlag = lineParts[1];
		String regexToRemove = lineParts[2];
		String[] additionParts = lineParts[3].split(SLASH);
		String addition = additionParts[0];
		String regexToMatch = lineParts[4];
		dataFields = (lineParts.length > 5? lineParts[5].split(SEPARATOR): new String[0]);

		type = TYPE.toEnum(ruleType);
		String[] classes = strategy.parseRuleFlags((additionParts.length > 1? additionParts[1]: null));
		continuationClasses = (classes.length > 0? classes: null);
		match = (!POINT.equals(regexToMatch)?
			Pattern.compile(isSuffix()? regexToMatch + REGEX_END_OF_LINE: REGEX_START_OF_LINE + regexToMatch).matcher(StringUtils.EMPTY): null);
		remove = (!ZERO.equals(regexToRemove)?
			Pattern.compile(isSuffix()? regexToRemove + REGEX_END_OF_LINE: REGEX_START_OF_LINE + regexToRemove).matcher(StringUtils.EMPTY): null);
		add = (ZERO.equals(addition)? StringUtils.EMPTY: addition);

		if(isSuffix() && StringUtils.isNotBlank(regexToRemove) && addition.length() > 1 && regexToRemove.charAt(0) == addition.charAt(0))
			log.warn("This line has characters in common between removed and added part: " + line);

		entry = PatternService.replaceFirst(line, REGEX_ENTRY, StringUtils.EMPTY);
	}

	public final boolean isSuffix(){
		return (type == TYPE.SUFFIX);
	}

	public String applyRule(String word, boolean isFullstrip) throws IllegalArgumentException{
		if(remove != null){
			word = PatternService.replaceFirst(word, remove, StringUtils.EMPTY);
			if(word.isEmpty() && !isFullstrip)
				throw new IllegalArgumentException("Cannot strip full words without the flag FULLSTRIP");
		}

		return (isSuffix()? word + add: add + word);
	}

	@Override
	public String toString(){
		return entry;
	}

}
