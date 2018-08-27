package unit731.hunspeller.parsers.affix;

import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.affix.strategies.ASCIIParsingStrategy;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


/**
 * Managed options:
 *		SET, FLAG, COMPLEXPREFIXES, LANG, AF, AM
 *		REP
 *		COMPOUNDRULE, COMPOUNDMIN, COMPOUNDFLAG, ONLYINCOMPOUND, COMPOUNDPERMITFLAG, COMPOUNDMORESUFFIXES, COMPOUNDFORBIDFLAG, COMPOUNDWORDMAX,
 *			CHECKCOMPOUNDDUP, CIRCUMFIX, CHECKCOMPOUNDCASE, CHECKCOMPOUNDTRIPLE, SIMPLIFIEDTRIPLE
 *		PFX, SFX
 *		FULLSTRIP, KEEPCASE, NEEDAFFIX, ICONV, OCONV
 */
public class AffixParser extends ReadWriteLockable{

	private static final String NO_LANGUAGE = "xxx";

	private static final String START = "^";
	private static final String END = "$";

	private static final Matcher MATCHER_ISO639_1 = PatternHelper.matcher("([a-z]{2})");
	private static final Matcher MATCHER_ISO639_2 = PatternHelper.matcher("([a-z]{2,3}(?:[-_\\/][a-z]{2,3})?)");

	private static final String DOUBLE_MINUS_SIGN = HyphenationParser.MINUS_SIGN + HyphenationParser.MINUS_SIGN;


	@AllArgsConstructor
	private static enum AliasesType{
		FLAG(AffixTag.ALIASES_FLAG),
		MORPHOLOGICAL_FIELD(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);


		@Getter
		private final AffixTag flag;

		public static AliasesType toEnum(String flag){
			AliasesType[] types = AliasesType.values();
			for(AliasesType type : types)
				if(type.getFlag().getCode().equals(flag))
					return type;
			return null;
		}
	}

	@AllArgsConstructor
	private static enum ConversionTableType{
		REPLACEMENT(AffixTag.REPLACEMENT_TABLE),
		INPUT(AffixTag.INPUT_CONVERSION_TABLE),
		OUTPUT(AffixTag.OUTPUT_CONVERSION_TABLE);


		@Getter
		private final AffixTag flag;

		public static ConversionTableType toEnum(String flag){
			ConversionTableType[] types = ConversionTableType.values();
			for(ConversionTableType type : types)
				if(type.getFlag().getCode().equals(flag))
					return type;
			return null;
		}
	}


	private final Map<String, Object> data = new HashMap<>();
	private Charset charset;
	private FlagParsingStrategy strategy = new ASCIIParsingStrategy();

	private final Set<String> terminalAffixes = new HashSet<>();


	private final Consumer<ParsingContext> FUN_COPY_OVER = context -> {
		addData(context.getRuleType(), context.getAllButFirstParameter());
	};
	private final Consumer<ParsingContext> FUN_COPY_OVER_AS_NUMBER = context -> {
		if(!NumberUtils.isCreatable(context.getFirstParameter()))
			throw new IllegalArgumentException("Error reading line \"" + context.toString()
				+ "\": The first parameter is not a number");
		addData(context.getRuleType(), Integer.parseInt(context.getAllButFirstParameter()));
	};
	private final Consumer<ParsingContext> FUN_COMPOUND_RULE = context -> {
		try{
			BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ "\": The first parameter is not a number");
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ ": Bad number of entries, it must be a positive integer");

			Set<String> compoundRules = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				line = DictionaryParser.cleanLine(line);

