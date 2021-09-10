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
package io.github.mtrevisan.hunlinter.parsers.enums;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


public enum InflectionTag{

	SINGULAR("singular", "s"),
	PLURAL("plural", "p"),
	MASCULINE("masculine", "m"),
	FEMENINE("femenine", "f"),
	SINGULAR_MASCULINE("singular+masculine", "s", "m"),
	SINGULAR_FEMENINE("singular+femenine", "s", "f"),
	PLURAL_MASCULINE("plural+masculine", "p", "m"),
	PLURAL_FEMENINE("plural+femenine", "p", "f"),

	FIRST_SINGULAR("first+singular", "1", "s"),
	FIRST_PLURAL("first+plural", "1", "p"),
	FIRST_SINGULAR_MASCULINE("first+singular+masculine", "1", "s", "m"),
	FIRST_PLURAL_MASCULINE("first+plural+masculine", "1", "p", "m"),
	FIRST_SINGULAR_FEMENINE("first+singular+femenine", "1", "s", "f"),
	FIRST_PLURAL_FEMENINE("first+plural+femenine", "1", "p", "f"),

	SECOND_SINGULAR("second+singular", "2", "s"),
	SECOND_PLURAL("second+plural", "2", "p"),
	SECOND_SINGULAR_MASCULINE("second+singular+masculine", "2", "s", "m"),
	SECOND_PLURAL_MASCULINE("second+plural+masculine", "2", "p", "m"),
	SECOND_SINGULAR_FEMENINE("second+singular+femenine", "2", "s", "f"),
	SECOND_PLURAL_FEMENINE("second+plural+femenine", "2", "p", "f"),

	THIRD("third", "3"),
	THIRD_SINGULAR("third+singular", "3", "s"),
	THIRD_PLURAL("third+plural", "3", "p"),
	THIRD_SINGULAR_MASCULINE("third+singular+masculine", "3", "s", "m"),
	THIRD_PLURAL_MASCULINE("third+plural+masculine", "3", "p", "m"),
	THIRD_SINGULAR_FEMENINE("third+singular+femenine", "3", "s", "f"),
	THIRD_PLURAL_FEMENINE("third+plural+femenine", "3", "p", "f"),

	ENUMERABLE("enumerable", "e"),
	NON_ENUMERABLE("non_enumerable", "ne"),
	PLURAL_NON_ENUMERABLE("plural+non_enumerable", "p", "ne"),
	MASCULINE_NON_ENUMERABLE("masculine+non_enumerable", "m", "ne"),
	FEMENINE_NON_ENUMERABLE("femenine+non_enumerable", "f", "ne"),
	SINGULAR_MASCULINE_NON_ENUMERABLE("singular+masculine+non_enumerable", "s", "m", "ne"),
	SINGULAR_FEMENINE_NON_ENUMERABLE("singular+femenine+non_enumerable", "s", "f", "ne"),

	PROCOMPLEMENTAR("procomplementar", "pc"),
	INTERROGATIVE("interrogative", "in"),

	INFANTIL("infantil", "inf"),
	SINGULAR_MASCULINE_INFANTIL("singular+masculine+infantil", "s", "m", "inf"),
	SINGULAR_FEMENINE_INFANTIL("singular+femenine+infantil", "s", "f", "inf"),
	MASCULINE_INFANTIL("masculine+infantil", "m", "inf"),
	FEMENINE_INFANTIL("femenine+infantil", "f", "inf"),
	MASCULINE_NON_ENUMERABLE_INFANTIL("masculine+non_enumerable+infantil", "m", "ne", "inf"),
	FEMENINE_NON_ENUMERABLE_INFANTIL("femenine+non_enumerable+infantil", "f", "ne", "inf"),
	SINGULAR_MASCULINE_NON_ENUMERABLE_INFANTIL("singular+masculine+non_enumerable+infantil", "s", "m", "ne", "inf"),
	SINGULAR_FEMENINE_NON_ENUMERABLE_INFANTIL("singular+femenine+non_enumerable+infantil", "s", "f", "ne", "inf"),

