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
package io.github.mtrevisan.hunlinter.datastructures.fsa.builders;

import io.github.mtrevisan.hunlinter.datastructures.fsa.stemming.DictionaryMetadata;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;


/**
 * @see <a href="http://wiki.languagetool.org/hunspell-support">LanguageTool - Spell check</a>
 * @see <a href="http://wiki.languagetool.org/developing-a-tagger-dictionary">LanguageTool - Developing a tagger dictionary</a>
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public final class MetadataBuilder{

	private static final String EQUALS = "=";

	private MetadataBuilder(){}


	public static Path getMetadataPath(final File outputFile){
		return DictionaryMetadata.getExpectedMetadataLocation(outputFile.toPath());
	}

	public static DictionaryMetadata read(final Path output) throws IOException{
		final Path metadataPath = DictionaryMetadata.getExpectedMetadataLocation(output);
		try(final InputStream is = new BufferedInputStream(Files.newInputStream(metadataPath))){
			return DictionaryMetadata.read(is);
		}
	}

	//https://www.posteditacat.xyz/en/languagetool-plugin-as-smart-spell-checker/
	public static void createPOSInfo(final AffixData affixData, final String encoder, final Path outputPath, final Charset charset)
			throws IOException{
		final Collection<String> content = new ArrayList<>(Arrays.asList(
			DictionaryAttribute.CREATION_DATE.propertyName + EQUALS + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("u-MM-dd")),
			DictionaryAttribute.SEPARATOR.propertyName + EQUALS + Inflection.POS_FSA_SEPARATOR,
			DictionaryAttribute.ENCODING.propertyName + EQUALS + charset.name().toLowerCase(Locale.ROOT),
			DictionaryAttribute.ENCODER.propertyName + EQUALS + encoder,
			DictionaryAttribute.FREQUENCY_INCLUDED.propertyName + EQUALS + "false"));
		if(affixData.getLanguage() != null)
			content.add(DictionaryAttribute.LOCALE.propertyName + EQUALS + affixData.getLanguage());
		final String replacementPairs = affixData.getReplacementPairs();
		if(replacementPairs != null)
			content.add(DictionaryAttribute.REPLACEMENT_PAIRS.propertyName + EQUALS + replacementPairs);
		final String equivalentChars = affixData.getEquivalentChars();
		if(equivalentChars != null)
			content.add(DictionaryAttribute.EQUIVALENT_CHARS.propertyName + EQUALS + equivalentChars);
		final String inputConversions = affixData.getInputConversions();
		if(inputConversions != null)
			content.add(DictionaryAttribute.INPUT_CONVERSION.propertyName + EQUALS + inputConversions);

		FileHelper.saveFile(outputPath, StringUtils.CR, charset, content);
	}

	//https://www.posteditacat.xyz/en/languagetool-plugin-as-smart-spell-checker/
	public static void createWordlistInfo(final AffixData affixData, final String encoder, final Path outputPath, final Charset charset)
			throws IOException{
		final Collection<String> content = new ArrayList<>(Arrays.asList(
			DictionaryAttribute.CREATION_DATE.propertyName + EQUALS + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("u-MM-dd")),
			DictionaryAttribute.SEPARATOR.propertyName + EQUALS + Inflection.POS_FSA_SEPARATOR,
			DictionaryAttribute.ENCODING.propertyName + EQUALS + charset.name().toLowerCase(Locale.ROOT),
			DictionaryAttribute.ENCODER.propertyName + EQUALS + encoder,
			DictionaryAttribute.FREQUENCY_INCLUDED.propertyName + EQUALS + "false"));
		if(affixData.getLanguage() != null)
			content.add(DictionaryAttribute.LOCALE.propertyName + EQUALS + affixData.getLanguage());
		final String replacementPairs = affixData.getReplacementPairs();
		if(replacementPairs != null)
			content.add(DictionaryAttribute.REPLACEMENT_PAIRS.propertyName + EQUALS + replacementPairs);
		final String equivalentChars = affixData.getEquivalentChars();
		if(equivalentChars != null)
			content.add(DictionaryAttribute.EQUIVALENT_CHARS.propertyName + EQUALS + equivalentChars);
		final String inputConversions = affixData.getInputConversions();
		if(inputConversions != null)
			content.add(DictionaryAttribute.INPUT_CONVERSION.propertyName + EQUALS + inputConversions);

		FileHelper.saveFile(outputPath, StringUtils.CR, charset, content);
	}

}
