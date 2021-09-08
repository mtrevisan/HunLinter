/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.parsers.affix;

import io.github.mtrevisan.hunlinter.parsers.affix.handlers.AffixHandler;
import io.github.mtrevisan.hunlinter.parsers.affix.handlers.AliasesHandler;
import io.github.mtrevisan.hunlinter.parsers.affix.handlers.CompoundRuleHandler;
import io.github.mtrevisan.hunlinter.parsers.affix.handlers.ConversionTableHandler;
import io.github.mtrevisan.hunlinter.parsers.affix.handlers.CopyOverAsNumberHandler;
import io.github.mtrevisan.hunlinter.parsers.affix.handlers.CopyOverHandler;
import io.github.mtrevisan.hunlinter.parsers.affix.handlers.Handler;
import io.github.mtrevisan.hunlinter.parsers.affix.handlers.RelationTableHandler;
import io.github.mtrevisan.hunlinter.parsers.affix.handlers.WordBreakTableHandler;
import io.github.mtrevisan.hunlinter.parsers.enums.AffixOption;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.parsers.vos.RuleEntry;
import io.github.mtrevisan.hunlinter.services.ParserHelper;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Managed options:
 *		General:
 *			SET, FLAG, COMPLEXPREFIXES, LANG, AF, AM
 *		Suggestions:
 *			TRY (only read), NOSUGGEST (only read), REP, MAP (only read)
 *		Compounding:
 *			BREAK (only read), COMPOUNDRULE, COMPOUNDMIN, COMPOUNDFLAG, ONLYINCOMPOUND, COMPOUNDPERMITFLAG, COMPOUNDFORBIDFLAG,
 *			COMPOUNDMORESUFFIXES, COMPOUNDWORDMAX, CHECKCOMPOUNDDUP, CHECKCOMPOUNDREP, CHECKCOMPOUNDCASE, CHECKCOMPOUNDTRIPLE,
 *			SIMPLIFIEDTRIPLE, FORCEUCASE
 *		Affix creation:
 *			PFX, SFX
 *		Others:
 *			CIRCUMFIX, FORBIDDENWORD, FULLSTRIP, KEEPCASE, ICONV, OCONV, NEEDAFFIX
 *
 * @see <a href="http://manpages.ubuntu.com/manpages/trusty/en/man4/hunspell.4.html">Ubuntu manuals 4</a>
 */
public class AffixParser{

	private static final String BAD_FIRST_LINE = "The first non-comment line in the affix file must be a 'SET charset', was: `{}`";
	private static final String GLOBAL_ERROR_MESSAGE = "{}, line {}";

	private static final String NO_LANGUAGE = "xxx";

	private static final String START = "^";
	private static final String END = "$";

	private static final Pattern PATTERN_ISO639_1 = RegexHelper.pattern("([a-z]{2})");
	private static final Pattern PATTERN_ISO639_2 = RegexHelper.pattern("([a-z]{2,3}(?:[-_\\/][a-z]{2,3})?)");

	private static final Handler COPY_OVER = new CopyOverHandler();
	private static final Handler COPY_OVER_AS_NUMBER = new CopyOverAsNumberHandler();
	private static final Handler COMPOUND_RULE = new CompoundRuleHandler();
	private static final Handler AFFIX = new AffixHandler();
	private static final Handler WORD_BREAK_TABLE = new WordBreakTableHandler();
	private static final Handler ALIASES = new AliasesHandler();
	private static final Handler REPLACEMENT_TABLE = new ConversionTableHandler(AffixOption.REPLACEMENT_TABLE);
	private static final Handler INPUT_CONVERSION_TABLE = new ConversionTableHandler(AffixOption.INPUT_CONVERSION_TABLE);
	private static final Handler OUTPUT_CONVERSION_TABLE = new ConversionTableHandler(AffixOption.OUTPUT_CONVERSION_TABLE);
	private static final Handler RELATION_TABLE = new RelationTableHandler(AffixOption.RELATION_TABLE);

