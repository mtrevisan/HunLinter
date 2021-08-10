/**
 * Copyright (c) 2021 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.services.system.charsets;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.IllegalCharsetNameException;


public class ISO8859_14Charset extends Charset{

	/**
	 * Initializes a new charset with the given canonical name and alias
	 * set.
	 *
	 * @param canonicalName The canonical name of this charset
	 * @param aliases       An array of this charset's aliases, or null if it has no aliases
	 * @throws IllegalCharsetNameException If the canonical name or any of the aliases are illegal
	 */
	protected ISO8859_14Charset(final String canonicalName, final String[] aliases){
		super(canonicalName, aliases);
	}

	@Override
	public boolean contains(Charset cs){
		return false;
	}

	@Override
	public CharsetDecoder newDecoder(){
		return new ISO8859_14Decoder();
	}

	@Override
	public CharsetEncoder newEncoder(){
		return null;
	}

}
