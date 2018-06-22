package unit731.hunspeller.parsers.affix;

import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.strategies.NumericalParsingStrategy;
import unit731.hunspeller.parsers.strategies.UTF8ParsingStrategy;
import unit731.hunspeller.parsers.strategies.DoubleASCIIParsingStrategy;
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
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.dictionary.AffixEntry;
import unit731.hunspeller.parsers.dictionary.RuleEntry;
import unit731.hunspeller.parsers.strategies.ASCIIParsingStrategy;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.PatternService;


/**
 * Options managed: SET, LANG, FLAG, COMPLEXPREFIXES, PFX, SFX, FULLSTRIP, KEEPCASE
 */
public class AffixParser{

	public static final String FLAG_TYPE_UTF_8 = "UTF-8";
	public static final String FLAG_TYPE_DOUBLE_CHAR = "long";
	public static final String FLAG_TYPE_NUMERIC = "num";

	private static final Pattern REGEX_PATTERN_SEPARATOR = PatternService.pattern("[\\s\\t]+");

	//General options
	/**
	 * Set character encoding of words and morphemes in affix and dictionary files. Possible values are UTF-8, ISO8859-1 through ISO8859-10,
	 * ISO8859-13 through ISO8859-15, KOI8-R, KOI8-U, MICROSOFT-CP1251, ISCII-DEVANAGARI
	 */
	private static final String TAG_CHARACTER_SET = "SET";
	/**
	 * Set flag type. Default type is the extended ASCII (8-bit) character. ‘UTF-8’ parameter sets UTF-8 encoded Unicode character flags.
	 * The ‘long’ value sets the double extended ASCII character flag type, the ‘num’ sets the decimal number flag type. Decimal flags numbered
	 * from 1 to 65000, and in flag fields are separated by comma
	 */
	private static final String TAG_FLAG = "FLAG";
	/** Set twofold prefix stripping (but single suffix stripping) for agglutinative languages with right-to-left writing system */
	private static final String TAG_COMPLEX_PREFIXES = "COMPLEXPREFIXES";
	/** Language code */
	private static final String TAG_LANGUAGE = "LANG";

	//Options for suggestions
	private static final String TAG_NO_SUGGEST = "NOSUGGEST";

	//Options for compounding
	private static final String TAG_COMPOUND_RULE = "COMPOUNDRULE";
	/** Minimum length of words in compound words */
	private static final String TAG_COMPOUND_MIN = "COMPOUNDMIN";
	/** Words may be in compound words (except when word shorter than COMPOUNDMIN), affixes with this flag also permits compounding of affixed words */
	private static final String TAG_COMPOUND_FLAG = "COMPOUNDFLAG";
	/** Words signed with this flag (or with a signed affix) may be first elements in compound words */
	private static final String TAG_COMPOUND_BEGIN = "COMPOUNDBEGIN";
	/** Words signed with this flag (or with a signed affix) may be middle elements in compound words */
	private static final String TAG_COMPOUND_MIDDLE = "COMPOUNDMIDDLE";
	/** Words signed with this flag (or with a signed affix) may be last elements in compound words */
	private static final String TAG_COMPOUND_LAST = "COMPOUNDLAST";
	/** Suffixes signed this flag may be only inside of compounds (this flag works also with words) */
	private static final String TAG_ONLY_IN_COMPOUND = "ONLYINCOMPOUND";
	/**
	 * Prefixes are allowed at the beginning of compounds, suffixes are allowed at the end of compounds by default.
	 * Affixes with this flag may be inside of compounds.
	 */
	private static final String TAG_COMPOUND_PERMIT_FLAG = "COMPOUNDPERMITFLAG";
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
	/** Affixes signed with this flag may be on a word when this word also has a prefix with this flag and vice versa */
	private static final String TAG_CIRCUMFIX = "CIRCUMFIX";

	//Options for affix creation
	private static final String TAG_PREFIX = AffixEntry.Type.PREFIX.getFlag();
	private static final String TAG_SUFFIX = AffixEntry.Type.SUFFIX.getFlag();

	//Other options
	/** With this flag the affix rules can strip full words, not only one less characters */
	private static final String TAG_FULLSTRIP = "FULLSTRIP";
	/** Forbid uppercased and capitalized forms of words signed with this flag */
	private static final String TAG_KEEP_CASE = "KEEPCASE";

