package unit731.hunspeller.gui;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import unit731.hunspeller.services.RecentItems;


/** A menu used to store and display recently used files. */
public class RecentFileMenu extends JMenu{

	private static final long serialVersionUID = 5949478291911784729L;


	private final RecentItems recentItems;
	private final Consumer<String> onSelectFile;


	public RecentFileMenu(RecentItems recentItems, Consumer<String> onSelectFile){
		super();

		Objects.requireNonNull(recentItems);
		Objects.requireNonNull(onSelectFile);

		this.recentItems = recentItems;
		this.onSelectFile = onSelectFile;

		addEntriesToMenu();
	}

	public void addEntry(String filePath){
		recentItems.push(filePath);

		addEntriesToMenu();
	}

	public void removeEntry(String filePath){
		recentItems.remove(filePath);

		addEntriesToMenu();
	}

	public boolean hasEntries(){
		return (recentItems.size() > 0);
	}

	public void clear(){
		recentItems.clear();
	}

	private void addEntriesToMenu(){
		//clear the existing items
		removeAll();

		List<String> items = recentItems.getItems();
		int i = 0;
		for(String item : items){
			JMenuItem newMenuItem = new JMenuItem(i + ": " + item);
			newMenuItem.setToolTipText(Integer.toString(i));
			newMenuItem.setActionCommand(item);
			newMenuItem.addActionListener(actionEvent -> {
				String path = actionEvent.getActionCommand();
				recentItems.push(path);

				addEntriesToMenu();

				onSelectFile.accept(path);
			});
			add(newMenuItem, i);

			i ++;
		}
	}

	private void writeObject(ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(RecentFileMenu.class.getName());
	}

	private void readObject(ObjectInputStream is) throws IOException{
		throw new NotSerializableException(RecentFileMenu.class.getName());
	}

}
