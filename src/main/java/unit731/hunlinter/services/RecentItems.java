package unit731.hunlinter.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.system.LoopHelper;


/**
 * A simple data structure to store recent items (e.g. recent file in a menu or recent search text in a search dialog).
 */
public class RecentItems{

	public interface RecentItemsObserver{
		void onRecentItemChange(RecentItems src);
	}

	private final static String RECENT_ITEM_PREFIX = "recentItem.";

	private final int maxItems;
	private final Preferences preferenceNode;

	private final List<String> items;
	private final List<RecentItemsObserver> observers = new ArrayList<>();


	public RecentItems(final int maxItems, final Preferences preferenceNode){
		Objects.requireNonNull(preferenceNode);

		this.maxItems = maxItems;
		this.preferenceNode = preferenceNode;
		items = new ArrayList<>(maxItems);

		loadFromPreferences();
	}

	public List<String> getItems(){
		return items;
	}

	public void push(final String item){
		items.remove(item);
		items.add(0, item);

		if(items.size() > maxItems)
			items.remove(items.size() - 1);

		update();
	}

	public void remove(final String item){
		items.remove(item);

		update();
	}

	public void clear(){
		items.clear();

		update();
	}

	public String get(final int index){
		return items.get(index);
	}

	public int indexOf(final String item){
		return items.indexOf(item);
	}

	public int size(){
		return items.size();
	}

	public void addObserver(final RecentItemsObserver observer){
		observers.add(observer);
	}

	public void removeObserver(final RecentItemsObserver observer){
		observers.remove(observer);
	}

	private void update(){
		LoopHelper.forEach(observers, observer -> observer.onRecentItemChange(this));

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
			if(i < size)
				preferenceNode.put(RECENT_ITEM_PREFIX + i, items.get(i));
			else
				preferenceNode.remove(RECENT_ITEM_PREFIX + i);
		}
	}

}
