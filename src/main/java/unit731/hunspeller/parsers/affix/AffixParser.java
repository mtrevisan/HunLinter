package unit731.hunspeller.parsers.affix;

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
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.handlers.AffixHandler;
import unit731.hunspeller.parsers.affix.handlers.AliasesHandler;
import unit731.hunspeller.parsers.affix.handlers.CompoundRuleHandler;
import unit731.hunspeller.parsers.affix.handlers.ConversionTableHandler;
import unit731.hunspeller.parsers.affix.handlers.CopyOverAsNumberHandler;
import unit731.hunspeller.parsers.affix.handlers.CopyOverHandler;
import unit731.hunspeller.parsers.affix.handlers.Handler;
import unit731.hunspeller.parsers.affix.handlers.WordBreakTableHandler;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.ParsingStrategyFactory;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


/**
 * Managed options:
 *		General:
 *			SET, FLAG, COMPLEXPREFIXES, LANG, AF, AM
 *		Suggestions:
 *			NOSUGGEST (only read), REP
 *		Compounding:
 *			BREAK (only read), COMPOUNDRULE, COMPOUNDMIN, COMPOUNDFLAG, ONLYINCOMPOUND, COMPOUNDPERMITFLAG, COMPOUNDFORBIDFLAG, COMPOUNDMORESUFFIXES,
 *			COMPOUNDWORDMAX, CHECKCOMPOUNDDUP, CHECKCOMPOUNDREP, CHECKCOMPOUNDCASE, CHECKCOMPOUNDTRIPLE, SIMPLIFIEDTRIPLE, FORCEUCASE
 *		Affix creation:
 *			PFX, SFX
 *		Others:
 *			CIRCUMFIX, FORBIDDENWORD, FULLSTRIP, KEEPCASE, ICONV, OCONV, NEEDAFFIX
 */
public class AffixParser extends ReadWriteLockable{

	private static final String NO_LANGUAGE = "xxx";

	private static final String START = "^";
	private static final String END = "$";

	private static final Pattern PATTERN_ISO639_1 = PatternHelper.pattern("([a-z]{2})");
	private static final Pattern PATTERN_ISO639_2 = PatternHelper.pattern("([a-z]{2,3}(?:[-_\\/][a-z]{2,3})?)");


	private final Map<String, Object> data = new HashMap<>();
	private Charset charset;
	private FlagParsingStrategy strategy = ParsingStrategyFactory.createASCIIParsingStrategy();

	private final Set<String> terminalAffixes = new HashSet<>();

	private final Map<AffixTag, String> parsingFunction = new HashMap<>();

	private static final String PARSING_HANDLER_KEY_COPY_OVER = "copyOver";
	private static final String PARSING_HANDLER_KEY_COPY_OVER_AS_NUMBER = "copyOverAsNumber";
	private static final String PARSING_HANDLER_KEY_COMPOUND_RULE = "compoundRule";
	private static final String PARSING_HANDLER_KEY_AFFIX = "affix";
	private static final String PARSING_HANDLER_KEY_WORD_BREAK_TABLE = "wordBreakTable";
	private static final String PARSING_HANDLER_KEY_ALIASES = "aliases";
	private static final String PARSING_HANDLER_KEY_REPLACEMENT_TABLE = "replacementTable";
	private static final String PARSING_HANDLER_KEY_INPUT_CONVERSION_TABLE = "inputConversionTable";
	private static final String PARSING_HANDLER_KEY_OUTPUT_CONVERSION_TABLE = "outputConversionTable";