	VENETHIAN("veneŧian", "ven"),
	SINGULAR_VENETHIAN("singular+veneŧian", "s", "ven"),
	PLURAL_VENETHIAN("plural+veneŧian", "p", "ven"),
	SINGULAR_MASCULINE_VENETHIAN("singular+masculine+veneŧian", "s", "m", "ven"),
	PLURAL_MASCULINE_VENETHIAN("plural+masculine+veneŧian", "p", "m", "ven"),
	SINGULAR_FEMENINE_VENETHIAN("singular+femenine+veneŧian", "s", "f", "ven"),
	PLURAL_FEMENINE_VENETHIAN("plural+femenine+veneŧian", "p", "f", "ven"),

	PADOAN("padoan", "pad"),
	PLURAL_MASCULINE_PADOAN("plural+masculine+padoan", "p", "m", "pad"),
	PLURAL_FEMENINE_PADOAN("plural+femenine+padoan", "p", "f", "pad"),
	PLURAL_PADOAN("plural+padoan", "p", "pad"),

	NORTHERN("northern", "nor"),
	ARSEDEXE("arsedexe", "nor", "ars"),
	PLURAL_MASCULINE_NORTHERN("plural+masculine+northern", "p", "m", "nor"),
	PLURAL_FEMENINE_NORTHERN("plural+femenine+northern", "p", "f", "nor"),

	TALIAN("talian", "tal"),
	FIRST_SINGULAR_TALIAN("first+singular+talian", "1", "s", "tal"),
	SINGULAR_TALIAN("singular+talian", "s", "tal"),
	SINGULAR_MASCULINE_TALIAN("singular+masculine+talian", "s", "m", "tal"),
	SINGULAR_FEMENINE_TALIAN("singular+femenine+talian", "s", "f", "tal"),
	PLURAL_MASCULINE_TALIAN("plural+masculine+talian", "p", "m", "tal"),
	PLURAL_FEMENINE_TALIAN("plural+femenine+talian", "p", "f", "tal"),

	TOSKAN("toskan", "tos"),
	FIRST_SINGULAR_TOSKAN("first+singular+toskan", "1", "s", "tos"),
	SINGULAR_TOSKAN("singular+toskan", "s", "tos"),
	SINGULAR_MASCULINE_TOSKAN("singular+masculine+toskan", "s", "m", "tos"),
	SINGULAR_FEMENINE_TOSKAN("singular+femenine+toskan", "s", "f", "tos"),
	SINGULAR_FEMENINE_TOSKAN_INFANTIL("singular+femenine+toskan+infantil", "s", "f", "tos", "inf"),
	PLURAL_MASCULINE_TOSKAN("plural+masculine+toskan", "p", "m", "tos"),
	PLURAL_FEMENINE_TOSKAN("plural+femenine+toskan", "p", "f", "tos"),

	TODESKO("todesko", "tod"),
	FIRST_SINGULAR_TODESKO("first+singular+todesko", "1", "s", "tod"),
	SINGULAR_TODESKO("singular+todesko", "s", "tod"),
	SINGULAR_MASCULINE_TODESKO("singular+masculine+todesko", "s", "m", "tod"),
	SINGULAR_FEMENINE_TODESKO("singular+femenine+todesko", "s", "f", "tod"),
	SINGULAR_FEMENINE_TODESKO_INFANTIL("singular+femenine+todesko+infantil", "s", "f", "tod", "inf"),
	PLURAL_MASCULINE_TODESKO("plural+masculine+todesko", "p", "m", "tod"),
	PLURAL_FEMENINE_TODESKO("plural+femenine+todesko", "p", "f", "tod"),
	MASCULINE_TODESKO("masculine+todesko", "m", "tod"),
	MASCULINE_NON_ENUMERABLE_TODESKO("masculine+non_enumerable+todesko", "m", "ne", "tod"),

	FRANTHEXE("franŧexe", "tod"),
	FIRST_SINGULAR_FRANTHEXE("first+singular+franŧexe", "1", "s", "tod"),
	SINGULAR_FRANTHEXE("singular+franŧexe", "s", "tod"),
	SINGULAR_MASCULINE_FRANTHEXE("singular+masculine+franŧexe", "s", "m", "tod"),
	SINGULAR_FEMENINE_FRANTHEXE("singular+femenine+franŧexe", "s", "f", "tod"),
	SINGULAR_FEMENINE_FRANTHEXE_INFANTIL("singular+femenine+franŧexe+infantil", "s", "f", "tod", "inf"),
	PLURAL_MASCULINE_FRANTHEXE("plural+masculine+franŧexe", "p", "m", "tod"),
	PLURAL_FEMENINE_FRANTHEXE("plural+femenine+franŧexe", "p", "f", "tod"),
	MASCULINE_FRANTHEXE("masculine+franŧexe", "m", "tod"),
	MASCULINE_NON_ENUMERABLE_FRANTHEXE("masculine+non_enumerable+franŧexe", "m", "ne", "tod"),
	FEMENINE_NON_ENUMERABLE_FRANTHEXE("femenine+non_enumerable+franŧexe", "f", "ne", "tod"),

