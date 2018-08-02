package unit731.hunspeller.parsers.affix;

import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.NumericalParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.DoubleASCIIParsingStrategy;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.dictionary.valueobjects.AffixEntry;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.parsers.affix.strategies.ASCIIParsingStrategy;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.PatternService;


/**
 * Managed options: SET, LANG, FLAG, COMPLEXPREFIXES, PFX, SFX, FULLSTRIP, KEEPCASE, ICONV, OCONV, CIRCUMFIX, NEEDAFFIX
 */
public class AffixParser{

	//General options
	/**
	 * Set character encoding of words and morphemes in affix and dictionary files. Possible values are UTF-8, ISO8859-1 through ISO8859-10,
	 * ISO8859-13 through ISO8859-15, KOI8-R, KOI8-U, MICROSOFT-CP1251, ISCII-DEVANAGARI
	 */
	private static final String TAG_CHARACTER_SET = "SET";
	/**
	 * Set flag type. Default type is the extended ASCII (8-bit) character. ‘UTF-8‘ parameter sets UTF-8 encoded Unicode character flags.
	 * The ‘long‘ value sets the double extended ASCII character flag type, the ‘num‘ sets the decimal number flag type. Decimal flags numbered
	 * from 1 to 65000, and in flag fields are separated by comma
	 */
	private static final String TAG_FLAG = "FLAG";
	/** Set twofold prefix stripping (but single suffix stripping) for agglutinative languages with right-to-left writing system */
	private static final String TAG_COMPLEX_PREFIXES = "COMPLEXPREFIXES";
	/** Language code */
	private static final String TAG_LANGUAGE = "LANG";
	/** Sets characters to ignore in dictionary words, affixes and input words */
	private static final String TAG_IGNORE = "IGNORE";
	/** Search and suggest words with one different character replaced by a neighbor character */
	private static final String TAG_KEY = "KEY";

	//Options for suggestions
	/** Words signed with this flag are not suggested (but still accepted when typed correctly) */
	private static final String TAG_NO_SUGGEST = "NOSUGGEST";
	/** Similar to NOSUGGEST, but it forbids to use the word in n-gram based (more, than 1-character distance) suggestions */
	private static final String TAG_NO_NGRAM_SUGGEST = "NONGRAMSUGGEST";

	//Options for compounding
	/** Define new break points for breaking words and checking word parts separately (use ^ and $ to delete characters at end and start of the word) */
	private static final String TAG_BREAK = "BREAK";
	/** Define custom compound patterns */
	private static final String TAG_COMPOUND_RULE = "COMPOUNDRULE";
	/** Minimum length of words in compound words */
	private static final String TAG_COMPOUND_MIN = "COMPOUNDMIN";
	/** Words signed with this flag (or with a signed affix) may be first elements in compound words */
	private static final String TAG_COMPOUND_BEGIN = "COMPOUNDBEGIN";
	/** Words signed with this flag (or with a signed affix) may be middle elements in compound words */
	private static final String TAG_COMPOUND_MIDDLE = "COMPOUNDMIDDLE";
	/** Words signed with this flag (or with a signed affix) may be last elements in compound words */
	private static final String TAG_COMPOUND_END = "COMPOUNDEND";
	/** Suffixes signed this flag may be only inside of compounds (this flag works also with words) */
	private static final String TAG_ONLY_IN_COMPOUND = "ONLYINCOMPOUND";
	/** Affixes with this flag may be inside of compounds (normally, prefixes are allowed at the beginning of compounds, suffixes are allowed at the end of compounds only). */
	private static final String TAG_COMPOUND_PERMIT_FLAG = "COMPOUNDPERMITFLAG";
	/** Allow twofold suffixes within compounds */
	private static final String TAG_COMPOUND_MORE_SUFFIXES = "COMPOUNDMORESUFFIXES";
	/** Signs the compounds in the dictionary (now it is used only in the Hungarian language specific code) */
	private static final String TAG_COMPOUND_ROOT = "COMPOUNDROOT";
	/** Suffixes with this flag forbid compounding of the affixed word */
	private static final String TAG_COMPOUND_FORBID_FLAG = "COMPOUNDFORBIDFLAG";
	/** Set maximum word count in a compound word (default is unlimited) */
	private static final String TAG_COMPOUND_WORD_MAX = "COMPOUNDWORDMAX";
	/** Forbid word duplication in compounds */
	private static final String TAG_CHECK_COMPOUND_DUPLICATION = "CHECKCOMPOUNDDUP";
	/** Forbid compounding, if the (usually bad) compound word may be a non compound word with a REP fault (useful for languages with 'compound friendly' orthography) */
	private static final String TAG_CHECK_COMPOUND_REPLACEMENT = "CHECKCOMPOUNDREP";
	/** Forbid upper case characters at word bound in compounds */
	private static final String TAG_CHECK_COMPOUND_CASE = "CHECKCOMPOUNDCASE";
	/** Forbid compounding, if compound word contains triple repeating letters (e.g. foo|ox or xo|oof) */
	private static final String TAG_CHECK_COMPOUND_TRIPLE = "CHECKCOMPOUNDTRIPLE";
	/** Allow simplified 2-letter forms of the compounds forbidden by CHECKCOMPOUNDTRIPLE (Schiff|fahrt -> Schiffahrt) */
	private static final String TAG_SIMPLIFIED_TRIPLE = "SIMPLIFIEDTRIPLE";
	/** Affixes signed with this flag may be on a word when this word also has a prefix with CIRCUMFIX flag and vice versa */
	private static final String TAG_CIRCUMFIX = "CIRCUMFIX";
	/** Signs forbidden word form (because affixed forms are also forbidden, we can subtract a subset from set of the accepted affixed and compound words) */
	private static final String TAG_FORBIDDEN_WORD = "FORBIDDENWORD";

