package unit731.hunspeller.parsers;

import unit731.hunspeller.parsers.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.strategies.NumericalParsingStrategy;
import unit731.hunspeller.parsers.strategies.UTF8ParsingStrategy;
import unit731.hunspeller.parsers.strategies.DoubleCharParsingStrategy;
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
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.regexptrie.RegExpTrie;
import unit731.hunspeller.resources.AffixEntry;
import unit731.hunspeller.resources.ParsingContext;
import unit731.hunspeller.resources.RuleEntry;


@Slf4j
public class AffixParser{

	public static final String FLAG_TYPE_UTF_8 = "UTF-8";
	public static final String FLAG_TYPE_DOUBLE_CHAR = "long";
	public static final String FLAG_TYPE_NUMERIC = "num";

	//FEFF because this is the Unicode char represented by the UTF-8 byte order mark (EF BB BF)
	private static final String BOM_MARKER = "\uFEFF";

	//General options
	/**
	 * Set character encoding of words and morphemes in affix and dictionary files. Possible values: UTF-8, ISO8859−1 through ISO8859−10,
	 * ISO8859−13 through ISO8859−15, KOI8-R, KOI8-U, microsoftcp1251, ISCII-DEVANAGARI
	 */
	private static final String TAG_CHARACTER_SET = "SET";
	/**
	 * Set flag type. Default type is the extended ASCII (8-bit) character. ‘UTF-8’ parameter sets UTF-8 encoded Unicode character flags.
	 * The ‘long’ value sets the double extended ASCII character flag type, the ‘num’ sets the decimal number flag type. Decimal flags numbered
	 * from 1 to 65000, and in flag fields are separated by comma
	 */
	private static final String TAG_FLAG = "FLAG";
	/** Set twofold prefix stripping (but single suffix stripping) for agglutinative languages with right-toleft writing system */
	private static final String TAG_COMPLEX_PREFIXES = "COMPLEXPREFIXES";
	private static final String TAG_LANGUAGE = "LANG";

	//Options for suggestions
	private static final String TAG_ALPHABET = "TRY";

	//Options for affix creation
	private static final String TAG_PREFIX = AffixEntry.TYPE.PREFIX.getFlag();
	private static final String TAG_SUFFIX = AffixEntry.TYPE.SUFFIX.getFlag();

	//Other options
	private static final String TAG_FULLSTRIP = "FULLSTRIP";
	private static final String TAG_KEEPCASE = "KEEPCASE";


	private final Map<String, Object> data = new HashMap<>();
	private Charset charset;
	private FlagParsingStrategy strategy;


	private final Consumer<ParsingContext> FUN_COPY_OVER = context -> {
		addData(context.getRuleType(), context.getAllButFirstParameter());
	};
	private final Consumer<ParsingContext> FUN_AFFIX = context -> {
		try{
			AffixEntry.TYPE ruleType = AffixEntry.TYPE.toEnum(context.getRuleType());
			BufferedReader br = context.getReader();
			boolean isSuffix = context.isSuffix();
			String ruleFlag = context.getFirstParameter();
			String combineable = context.getSecondParameter();
			int numEntries = Integer.parseInt(context.getThirdParameter());

			String flag = getFlag();
			strategy = createFlagParsingStrategy(flag);

//			RegExpTrie<AffixEntry> prefixEntries = new RegExpTrie<>();
//			RegExpTrie<AffixEntry> suffixEntries = new RegExpTrie<>();
			List<AffixEntry> entries = new ArrayList<>();
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();

				AffixEntry entry = new AffixEntry(line, strategy);
				if(entry.getType() != ruleType)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at index " + i + ": mismatched rule type (expected "
						+ ruleType + ")");
				if(!ruleFlag.equals(entry.getRuleFlag()))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at index " + i + ": mismatched rule flag (expected "
						+ ruleFlag + ")");
				if(!containsUnique(entry.getContinuationClasses()))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at index " + i + ": multiple rule flags");

				if(entries.contains(entry))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at index " + i + ": duplicated line");

				entries.add(entry);
//				String regexToMatch = (entry.getMatch() != null? entry.getMatch().pattern().pattern().replaceFirst("^\\^", "").replaceFirst("\\$$", ""): ".");
//				if(entry.isSuffix())
//					suffixEntries.add(new StringBuilder(regexToMatch).reverse().toString(), entry);
//				else
//					prefixEntries.add(regexToMatch, entry);
			}

			addData(ruleFlag, new RuleEntry(isSuffix, combineable, entries));
//			addData(ruleFlag, new RuleEntry(isSuffix, combineable, prefixEntries, suffixEntries));
		}
		catch(IOException e){
			throw new RuntimeException(e);
		}
	};
	/** Determines the appropriate {@link FlagParsingStrategy} based on the FLAG definition line taken from the affix file */
	private static FlagParsingStrategy createFlagParsingStrategy(String flag){
		FlagParsingStrategy stategy = null;
		if(flag == null)
			stategy = new UTF8ParsingStrategy();
		else
			switch(flag){
				case FLAG_TYPE_UTF_8:
					stategy = new UTF8ParsingStrategy();
					break;

				case FLAG_TYPE_DOUBLE_CHAR:
					stategy = new DoubleCharParsingStrategy();
					break;

				case FLAG_TYPE_NUMERIC:
					stategy = new NumericalParsingStrategy();
					break;

				default:
					String errorMessage = "Unknown flag type: " + flag;
					log.error(errorMessage);
					throw new IllegalArgumentException(errorMessage);
			}
		return stategy;
	}
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
//		RULE_FUNCTION.put("NAME", FUN_DO_NOTHING);
//		RULE_FUNCTION.put("VERSION", FUN_DO_NOTHING);
//		RULE_FUNCTION.put("HOME", FUN_DO_NOTHING);
		RULE_FUNCTION.put(TAG_CHARACTER_SET, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_FLAG, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_COMPLEX_PREFIXES, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_LANGUAGE, FUN_COPY_OVER);
		//Options for suggestions