	LATIN("latin", "lat"),
	FIRST_SINGULAR_LATIN("first+singular+latin", "1", "s", "lat"),
	SINGULAR_LATIN("singular+latin", "s", "lat"),
	SINGULAR_MASCULINE_LATIN("singular+masculine+latin", "s", "m", "lat"),
	SINGULAR_FEMENINE_LATIN("singular+femenine+latin", "s", "f", "lat"),
	SINGULAR_FEMENINE_LATIN_INFANTIL("singular+femenine+franŧexe+latin", "s", "f", "lat", "inf"),
	PLURAL_MASCULINE_LATIN("plural+masculine+latin", "p", "m", "lat"),
	PLURAL_FEMENINE_LATIN("plural+femenine+latin", "p", "f", "lat"),
	MASCULINE_LATIN("masculine+latin", "m", "lat"),
	MASCULINE_NON_ENUMERABLE_LATIN("masculine+non_enumerable+latin", "m", "ne", "lat"),
	FEMENINE_NON_ENUMERABLE_LATIN("femenine+non_enumerable+latin", "f", "ne", "lat"),

	INDIAN("indian", "ind"),
	SINGULAR_MASCULINE_INDIAN("singular+masculine+indian", "s", "m", "ind"),
	SINGULAR_FEMENINE_INDIAN("singular+femenine+indian", "s", "f", "ind"),
	PLURAL_MASCULINE_INDIAN("plural+masculine+indian", "p", "m", "ind"),
	PLURAL_FEMENINE_INDIAN("plural+femenine+indian", "p", "f", "ind"),

	MEREGAN("meregan", "mer"),
	SINGULAR_MASCULINE_MEREGAN("singular+masculine+meregan", "s", "m", "mer"),
	SINGULAR_FEMENINE_MEREGAN("singular+femenine+meregan", "s", "f", "mer"),
	PLURAL_MASCULINE_MEREGAN("plural+masculine+meregan", "p", "m", "mer"),
	PLURAL_FEMENINE_MEREGAN("plural+femenine+meregan", "p", "f", "mer"),

	INGLEXE("inglexe", "ing"),
	MASCULINE_NON_ENUMERABLE_INGLEXE("masculine+non_enumerable+inglexe", "m", "ne", "ing"),
	FEMENINE_NON_ENUMERABLE_INGLEXE("femenine+non_enumerable+inglexe", "f", "ne", "ing"),
	SINGULAR_MASCULINE_INGLEXE("singular+masculine+inglexe", "s", "m", "ing"),
	SINGULAR_FEMENINE_INGLEXE("singular+femenine+inglexe", "s", "f", "ing"),
	PLURAL_MASCULINE_INGLEXE("plural+masculine+inglexe", "p", "m", "ing"),
	PLURAL_FEMENINE_INGLEXE("plural+femenine+inglexe", "p", "f", "ing"),

	XVEDEXE("xvedexe", "xve"),
	MASCULINE_NON_ENUMERABLE_XVEDEXE("masculine+non_enumerable+xvedexe", "m", "ne", "xve"),
	FEMENINE_NON_ENUMERABLE_XVEDEXE("femenine+non_enumerable+xvedexe", "f", "ne", "xve"),

	POLAKO("polako", "pol"),
	SINGULAR_MASCULINE_POLAKO("singular+masculine+polako", "s", "m", "pol"),
	SINGULAR_FEMENINE_POLAKO("singular+femenine+polako", "s", "f", "pol"),
	PLURAL_MASCULINE_POLAKO("plural+masculine+polako", "p", "m", "pol"),
	PLURAL_FEMENINE_POLAKO("plural+femenine+polako", "p", "f", "pol"),

