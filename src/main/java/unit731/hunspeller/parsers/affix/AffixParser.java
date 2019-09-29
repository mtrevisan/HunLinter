package unit731.hunspeller.parsers.affix;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.affix.handlers.AffixHandler;
import unit731.hunspeller.parsers.affix.handlers.AliasesHandler;
import unit731.hunspeller.parsers.affix.handlers.CompoundRuleHandler;
import unit731.hunspeller.parsers.affix.handlers.ConversionTableHandler;
import unit731.hunspeller.parsers.affix.handlers.CopyOverAsNumberHandler;
import unit731.hunspeller.parsers.affix.handlers.CopyOverHandler;
import unit731.hunspeller.parsers.affix.handlers.Handler;
import unit731.hunspeller.parsers.affix.handlers.WordBreakTableHandler;
import unit731.hunspeller.parsers.enums.AffixOption;
import unit731.hunspeller.parsers.vos.RuleEntry;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.ParserHelper;
import unit731.hunspeller.services.PatternHelper;


/**
 * Managed options:
 *		General:
 *			SET, FLAG, COMPLEXPREFIXES, LANG, AF, AM
 *		Suggestions:
 *			TRY (only read), NOSUGGEST (only read), REP
 *		Compounding:
 *			BREAK (only read), COMPOUNDRULE, COMPOUNDMIN, COMPOUNDFLAG, ONLYINCOMPOUND, COMPOUNDPERMITFLAG, COMPOUNDFORBIDFLAG,
 *			COMPOUNDMORESUFFIXES, COMPOUNDWORDMAX, CHECKCOMPOUNDDUP, CHECKCOMPOUNDREP, CHECKCOMPOUNDCASE, CHECKCOMPOUNDTRIPLE, SIMPLIFIEDTRIPLE,
 *			FORCEUCASE
 *		Affix creation:
 *			PFX, SFX
 *		Others:
 *			CIRCUMFIX, FORBIDDENWORD, FULLSTRIP, KEEPCASE, ICONV, OCONV, NEEDAFFIX
 */
public class AffixParser{

	private static final String NO_LANGUAGE = "xxx";

	private static final String START = "^";
	private static final String END = "$";

	private static final Pattern PATTERN_ISO639_1 = PatternHelper.pattern("([a-z]{2})");
	private static final Pattern PATTERN_ISO639_2 = PatternHelper.pattern("([a-z]{2,3}(?:[-_\\/][a-z]{2,3})?)");

	private static final Handler COPY_OVER = new CopyOverHandler();
	private static final Handler COPY_OVER_AS_NUMBER = new CopyOverAsNumberHandler();
	private static final Handler COMPOUND_RULE = new CompoundRuleHandler();
	private static final Handler AFFIX = new AffixHandler();
	private static final Handler WORD_BREAK_TABLE = new WordBreakTableHandler();
	private static final Handler ALIASES = new AliasesHandler();
	private static final Handler REPLACEMENT_TABLE = new ConversionTableHandler(AffixOption.REPLACEMENT_TABLE);
	private static final Handler INPUT_CONVERSION_TABLE = new ConversionTableHandler(AffixOption.INPUT_CONVERSION_TABLE);
	private static final Handler OUTPUT_CONVERSION_TABLE = new ConversionTableHandler(AffixOption.OUTPUT_CONVERSION_TABLE);

