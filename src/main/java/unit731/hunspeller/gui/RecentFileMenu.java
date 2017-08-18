package unit731.hunspeller.gui;

import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import unit731.hunspeller.services.RecentItems;


/** A menu used to store and display recently used files. */
public abstract class RecentFileMenu extends JMenu{

	private static final long serialVersionUID = 5949478291911784729L;


	private final RecentItems recentItems;


	public RecentFileMenu(RecentItems recentItems, String text, char mnemonic){
		super();

		setText(text);
		setMnemonic(mnemonic);

		this.recentItems = recentItems;

		addEntries();
	}

	private void addEntries(){
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

				addEntries();

				onSelectFile(path);
			});
			add(newMenuItem, i);

			i ++;
		}
	}

	/**
	 * Event that fires when a recent file is selected from the menu. Override this when implementing.
	 *
	 * @param filePath The file that was selected.
	 */
	public abstract void onSelectFile(String filePath);

}
