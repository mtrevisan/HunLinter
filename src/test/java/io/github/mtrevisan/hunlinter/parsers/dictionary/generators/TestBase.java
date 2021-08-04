/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.parsers.dictionary.generators;

import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.parsers.affix.strategies.FlagParsingStrategy;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;


class TestBase{

	protected AffixData affixData;
	protected WordGenerator wordGenerator;


	protected void loadData(File affFile, String language) throws IOException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);
		affixData = affParser.getAffixData();
		wordGenerator = new WordGenerator(affixData, null, null);
	}

	protected void loadData(File affFile, File dicFile, String language) throws IOException{
		AffixParser affParser = new AffixParser();
		affParser.parse(affFile, language);
		affixData = affParser.getAffixData();
		Charset charset = affixData.getCharset();
		DictionaryParser dicParser = new DictionaryParser(dicFile, language, charset);
		wordGenerator = new WordGenerator(affixData, dicParser, null);
	}

	protected Inflection createInflection(String word, String continuationFlags, String morphologicalFields){
		FlagParsingStrategy strategy = affixData.getFlagParsingStrategy();
		return new Inflection(word, continuationFlags, morphologicalFields, null, strategy);
	}

}