	private static final Map<String, Handler> PARSING_HANDLERS = new HashMap<>();
	static{
		PARSING_HANDLERS.put(PARSING_HANDLER_KEY_COPY_OVER, new CopyOverHandler());
		PARSING_HANDLERS.put(PARSING_HANDLER_KEY_COPY_OVER_AS_NUMBER, new CopyOverAsNumberHandler());
		PARSING_HANDLERS.put(PARSING_HANDLER_KEY_COMPOUND_RULE, new CompoundRuleHandler());
		PARSING_HANDLERS.put(PARSING_HANDLER_KEY_AFFIX, new AffixHandler());
		PARSING_HANDLERS.put(PARSING_HANDLER_KEY_WORD_BREAK_TABLE, new WordBreakTableHandler());
		PARSING_HANDLERS.put(PARSING_HANDLER_KEY_ALIASES, new AliasesHandler());
		PARSING_HANDLERS.put(PARSING_HANDLER_KEY_REPLACEMENT_TABLE, new ConversionTableHandler(AffixTag.REPLACEMENT_TABLE));
		PARSING_HANDLERS.put(PARSING_HANDLER_KEY_INPUT_CONVERSION_TABLE, new ConversionTableHandler(AffixTag.INPUT_CONVERSION_TABLE));
		PARSING_HANDLERS.put(PARSING_HANDLER_KEY_OUTPUT_CONVERSION_TABLE, new ConversionTableHandler(AffixTag.OUTPUT_CONVERSION_TABLE));
	}


