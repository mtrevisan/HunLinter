package unit731.hunspeller.gui;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import unit731.hunspeller.services.RecentItems;


/** A menu used to store and display recently used files. */
public class RecentFilesMenu extends JMenu{

	private static final long serialVersionUID = 5949478291911784729L;


	private final RecentItems recentItems;
	private final Consumer<Path> onSelectFile;


	public RecentFilesMenu(final RecentItems recentItems, final Consumer<Path> onSelectFile){
		super();

		Objects.requireNonNull(recentItems);
		Objects.requireNonNull(onSelectFile);

		this.recentItems = recentItems;
		this.onSelectFile = onSelectFile;

		addEntriesToMenu();
	}

	public void addEntry(final String filePath){
		recentItems.push(filePath);

		addEntriesToMenu();
	}

	public void removeEntry(final String filePath){
		recentItems.remove(filePath);

		addEntriesToMenu();
	}

	public boolean hasEntries(){
		return (recentItems.size() > 0);
	}

	public void clear(){
		//clear the existing items
		removeAll();

		recentItems.clear();
	}

	private void addEntriesToMenu(){
		//clear the existing items
		removeAll();

		final List<String> items = recentItems.getItems();
		int i = 0;
		for(final String item : items){
			final JMenuItem newMenuItem = new JMenuItem(i + ": " + item);
			newMenuItem.setToolTipText(Integer.toString(i));
			newMenuItem.setActionCommand(item);
			newMenuItem.addActionListener(actionEvent -> {
				final String path = actionEvent.getActionCommand();
				recentItems.push(path);

				addEntriesToMenu();

				onSelectFile.accept(Path.of(path));
			});
			add(newMenuItem, i);

			i ++;
		}
	}

	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

}