				String[] lineParts = StringUtils.split(line);
				AffixTag tag = AffixTag.toEnum(lineParts[0]);
				if(tag != AffixTag.COMPOUND_RULE)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": mismatched compound rule type (expected "
						+ AffixTag.COMPOUND_RULE + ")");
				String rule = lineParts[1];
				if(StringUtils.isBlank(rule))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": compound rule type cannot be empty");
				List<String> compounds = strategy.extractCompoundRule(rule);
				if(compounds.isEmpty())
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": compound rule is bad formatted");

				boolean inserted = compoundRules.add(rule);
				if(!inserted)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": duplicated line");
			}

			addData(AffixTag.COMPOUND_RULE, compoundRules);
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	};
	private final Consumer<ParsingContext> FUN_AFFIX = context -> {
		try{
			AffixEntry.Type ruleType = AffixEntry.Type.toEnum(context.getRuleType());
			BufferedReader br = context.getReader();
			boolean isSuffix = context.isSuffix();
			String ruleFlag = context.getFirstParameter();
			char combineable = context.getSecondParameter().charAt(0);
			if(!NumberUtils.isCreatable(context.getThirdParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ "\": The third parameter is not a number");
			int numEntries = Integer.parseInt(context.getThirdParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ ": Bad number of entries, it must be a positive integer");

//List<AffixEntry> prefixEntries = new ArrayList<>();
//List<AffixEntry> suffixEntries = new ArrayList<>();
			List<String> aliasesFlag = getData(AffixTag.ALIASES_FLAG);
			List<String> aliasesMorphologicalField = getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);
			List<AffixEntry> entries = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				line = DictionaryParser.cleanLine(line);

				AffixEntry entry = new AffixEntry(line, strategy, aliasesFlag, aliasesMorphologicalField);
				if(entry.getType() != ruleType)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": mismatched rule type (expected "
						+ ruleType + ")");
				if(!ruleFlag.equals(entry.getFlag()))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": mismatched rule flag (expected "
						+ ruleFlag + ")");
				if(!entry.containsUniqueContinuationFlags())
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": multiple rule flags");

				boolean inserted = entries.add(entry);
				if(!inserted)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": duplicated line");

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

			addData(ruleFlag, new RuleEntry(isSuffix, combineable, entries));
