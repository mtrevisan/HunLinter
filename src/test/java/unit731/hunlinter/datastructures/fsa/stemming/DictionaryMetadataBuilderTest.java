package unit731.hunlinter.datastructures.fsa.stemming;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;


class DictionaryMetadataBuilderTest{

	@Test
	void allConstantsHaveBuilderMethods(){
		Set<DictionaryAttribute> keySet = new DictionaryMetadataBuilder()
			.convertCase()
			.encoding(Charset.defaultCharset())
			.frequencyIncluded()
			.ignoreAllUppercase()
			.ignoreCamelCase()
			.ignoreDiacritics()
			.ignoreNumbers()
			.ignorePunctuation()
			.separator('+')
			.supportRunOnWords()
			.encoder(EncoderType.SUFFIX)
			.withEquivalentChars(Collections.emptyMap())
			.withReplacementPairs(Collections.emptyMap())
			.withInputConversionPairs(Collections.emptyMap())
			.withOutputConversionPairs(Collections.emptyMap())
			.locale(Locale.getDefault())
			.license("")
			.author("")
			.creationDate("")
			.toMap()
			.keySet();

		Set<DictionaryAttribute> all = EnumSet.allOf(DictionaryAttribute.class);
		all.removeAll(keySet);

		Assertions.assertTrue(all.isEmpty());
	}

}
