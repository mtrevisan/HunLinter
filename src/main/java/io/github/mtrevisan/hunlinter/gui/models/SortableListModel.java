/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.gui.models;

import javax.swing.*;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;


public class SortableListModel extends AbstractListModel<String>{

	@Serial
	private static final long serialVersionUID = -2766941679426379241L;


	private final ArrayList<String> delegate = new ArrayList<>();


	public void ensureCapacity(final int capacity){
		delegate.ensureCapacity(capacity);
	}

	/**
	 * Adds all of the elements present in the collection to the list.
	 *
	 * @param c	The collection which contains the elements to add
	 * @throws NullPointerException	If {@code c} is {@code null}
	 */
	public void addAll(final Collection<? extends String> c){
		if(!c.isEmpty()){
			final int startIndex = getSize();

			delegate.addAll(c);

			fireIntervalAdded(this, startIndex, getSize() - 1);
		}
	}

	@Override
	public String getElementAt(final int index){
		return delegate.get(index);
	}

	public void replaceAll(final Collection<? extends String> c, final int startIndex){
		if(!c.isEmpty()){
			final int size = getSize();
			if(startIndex >= size)
				addAll(c);
			else if(startIndex + c.size() <= size){
				int endIndex = startIndex;
				for(final String elem : c)
					delegate.set(endIndex ++, elem);

				fireIntervalAdded(this, startIndex, endIndex - 1);
			}
		}
	}

	@Override
	public int getSize(){
		return delegate.size();
	}

	public boolean isEmpty(){
		return delegate.isEmpty();
	}

	/**
	 * Removes all of the elements from this list.
	 * The list will be empty after this call returns (unless it throws an exception).
	 */
	public void clear(){
		final int index1 = delegate.size() - 1;
		delegate.clear();

		if(index1 >= 0)
			fireIntervalRemoved(this, 0, index1);
	}

	@Override
	public String toString(){
		return delegate.toString();
	}

}
