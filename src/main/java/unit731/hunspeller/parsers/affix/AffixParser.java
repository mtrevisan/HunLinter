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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.affix.strategies.ASCIIParsingStrategy;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.PatternService;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


/**
 * Managed options:
 *		SET, FLAG, COMPLEXPREFIXES, LANG
 *		COMPOUNDRULE, COMPOUNDMIN, ONLYINCOMPOUND, CIRCUMFIX
 *		PFX, SFX
 *		FULLSTRIP, KEEPCASE, NEEDAFFIX, ICONV, OCONV
 */
public class AffixParser extends ReadWriteLockable{

	private static final String DOUBLE_MINUS_SIGN = HyphenationParser.MINUS_SIGN + HyphenationParser.MINUS_SIGN;

	private static final Matcher COMMENT = PatternService.matcher("^$|^\\s*#.*$");


	@AllArgsConstructor
	private static enum ConversionTableType{
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
	@Getter
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
			List<AffixEntry> entries = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();

				line = removeComment(line);

				AffixEntry entry = new AffixEntry(line, strategy);
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
	private final Consumer<ParsingContext> FUN_CONVERSION_TABLE = context -> {
		try{
			ConversionTableType conversionTableType = ConversionTableType.toEnum(context.getRuleType());
			BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ "\": The first parameter is not a number");
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ ": Bad number of entries, it must be a positive integer");

			Map<String, String> conversionTable = new HashMap<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();

				String[] parts = StringUtils.split(line);
				if(parts.length != 3)
					throw new IllegalArgumentException("Error reading line \"" + context.toString()
						+ ": Bad number of entries, it must be <tag> <pattern-from> <pattern-to>");

				conversionTable.put(parts[1], parts[2]);
			}

			addData(conversionTableType.getFlag(), conversionTable);
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
		//Options for suggestions
//		RULE_FUNCTION.put(AffixTag.KEY, FUN_COPY_OVER);
//		RULE_FUNCTION.put("TRY", FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.NO_SUGGEST, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.NO_NGRAM_SUGGEST, FUN_COPY_OVER);
//		RULE_FUNCTION.put("REP", FUN_COPY_OVER);
//		RULE_FUNCTION.put("MAP", FUN_MAP);
		//Options for compounding
		//default break table contains: "-", "^-", and "-$"
		RULE_FUNCTION.put(AffixTag.BREAK, FUN_WORD_BREAK_TABLE);
		RULE_FUNCTION.put(AffixTag.COMPOUND_RULE, FUN_COMPOUND_RULE);
		RULE_FUNCTION.put(AffixTag.COMPOUND_MIN, FUN_COPY_OVER_AS_NUMBER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_FLAG, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_BEGIN, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_MIDDLE, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_END, FUN_COPY_OVER);
		RULE_FUNCTION.put(AffixTag.ONLY_IN_COMPOUND, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_PERMIT_FLAG, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_MORE_SUFFIXES, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_ROOT, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_FORBID_FLAG, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.COMPOUND_WORD_MAX, FUN_COPY_OVER_AS_NUMBER);
//		RULE_FUNCTION.put(AffixTag.CHECK_COMPOUND_DUPLICATION, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.CHECK_COMPOUND_REPLACEMENT, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.CHECK_COMPOUND_CASE, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.CHECK_COMPOUND_TRIPLE, FUN_COPY_OVER);
//		RULE_FUNCTION.put(AffixTag.SIMPLIFIED_TRIPLE, FUN_COPY_OVER);
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
			charset = FileService.determineCharset(affFile.toPath());
			try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(affFile.toPath(), charset))){
				String line;
				while((line = br.readLine()) != null){
					//ignore any BOM marker on first line
					if(br.getLineNumber() == 1)
						line = FileService.clearBOMMarker(line);

					line = removeComment(line);
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
			if(!containsData(AffixTag.LANGUAGE))
				//try to infer language from filename
				addData(AffixTag.LANGUAGE, affFile.getName().replaceFirst("\\..+$", StringUtils.EMPTY));
			if(!containsData(AffixTag.BREAK)){
				Set<String> wordBreakCharacters = new HashSet<>(3);
				wordBreakCharacters.add(HyphenationParser.MINUS_SIGN);
				wordBreakCharacters.add("^" + HyphenationParser.MINUS_SIGN);
				wordBreakCharacters.add(HyphenationParser.MINUS_SIGN + "$");
				addData(AffixTag.BREAK, wordBreakCharacters);
			}
//			if(isComplexPrefixes()){
//				String compoundBegin = getData(AffixTag.COMPOUND_BEGIN);
//				String compoundEnd = getData(AffixTag.COMPOUND_END);
//				addData(AffixTag.COMPOUND_BEGIN, compoundEnd);
//				addData(AffixTag.COMPOUND_END, compoundBegin);
//
//				RuleEntry prefixes = getData(AffixTag.PREFIX);
//				RuleEntry suffixes = getData(AffixTag.SUFFIX);
//				addData(AffixTag.PREFIX, suffixes);
//				addData(AffixTag.SUFFIX, prefixes);
//			}
//			if(!containsData(AffixTag.KEY))
//				addData(AffixTag.KEY, "qwertyuiop|asdfghjkl|zxcvbnm");


			terminalAffixes.add(getKeepCaseFlag());
			terminalAffixes.add(getCircumfixFlag());
			terminalAffixes.add(getNeedAffixFlag());
			terminalAffixes.add(getOnlyInCompoundFlag());
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

	/**
	 * Removes comment lines and then cleans up blank lines and trailing whitespace.
	 *
	 * @param {String} data	The data from an affix file.
	 * @return {String}		The cleaned-up data.
	 */
	private static String removeComment(String line){
		//remove comments
		line = PatternService.clear(line, COMMENT);
		//trim the entire string
		return StringUtils.strip(line);
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

	public String applyInputConversionTable(String word){
		return applyConversionTable(word, getData(AffixTag.INPUT_CONVERSION_TABLE));
	}

	public String applyOutputConversionTable(String word){
		return applyConversionTable(word, getData(AffixTag.OUTPUT_CONVERSION_TABLE));
	}

	private String applyConversionTable(String word, Map<String, String> table){
		if(table != null){
			int size = table.size();
			word = StringUtils.replaceEach(word, table.keySet().toArray(new String[size]), table.values().toArray(new String[size]));
		}
		return word;
	}

	public Set<String> getWordBreakCharacters(){
		return getData(AffixTag.BREAK);
	}

	public String getOnlyInCompoundFlag(){
		return getData(AffixTag.ONLY_IN_COMPOUND);
	}

	public int getCompoundMinimumLength(){
		return getData(AffixTag.COMPOUND_MIN);
	}

	public String getCircumfixFlag(){
		return getData(AffixTag.CIRCUMFIX);
	}

}