	private static final Matcher REGEX_COMMENT = PatternService.matcher("^$|^\\s*#.*$");


	private final Map<String, Object> data = new HashMap<>();
	private Charset charset;
	@Getter
	private FlagParsingStrategy strategy;


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
			if(numEntries == 0)
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ ": Bad number of entries, it must be a positive integer");

			String flag = getFlag();
			strategy = getFlagParsingStrategy(flag);

			Set<String> compoundRules = new HashSet<>();
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();

				String[] lineParts = PatternService.split(line, REGEX_PATTERN_SEPARATOR);
				String tag = lineParts[0];
				if(!TAG_COMPOUND_RULE.equals(tag))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": mismatched rule type (expected "
						+ TAG_COMPOUND_RULE + ")");
				String rule = lineParts[1];
				if(StringUtils.isBlank(rule))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": rule type cannot be empty");

				if(compoundRules.contains(rule))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": duplicated line");

				compoundRules.add(rule);
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
			if(numEntries == 0)
				throw new IllegalArgumentException("Error reading line \"" + context.toString()
					+ ": Bad number of entries, it must be a positive integer");

			String flag = getFlag();
			strategy = getFlagParsingStrategy(flag);

//List<AffixEntry> prefixEntries = new ArrayList<>();
//List<AffixEntry> suffixEntries = new ArrayList<>();
			List<AffixEntry> entries = new ArrayList<>();
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
				if(!containsUnique(entry.getRuleFlags()))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": multiple rule flags");

				if(entries.contains(entry))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": duplicated line");

				entries.add(entry);
