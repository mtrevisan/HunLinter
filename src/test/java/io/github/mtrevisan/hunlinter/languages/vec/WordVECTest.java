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
package io.github.mtrevisan.hunlinter.languages.vec;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class WordVECTest{

	@Test
	void stressOnLast(){
		Assertions.assertEquals("pi", WordVEC.markDefaultStress("pi"));

		Assertions.assertEquals("pí", WordVEC.markDefaultStress("pí"));
	}

	@Test
	void stressOnPenultimate1(){
		Assertions.assertEquals("buxaraa", WordVEC.markDefaultStress("buxaraa"));
		Assertions.assertEquals("buxaraa", WordVEC.markDefaultStress("buxaràa"));
	}

	@Test
	void stressOnPenultimate2(){
		Assertions.assertEquals("fenio", WordVEC.markDefaultStress("fenio"));

		Assertions.assertEquals("fenío", WordVEC.markDefaultStress("fenïo"));
		Assertions.assertEquals("fenío", WordVEC.markDefaultStress("fenío"));
	}

	@Test
	void stressOnPenultimate3(){
		Assertions.assertEquals("frève", WordVEC.markDefaultStress("frève"));
	}

	@Test
	void stressOnAntepenultimate1(){
		Assertions.assertEquals("gràvïo", WordVEC.markDefaultStress("gràvïo"));
	}

	@Test
	void stressOnAntepenultimate2(){
		Assertions.assertEquals("ankúđen", WordVEC.markDefaultStress("ankúđen"));
	}

	@Test
	void stressOnAntepenultimateCompoundWord(){
		Assertions.assertEquals("gravï-io", WordVEC.markDefaultStress("gravï-io"));
		Assertions.assertEquals("gravï-io", WordVEC.markDefaultStress("gràvï-io"));
	}

}