	public AffixParser(){
		//General options
//		ruleFunction.put("NAME", PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put("VERSION", PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put("HOME", PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.CHARACTER_SET, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.FLAG, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.COMPLEX_PREFIXES, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.LANGUAGE, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.IGNORE, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.ALIASES_FLAG, PARSING_HANDLER_KEY_ALIASES);
		parsingFunction.put(AffixTag.ALIASES_MORPHOLOGICAL_FIELD, PARSING_HANDLER_KEY_ALIASES);

		//Options for suggestions
//		ruleFunction.put(AffixTag.KEY, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.TRY, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.NO_SUGGEST, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.MAX_COMPOUND_SUGGEST, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.MAX_NGRAM_SUGGEST, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.MAX_NGRAM_SIMILARITY_FACTOR, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.ONLY_MAX_NGRAM_SIMILARITY_FACTOR, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.NO_SPLIT_SUGGEST, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.NO_NGRAM_SUGGEST, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.SUGGESTIONS_WITH_DOTS, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.REPLACEMENT_TABLE, PARSING_HANDLER_KEY_REPLACEMENT_TABLE);
//		ruleFunction.put(AffixTag.MAP_TABLE, PARSING_HANDLER_KEY_MAP);
//		ruleFunction.put(AffixTag.PHONE_TABLE, PARSING_HANDLER_KEY_MAP);
//		ruleFunction.put(AffixTag.WARN, PARSING_HANDLER_KEY_MAP);
//		ruleFunction.put(AffixTag.FORBID_WARN, PARSING_HANDLER_KEY_MAP);

		//Options for compounding
		parsingFunction.put(AffixTag.BREAK, PARSING_HANDLER_KEY_WORD_BREAK_TABLE);
		parsingFunction.put(AffixTag.COMPOUND_RULE, PARSING_HANDLER_KEY_COMPOUND_RULE);
		parsingFunction.put(AffixTag.COMPOUND_MIN, PARSING_HANDLER_KEY_COPY_OVER_AS_NUMBER);
		parsingFunction.put(AffixTag.COMPOUND_FLAG, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.COMPOUND_BEGIN, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.COMPOUND_MIDDLE, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.COMPOUND_END, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.ONLY_IN_COMPOUND, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.COMPOUND_PERMIT_FLAG, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.COMPOUND_FORBID_FLAG, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.COMPOUND_MORE_SUFFIXES, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.COMPOUND_ROOT, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.COMPOUND_WORD_MAX, PARSING_HANDLER_KEY_COPY_OVER_AS_NUMBER);
		parsingFunction.put(AffixTag.CHECK_COMPOUND_DUPLICATION, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.CHECK_COMPOUND_REPLACEMENT, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.CHECK_COMPOUND_CASE, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.CHECK_COMPOUND_TRIPLE, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.SIMPLIFIED_TRIPLE, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.CHECK_COMPOUND_PATTERN, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.FORCE_UPPERCASE, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.COMPOUND_SYLLABLE, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.SYLLABLE_NUMBER, PARSING_HANDLER_KEY_COPY_OVER);

//Options for affix creation
		parsingFunction.put(AffixTag.PREFIX, PARSING_HANDLER_KEY_AFFIX);
		parsingFunction.put(AffixTag.SUFFIX, PARSING_HANDLER_KEY_AFFIX);

		//Other options
		parsingFunction.put(AffixTag.CIRCUMFIX, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.FORBIDDEN_WORD, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.FULLSTRIP, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.KEEP_CASE, PARSING_HANDLER_KEY_COPY_OVER);
		parsingFunction.put(AffixTag.INPUT_CONVERSION_TABLE, PARSING_HANDLER_KEY_INPUT_CONVERSION_TABLE);
		parsingFunction.put(AffixTag.OUTPUT_CONVERSION_TABLE, PARSING_HANDLER_KEY_OUTPUT_CONVERSION_TABLE);
		parsingFunction.put(AffixTag.NEED_AFFIX, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.WORD_CHARS, PARSING_HANDLER_KEY_COPY_OVER);
//		ruleFunction.put(AffixTag.CHECK_SHARPS, PARSING_HANDLER_KEY_COPY_OVER);
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
					AffixTag ruleType = AffixTag.createFromCode(context.getRuleType());
					String funKey = parsingFunction.get(ruleType);
					if(funKey != null){
						try{
							Handler handler = PARSING_HANDLERS.get(funKey);
							handler.parse(context, strategy, this::addData, this::getData);

							if(ruleType == AffixTag.FLAG)
								//determines the appropriate {@link FlagParsingStrategy} based on the FLAG definition line taken from the affix file
								strategy = ParsingStrategyFactory.createFromFlag(getFlag());
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
				String[] languages = PatternHelper.extract(filename, PATTERN_ISO639_2);
				if(languages.length == 0)
					languages = PatternHelper.extract(filename, PATTERN_ISO639_1);
				String language = (languages.length > 0? languages[0]: NO_LANGUAGE);
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
//			if(!containsData(AffixTag.WORD_CHARS))
//				addData(AffixTag.WORD_CHARS, "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM");


			terminalAffixes.add(getNoSuggestFlag());
			terminalAffixes.add(getCompoundFlag());
			terminalAffixes.add(getForbiddenWordFlag());
			terminalAffixes.add(getCompoundBeginFlag());
			terminalAffixes.add(getCompoundMiddleFlag());
			terminalAffixes.add(getCompoundEndFlag());
			terminalAffixes.add(getOnlyInCompoundFlag());
			terminalAffixes.add(getPermitCompoundFlag());
			terminalAffixes.add(getForbidCompoundFlag());
			terminalAffixes.add(getForceCompoundUppercaseFlag());
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
			strategy = ParsingStrategyFactory.createASCIIParsingStrategy();
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
		String[] flags = strategy.extractCompoundRule(compoundRule);
		return ArrayUtils.contains(flags, flag);
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

	public String getNoSuggestFlag(){
		return getData(AffixTag.NO_SUGGEST);
	}

	//FIXME to remove?
	public ConversionTable getReplacementTable(){
		return getData(AffixTag.REPLACEMENT_TABLE);
	}

	public String applyReplacementTable(String word){
		ConversionTable table = getData(AffixTag.REPLACEMENT_TABLE);
		return (table != null? table.applyConversionTable(word): word);
	}

	public String applyInputConversionTable(String word){
		ConversionTable table = getData(AffixTag.INPUT_CONVERSION_TABLE);
		return (table != null? table.applyConversionTable(word): word);
	}

	public String applyOutputConversionTable(String word){
		ConversionTable table = getData(AffixTag.OUTPUT_CONVERSION_TABLE);
		return (table != null? table.applyConversionTable(word): word);
	}

	public Set<String> getWordBreakCharacters(){
		return getData(AffixTag.BREAK);
	}

	public String getCompoundBeginFlag(){
		return getData(AffixTag.COMPOUND_BEGIN);
	}

	public String getCompoundMiddleFlag(){
		return getData(AffixTag.COMPOUND_MIDDLE);
	}

	public String getCompoundEndFlag(){
		return getData(AffixTag.COMPOUND_END);
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

	public boolean isCheckCompoundReplacement(){
		return containsData(AffixTag.CHECK_COMPOUND_REPLACEMENT);
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

	public String getForbiddenWordFlag(){
		return getData(AffixTag.FORBIDDEN_WORD);
	}

	public String getForceCompoundUppercaseFlag(){
		return getData(AffixTag.FORCE_UPPERCASE);
	}

}