//String regexToMatch = (Objects.nonNull(entry.getMatch())? entry.getMatch().pattern().pattern().replaceFirst("^\\^", StringUtils.EMPTY).replaceFirst("\\$$", StringUtils.EMPTY): ".");
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

	/** Determines the appropriate {@link FlagParsingStrategy} based on the FLAG definition line taken from the affix file */
	private static FlagParsingStrategy getFlagParsingStrategy(String flag){
		FlagParsingStrategy stategy = null;
		if(Objects.isNull(flag))
			stategy = new ASCIIParsingStrategy();
		else
			switch(flag){
				case FLAG_TYPE_UTF_8:
					stategy = new UTF8ParsingStrategy();
					break;

				case FLAG_TYPE_DOUBLE_CHAR:
					stategy = new DoubleASCIIParsingStrategy();
					break;

				case FLAG_TYPE_NUMERIC:
					stategy = new NumericalParsingStrategy();
					break;

				default:
					throw new IllegalArgumentException("Unknown flag type: " + flag);
			}
		return stategy;
	}

	private static boolean containsUnique(String[] list){
		if(Objects.isNull(list))
			return true;

		Set<String> set = new HashSet<>();
		return Arrays.stream(list)
			.allMatch(set::add);
	}

	private final Map<String, Consumer<ParsingContext>> RULE_FUNCTION = new HashMap<>();


	public AffixParser(){
		//General options
//		RULE_FUNCTION.put("NAME", FUN_DO_NOTHING);
//		RULE_FUNCTION.put("VERSION", FUN_DO_NOTHING);
//		RULE_FUNCTION.put("HOME", FUN_DO_NOTHING);
		RULE_FUNCTION.put(TAG_CHARACTER_SET, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_FLAG, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_COMPLEX_PREFIXES, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_LANGUAGE, FUN_COPY_OVER);
		//Options for suggestions
//		RULE_FUNCTION.put("KEY", FUN_DO_NOTHING);
//		RULE_FUNCTION.put("TRY", FUN_DO_NOTHING);
		RULE_FUNCTION.put(TAG_NO_SUGGEST, FUN_COPY_OVER);
//		RULE_FUNCTION.put("REP", FUN_DO_NOTHING);
//		RULE_FUNCTION.put("MAP", FUN_DO_NOTHING);
		//Options for compounding
		//default break table contains: "-", "^-", and "-$"
//		RULE_FUNCTION.put("BREAK", FUN_DO_NOTHING);
		RULE_FUNCTION.put(TAG_COMPOUND_RULE, FUN_COMPOUND_RULE);
		RULE_FUNCTION.put(TAG_COMPOUND_MIN, FUN_COPY_OVER_AS_NUMBER);
		RULE_FUNCTION.put(TAG_COMPOUND_FLAG, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_COMPOUND_BEGIN, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_COMPOUND_MIDDLE, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_COMPOUND_LAST, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_ONLY_IN_COMPOUND, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_COMPOUND_PERMIT_FLAG, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_COMPOUND_FORBID_FLAG, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_COMPOUND_WORD_MAX, FUN_COPY_OVER_AS_NUMBER);
		RULE_FUNCTION.put(TAG_CHECK_COMPOUND_DUPLICATION, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_CHECK_COMPOUND_REPLACEMENT, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_CHECK_COMPOUND_CASE, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_CHECK_COMPOUND_TRIPLE, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_SIMPLIFIED_TRIPLE, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_CIRCUMFIX, FUN_COPY_OVER);
		//Options for affix creation
		RULE_FUNCTION.put(TAG_PREFIX, FUN_AFFIX);
		RULE_FUNCTION.put(TAG_SUFFIX, FUN_AFFIX);
		//Other options
		RULE_FUNCTION.put(TAG_FULLSTRIP, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_KEEP_CASE, FUN_COPY_OVER);
//		RULE_FUNCTION.put("ICONV", FUN_DO_NOTHING);
//		RULE_FUNCTION.put("WORDCHARS", FUN_DO_NOTHING);
	}

	/**
	 * Parse the rules out from a .aff file.
	 *
	 * @param affFile	The content of the affix file
	 * @throws IOException	If an I/O error occurs
	 * @throws	IllegalArgumentException	If something is wrong while parsing the file (eg. missing rule)
	 */
	public void parse(File affFile) throws IOException, IllegalArgumentException{
		boolean encodingRead = false;
		charset = FileService.determineCharset(affFile.toPath());
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(affFile.toPath(), charset))){
			String line;
			while(Objects.nonNull(line = br.readLine())){
				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1 && line.startsWith(FileService.BOM_MARKER))
					line = line.substring(1);

				line = removeComment(line);
				if(line.isEmpty())
					continue;

				if(!encodingRead && !line.startsWith(TAG_CHARACTER_SET + StringUtils.SPACE))
					throw new IllegalArgumentException("The first non–comment line in the affix file must be a 'SET charset', was: '" + line + "'");
				else
					encodingRead = true;

				ParsingContext context = new ParsingContext(line, br);
				Consumer<ParsingContext> fun = RULE_FUNCTION.get(context.getRuleType());
				if(Objects.nonNull(fun)){
					try{
						fun.accept(context);
					}
					catch(RuntimeException e){
						throw new IllegalArgumentException(e.getMessage() + " on line " + br.getLineNumber());
					}
				}
			}
		}

		if(!containsData(TAG_COMPOUND_MIN))
			addData(TAG_COMPOUND_MIN, 3);
		//apply default charset
		if(!containsData(TAG_CHARACTER_SET))
			addData(TAG_CHARACTER_SET, StandardCharsets.ISO_8859_1);
		if(!containsData(TAG_LANGUAGE))
			//try to infer language from filename
			addData(TAG_LANGUAGE, affFile.getName().replaceFirst("\\..+$", StringUtils.EMPTY));

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
	private String removeComment(String line){
		//remove comments
		line = PatternService.clear(line, REGEX_COMMENT);
		//trim the entire string
		return StringUtils.strip(line);
	}

	private boolean containsData(String key){
		return data.containsKey(key);
	}

	@SuppressWarnings("unchecked")
	public <T> T getData(String key){
		return (T)data.get(key);
	}

	private <T> void addData(String key, T value){
		data.put(key, value);
	}

	private void clearData(){
		data.clear();
	}

	public String getLanguage(){
		return getData(TAG_LANGUAGE);
	}

	public String getKeepCaseFlag(){
		return getData(TAG_KEEP_CASE);
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
		Boolean isSuffix = null;
		Object affix = getData(affixCode);
		if(Objects.nonNull(affix) && RuleEntry.class.isAssignableFrom(affix.getClass()))
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
		return getData(TAG_FLAG);
	}

	public FlagParsingStrategy getFlagParsingStrategy(){
		return strategy;
	}

}