	private static final Map<AffixOption, Handler> PARSING_HANDLERS = new HashMap<>();
	static{
		//General options
//		PARSING_HANDLERS.put("NAME", COPY_OVER);
//		PARSING_HANDLERS.put("VERSION", COPY_OVER);
//		PARSING_HANDLERS.put("HOME", COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.CHARACTER_SET, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.COMPLEX_PREFIXES, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.LANGUAGE, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.IGNORE, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.ALIASES_FLAG, ALIASES);
		PARSING_HANDLERS.put(AffixOption.ALIASES_MORPHOLOGICAL_FIELD, ALIASES);

		//Options for suggestions
//		PARSING_HANDLERS.put(AffixOption.KEY, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.TRY, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.NO_SUGGEST_FLAG, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.MAX_COMPOUND_SUGGEST, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.MAX_NGRAM_SUGGEST, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.MAX_NGRAM_SIMILARITY_FACTOR, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.ONLY_MAX_NGRAM_SIMILARITY_FACTOR, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.NO_SPLIT_SUGGEST, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.NO_NGRAM_SUGGEST, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.SUGGESTIONS_WITH_DOTS, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.REPLACEMENT_TABLE, REPLACEMENT_TABLE);
//		PARSING_HANDLERS.put(AffixOption.MAP_TABLE, MAP);
//		PARSING_HANDLERS.put(AffixOption.PHONE_TABLE, MAP);
//		PARSING_HANDLERS.put(AffixOption.WARN, MAP);
//		PARSING_HANDLERS.put(AffixOption.FORBID_WARN, MAP);

		//Options for compounding
		PARSING_HANDLERS.put(AffixOption.WORD_BREAK_CHARACTERS, WORD_BREAK_TABLE);
		PARSING_HANDLERS.put(AffixOption.COMPOUND_RULE, COMPOUND_RULE);
		PARSING_HANDLERS.put(AffixOption.COMPOUND_MINIMUM_LENGTH, COPY_OVER_AS_NUMBER);
		PARSING_HANDLERS.put(AffixOption.COMPOUND_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.COMPOUND_BEGIN_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.COMPOUND_MIDDLE_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.COMPOUND_END_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.ONLY_IN_COMPOUND_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.PERMIT_COMPOUND_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.FORBID_COMPOUND_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.ALLOW_TWOFOLD_AFFIXES_IN_COMPOUND, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.COMPOUND_ROOT, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.COMPOUND_MAX_WORD_COUNT, COPY_OVER_AS_NUMBER);
		PARSING_HANDLERS.put(AffixOption.FORBID_DUPLICATES_IN_COMPOUND, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.CHECK_COMPOUND_REPLACEMENT, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.FORBID_DIFFERENT_CASES_IN_COMPOUND, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.FORBID_TRIPLES_IN_COMPOUND, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.SIMPLIFIED_TRIPLES_IN_COMPOUND, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.CHECK_COMPOUND_PATTERN, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.FORCE_COMPOUND_UPPERCASE_FLAG, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.COMPOUND_SYLLABLE, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.SYLLABLE_NUMBER, COPY_OVER);

		//Options for affix creation
		PARSING_HANDLERS.put(AffixOption.PREFIX, AFFIX);
		PARSING_HANDLERS.put(AffixOption.SUFFIX, AFFIX);

		//Other options
		PARSING_HANDLERS.put(AffixOption.CIRCUMFIX_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.FORBIDDEN_WORD_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.FULLSTRIP, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.KEEP_CASE_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixOption.INPUT_CONVERSION_TABLE, INPUT_CONVERSION_TABLE);
		PARSING_HANDLERS.put(AffixOption.OUTPUT_CONVERSION_TABLE, OUTPUT_CONVERSION_TABLE);
		PARSING_HANDLERS.put(AffixOption.NEED_AFFIX_FLAG, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.WORD_CHARS, COPY_OVER);
//		PARSING_HANDLERS.put(AffixOption.CHECK_SHARPS, COPY_OVER);
	}


	private final AffixData data = new AffixData();


