package unit731.hunspeller.parsers.dictionary;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import unit731.hunspeller.interfaces.Productable;
import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;


@Getter
public class DictionaryEntry implements Productable{

	private static final Matcher ENTRY_PATTERN = PatternService.matcher("^(?<word>[^\\t\\s\\/]+)(\\/(?<flags>[^\\t\\s]+))?(?:[\\t\\s]+(?<morphologicalFields>.+))?$");
	private static final Pattern REGEX_PATTERN_SEPARATOR = PatternService.pattern("[\\s\\t]+");


	@Setter
	private String word;
	private final String[] continuationFlags;
	private final String[] morphologicalFields;
	private final boolean combineable;

	private final FlagParsingStrategy strategy;


	public DictionaryEntry(String line, FlagParsingStrategy strategy){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		Matcher m = ENTRY_PATTERN.reset(line);
		if(!m.find())
			throw new IllegalArgumentException("Cannot parse dictionary line " + line);

		word = m.group("word");
		String dicFlags = m.group("flags");
		continuationFlags = strategy.parseFlags(dicFlags);
		String dicMorphologicalFields = m.group("morphologicalFields");
		morphologicalFields = (Objects.nonNull(dicMorphologicalFields)? PatternService.split(dicMorphologicalFields, REGEX_PATTERN_SEPARATOR): new String[0]);
		combineable = true;

		this.strategy = strategy;
	}

	public DictionaryEntry(Productable productable, String continuationFlags, FlagParsingStrategy strategy){
		Objects.requireNonNull(productable);
		Objects.requireNonNull(continuationFlags);
		Objects.requireNonNull(strategy);

		word = productable.getWord();
		this.continuationFlags = strategy.parseFlags(continuationFlags);
		morphologicalFields = productable.getMorphologicalFields();
		combineable = productable.isCombineable();

		this.strategy = strategy;
	}

	public List<String> getPrefixes(Function<String, RuleEntry> ruleEntryExtractor){
		return Arrays.stream(continuationFlags)
			.filter(rf -> {
				RuleEntry r = ruleEntryExtractor.apply(rf);
				return (Objects.nonNull(r) && !r.isSuffix());
			})
			.collect(Collectors.toList());
	}

	@Override
	public boolean containsContinuationFlag(String ... continuationFlags){
		for(String flag : this.continuationFlags)
			if(ArrayUtils.contains(continuationFlags, flag))
				return true;
		return false;
	}

	@Override
	public boolean containsMorphologicalField(String morphologicalField){
		return ArrayUtils.contains(morphologicalFields, morphologicalField);
	}

	@Override
	public String toString(){
		return word + strategy.joinFlags(continuationFlags);
	}

}
