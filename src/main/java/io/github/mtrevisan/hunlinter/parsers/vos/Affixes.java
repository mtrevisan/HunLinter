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
package io.github.mtrevisan.hunlinter.parsers.vos;

import java.util.ArrayList;
import java.util.List;


public class Affixes{

	public static final int INDEX_PREFIXES = 0;
	public static final int INDEX_SUFFIXES = 1;
	public static final int INDEX_TERMINALS = 2;

	private final List<String> prefixes;
	private final List<String> suffixes;
	private final List<String> terminals;


	public Affixes(final List<String> prefixes, final List<String> suffixes, final List<String> terminals){
		this.prefixes = prefixes;
		this.suffixes = suffixes;
		this.terminals = terminals;
	}

	public final List<List<String>> extractAllAffixes(final boolean reverseAffixes){
		final List<List<String>> result = new ArrayList<>(3);
		result.add(reverseAffixes? suffixes: prefixes);
		result.add(reverseAffixes? prefixes: suffixes);
		result.add(terminals);
		return result;
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final Affixes rhs = (Affixes)obj;
		return (prefixes.equals(rhs.prefixes)
			&& suffixes.equals(rhs.suffixes)
			&& terminals.equals(rhs.terminals));
	}

	@Override
	public final int hashCode(){
		int result = (prefixes == null? 0: prefixes.hashCode());
		result = 31 * result + (suffixes == null? 0: suffixes.hashCode());
		result = 31 * result + (terminals == null? 0: terminals.hashCode());
		return result;
	}

}
