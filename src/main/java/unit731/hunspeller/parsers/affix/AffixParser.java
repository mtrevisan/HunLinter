package unit731.hunspeller.parsers.affix;

import java.io.BufferedReader;
import java.io.EOFException;
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
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.parsers.affix.dtos.ParsingContext;
import unit731.hunspeller.parsers.affix.strategies.ASCIIParsingStrategy;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.parsers.dictionary.dtos.RuleEntry;
import unit731.hunspeller.parsers.dictionary.vos.AffixEntry;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.PatternHelper;
import unit731.hunspeller.services.concurrency.ReadWriteLockable;


/**
 * Managed options:
 *		SET, FLAG, COMPLEXPREFIXES, LANG, AF, AM
 *		NOSUGGEST (only read), REP
 *		BREAK (only read), COMPOUNDRULE, COMPOUNDMIN, COMPOUNDFLAG, ONLYINCOMPOUND, COMPOUNDPERMITFLAG, COMPOUNDFORBIDFLAG, COMPOUNDMORESUFFIXES, COMPOUNDWORDMAX,
 *			CHECKCOMPOUNDDUP, CHECKCOMPOUNDREP, CHECKCOMPOUNDCASE, CHECKCOMPOUNDTRIPLE, SIMPLIFIEDTRIPLE, FORCEUCASE
 *		PFX, SFX
 *		CIRCUMFIX, FORBIDDENWORD, FULLSTRIP, KEEPCASE, ICONV, OCONV, NEEDAFFIX
 */
public class AffixParser extends ReadWriteLockable{

	private static final String NO_LANGUAGE = "xxx";

	private static final String START = "^";
	private static final String END = "$";

	private static final Pattern PATTERN_ISO639_1 = PatternHelper.pattern("([a-z]{2})");
	private static final Pattern PATTERN_ISO639_2 = PatternHelper.pattern("([a-z]{2,3}(?:[-_\\/][a-z]{2,3})?)");

	private static final String DOUBLE_MINUS_SIGN = HyphenationParser.MINUS_SIGN + HyphenationParser.MINUS_SIGN;


	private static enum AliasesType{
		FLAG(AffixTag.ALIASES_FLAG),
		MORPHOLOGICAL_FIELD(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);


		private final AffixTag flag;


		AliasesType(AffixTag flag){
			this.flag = flag;
		}

		public static AliasesType toEnum(String code){
			return Arrays.stream(values())
				.filter(tag -> tag.flag.getCode().equals(code))
				.findFirst()
				.orElse(null);
		}

		public boolean is(String flag){
			return this.flag.getCode().equals(flag);
		}

		public AffixTag getFlag(){
			return flag;
		}

	}

	private static enum ConversionTableType{
		REPLACEMENT(AffixTag.REPLACEMENT_TABLE),
		INPUT(AffixTag.INPUT_CONVERSION_TABLE),
		OUTPUT(AffixTag.OUTPUT_CONVERSION_TABLE);


		private final AffixTag flag;


		ConversionTableType(AffixTag flag){
			this.flag = flag;
		}

		public static ConversionTableType toEnum(String code){
			return Arrays.stream(values())
				.filter(tag -> tag.flag.getCode().equals(code))
				.findFirst()
				.orElse(null);
		}

		public AffixTag getFlag(){
			return flag;
		}

	}


	private final Map<String, Object> data = new HashMap<>();
	private Charset charset;
	private FlagParsingStrategy strategy = new ASCIIParsingStrategy();

	private final Set<String> terminalAffixes = new HashSet<>();


