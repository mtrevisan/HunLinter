package unit731.hunlinter.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;


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


	public RecentItems(int maxItems, Preferences preferenceNode){
		Objects.requireNonNull(preferenceNode);

		this.maxItems = maxItems;
		this.preferenceNode = preferenceNode;
		items = new ArrayList<>(maxItems);

		loadFromPreferences();
	}

	public List<String> getItems(){
		return items;
	}

	public void push(String item){
		items.remove(item);
		items.add(0, item);

		if(items.size() > maxItems)
			items.remove(items.size() - 1);

		update();
	}

	public void remove(String item){
		items.remove(item);

		update();
	}

	public void clear(){
		items.clear();

		update();
	}

	public String get(int index){
		return items.get(index);
	}

	public int indexOf(String item){
		return items.indexOf(item);
	}

	public int size(){
		return items.size();
	}

	public void addObserver(RecentItemsObserver observer){
		observers.add(observer);
	}

	public void removeObserver(RecentItemsObserver observer){
		observers.remove(observer);
	}

	private void update(){
		observers.forEach(observer -> observer.onRecentItemChange(this));

		storeToPreferences();
	}

	private void loadFromPreferences(){
		IntStream.range(0, maxItems)
			.mapToObj(i -> preferenceNode.get(RECENT_ITEM_PREFIX + i, StringUtils.EMPTY))
			.takeWhile(Predicate.not(String::isEmpty))
			.forEach(items::add);
	}

	private void storeToPreferences(){
		int size = items.size();
		for(int i = 0; i < maxItems; i ++){
			if(i < size)
				preferenceNode.put(RECENT_ITEM_PREFIX + i, items.get(i));
			else
				preferenceNode.remove(RECENT_ITEM_PREFIX + i);
		}
	}

}