	//Options for affix creation
	private static final String TAG_PREFIX = AffixEntry.Type.PREFIX.getFlag();
	private static final String TAG_SUFFIX = AffixEntry.Type.SUFFIX.getFlag();

	//Other options
	/** With this flag the affix rules can strip full words, not only one less characters */
	private static final String TAG_FULLSTRIP = "FULLSTRIP";
	/** Forbid uppercased and capitalized forms of words signed with this flag */
	private static final String TAG_KEEP_CASE = "KEEPCASE";
	/**
	 * Signs virtual stems in the dictionary, words are valid only when affixed, except if the dictionary word has a homonym or a zero affix
	 * (it works also with prefixes and prefix + suffix combinations)
	 */
	private static final String TAG_NEED_AFFIX = "NEEDAFFIX";
	/** Extends tokenizer of Hunspell command line interface with additional word character */
	private static final String TAG_WORD_CHARS = "WORDCHARS";
	/** Define input conversion table */
	private static final String TAG_INPUT_CONVERSION_TABLE = "ICONV";
	/** Define output conversion table */
	private static final String TAG_OUTPUT_CONVERSION_TABLE = "OCONV";

	private static final String DOUBLE_MINUS_SIGN = HyphenationParser.MINUS_SIGN + HyphenationParser.MINUS_SIGN;

	private static final Matcher COMMENT = PatternService.matcher("^$|^\\s*#.*$");

	private final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();


	@AllArgsConstructor
	private static enum ConversionTableType{
		INPUT(TAG_INPUT_CONVERSION_TABLE),
		OUTPUT(TAG_OUTPUT_CONVERSION_TABLE);


		@Getter
		private final String flag;

		public static ConversionTableType toEnum(String flag){
			ConversionTableType[] types = ConversionTableType.values();
			for(ConversionTableType type : types)
				if(type.getFlag().equals(flag))
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
		addData(context.getRuleType(), Integer.valueOf(context.getAllButFirstParameter()));
	};
	private final Consumer<ParsingContext> FUN_COMPOUND_RULE = context -> {
		try{
			BufferedReader br = context.getReader();
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ ": Bad number of entries, it must be a positive integer");

			Set<String> compoundRules = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();

				String[] lineParts = StringUtils.split(line);
				String tag = lineParts[0];
				if(!TAG_COMPOUND_RULE.equals(tag))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": mismatched rule type (expected "
						+ TAG_COMPOUND_RULE + ")");
				String rule = lineParts[1];
				//FIXME interpret the rule?
				if(StringUtils.isBlank(rule))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": rule type cannot be empty");