//		RULE_FUNCTION.put("KEY", FUN_DO_NOTHING);
//		RULE_FUNCTION.put(TAG_ALPHABET, FUN_DO_NOTHING);
//		RULE_FUNCTION.put("REP", FUN_DO_NOTHING);
//		RULE_FUNCTION.put("MAP", FUN_DO_NOTHING);
		//Options for compounding
//		RULE_FUNCTION.put("BREAK", FUN_DO_NOTHING);
		//Options for affix creation
		RULE_FUNCTION.put(TAG_PREFIX, FUN_AFFIX);
		RULE_FUNCTION.put(TAG_SUFFIX, FUN_AFFIX);
		//Other options
		RULE_FUNCTION.put(TAG_FULLSTRIP, FUN_COPY_OVER);
		RULE_FUNCTION.put(TAG_KEEPCASE, FUN_COPY_OVER);
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
		charset = extractCharset(affFile);
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(affFile.toPath(), charset))){
			String line;
			while((line = br.readLine()) != null){
				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1 && line.startsWith(BOM_MARKER))
					line = line.substring(1);

				line = removeComment(line);
				if(line.isEmpty())
					continue;

				ParsingContext context = new ParsingContext(line, br);
				Consumer<ParsingContext> fun = RULE_FUNCTION.get(context.getRuleType());
				if(fun != null){
					try{
						fun.accept(context);
					}
					catch(RuntimeException e){
						throw new IllegalArgumentException(e.getMessage());
					}
				}
			}
		}
	}

	private Charset extractCharset(File file) throws IOException{
		String charsetCode = StandardCharsets.UTF_8.name();
		try(LineNumberReader br = new LineNumberReader(Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8))){
			String line;
			while((line = br.readLine()) != null){
				//ignore any BOM marker on first line
				if(br.getLineNumber() == 1 && line.startsWith(BOM_MARKER))
					line = line.substring(1);

				line = removeComment(line);
				if(line.isEmpty())
					continue;

				ParsingContext context = new ParsingContext(line, br);
				if(!TAG_CHARACTER_SET.equals(context.getRuleType()))
					continue;

				Consumer<ParsingContext> fun = RULE_FUNCTION.get(TAG_CHARACTER_SET);
				if(fun != null){
					try{
						fun.accept(context);
					}
					catch(RuntimeException e){
						throw new IllegalArgumentException(e.getMessage());
					}
				}
				break;
			}
		}
		return Charset.forName(charsetCode);
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
		line = StringUtils.replaceAll(line, "^$|\\s*#.*$", StringUtils.EMPTY);
		//trim the entire string
		line = StringUtils.replaceAll(line, "^[^\\S\\r\\n]+|[^\\S\\r\\n]+$|^\\r?\\n$", StringUtils.EMPTY);
		return line;
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

	public Charset getCharset(){
		return Charset.forName(getData(TAG_CHARACTER_SET));
	}

	public String getAlphabet(){
		return getData(TAG_ALPHABET);
	}

	public String getKeepcase(){
		return getData(TAG_KEEPCASE);
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
		if(affix instanceof RuleEntry)
			isSuffix = ((RuleEntry)affix).isSuffix();
		return isSuffix;
	}

	public String getFlag(){
		return getData(TAG_FLAG);
	}

	public FlagParsingStrategy getFlagParsingStrategy(){
		return strategy;
	}

}
