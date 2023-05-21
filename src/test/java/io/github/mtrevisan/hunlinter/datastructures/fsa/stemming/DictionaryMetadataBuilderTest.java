/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.datastructures.fsa.stemming;

import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.DictionaryAttribute;
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