	private static final Map<AffixOption, Handler> PARSING_HANDLERS = new EnumMap<>(AffixOption.class);
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
		PARSING_HANDLERS.put(AffixOption.RELATION_TABLE, RELATION_TABLE);
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
	 * @param affFile	The content of the affix file.
	 * @param configurationLanguage    The language implemented by the affix file.
	 * @throws IOException	If an I/O error occurs.
	 * @throws LinterException   If something is wrong while parsing the file (e.g. a missing rule).
	 */
	public final void parse(final File affFile, final String configurationLanguage) throws IOException{
		clear();

		int index = 0;
		boolean encodingRead = false;
		final Path affPath = affFile.toPath();
		final Charset charset = FileHelper.determineCharset(affPath);
		try(final Scanner scanner = FileHelper.createScanner(affPath, charset)){
			final ParsingContext context = new ParsingContext();
			final String prefix = AffixOption.CHARACTER_SET.getCode() + StringUtils.SPACE;
			while(scanner.hasNextLine()){
				final String line = scanner.nextLine();
				index ++;
				if(ParserHelper.isDictionaryComment(line))
					continue;

				if(!encodingRead && !line.startsWith(prefix))
					throw new LinterException(BAD_FIRST_LINE, line);
				encodingRead = true;

				context.update(line, index, scanner);
				final AffixOption ruleType = AffixOption.createFromCode(context.getRuleType());
				final Handler handler = lookupHandlerByRuleType(ruleType);
				if(handler != null){
					try{
						index += handler.parse(context, data);
					}
					catch(final RuntimeException e){
						throw new LinterException(GLOBAL_ERROR_MESSAGE, e.getMessage(), index);
					}
				}
			}
		}

		postProcessData(affFile);

		if(configurationLanguage != null && !configurationLanguage.equals(data.getLanguage()))
			data.setLanguage(configurationLanguage);

		data.close();

		data.verify();
	}

	private void postProcessData(final File affFile){
		postProcessCharset();
//		postProcessKey();
//		postProcessWordChars();
		postProcessLanguage(affFile);
		postProcessWordBreak();
		postProcessComplexPrefixes();
		postProcessCompoundMinimumLength();
	}

	private void postProcessCharset(){
		//apply default charset
		if(!data.containsData(AffixOption.CHARACTER_SET))
			data.addData(AffixOption.CHARACTER_SET, StandardCharsets.ISO_8859_1);
	}

	private void postProcessKey(){
//		if(!containsData(AffixOption.KEY))
//			data.addData(AffixOption.KEY, "qwertyuiop|asdfghjkl|zxcvbnm");
	}

	private void postProcessWordChars(){
//		if(!containsData(AffixOption.WORD_CHARS))
//			data.addData(AffixOption.WORD_BREAK_CHARACTERS, "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM");
	}

	private void postProcessLanguage(final File affFile){
		if(!data.containsData(AffixOption.LANGUAGE)){
			//try to infer language from filename
			final String filename = FilenameUtils.removeExtension(affFile.getName());
			String[] languages = RegexHelper.extract(filename, PATTERN_ISO639_2);
			if(languages.length == 0)
				languages = RegexHelper.extract(filename, PATTERN_ISO639_1);
			final String language = (languages.length > 0? languages[0]: NO_LANGUAGE);
			data.addData(AffixOption.LANGUAGE, language);
		}
	}

	private void postProcessWordBreak(){
		if(!data.containsData(AffixOption.WORD_BREAK_CHARACTERS)){
			final Set<String> wordBreakCharacters = new HashSet<>(3);
			wordBreakCharacters.add(HyphenationParser.MINUS_SIGN);
			wordBreakCharacters.add(START + HyphenationParser.MINUS_SIGN);
			wordBreakCharacters.add(HyphenationParser.MINUS_SIGN + END);
			data.addData(AffixOption.WORD_BREAK_CHARACTERS, wordBreakCharacters);
		}
	}

	private void postProcessComplexPrefixes(){
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
	}

	private void postProcessCompoundMinimumLength(){
		if(!data.containsData(AffixOption.COMPOUND_MINIMUM_LENGTH))
			data.addData(AffixOption.COMPOUND_MINIMUM_LENGTH, 3);
		else{
			final int compoundMin = data.getData(AffixOption.COMPOUND_MINIMUM_LENGTH);
			if(compoundMin < 1)
				data.addData(AffixOption.COMPOUND_MINIMUM_LENGTH, 1);
		}
	}

	private Handler lookupHandlerByRuleType(final AffixOption ruleType){
		return PARSING_HANDLERS.get(ruleType);
	}

	public final AffixData getAffixData(){
		return data;
	}

	public final String getLanguage(){
		return data.getLanguage();
	}

	public final void clear(){
		data.clear();
	}

}
