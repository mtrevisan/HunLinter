package unit731.hunspeller.parsers.dictionary;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;


@Getter
public class DictionaryEntry implements Productable{

	private static final Matcher ENTRY_PATTERN = PatternService.matcher("^(?<word>[^\\t\\s\\/]+)(\\/(?<flags>[^\\t\\s]+))?(?:[\\t\\s]+(?<dataFields>.+))?$");
	private static final Pattern REGEX_PATTERN_SEPARATOR = PatternService.pattern("[\\s\\t]+");

	private static final String TAB = "\t";


	private final String word;
	private final String[] ruleFlags;
	private final String[] dataFields;

	private final FlagParsingStrategy strategy;


	public DictionaryEntry(RuleProductionEntry production, FlagParsingStrategy strategy){
		this(production.getWord(), production.getRuleFlags(), production.getDataFields(), strategy);
	}

	public DictionaryEntry(String word, String[] dicFlags, String[] dataFields, FlagParsingStrategy strategy){
		Objects.requireNonNull(word);
		Objects.requireNonNull(dicFlags);
		Objects.requireNonNull(dataFields);
		Objects.requireNonNull(strategy);

		this.word = word;
		ruleFlags = (dicFlags != null? dicFlags: new String[0]);
		this.dataFields = dataFields;

		this.strategy = strategy;
	}

	public DictionaryEntry(String line, FlagParsingStrategy strategy){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		Matcher m = ENTRY_PATTERN.reset(line);
		if(!m.find())
			throw new IllegalArgumentException("Cannot parse dictionary line " + line);

		word = m.group("word");
		String dicFlags = m.group("flags");
		ruleFlags = strategy.parseRuleFlags(dicFlags);
		String dicDataFields = m.group("dataFields");
		dataFields = (dicDataFields != null? PatternService.split(dicDataFields, REGEX_PATTERN_SEPARATOR): new String[0]);

		this.strategy = strategy;
	}

	public List<String> getPrefixes(Function<String, RuleEntry> ruleEntryExtractor){
		return Arrays.stream(ruleFlags)
			.filter(rf -> {
				RuleEntry r = ruleEntryExtractor.apply(rf);
				return (r != null && !r.isSuffix());
			})
			.collect(Collectors.toList());
	}

	public boolean containsRuleFlag(String ruleFlag){
		return Arrays.stream(ruleFlags)
			.anyMatch(flag -> flag.equals(ruleFlag));
	}

	public boolean containsDataField(String dataField){
		return Arrays.stream(dataFields)
			.anyMatch(field -> field.equals(dataField));
	}

	@Override
	public String toString(){
		StringJoiner sj = (new StringJoiner(StringUtils.EMPTY))
			.add(word)
			.add(strategy.joinRuleFlags(ruleFlags));
		if(dataFields != null && dataFields.length > 0)
			sj.add(TAB)
				.add(String.join(StringUtils.EMPTY, dataFields));
		return sj.toString();
	}

	public String toWordAndFlagString(){
		return (new StringJoiner(StringUtils.EMPTY))
			.add(word)
			.add(strategy.joinRuleFlags(ruleFlags))
			.toString();
	}

}