	private final Consumer<ParsingContext> funCopyOver = context -> {
		addData(context.getRuleType(), context.getAllButFirstParameter());
	};
	private final Consumer<ParsingContext> funCopyOverAsNumber = context -> {
		if(!NumberUtils.isCreatable(context.getFirstParameter()))
			throw new IllegalArgumentException("Error reading line \"" + context + "\": The first parameter is not a number");
		addData(context.getRuleType(), Integer.parseInt(context.getAllButFirstParameter()));
	};
	private final Consumer<ParsingContext> funCompoundRule = context -> {
		try{
			BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context + "\": The first parameter is not a number");
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be a positive integer");

			Set<String> compoundRules = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				if(line == null)
					throw new EOFException("Unexpected EOF while reading Dictionary file");

				line = DictionaryParser.cleanLine(line);

				String[] lineParts = StringUtils.split(line);
				AffixTag tag = AffixTag.toEnum(lineParts[0]);
				if(tag != AffixTag.COMPOUND_RULE)
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": mismatched compound rule type (expected "
						+ AffixTag.COMPOUND_RULE + ")");
				String rule = lineParts[1];
				if(StringUtils.isBlank(rule))
					throw new IllegalArgumentException("Error reading line \"" + line + "\" at row " + i + ": compound rule type cannot be empty");
				String[] compounds = strategy.extractCompoundRule(rule);
				if(compounds.length == 0)
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
	private final Consumer<ParsingContext> funAffix = context -> {
		try{
			AffixEntry.Type ruleType = AffixEntry.Type.toEnum(context.getRuleType());
			BufferedReader br = context.getReader();
			boolean isSuffix = AffixEntry.Type.SUFFIX.is(context.getRuleType());
			String ruleFlag = context.getFirstParameter();
			char combineable = context.getSecondParameter().charAt(0);
			if(!NumberUtils.isCreatable(context.getThirdParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context + "\": The third parameter is not a number");
			int numEntries = Integer.parseInt(context.getThirdParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be a positive integer");

//List<AffixEntry> prefixEntries = new ArrayList<>();
//List<AffixEntry> suffixEntries = new ArrayList<>();
			List<String> aliasesFlag = getData(AffixTag.ALIASES_FLAG);
			List<String> aliasesMorphologicalField = getData(AffixTag.ALIASES_MORPHOLOGICAL_FIELD);
			List<AffixEntry> entries = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				if(line == null)
					throw new EOFException("Unexpected EOF while reading Dictionary file");

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
	private final Consumer<ParsingContext> funWordBreakTable = context -> {
		try{
			BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context + "\": The first parameter is not a number");
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be a positive integer");

			Set<String> wordBreakCharacters = new HashSet<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				if(line == null)
					throw new EOFException("Unexpected EOF while reading Dictionary file");

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
	private final Consumer<ParsingContext> funAliases = context -> {
		try{
			AliasesType aliasesType = AliasesType.toEnum(context.getRuleType());
			BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context + "\": The first parameter is not a number");
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be a positive integer");

			List<String> aliases = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				if(line == null)
					throw new EOFException("Unexpected EOF while reading Dictionary file");

				line = DictionaryParser.cleanLine(line);

				String[] parts = StringUtils.split(line);
				if(parts.length != 2)
					throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be <tag> <flag/morphological field>");
				if(!aliasesType.is(parts[0]))
					throw new IllegalArgumentException("Error reading line \"" + context + ": Bad tag, it must be " + aliasesType.getFlag().getCode());

				aliases.add(parts[1]);
			}

			addData(aliasesType.getFlag(), aliases);
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	};
	private final Consumer<ParsingContext> funConversionTable = context -> {
		try{
			ConversionTableType conversionTableType = ConversionTableType.toEnum(context.getRuleType());
			AffixTag tag = conversionTableType.getFlag();
			BufferedReader br = context.getReader();
			if(!NumberUtils.isCreatable(context.getFirstParameter()))
				throw new IllegalArgumentException("Error reading line \"" + context + "\": The first parameter is not a number");
			int numEntries = Integer.parseInt(context.getFirstParameter());
			if(numEntries <= 0)
				throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be a positive integer");

			List<Pair<String, String>> conversionTable = new ArrayList<>(numEntries);
			for(int i = 0; i < numEntries; i ++){
				String line = br.readLine();
				if(line == null)
					throw new EOFException("Unexpected EOF while reading Dictionary file");

				line = DictionaryParser.cleanLine(line);

				String[] parts = StringUtils.split(line);
				if(parts.length != 3)
					throw new IllegalArgumentException("Error reading line \"" + context + ": Bad number of entries, it must be <tag> <pattern-from> <pattern-to>");
				if(!tag.getCode().equals(parts[0]))
					throw new IllegalArgumentException("Error reading line \"" + context + ": Bad tag, it must be " + tag.getCode());

				conversionTable.add(Pair.of(parts[1], StringUtils.replaceChars(parts[2], '_', ' ')));
			}

			addData(tag, conversionTable);
		}
		catch(IOException e){
			throw new RuntimeException(e.getMessage());
		}
	};

	private final Map<AffixTag, Consumer<ParsingContext>> ruleFunction = new HashMap<>();

	private ConversionTable replacementTable;
	private ConversionTable inputConversionTable;
	private ConversionTable outputConversionTable;


	public AffixParser(){
		//General options
//		ruleFunction.put("NAME", funCopyOver);
//		ruleFunction.put("VERSION", funCopyOver);
//		ruleFunction.put("HOME", funCopyOver);
		ruleFunction.put(AffixTag.CHARACTER_SET, funCopyOver);
		ruleFunction.put(AffixTag.FLAG, funCopyOver);
		ruleFunction.put(AffixTag.COMPLEX_PREFIXES, funCopyOver);
		ruleFunction.put(AffixTag.LANGUAGE, funCopyOver);
//		ruleFunction.put(AffixTag.IGNORE, funCopyOver);
		ruleFunction.put(AffixTag.ALIASES_FLAG, funAliases);
		ruleFunction.put(AffixTag.ALIASES_MORPHOLOGICAL_FIELD, funAliases);

		//Options for suggestions
//		ruleFunction.put(AffixTag.KEY, funCopyOver);
//		ruleFunction.put(AffixTag.TRY, funCopyOver);
		ruleFunction.put(AffixTag.NO_SUGGEST, funCopyOver);
//		ruleFunction.put(AffixTag.MAX_COMPOUND_SUGGEST, funCopyOver);
//		ruleFunction.put(AffixTag.MAX_NGRAM_SUGGEST, funCopyOver);
//		ruleFunction.put(AffixTag.MAX_NGRAM_SIMILARITY_FACTOR, funCopyOver);
//		ruleFunction.put(AffixTag.ONLY_MAX_NGRAM_SIMILARITY_FACTOR, funCopyOver);
//		ruleFunction.put(AffixTag.NO_SPLIT_SUGGEST, funCopyOver);
//		ruleFunction.put(AffixTag.NO_NGRAM_SUGGEST, funCopyOver);
//		ruleFunction.put(AffixTag.SUGGESTIONS_WITH_DOTS, funCopyOver);
		ruleFunction.put(AffixTag.REPLACEMENT_TABLE, funConversionTable);
//		ruleFunction.put(AffixTag.MAP_TABLE, funMap);
//		ruleFunction.put(AffixTag.PHONE_TABLE, funMap);
//		ruleFunction.put(AffixTag.WARN, funMap);
//		ruleFunction.put(AffixTag.FORBID_WARN, funMap);

		//Options for compounding
		ruleFunction.put(AffixTag.BREAK, funWordBreakTable);
		ruleFunction.put(AffixTag.COMPOUND_RULE, funCompoundRule);
		ruleFunction.put(AffixTag.COMPOUND_MIN, funCopyOverAsNumber);
		ruleFunction.put(AffixTag.COMPOUND_FLAG, funCopyOver);
		ruleFunction.put(AffixTag.COMPOUND_BEGIN, funCopyOver);
		ruleFunction.put(AffixTag.COMPOUND_MIDDLE, funCopyOver);
		ruleFunction.put(AffixTag.COMPOUND_END, funCopyOver);
		ruleFunction.put(AffixTag.ONLY_IN_COMPOUND, funCopyOver);
		ruleFunction.put(AffixTag.COMPOUND_PERMIT_FLAG, funCopyOver);
		ruleFunction.put(AffixTag.COMPOUND_FORBID_FLAG, funCopyOver);
		ruleFunction.put(AffixTag.COMPOUND_MORE_SUFFIXES, funCopyOver);
//		ruleFunction.put(AffixTag.COMPOUND_ROOT, funCopyOver);
		ruleFunction.put(AffixTag.COMPOUND_WORD_MAX, funCopyOverAsNumber);
		ruleFunction.put(AffixTag.CHECK_COMPOUND_DUPLICATION, funCopyOver);
		ruleFunction.put(AffixTag.CHECK_COMPOUND_REPLACEMENT, funCopyOver);
		ruleFunction.put(AffixTag.CHECK_COMPOUND_CASE, funCopyOver);
		ruleFunction.put(AffixTag.CHECK_COMPOUND_TRIPLE, funCopyOver);
		ruleFunction.put(AffixTag.SIMPLIFIED_TRIPLE, funCopyOver);
//		ruleFunction.put(AffixTag.CHECK_COMPOUND_PATTERN, funCopyOver);
		ruleFunction.put(AffixTag.FORCE_UPPERCASE, funCopyOver);
//		ruleFunction.put(AffixTag.COMPOUND_SYLLABLE, funCopyOver);
//		ruleFunction.put(AffixTag.SYLLABLE_NUMBER, funCopyOver);

//Options for affix creation
		ruleFunction.put(AffixTag.PREFIX, funAffix);
		ruleFunction.put(AffixTag.SUFFIX, funAffix);

		//Other options
		ruleFunction.put(AffixTag.CIRCUMFIX, funCopyOver);
		ruleFunction.put(AffixTag.FORBIDDEN_WORD, funCopyOver);
		ruleFunction.put(AffixTag.FULLSTRIP, funCopyOver);
		ruleFunction.put(AffixTag.KEEP_CASE, funCopyOver);
		ruleFunction.put(AffixTag.INPUT_CONVERSION_TABLE, funConversionTable);
		ruleFunction.put(AffixTag.OUTPUT_CONVERSION_TABLE, funConversionTable);
		ruleFunction.put(AffixTag.NEED_AFFIX, funCopyOver);
//		ruleFunction.put(AffixTag.WORD_CHARS, funCopyOver);
//		ruleFunction.put(AffixTag.CHECK_SHARPS, funCopyOver);
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
					Consumer<ParsingContext> fun = ruleFunction.get(ruleType);
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

			replacementTable = new ConversionTable(getData(AffixTag.REPLACEMENT_TABLE));
			inputConversionTable = new ConversionTable(getData(AffixTag.INPUT_CONVERSION_TABLE));
			outputConversionTable = new ConversionTable(getData(AffixTag.OUTPUT_CONVERSION_TABLE));
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
			strategy = new ASCIIParsingStrategy();
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
	public List<Pair<String, String>> getReplacementTable(){
		return getData(AffixTag.REPLACEMENT_TABLE);
	}

	public String applyReplacementTable(String word){
		return replacementTable.applyConversionTable(word);
	}

	public String applyInputConversionTable(String word){
		return inputConversionTable.applyConversionTable(word);
	}

	public String applyOutputConversionTable(String word){
		return outputConversionTable.applyConversionTable(word);
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
