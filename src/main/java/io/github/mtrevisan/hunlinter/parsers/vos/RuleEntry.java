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
import io.github.mtrevisan.hunlinter.services.system.LoopHelper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;


public class RuleEntry{

	private static final char COMBINABLE = 'Y';
	private static final char NOT_COMBINABLE = 'N';


	private final AffixType type;
	/** ID used to represent the affix. */
	private final String flag;
	//cross product flag
	private final boolean combinable;
	private AffixEntry[] entries;
//private final List<AffixEntry> prefixEntries;
//private final List<AffixEntry> suffixEntries;


	public RuleEntry(final AffixType type, final String flag, final char combinable){
		Objects.requireNonNull(type, "Type cannot be null");
		Objects.requireNonNull(flag, "Flag cannot be null");

		this.type = type;
		this.flag = flag;
		this.combinable = (combinable == COMBINABLE);
	}

	public void setEntries(final AffixEntry... entries){
		this.entries = entries;
		LoopHelper.forEach(entries, entry -> entry.setParent(this));
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

	public AffixType getType(){
		return type;
	}

	public String getFlag(){
		return flag;
	}

	public boolean isCombinable(){
		return combinable;
	}

	public char combinableChar(){
		return (combinable? COMBINABLE: NOT_COMBINABLE);
	}

	public AffixEntry[] getEntries(){
		return entries;
	}

	public boolean isProductiveFor(final String word){
		return (LoopHelper.match(entries, entry -> entry.canApplyTo(word)) != null);
	}


	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final RuleEntry rhs = (RuleEntry)obj;
		return new EqualsBuilder()
			.append(type, rhs.type)
			.append(flag, rhs.flag)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(type)
			.append(flag)
			.toHashCode();
	}

}