	LONGOBARDO("longobardo", "lon"),
	SINGULAR_MASCULINE_LONGOBARDO("singular+masculine+longobardo", "s", "m", "lon"),
	SINGULAR_FEMENINE_LONGOBARDO("singular+femenine+longobardo", "s", "f", "lon"),
	PLURAL_MASCULINE_LONGOBARDO("plural+masculine+longobardo", "p", "m", "lon"),
	PLURAL_FEMENINE_LONGOBARDO("plural+femenine+longobardo", "p", "f", "lon"),

	LONBARDO("lonbardo", "lob"),
	FIRST_SINGULAR_LONBARDO("first+singular+lonbardo", "1", "s", "lob"),
	MASCULINE_NON_ENUMERABLE_LONBARDO("masculine+non_enumerable+lonbardo", "m", "ne", "lob"),

	BOLONHEXE("boloñexe", "bol"),
	SINGULAR_MASCULINE_BOLONHEXE("singular+masculine+boloñexe", "s", "m", "bol"),
	SINGULAR_FEMENINE_BOLONHEXE("singular+femenine+boloñexe", "s", "f", "bol"),
	PLURAL_MASCULINE_BOLONHEXE("plural+masculine+boloñexe", "p", "m", "bol"),
	PLURAL_FEMENINE_BOLONHEXE("plural+femenine+boloñexe", "p", "f", "bol"),

	XLOVEN("xlovèn", "xlo"),
	SINGULAR_MASCULINE_XLOVEN("singular+masculine+xlovèn", "s", "m", "xlo"),
	SINGULAR_FEMENINE_XLOVEN("singular+femenine+xlovèn", "s", "f", "xlo"),
	PLURAL_MASCULINE_XLOVEN("plural+masculine+xlovèn", "p", "m", "xlo"),
	PLURAL_FEMENINE_XLOVEN("plural+femenine+xlovèn", "p", "f", "xlo"),

	XLAVO("xlavo", "xla"),
	SINGULAR_MASCULINE_XLAVO("singular+masculine+xlavo", "s", "m", "xla"),
	SINGULAR_FEMENINE_XLAVO("singular+femenine+xlavo", "s", "f", "xla"),
	PLURAL_MASCULINE_XLAVO("plural+masculine+xlavo", "p", "m", "xla"),
	PLURAL_FEMENINE_XLAVO("plural+femenine+xlavo", "p", "f", "xla"),

	EBRAEGO("ebràego", "ebr"),

	GREGO("grègo", "gre"),
	SINGULAR_MASCULINE_GREGO("singular+masculine+grègo", "s", "m", "gre"),
	SINGULAR_FEMENINE_GREGO("singular+femenine+grègo", "s", "f", "gre"),
	MASCULINE_NON_ENUMERABLE_GREGO("masculine+non_enumerable+grègo", "m", "ne", "gre"),

	ARABO("àrabo", "ara"),
	SINGULAR_ARABO("singular+àrabo", "s", "ara"),
	PLURAL_ARABO("plural+àrabo", "p", "ara"),
	FIRST_SINGULAR_ARABO("first+singular+àrabo", "1", "s", "ara"),
	SINGULAR_MASCULINE_ARABO("singular+masculine+àrabo", "s", "m", "ara"),
	SINGULAR_FEMENINE_ARABO("singular+femenine+àrabo", "s", "f", "ara"),

	SPANHOL("spañòl", "spa"),
	SINGULAR_MASCULINE_SPANHOL("singular+masculine+spañòl", "s", "m", "spa"),
	SINGULAR_FEMENINE_SPANHOL("singular+femenine+spañòl", "s", "f", "spa"),

	NORDIC("nordic", "n");


	private static final Map<String, InflectionTag> VALUES;
	static{
		final EnumSet<InflectionTag> tags = EnumSet.allOf(InflectionTag.class);
		final Map<String, InflectionTag> map = new HashMap<>(tags.size());
		for(final InflectionTag tag : tags)
			map.put(MorphologicalTag.INFLECTIONAL_SUFFIX.getCode() + tag.code, tag);
		VALUES = Collections.unmodifiableMap(map);
	}

	private final String code;
	private final String[] tags;


	InflectionTag(final String code, final String... tags){
		this.code = code;
		this.tags = tags;
	}

	public static InflectionTag createFromCodeAndValue(final String codeAndValue){
		return VALUES.get(codeAndValue);
	}

	public String getCode(){
		return code;
	}

	public String[] getTags(){
		return tags;
	}

}
