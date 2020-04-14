package unit731.hunlinter.services.fsa.builders;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.fsa.stemming.DictionaryMetadata;
import unit731.hunlinter.services.system.FileHelper;

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
import java.util.List;


/**
 * @see <a href="http://wiki.languagetool.org/hunspell-support>LanguageTool - Spell check</a>
 * @see <a href="http://wiki.languagetool.org/developing-a-tagger-dictionary">LanguageTool - Developing a tagger dictionary</a>
 */
public class MetadataBuilder{

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

	public static void create(final AffixData affixData, final String encoder, final Path outputPath, final Charset charset)
			throws IOException{
		final List<String> content = new ArrayList<>(Arrays.asList(
			"fsa.dict.created=" + ZonedDateTime.now().format(DateTimeFormatter.ofPattern("u-MM-dd")),
			"fsa.dict.separator=" + Inflection.POS_FSA_SEPARATOR,
			"fsa.dict.encoding=" + charset.name().toLowerCase(),
			"fsa.dict.encoder=" + encoder));
		if(affixData.getLanguage() != null)
			content.add("fsa.dict.speller.locale=" + affixData.getLanguage());
		final String replacementPairs = affixData.getReplacementPairs();
		if(replacementPairs != null)
			content.add("fsa.dict.speller.replacement-pairs=" + replacementPairs);
		final String equivalentChars = affixData.getEquivalentChars();
		if(equivalentChars != null)
			content.add("fsa.dict.speller.equivalent-chars=" + equivalentChars);
		final String inputConversions = affixData.getInputConversions();
		if(inputConversions != null)
			content.add("fsa.dict.input-conversion=" + inputConversions);

		FileHelper.saveFile(outputPath, StringUtils.CR, charset, content);
	}

}
