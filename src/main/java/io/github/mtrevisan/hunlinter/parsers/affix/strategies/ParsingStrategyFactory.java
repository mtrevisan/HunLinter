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
package io.github.mtrevisan.hunlinter.parsers.affix.strategies;

import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;

import java.util.HashMap;
import java.util.Map;


public final class ParsingStrategyFactory{

	private static final String UNKNOWN_TYPE = "Unknown strategy type: {}";


	private static final Map<String, FlagParsingStrategy> STRATEGIES = new HashMap<>(4);
	static{
		STRATEGIES.put(null, CharsetParsingStrategy.getASCIIInstance());
		STRATEGIES.put("UTF-8", CharsetParsingStrategy.getUTF8Instance());
		STRATEGIES.put("long", DoubleASCIIParsingStrategy.getInstance());
		STRATEGIES.put("num", NumericalParsingStrategy.getInstance());
	}


	private ParsingStrategyFactory(){}

	public static FlagParsingStrategy createFromFlag(final String flag){
		final FlagParsingStrategy strategy = STRATEGIES.get(flag);
		if(strategy == null)
			throw new LinterException(UNKNOWN_TYPE, flag);

		return strategy;
	}

	public static FlagParsingStrategy createASCIIParsingStrategy(){
		return CharsetParsingStrategy.getASCIIInstance();
	}

	public static FlagParsingStrategy createDoubleASCIIParsingStrategy(){
		return DoubleASCIIParsingStrategy.getInstance();
	}

	public static FlagParsingStrategy createNumericalParsingStrategy(){
		return NumericalParsingStrategy.getInstance();
	}

	public static FlagParsingStrategy createUTF8ParsingStrategy(){
		return CharsetParsingStrategy.getUTF8Instance();
	}

}