//addData(ruleFlag, new RuleEntry(isSuffix, combineable, entries, prefixEntries, suffixEntries));
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	};
	private final Consumer<ParsingContext> FUN_WORD_BREAK_TABLE = context -> {
		try{
			BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ "\": The first parameter is not a number");
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ ": Bad number of entries, it must be a positive integer");

			Set<String> wordBreakCharacters = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				line = DictionaryParser.cleanLine(line);

				String[] lineParts = StringUtils.split(line);
				AffixTag tag = AffixTag.toEnum(lineParts[0]);
				if(tag != AffixTag.BREAK)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": mismatched type (expected "
						+ AffixTag.BREAK + ")");
				String breakCharacter = lineParts[1];
				if(StringUtils.isBlank(breakCharacter))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": break character cannot be empty");
				if(DOUBLE_MINUS_SIGN.equals(breakCharacter))
					breakCharacter = HyphenationParser.EN_DASH;

				boolean inserted = wordBreakCharacters.add(breakCharacter);
				if(!inserted)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": duplicated line");
			}

			addData(AffixTag.BREAK, wordBreakCharacters);
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	};
	private final Consumer<ParsingContext> FUN_ALIASES = context -> {
		try{
			AliasesType aliasesType = AliasesType.toEnum(context.getRuleType());
			BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ "\": The first parameter is not a number");
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ ": Bad number of entries, it must be a positive integer");

			List<String> aliases = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				line = DictionaryParser.cleanLine(line);

				String[] parts = StringUtils.split(line);
				if(parts.length != 2)
					throw new IllegalArgumentException("Error reading line \"" + context.toString()
						+ ": Bad number of entries, it must be <tag> <flag/morphological field>");
				if(!aliasesType.getFlag().getCode().equals(parts[0]))
					throw new IllegalArgumentException("Error reading line \"" + context.toString()
						+ ": Bad tag, it must be " + aliasesType.getFlag().getCode());

				aliases.add(parts[1]);
			}

			addData(aliasesType.getFlag(), aliases);
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	};
	private final Consumer<ParsingContext> FUN_CONVERSION_TABLE = context -> {
		try{
			ConversionTableType conversionTableType = ConversionTableType.toEnum(context.getRuleType());
			AffixTag tag = conversionTableType.getFlag();
			BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ "\": The first parameter is not a number");
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ ": Bad number of entries, it must be a positive integer");

			List<Pair<String, String>> conversionTable = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				line = DictionaryParser.cleanLine(line);

				String[] parts = StringUtils.split(line);
				if(parts.length != 3)
					throw new IllegalArgumentException("Error reading line \"" + context.toString()
						+ ": Bad number of entries, it must be <tag> <pattern-from> <pattern-to>");
				if(!tag.getCode().equals(parts[0]))
					throw new IllegalArgumentException("Error reading line \"" + context.toString()
						+ ": Bad tag, it must be " + tag.getCode());

				conversionTable.add(Pair.of(parts[1], StringUtils.replaceChars(parts[2], '_', ' ')));
			}

			addData(tag, conversionTable);
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	};

	private final Map<AffixTag, Consumer<ParsingContext>> RULE_FUNCTION = new HashMap<>();


	public AffixParser(){
		//General options
//		RULE_FUNCTION.put("NAME", FUN_COPY_OVER);
//		RULE_FUNCTION.put("VERSION", FUN_COPY_OVER);
//		RULE_FUNCTION.put("HOME", FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.CHARACTER_SET, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.FLAG, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.COMPLEX_PREFIXES, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.LANGUAGE, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.IGNORE, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.ALIASES_FLAG, FUN_ALIASES);
		RULE_FUNCTION.put(AffixTag.ALIASES_MORPHOLOGICAL_FIELD, FUN_ALIASES);
		//Options for suggestions
//		RULE_FUNCTION.put(AffixTag.KEY, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.TRY, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.NO_SUGGEST, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.NO_NGRAM_SUGGEST, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.REPLACEMENT_TABLE, FUN_CONVERSION_TABLE);
//		RULE_FUNCTION.put(AffixTag.MAP_TABLE, FUN_MAP);
		//Options for compounding
		//default break table contains: "-", "^-", and "-$"
		RULE_FUNCTION.put(AffixTag.BREAK, FUN_WORD_BREAK_TABLE);
		RULE_FUNCTION.put(AffixTag.COMPOUND_RULE, FUN_COMPOUND_RULE);
		RULE_FUNCTION.put(AffixTag.COMPOUND_MIN, FUN_COPY_OVER_AS_NUMBER);
		RULE_FUNCTION.put(AffixTag.COMPOUND_FLAG, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_BEGIN, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_MIDDLE, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_END, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.ONLY_IN_COMPOUND, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.COMPOUND_PERMIT_FLAG, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.COMPOUND_MORE_SUFFIXES, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_ROOT, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.COMPOUND_FORBID_FLAG, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.COMPOUND_WORD_MAX, FUN_COPY_OVER_AS_NUMBER);
		RULE_FUNCTION.put(AffixTag.CHECK_COMPOUND_DUPLICATION, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.CHECK_COMPOUND_REPLACEMENT, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.CHECK_COMPOUND_CASE, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.CHECK_COMPOUND_TRIPLE, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.SIMPLIFIED_TRIPLE, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.CIRCUMFIX, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.FORBIDDEN_WORD, FUN_COPY_OVER);
		//Options for affix creation
		RULE_FUNCTION.put(AffixTag.PREFIX, FUN_AFFIX);
		RULE_FUNCTION.put(AffixTag.SUFFIX, FUN_AFFIX);
		//Other options
		RULE_FUNCTION.put(AffixTag.FULLSTRIP, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.KEEP_CASE, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.NEED_AFFIX, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.INPUT_CONVERSION_TABLE, FUN_CONVERSION_TABLE);
		RULE_FUNCTION.put(AffixTag.OUTPUT_CONVERSION_TABLE, FUN_CONVERSION_TABLE);
//		RULE_FUNCTION.put(AffixTag.WORD_CHARS, FUN_COPY_OVER);
	}

	/**
	 * Parse the rules out from a .aff file.
	 *
	 * @param affFile	The content of the affix file
	 * @throws IOException	If an I/O error occurs
	 * @throws	IllegalArgumentException	If something is wrong while parsing the file (eg. missing rule)
	 */
	public void parse(File affFile) throws IOException, IllegalArgumentException{
		acquireWriteLock();
		try{
			clearData();

			boolean encodingRead = false;
			charset = FileHelper.determineCharset(affFile.toPath());
			try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(affFile.toPath(), charset))){
				String line;
				while((line = br.readLine()) != null){
					//ignore any BOM marker on first line
					if(br.getLineNumber() == 1)
						line = FileHelper.clearBOMMarker(line);

					line = DictionaryParser.cleanLine(line);
					if(line.isEmpty())
						continue;

					if(!encodingRead && !line.startsWith(AffixTag.CHARACTER_SET.getCode() + StringUtils.SPACE))
						throw new IllegalArgumentException("The first non–comment line in the affix file must be a 'SET charset', was: '" + line + "'");
					else
						encodingRead = true;

					ParsingContext context = new ParsingContext(line, br);
					AffixTag ruleType = AffixTag.toEnum(context.getRuleType());
					Consumer<ParsingContext> fun = RULE_FUNCTION.get(ruleType);
					if(fun != null){
						try{
							fun.accept(context);

							if(ruleType == AffixTag.FLAG){
								String flag = getFlag();
								//determines the appropriate {@link FlagParsingStrategy} based on the FLAG definition line taken from the affix file
								strategy = FlagParsingStrategy.Type.toEnum(flag).getStategy();
								if(strategy == null)
									throw new IllegalArgumentException("Unknown flag type: " + flag);
							}
						}
						catch(RuntimeException e){
							throw new IllegalArgumentException(e.getMessage() + " on line " + br.getLineNumber());
						}
					}
				}
			}

			if(!containsData(AffixTag.COMPOUND_MIN))
				addData(AffixTag.COMPOUND_MIN, 3);
			else{
				int compoundMin = getData(AffixTag.COMPOUND_MIN);
				if(compoundMin < 1)
					addData(AffixTag.COMPOUND_MIN, 1);
			}
			//apply default charset
			if(!containsData(AffixTag.CHARACTER_SET))
				addData(AffixTag.CHARACTER_SET, StandardCharsets.ISO_8859_1);
			if(!containsData(AffixTag.LANGUAGE)){
				//try to infer language from filename
				String filename = FilenameUtils.removeExtension(affFile.getName());
				List<String> languages = PatternHelper.extract(filename, MATCHER_ISO639_2);
				if(languages.isEmpty())
					languages = PatternHelper.extract(filename, MATCHER_ISO639_1);
				String language = (!languages.isEmpty()? languages.get(0): NO_LANGUAGE);
				addData(AffixTag.LANGUAGE, language);
			}
			if(!containsData(AffixTag.BREAK)){
				Set<String> wordBreakCharacters = new HashSet<>(3);
				wordBreakCharacters.add(HyphenationParser.MINUS_SIGN);
				wordBreakCharacters.add(START + HyphenationParser.MINUS_SIGN);
				wordBreakCharacters.add(HyphenationParser.MINUS_SIGN + END);
				addData(AffixTag.BREAK, wordBreakCharacters);
			}
			//swap tags:
			if(isComplexPrefixes()){
//				String compoundBegin = getData(AffixTag.COMPOUND_BEGIN);
//				String compoundEnd = getData(AffixTag.COMPOUND_END);
//				addData(AffixTag.COMPOUND_BEGIN, compoundEnd);
//				addData(AffixTag.COMPOUND_END, compoundBegin);

				RuleEntry prefixes = getData(AffixTag.PREFIX);
				RuleEntry suffixes = getData(AffixTag.SUFFIX);
				addData(AffixTag.PREFIX, suffixes);
				addData(AffixTag.SUFFIX, prefixes);
			}
