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
package io.github.mtrevisan.hunlinter.services.semanticversioning;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class VersionCompareTest{

	@Test
	void shouldReturnFalseIfOtherVersionIsNull(){
		Version v1 = new Version("2.3.7");
		Version v2 = null;

		Assertions.assertNotEquals(v1, v2);
	}

	@Test
	void preReleaseShouldHaveLowerPrecedenceThanAssociatedNormal(){
		Version v1 = new Version("1.3.7");
		Version v2 = new Version("1.3.7-alpha");

		Assertions.assertTrue(v1.compareTo(v2) > 0);
		Assertions.assertTrue(v2.compareTo(v1) < 0);
	}

	@Test
	void preRelease1(){
		Version v1 = new Version("2.3.7-alpha");
		Version v2 = new Version("2.3.7-beta");

		Assertions.assertTrue(v1.lessThan(v2));
	}

	@Test
	void preRelease2(){
		Version v1 = new Version("2.3.7-beta.1");
		Version v2 = new Version("2.3.7-beta.2");

		Assertions.assertTrue(v1.lessThan(v2));
	}

}
