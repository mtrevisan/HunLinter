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

import io.github.mtrevisan.hunlinter.parsers.enums.AffixType;

import java.util.List;
import java.util.Objects;


public class RuleEntry{

	private static final char COMBINABLE = 'Y';
	private static final char NOT_COMBINABLE = 'N';


	private final AffixType type;
	/** ID used to represent the affix. */
	private final String flag;
	//cross product flag
	private final boolean combinable;
	private List<AffixEntry> entries;
//private final List<AffixEntry> prefixEntries;
//private final List<AffixEntry> suffixEntries;


	public RuleEntry(final AffixType type, final String flag, final char combinable){
		Objects.requireNonNull(type, "Type cannot be null");
		Objects.requireNonNull(flag, "Flag cannot be null");

		this.type = type;
		this.flag = flag;
		this.combinable = (combinable == COMBINABLE);
	}

	public final void setEntries(final List<AffixEntry> entries){
		this.entries = entries;

		final int size = (entries != null? entries.size(): 0);
		for(int i = 0; i < size; i ++)
			entries.get(i)
				.setParent(this);
	}

//public RuleEntry(boolean isSuffix, char combinable, List<AffixEntry> entries, List<AffixEntry> prefixEntries, List<AffixEntry> suffixEntries){
//	Objects.requireNonNull(combinable, "Combinable cannot be null");
//	Objects.requireNonNull(prefixEntries, "Prefix entries cannot be null");
//	Objects.requireNonNull(suffixEntries, "Suffix entries cannot be null");
//
//	this.isSuffix = isSuffix;
//	this.combinable = (combinable == YES);
//	this.entries = entries;
//	this.prefixEntries = prefixEntries;
//	this.suffixEntries = suffixEntries;
//}

	public final AffixType getType(){
		return type;
	}

	public final String getFlag(){
		return flag;
	}

	public final boolean isCombinable(){
		return combinable;
	}

	public final char combinableChar(){
		return (combinable? COMBINABLE: NOT_COMBINABLE);
	}

	public final List<AffixEntry> getEntries(){
		return entries;
	}

	public final boolean isProductiveFor(final String word){
		if(entries != null)
			for(int i = 0; i < entries.size(); i ++)
				if(entries.get(i).canApplyTo(word))
					return true;
		return false;
	}

	@Override
	public final boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final RuleEntry rhs = (RuleEntry)obj;
		return (type == rhs.type
			&& flag.equals(rhs.flag));
	}

	@Override
	public final int hashCode(){
		int result = type.hashCode();
		result = 31 * result + flag.hashCode();
		return result;
	}

}
