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
package io.github.mtrevisan.hunlinter.services.semanticversioning;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class VersionTest{

	@Test
	void shouldParseNormalVersion(){
		Version version = new Version("1.0.0");

		Assertions.assertEquals(new Version(1, 0, 0), version);
		Assertions.assertEquals("1.0.0", version.toString());
	}

	@Test
	void shouldRaiseErrorIfNumericIdentifierHasLeadingZeros(){
		Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
			() -> new Version("01.1.0"));

		Assertions.assertEquals("Numeric identifier MUST NOT contain leading zeros", exception.getMessage());
	}

	@Test
	void shouldParsePreReleaseVersion(){
		Version version = new Version("1.1.0-beta");

		Assertions.assertEquals(new Version(1, 1, 0, new String[]{"beta"}), version);
		Assertions.assertEquals("1.1.0-beta", version.toString());
	}

	@Test
	void shouldParsePreReleaseVersionAndBuild(){
		Version version = new Version("1.0.0-rc.2+build.05");

		Assertions.assertEquals(new Version(1, 0, 0, new String[]{"rc", "2"}, new String[]{"build", "05"}), version);
		Assertions.assertEquals("1.0.0-rc.2+build.05", version.toString());
	}

	@Test
	void shouldParseBuild(){
		Version version = new Version("1.2.3+build");

		Assertions.assertEquals(new Version(1, 2, 3, new String[0], new String[]{"build"}), version);
		Assertions.assertEquals("1.2.3+build", version.toString());
	}

	@Test
	void shouldRaiseErrorForIllegalInputString(){
		for(String illegal : new String[]{"", null}){
			Throwable exception = Assertions.assertThrows(IllegalArgumentException.class,
				() -> new Version(illegal));

			Assertions.assertEquals("Argument is not a valid version", exception.getMessage());
		}
	}

}
