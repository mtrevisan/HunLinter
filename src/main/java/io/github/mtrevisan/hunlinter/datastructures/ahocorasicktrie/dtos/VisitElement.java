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
package io.github.mtrevisan.hunlinter.datastructures.ahocorasicktrie.dtos;

import java.io.Serializable;
import java.util.Objects;


public class VisitElement<V extends Serializable>{

	private final int nodeId;
	private final String key;
	private final V value;


	public VisitElement(final int nodeId, final String key, final V value){
		this.nodeId = nodeId;
		this.value = value;
		this.key = key;
	}

	public int getNodeId(){
		return nodeId;
	}

	public String getKey(){
		return key;
	}

	public V getValue(){
		return value;
	}

	@Override
	public boolean equals(final Object obj){
		if(this == obj)
			return true;
		if(obj == null || getClass() != obj.getClass())
			return false;

		final VisitElement<?> rhs = (VisitElement<?>)obj;
		return (nodeId == rhs.nodeId
			&& key.equals(rhs.key)
			&& value.equals(rhs.value));
	}

	@Override
	public int hashCode(){
		return Objects.hash(nodeId, key, value);
	}

}