//			if(!containsData(AffixTag.KEY))
//				addData(AffixTag.KEY, "qwertyuiop|asdfghjkl|zxcvbnm");


			terminalAffixes.add(getCompoundFlag());
			terminalAffixes.add(getOnlyInCompoundFlag());
			terminalAffixes.add(getPermitCompoundFlag());
			terminalAffixes.add(getForbidCompoundFlag());
			terminalAffixes.add(getCircumfixFlag());
			terminalAffixes.add(getKeepCaseFlag());
			terminalAffixes.add(getNeedAffixFlag());
		}
		finally{
			releaseWriteLock();
		}

//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(data));
//7 490 848 B
	}

	public void clear(){
		clearData();
	}

	private boolean containsData(AffixTag key){
		return containsData(key.getCode());
	}

	private boolean containsData(String key){
		return data.containsKey(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T getData(AffixTag key){
		return getData(key.getCode());
	}

	@SuppressWarnings("unchecked")
	public <T> T getData(String key){
		return (T)data.get(key);
	}

	private <T> void addData(AffixTag key, T value){
		addData(key.getCode(), value);
	}

	@SuppressWarnings("unchecked")
	private <T> void addData(String key, T value){
		T prevValue = (T)data.put(key, value);

		if(prevValue != null)
			throw new IllegalArgumentException("Duplicated flag: " + key);
	}

	private void clearData(){
		acquireWriteLock();
		try{
			data.clear();
			terminalAffixes.clear();
		}
		finally{
			releaseWriteLock();
		}
	}

	public String getLanguage(){
		return getData(AffixTag.LANGUAGE);
	}

	public String getKeepCaseFlag(){
		return getData(AffixTag.KEEP_CASE);
	}

	public String getNeedAffixFlag(){
		return getData(AffixTag.NEED_AFFIX);
	}

	public boolean isTerminalAffix(String flag){
		return terminalAffixes.contains(flag);
	}

	public Set<String> getCompoundRules(){
		return getData(AffixTag.COMPOUND_RULE);
	}

	public boolean isManagedByCompoundRule(String flag){
		boolean found = false;
		Set<String> compoundRules = getCompoundRules();
		if(compoundRules != null)
			for(String rule : compoundRules)
				if(isManagedByCompoundRule(rule, flag)){
					found = true;
					break;
				}
		return found;
	}

	public boolean isManagedByCompoundRule(String compoundRule, String flag){
		List<String> flags = strategy.extractCompoundRule(compoundRule);
		flags = strategy.cleanCompoundRuleComponents(flags);
		return flags.contains(flag);
	}

	public Charset getCharset(){
		return Charset.forName(getData(AffixTag.CHARACTER_SET));
	}

	public boolean isFullstrip(){
		return containsData(AffixTag.FULLSTRIP);
	}

	/**
	 * 2-stage prefix plus 1-stage suffix instead of 2-stage suffix plus 1-stage prefix
	 * 
	 * @return Whether the prefix is complex
	 */
	public boolean isComplexPrefixes(){
		return containsData(AffixTag.COMPLEX_PREFIXES);
	}

	public Boolean isSuffix(String affixCode){
		Boolean isSuffix = null;
		Object affix = getData(affixCode);
		if(affix != null && RuleEntry.class.isAssignableFrom(affix.getClass()))
			isSuffix = ((RuleEntry)affix).isSuffix();
		return isSuffix;
	}

	public boolean isForbidDifferentCasesInCompound(){
		return containsData(AffixTag.CHECK_COMPOUND_CASE);
	}

	public boolean isForbidTriplesInCompound(){
		return containsData(AffixTag.CHECK_COMPOUND_TRIPLE);
	}

	public boolean isSimplifyTriplesInCompound(){
		return containsData(AffixTag.SIMPLIFIED_TRIPLE);
	}

	public Set<String> getProductiveAffixes(){
		//keeps only items with RuleEntry as value
		Set<String> affixes = new HashSet<>();
		Set<String> keys = data.keySet();
		for(String key : keys){
			Object affix = getData(key);
			if(RuleEntry.class.isAssignableFrom(affix.getClass()))
				affixes.add(key);
		}
		return affixes;
	}

	public String getFlag(){
		return getData(AffixTag.FLAG);
	}

	public FlagParsingStrategy getFlagParsingStrategy(){
		return strategy;
	}

	public boolean isAffixProductive(String word, String affix){
		word = applyInputConversionTable(word);

		boolean productive;
		RuleEntry rule = getData(affix);
		if(rule != null){
			List<AffixEntry> applicableAffixes = extractListOfApplicableAffixes(word, rule.getEntries());
			productive = !applicableAffixes.isEmpty();
		}
		else
			productive = isManagedByCompoundRule(affix);
		return productive;
	}

	public static List<AffixEntry> extractListOfApplicableAffixes(String word, List<AffixEntry> entries){
		//extract the list of applicable affixes...
		List<AffixEntry> applicableAffixes = new ArrayList<>();
		for(AffixEntry entry : entries)
			if(entry.match(word))
				applicableAffixes.add(entry);
		return applicableAffixes;
	}

	public String applyReplacementTable(String word){
		return applyConversionTable(word, getData(AffixTag.REPLACEMENT_TABLE));
	}

	public String applyInputConversionTable(String word){
		return applyConversionTable(word, getData(AffixTag.INPUT_CONVERSION_TABLE));
	}

	public String applyOutputConversionTable(String word){
		return applyConversionTable(word, getData(AffixTag.OUTPUT_CONVERSION_TABLE));
	}

	private String applyConversionTable(String word, List<Pair<String, String>> table){
		if(table != null){
			//collect input patterns that matches the given word
			List<Pair<String, String>> appliablePatterns = collectInputPatterns(table, word);

			for(Pair<String, String> entry : appliablePatterns){
				String key = entry.getKey();
				String value = entry.getValue();

				if(key.charAt(0) == '^')
					word = value + word.substring(key.length() - 1);
				else if(key.charAt(key.length() - 1) == '$')
					word = word.substring(0, word.length() - key.length() + 1) + value;
				else
					word = StringUtils.replace(word, key, value);
			}
		}
		return word;
	}

	private List<Pair<String, String>> collectInputPatterns(List<Pair<String, String>> table, String word){
		List<Pair<String, String>> startPatterns = new ArrayList<>();
		List<Pair<String, String>> insidePatterns = new ArrayList<>();
		List<Pair<String, String>> endPatterns = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		for(Pair<String, String> entry : table){
			String key = entry.getKey();
			String value = entry.getValue();
			
			if(key.charAt(0) == '^'){
				key = key.substring(1);
				if(word.startsWith(key))
					startPatterns.add(Pair.of(key, value));
			}
			else if(key.charAt(key.length() - 1) == '$'){
				key = key.substring(0, key.length() - 1);
				if(word.endsWith(key)){
					sb.setLength(0);
					key = sb.append(key)
						.reverse()
						.toString();
					endPatterns.add(Pair.of(key, value));
				}
			}
			else if(word.contains(key))
				insidePatterns.add(Pair.of(key, value));
		}

		//keep only the longest input pattern
		startPatterns = keepLongestInputPattern(startPatterns, key -> {
			sb.setLength(0);
			return sb.append('^').append(key).toString();
		});
		insidePatterns = keepLongestInputPattern(insidePatterns, Function.identity());
		endPatterns = keepLongestInputPattern(endPatterns, key -> {
			sb.setLength(0);
			return sb.append(key).reverse().append('$').toString();
		});

		startPatterns.addAll(insidePatterns);
		startPatterns.addAll(endPatterns);
		return startPatterns;
	}

	private List<Pair<String, String>> keepLongestInputPattern(List<Pair<String, String>> table, Function<String, String> keyRemapper){
		List<Pair<String, String>> result = table;
		if(!table.isEmpty()){
			table.sort(Comparator.comparing(entry -> entry.getKey().length()));

			int size = table.size();
			for(int i = 0; i < size; i ++){
				Pair<String, String> entry = table.get(i);
				if(entry != null){
					String key = entry.getKey();
					for(int j = i + 1; j < size; j ++){
						Pair<String, String> entry2 = table.get(j);
						if(entry2 != null){
							String key2 = entry2.getKey();
							if(key2.startsWith(key))
								table.set(i, null);
						}
					}
				}
			}

			result = table.stream()
				.filter(Objects::nonNull)
				.map(entry -> Pair.of(keyRemapper.apply(entry.getKey()), entry.getValue()))
				.collect(Collectors.toList());
		}
		return result;
	}

	public Set<String> getWordBreakCharacters(){
		return getData(AffixTag.BREAK);
	}

	public String getOnlyInCompoundFlag(){
		return getData(AffixTag.ONLY_IN_COMPOUND);
	}

	public boolean allowTwofoldAffixesInCompound(){
		return containsData(AffixTag.COMPOUND_MORE_SUFFIXES);
	}

	public String getPermitCompoundFlag(){
		return getData(AffixTag.COMPOUND_PERMIT_FLAG);
	}

	public String getForbidCompoundFlag(){
		return getData(AffixTag.COMPOUND_FORBID_FLAG);
	}

	public int getCompoundMaxWordCount(){
		return getData(AffixTag.COMPOUND_WORD_MAX);
	}

	public boolean isForbidDuplicationsInCompound(){
		return containsData(AffixTag.CHECK_COMPOUND_DUPLICATION);
	}

	public int getCompoundMinimumLength(){
		return getData(AffixTag.COMPOUND_MIN);
	}

	public String getCompoundFlag(){
		return getData(AffixTag.COMPOUND_FLAG);
	}

	public String getCircumfixFlag(){
		return getData(AffixTag.CIRCUMFIX);
	}

}
