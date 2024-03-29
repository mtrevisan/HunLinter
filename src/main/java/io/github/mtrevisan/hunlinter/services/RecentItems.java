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
package io.github.mtrevisan.hunlinter.services;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;


/**
 * A simple data structure to store recent items (e.g. recent file in a menu or recent search text in a search dialog).
 */
public class RecentItems{

	public interface RecentItemsObserver{
		void onRecentItemChange(RecentItems src);
	}

	private static final String RECENT_ITEM_PREFIX = "recentItem.";

	private final int maxItems;
	private final Preferences preferenceNode;

	private final List<String> items;
	private final Collection<RecentItemsObserver> observers = new ArrayList<>(0);


	public RecentItems(final int maxItems, final Preferences preferenceNode){
		Objects.requireNonNull(preferenceNode, "Preference node cannot be null");

		this.maxItems = maxItems;
		this.preferenceNode = preferenceNode;
		items = new ArrayList<>(maxItems);

		loadFromPreferences();
	}

	public final List<String> getItems(){
		return items;
	}

	public final void push(final String item){
		items.remove(item);
		items.add(0, item);

		if(items.size() > maxItems)
			items.remove(items.size() - 1);

		update();
	}

	public final void remove(final String item){
		items.remove(item);

		update();
	}

	public final void clear(){
		items.clear();

		update();
	}

	public final String get(final int index){
		return items.get(index);
	}

	public final int indexOf(final String item){
		return items.indexOf(item);
	}

	public final int size(){
		return items.size();
	}

	public final void addObserver(final RecentItemsObserver observer){
		observers.add(observer);
	}

	public final void removeObserver(final RecentItemsObserver observer){
		observers.remove(observer);
	}

	private void update(){
		for(final RecentItemsObserver observer : observers)
			observer.onRecentItemChange(this);

		storeToPreferences();
	}

	private void loadFromPreferences(){
		for(int i = 0; i < maxItems; i ++){
			final String s = preferenceNode.get(RECENT_ITEM_PREFIX + i, StringUtils.EMPTY);
			if(!s.isEmpty())
				items.add(s);
		}
	}

	private void storeToPreferences(){
		final int size = items.size();
		for(int i = 0; i < maxItems; i ++){
			final String key = RECENT_ITEM_PREFIX + i;
			if(i < size)
				preferenceNode.put(key, items.get(i));
			else
				preferenceNode.remove(key);
		}
	}

}