				boolean inserted = compoundRules.add(rule);
				if(!inserted)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": duplicated line");
			}

			addData(TAG_COMPOUND_RULE, compoundRules);
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
				if(!containsUnique(entry.getContinuationFlags()))
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
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ ": Bad number of entries, it must be a positive integer");

			Set<String> wordBreakCharacters = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();

				String[] lineParts = StringUtils.split(line);
				String tag = lineParts[0];
				if(!TAG_BREAK.equals(tag))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": mismatched type (expected "
						+ TAG_BREAK + ")");
				String breakCharacter = lineParts[1];
				if(StringUtils.isBlank(breakCharacter))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": break character cannot be empty");
				if(DOUBLE_MINUS_SIGN.equals(breakCharacter))
					breakCharacter = HyphenationParser.EN_DASH;

				boolean inserted = wordBreakCharacters.add(breakCharacter);
				if(!inserted)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": duplicated line");
			}

			addData(TAG_BREAK, wordBreakCharacters);
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	};
	private final Consumer<ParsingContext> FUN_CONVERSION_TABLE = context -> {
		try{
			ConversionTableType conversionTableType = ConversionTableType.toEnum(context.getRuleType());
			BufferedReader br = context.getReader();
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

	private static boolean containsUnique(String[] list){
		if(list == null)
			return true;

		Set<String> set = new HashSet<>();
		return Arrays.stream(list)
			.allMatch(set::add);
	}

	private final Map<String, Consumer<ParsingContext>> RULE_FUNCTION = new HashMap<>();


	public AffixParser(){
		//General options
//		RULE_FUNCTION.put("NAME", FUN_COPY_OVER);
//		RULE_FUNCTION.put("VERSION", FUN_COPY_OVER);
//		RULE_FUNCTION.put("HOME", FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_CHARACTER_SET, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_FLAG, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_COMPLEX_PREFIXES, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_LANGUAGE, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_IGNORE, FUN_COPY_OVER);
		//Options for suggestions
//		RULE_FUNCTION.put(TAG_KEY, FUN_COPY_OVER);
//		RULE_FUNCTION.put("TRY", FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_NO_SUGGEST, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_NO_NGRAM_SUGGEST, FUN_COPY_OVER);
//		RULE_FUNCTION.put("REP", FUN_COPY_OVER);
//		RULE_FUNCTION.put("MAP", FUN_MAP);
		//Options for compounding
		//default break table contains: "-", "^-", and "-$"
		RULE_FUNCTION.put(TAG_BREAK, FUN_WORD_BREAK_TABLE);
		RULE_FUNCTION.put(TAG_COMPOUND_RULE, FUN_COMPOUND_RULE);
//		RULE_FUNCTION.put(TAG_COMPOUND_MIN, FUN_COPY_OVER_AS_NUMBER);
//		RULE_FUNCTION.put(TAG_COMPOUND_FLAG, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_COMPOUND_BEGIN, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_COMPOUND_MIDDLE, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_COMPOUND_END, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_ONLY_IN_COMPOUND, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_COMPOUND_PERMIT_FLAG, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_COMPOUND_MORE_SUFFIXES, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_COMPOUND_ROOT, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_COMPOUND_FORBID_FLAG, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_COMPOUND_WORD_MAX, FUN_COPY_OVER_AS_NUMBER);
//		RULE_FUNCTION.put(TAG_CHECK_COMPOUND_DUPLICATION, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_CHECK_COMPOUND_REPLACEMENT, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_CHECK_COMPOUND_CASE, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_CHECK_COMPOUND_TRIPLE, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_SIMPLIFIED_TRIPLE, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_CIRCUMFIX, FUN_COPY_OVER);
//		RULE_FUNCTION.put(TAG_FORBIDDEN_WORD, FUN_COPY_OVER);
		//Options for affix creation
		RULE_FUNCTION.put(TAG_PREFIX, FUN_AFFIX);
		RULE_FUNCTION.put(TAG_SUFFIX, FUN_AFFIX);
		//Other options
		RULE_FUNCTION.put(TAG_FULLSTRIP, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_KEEP_CASE, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_NEED_AFFIX, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_INPUT_CONVERSION_TABLE, FUN_CONVERSION_TABLE);
		RULE_FUNCTION.put(TAG_OUTPUT_CONVERSION_TABLE, FUN_CONVERSION_TABLE);
//		RULE_FUNCTION.put(TAG_WORD_CHARS, FUN_COPY_OVER);
	}

	/**
	 * Parse the rules out from a .aff file.
	 *
	 * @param affFile	The content of the affix file
	 * @throws IOException	If an I/O error occurs
	 * @throws	IllegalArgumentException	If something is wrong while parsing the file (eg. missing rule)
	 */
	public void parse(File affFile) throws IOException, IllegalArgumentException{
		READ_WRITE_LOCK.writeLock().lock();
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

					if(!encodingRead && !line.startsWith(TAG_CHARACTER_SET + StringUtils.SPACE))
						throw new IllegalArgumentException("The first non–comment line in the affix file must be a 'SET charset', was: '" + line + "'");
					else
						encodingRead = true;

					ParsingContext context = new ParsingContext(line, br);
					String ruleType = context.getRuleType();
					Consumer<ParsingContext> fun = RULE_FUNCTION.get(ruleType);
					if(fun != null){
						try{
							fun.accept(context);

							if(TAG_FLAG.equals(ruleType)){
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

//			if(!containsData(TAG_COMPOUND_MIN))
//				addData(TAG_COMPOUND_MIN, 3);
//			Integer compoundMin = getData(TAG_COMPOUND_MIN);
//			if(compoundMin != null && compoundMin < 1)
//				addData(TAG_COMPOUND_MIN, 1);
			//apply default charset
			if(!containsData(TAG_CHARACTER_SET))
				addData(TAG_CHARACTER_SET, StandardCharsets.ISO_8859_1);
			if(!containsData(TAG_LANGUAGE))
				//try to infer language from filename
				addData(TAG_LANGUAGE, affFile.getName().replaceFirst("\\..+$", StringUtils.EMPTY));
			if(!containsData(TAG_BREAK)){
				Set<String> wordBreakCharacters = new HashSet<>(3);
				wordBreakCharacters.add(HyphenationParser.MINUS_SIGN);
				wordBreakCharacters.add("^" + HyphenationParser.MINUS_SIGN);
				wordBreakCharacters.add(HyphenationParser.MINUS_SIGN + "$");
				addData(TAG_BREAK, wordBreakCharacters);
			}
//			if(isComplexPrefixes()){
//				String compoundBegin = getData(TAG_COMPOUND_BEGIN);
//				String compoundEnd = getData(TAG_COMPOUND_END);
//				addData(TAG_COMPOUND_BEGIN, compoundEnd);
//				addData(TAG_COMPOUND_END, compoundBegin);
//
//				RuleEntry prefixes = getData(TAG_PREFIX);
//				RuleEntry suffixes = getData(TAG_SUFFIX);
//				addData(TAG_PREFIX, suffixes);
//				addData(TAG_SUFFIX, prefixes);
//			}
//			if(!containsData(TAG_KEY))
//				addData(TAG_KEY, "qwertyuiop|asdfghjkl|zxcvbnm");


			terminalAffixes.add(getKeepCaseFlag());
			terminalAffixes.add(getCircumfixFlag());
			terminalAffixes.add(getNeedAffixFlag());
		}
		finally{
			READ_WRITE_LOCK.writeLock().unlock();
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

	private boolean containsData(String key){
		READ_WRITE_LOCK.readLock().lock();
		try{
			return data.containsKey(key);
		}
		finally{
			READ_WRITE_LOCK.readLock().unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T getData(String key){
		READ_WRITE_LOCK.readLock().lock();
		try{
			return (T)data.get(key);
		}
		finally{
			READ_WRITE_LOCK.readLock().unlock();
		}
	}

	private <T> void addData(String key, T value){
		data.put(key, value);
	}

	private void clearData(){
		READ_WRITE_LOCK.writeLock().lock();
		try{
			data.clear();
		}
		finally{
			READ_WRITE_LOCK.writeLock().unlock();
		}
	}

	public String getLanguage(){
		return getData(TAG_LANGUAGE);
	}

	public String getKeepCaseFlag(){
		return getData(TAG_KEEP_CASE);
	}

	public String getNeedAffixFlag(){
		return getData(TAG_NEED_AFFIX);
	}

	public boolean isTerminalAffix(String flag){
		READ_WRITE_LOCK.writeLock().lock();
		try{
			return terminalAffixes.contains(flag);
		}
		finally{
			READ_WRITE_LOCK.writeLock().unlock();
		}
	}

	public Set<String> getCompoundRules(){
		return getData(TAG_COMPOUND_RULE);
	}

	public boolean isManagedByCompoundRule(String flag){
		READ_WRITE_LOCK.readLock().lock();
		try{
			//TODO migliorare con una regex
			boolean found = false;
			Set<String> compoundRules = getCompoundRules();
			if(strategy instanceof DoubleASCIIParsingStrategy || strategy instanceof NumericalParsingStrategy)
				flag = "(" + flag + ")";
			for(String rule : compoundRules)
				if(rule.contains(flag)){
					found = true;
					break;
				}
			return found;
		}
		finally{
			READ_WRITE_LOCK.readLock().unlock();
		}
	}

	public boolean isManagedByCompoundRule(String compoundRule, String flag){
		READ_WRITE_LOCK.readLock().lock();
		try{
			//TODO migliorare con una regex
			if(strategy instanceof DoubleASCIIParsingStrategy || strategy instanceof NumericalParsingStrategy)
				flag = "(" + flag + ")";
			return compoundRule.contains(flag);
		}
		finally{
			READ_WRITE_LOCK.readLock().unlock();
		}
	}

	public Charset getCharset(){
		return Charset.forName(getData(TAG_CHARACTER_SET));
	}

	public boolean isFullstrip(){
		return containsData(TAG_FULLSTRIP);
	}

	/**
	 * 2-stage prefix plus 1-stage suffix instead of 2-stage suffix plus 1-stage prefix
	 * 
	 * @return Whether the prefix is complex
	 */
	public boolean isComplexPrefixes(){
		return containsData(TAG_COMPLEX_PREFIXES);
	}

	public Boolean isSuffix(String affixCode){
		READ_WRITE_LOCK.readLock().lock();
		try{
			Boolean isSuffix = null;
			Object affix = getData(affixCode);
			if(affix != null && RuleEntry.class.isAssignableFrom(affix.getClass()))
				isSuffix = ((RuleEntry)affix).isSuffix();
			return isSuffix;
		}
		finally{
			READ_WRITE_LOCK.readLock().unlock();
		}
	}

	public Set<String> getProductiveAffixes(){
		READ_WRITE_LOCK.readLock().lock();
		try{
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
		finally{
			READ_WRITE_LOCK.readLock().unlock();
		}
	}

	public String getFlag(){
		return getData(TAG_FLAG);
	}

	public FlagParsingStrategy getFlagParsingStrategy(){
		READ_WRITE_LOCK.readLock().lock();
		try{
			return strategy;
		}
		finally{
			READ_WRITE_LOCK.readLock().unlock();
		}
	}

	public boolean isAffixProductive(String word, String affix){
		READ_WRITE_LOCK.readLock().lock();
		try{
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
		finally{
			READ_WRITE_LOCK.readLock().unlock();
		}
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
		return applyConversionTable(word, getData(TAG_INPUT_CONVERSION_TABLE));
	}

	public String applyOutputConversionTable(String word){
		return applyConversionTable(word, getData(TAG_OUTPUT_CONVERSION_TABLE));
	}

	private String applyConversionTable(String word, Map<String, String> table){
		READ_WRITE_LOCK.readLock().lock();
		try{
			if(table != null){
				int size = table.size();
				word = StringUtils.replaceEach(word, table.keySet().toArray(new String[size]), table.values().toArray(new String[size]));
			}
			return word;
		}
		finally{
			READ_WRITE_LOCK.readLock().unlock();
		}
	}

	public Set<String> getWordBreakCharacters(){
		return getData(TAG_BREAK);
	}

	public String getCircumfixFlag(){
		return getData(TAG_CIRCUMFIX);
	}

}
