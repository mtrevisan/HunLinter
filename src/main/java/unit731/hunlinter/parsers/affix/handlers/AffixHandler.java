package unit731.hunlinter.parsers.affix.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunlinter.parsers.enums.AffixOption;
import unit731.hunlinter.parsers.affix.ParsingContext;
import unit731.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunlinter.parsers.enums.AffixType;
import unit731.hunlinter.parsers.vos.RuleEntry;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.ParserHelper;


public class AffixHandler implements Handler{

	private static final MessageFormat BAD_THIRD_PARAMETER = new MessageFormat("Error reading line ''{0}'': the third parameter is not a number");
	private static final MessageFormat BAD_NUMBER_OF_ENTRIES = new MessageFormat("Error reading line ''{0}'': bad number of entries, ''{1}'' must be a positive integer");
	private static final MessageFormat DUPLICATED_LINE = new MessageFormat("Duplicated line");
	private static final MessageFormat MISMATCHED_RULE_TYPE = new MessageFormat("Mismatched rule type (expected ''{0}'')");
	private static final MessageFormat MISMATCHED_RULE_FLAG = new MessageFormat("Mismatched rule flag (expected ''{0}'')");


	@Override
	public void parse(final ParsingContext context, final FlagParsingStrategy strategy, final BiConsumer<String, Object> addData,
			final Function<AffixOption, List<String>> getData){
		try{
			final boolean isSuffix = AffixType.SUFFIX.is(context.getRuleType());
			final String ruleFlag = context.getFirstParameter();
			final char combinable = context.getSecondParameter().charAt(0);
			if(!NumberUtils.isCreatable(context.getThirdParameter()))
				throw new LinterException(BAD_THIRD_PARAMETER.format(new Object[]{context}));

			final List<AffixEntry> entries = readEntries(context, strategy, getData);

			addData.accept(ruleFlag, new RuleEntry(isSuffix, combinable, entries));
		}
		catch(final IOException e){
			throw new RuntimeException(e.getMessage());
		}
	}

	private List<AffixEntry> readEntries(final ParsingContext context, final FlagParsingStrategy strategy,
			final Function<AffixOption, List<String>> getData) throws IOException{
		final int numEntries = Integer.parseInt(context.getThirdParameter());
		if(numEntries <= 0)
			throw new LinterException(BAD_NUMBER_OF_ENTRIES.format(new Object[]{context, context.getThirdParameter()}));

		final BufferedReader br = context.getReader();
		final AffixType ruleType = AffixType.createFromCode(context.getRuleType());
		final String ruleFlag = context.getFirstParameter();

		//List<AffixEntry> prefixEntries = new ArrayList<>();
		//List<AffixEntry> suffixEntries = new ArrayList<>();
		final List<String> aliasesFlag = getData.apply(AffixOption.ALIASES_FLAG);
		final List<String> aliasesMorphologicalField = getData.apply(AffixOption.ALIASES_MORPHOLOGICAL_FIELD);
		String line;
		final List<AffixEntry> entries = new ArrayList<>(numEntries);
		for(int i = 0; i < numEntries; i ++){
			line = ParserHelper.extractLine(br);

			final AffixEntry entry = new AffixEntry(line, strategy, aliasesFlag, aliasesMorphologicalField);

			checkValidity(entry, ruleType, ruleFlag);

			if(entries.contains(entry))
				throw new LinterException(DUPLICATED_LINE.format(new Object[0]));

			final boolean inserted = entries.add(entry);

//String regexToMatch = (entry.getMatch() != null? entry.getMatch().pattern().pattern().replaceFirst("^\\^", StringUtils.EMPTY).replaceFirst("\\$$", StringUtils.EMPTY): ".");
//String[] arr = RegExpTrieSequencer.extractCharacters(regexToMatch);
//List<AffixEntry> lst = new ArrayList<>();
//lst.add(entry);
//if(entry.isSuffix()){
//	ArrayUtils.reverse(arr);
//	suffixEntries.add(arr, lst);
//}
//else
//	prefixEntries.put(arr, lst);
		}
		return entries;
	}

	private void checkValidity(final AffixEntry entry, final AffixType ruleType, final String ruleFlag){
		if(entry.getType() != ruleType)
			throw new LinterException(MISMATCHED_RULE_TYPE.format(new Object[]{ruleType}));
		if(!ruleFlag.equals(entry.getFlag()))
			throw new LinterException(MISMATCHED_RULE_FLAG.format(new Object[]{ruleFlag}));

		entry.validate();
	}

}