	/**
	 * Parse the rules out from a .aff file.
	 *
	 * @param affFile	The content of the affix file
	 * @throws IOException	If an I/O error occurs
	 * @throws	IllegalArgumentException	If something is wrong while parsing the file (eg. missing rule)
	 */
	public void parse(final File affFile) throws IOException, IllegalArgumentException{
		data.clear();

		boolean encodingRead = false;
		Charset charset = FileHelper.determineCharset(affFile.toPath());
		try(final LineNumberReader br = FileHelper.createReader(affFile.toPath(), charset)){
			String line;
			while((line = br.readLine()) != null){
				line = ParserHelper.cleanLine(line);
				if(line.isEmpty())
					continue;

				if(!encodingRead && !line.startsWith(AffixOption.CHARACTER_SET.getCode() + StringUtils.SPACE))
					throw new IllegalArgumentException("The first non–comment line in the affix file must be a 'SET charset', was: '" + line + "'");
				else
					encodingRead = true;

				final ParsingContext context = new ParsingContext(line, br);
				final AffixOption ruleType = AffixOption.createFromCode(context.getRuleType());
				final Handler handler = lookupHandlerByRuleType(ruleType);
				if(handler != null){
					try{
						handler.parse(context, data.getFlagParsingStrategy(), data::addData, data::getData);
					}
					catch(final RuntimeException e){
						throw new IllegalArgumentException(e.getMessage() + ", line " + br.getLineNumber());
					}
				}
			}
		}

		postProcessData(affFile);

		data.close();

		data.verify();

//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(data));
//7 490 848 B
	}

	private void postProcessData(final File affFile){
		if(!data.containsData(AffixOption.COMPOUND_MINIMUM_LENGTH))
			data.addData(AffixOption.COMPOUND_MINIMUM_LENGTH, 3);
		else{
			int compoundMin = data.getData(AffixOption.COMPOUND_MINIMUM_LENGTH);
			if(compoundMin < 1)
				data.addData(AffixOption.COMPOUND_MINIMUM_LENGTH, 1);
		}
		//apply default charset
		if(!data.containsData(AffixOption.CHARACTER_SET))
			data.addData(AffixOption.CHARACTER_SET, StandardCharsets.ISO_8859_1);
		if(!data.containsData(AffixOption.LANGUAGE)){
			//try to infer language from filename
			final String filename = FilenameUtils.removeExtension(affFile.getName());
			String[] languages = PatternHelper.extract(filename, PATTERN_ISO639_2);
			if(languages.length == 0)
				languages = PatternHelper.extract(filename, PATTERN_ISO639_1);
			final String language = (languages.length > 0? languages[0]: NO_LANGUAGE);
			data.addData(AffixOption.LANGUAGE, language);
		}
		if(!data.containsData(AffixOption.WORD_BREAK_CHARACTERS)){
			final Set<String> wordBreakCharacters = new HashSet<>(3);
			wordBreakCharacters.add(HyphenationParser.MINUS_SIGN);
			wordBreakCharacters.add(START + HyphenationParser.MINUS_SIGN);
			wordBreakCharacters.add(HyphenationParser.MINUS_SIGN + END);
			data.addData(AffixOption.WORD_BREAK_CHARACTERS, wordBreakCharacters);
		}
		//swap options:
		if(data.isComplexPrefixes()){
			final String compoundBegin = data.getData(AffixOption.COMPOUND_BEGIN_FLAG);
			final String compoundEnd = data.getData(AffixOption.COMPOUND_END_FLAG);
			data.addData(AffixOption.COMPOUND_BEGIN_FLAG, compoundEnd);
			data.addData(AffixOption.COMPOUND_END_FLAG, compoundBegin);

			final RuleEntry prefixes = data.getData(AffixOption.PREFIX);
			final RuleEntry suffixes = data.getData(AffixOption.SUFFIX);
			data.addData(AffixOption.PREFIX, suffixes);
			data.addData(AffixOption.SUFFIX, prefixes);
		}
//		if(!containsData(AffixOption.KEY))
//			data.addData(AffixOption.KEY, "qwertyuiop|asdfghjkl|zxcvbnm");
//		if(!containsData(AffixOption.WORD_CHARS))
//			data.addData(AffixOption.WORD_BREAK_CHARACTERS, "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM");
	}

	private Handler lookupHandlerByRuleType(final AffixOption ruleType){
		return PARSING_HANDLERS.get(ruleType);
	}

	public AffixData getAffixData(){
		return data;
	}

	public void clear(){
		data.clear();
	}

}
