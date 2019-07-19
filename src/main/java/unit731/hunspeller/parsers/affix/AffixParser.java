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
import unit731.hunspeller.parsers.vos.RuleEntry;
import unit731.hunspeller.parsers.enums.AffixTag;
import unit731.hunspeller.parsers.hyphenation.HyphenationParser;
import unit731.hunspeller.services.FileHelper;
import unit731.hunspeller.services.ParserHelper;
import unit731.hunspeller.services.PatternHelper;


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
	private static final Handler REPLACEMENT_TABLE = new ConversionTableHandler(AffixTag.REPLACEMENT_TABLE);
	private static final Handler INPUT_CONVERSION_TABLE = new ConversionTableHandler(AffixTag.INPUT_CONVERSION_TABLE);
	private static final Handler OUTPUT_CONVERSION_TABLE = new ConversionTableHandler(AffixTag.OUTPUT_CONVERSION_TABLE);

	private static final Map<AffixTag, Handler> PARSING_HANDLERS = new HashMap<>();
	static{
		//General options
//		PARSING_HANDLERS.put("NAME", COPY_OVER);
//		PARSING_HANDLERS.put("VERSION", COPY_OVER);
//		PARSING_HANDLERS.put("HOME", COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.CHARACTER_SET, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.COMPLEX_PREFIXES, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.LANGUAGE, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.IGNORE, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.ALIASES_FLAG, ALIASES);
		PARSING_HANDLERS.put(AffixTag.ALIASES_MORPHOLOGICAL_FIELD, ALIASES);

		//Options for suggestions
//		PARSING_HANDLERS.put(AffixTag.KEY, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.TRY, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.NO_SUGGEST_FLAG, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.MAX_COMPOUND_SUGGEST, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.MAX_NGRAM_SUGGEST, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.MAX_NGRAM_SIMILARITY_FACTOR, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.ONLY_MAX_NGRAM_SIMILARITY_FACTOR, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.NO_SPLIT_SUGGEST, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.NO_NGRAM_SUGGEST, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.SUGGESTIONS_WITH_DOTS, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.REPLACEMENT_TABLE, REPLACEMENT_TABLE);
//		PARSING_HANDLERS.put(AffixTag.MAP_TABLE, MAP);
//		PARSING_HANDLERS.put(AffixTag.PHONE_TABLE, MAP);
//		PARSING_HANDLERS.put(AffixTag.WARN, MAP);
//		PARSING_HANDLERS.put(AffixTag.FORBID_WARN, MAP);

		//Options for compounding
		PARSING_HANDLERS.put(AffixTag.WORD_BREAK_CHARACTERS, WORD_BREAK_TABLE);
		PARSING_HANDLERS.put(AffixTag.COMPOUND_RULE, COMPOUND_RULE);
		PARSING_HANDLERS.put(AffixTag.COMPOUND_MINIMUM_LENGTH, COPY_OVER_AS_NUMBER);
		PARSING_HANDLERS.put(AffixTag.COMPOUND_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.COMPOUND_BEGIN_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.COMPOUND_MIDDLE_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.COMPOUND_END_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.ONLY_IN_COMPOUND_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.PERMIT_COMPOUND_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.FORBID_COMPOUND_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.ALLOW_TWOFOLD_AFFIXES_IN_COMPOUND, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.COMPOUND_ROOT, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.COMPOUND_MAX_WORD_COUNT, COPY_OVER_AS_NUMBER);
		PARSING_HANDLERS.put(AffixTag.FORBID_DUPLICATES_IN_COMPOUND, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.CHECK_COMPOUND_REPLACEMENT, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.FORBID_DIFFERENT_CASES_IN_COMPOUND, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.FORBID_TRIPLES_IN_COMPOUND, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.SIMPLIFIED_TRIPLES_IN_COMPOUND, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.CHECK_COMPOUND_PATTERN, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.FORCE_COMPOUND_UPPERCASE_FLAG, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.COMPOUND_SYLLABLE, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.SYLLABLE_NUMBER, COPY_OVER);

		//Options for affix creation
		PARSING_HANDLERS.put(AffixTag.PREFIX, AFFIX);
		PARSING_HANDLERS.put(AffixTag.SUFFIX, AFFIX);

		//Other options
		PARSING_HANDLERS.put(AffixTag.CIRCUMFIX_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.FORBIDDEN_WORD_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.FULLSTRIP, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.KEEP_CASE_FLAG, COPY_OVER);
		PARSING_HANDLERS.put(AffixTag.INPUT_CONVERSION_TABLE, INPUT_CONVERSION_TABLE);
		PARSING_HANDLERS.put(AffixTag.OUTPUT_CONVERSION_TABLE, OUTPUT_CONVERSION_TABLE);
		PARSING_HANDLERS.put(AffixTag.NEED_AFFIX_FLAG, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.WORD_CHARS, COPY_OVER);
//		PARSING_HANDLERS.put(AffixTag.CHECK_SHARPS, COPY_OVER);
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

				if(!encodingRead && !line.startsWith(AffixTag.CHARACTER_SET.getCode() + StringUtils.SPACE))
					throw new IllegalArgumentException("The first non–comment line in the affix file must be a 'SET charset', was: '" + line + "'");
				else
					encodingRead = true;

				final ParsingContext context = new ParsingContext(line, br);
				final AffixTag ruleType = AffixTag.createFromCode(context.getRuleType());
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

//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(data));
//7 490 848 B
	}

	private void postProcessData(final File affFile){
		if(!data.containsData(AffixTag.COMPOUND_MINIMUM_LENGTH))
			data.addData(AffixTag.COMPOUND_MINIMUM_LENGTH, 3);
		else{
			int compoundMin = data.getData(AffixTag.COMPOUND_MINIMUM_LENGTH);
			if(compoundMin < 1)
				data.addData(AffixTag.COMPOUND_MINIMUM_LENGTH, 1);
		}
		//apply default charset
		if(!data.containsData(AffixTag.CHARACTER_SET))
			data.addData(AffixTag.CHARACTER_SET, StandardCharsets.ISO_8859_1);
		if(!data.containsData(AffixTag.LANGUAGE)){
			//try to infer language from filename
			final String filename = FilenameUtils.removeExtension(affFile.getName());
			String[] languages = PatternHelper.extract(filename, PATTERN_ISO639_2);
			if(languages.length == 0)
				languages = PatternHelper.extract(filename, PATTERN_ISO639_1);
			final String language = (languages.length > 0? languages[0]: NO_LANGUAGE);
			data.addData(AffixTag.LANGUAGE, language);
		}
		if(!data.containsData(AffixTag.WORD_BREAK_CHARACTERS)){
			final Set<String> wordBreakCharacters = new HashSet<>(3);
			wordBreakCharacters.add(HyphenationParser.MINUS_SIGN);
			wordBreakCharacters.add(START + HyphenationParser.MINUS_SIGN);
			wordBreakCharacters.add(HyphenationParser.MINUS_SIGN + END);
			data.addData(AffixTag.WORD_BREAK_CHARACTERS, wordBreakCharacters);
		}
		//swap tags:
		if(data.isComplexPrefixes()){
			final String compoundBegin = data.getData(AffixTag.COMPOUND_BEGIN_FLAG);
			final String compoundEnd = data.getData(AffixTag.COMPOUND_END_FLAG);
			data.addData(AffixTag.COMPOUND_BEGIN_FLAG, compoundEnd);
			data.addData(AffixTag.COMPOUND_END_FLAG, compoundBegin);

			final RuleEntry prefixes = data.getData(AffixTag.PREFIX);
			final RuleEntry suffixes = data.getData(AffixTag.SUFFIX);
			data.addData(AffixTag.PREFIX, suffixes);
			data.addData(AffixTag.SUFFIX, prefixes);
		}
//		if(!containsData(AffixTag.KEY))
//			data.addData(AffixTag.KEY, "qwertyuiop|asdfghjkl|zxcvbnm");
//		if(!containsData(AffixTag.WORD_CHARS))
//			data.addData(AffixTag.WORD_BREAK_CHARACTERS, "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM");
	}

	private Handler lookupHandlerByRuleType(final AffixTag ruleType){
		return PARSING_HANDLERS.get(ruleType);
	}

	public AffixData getAffixData(){
		return data;
	}

	public void clear(){
		data.clear();
	}

}
