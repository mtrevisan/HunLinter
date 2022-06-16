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
package io.github.mtrevisan.hunlinter.gui.components;

import io.github.mtrevisan.hunlinter.services.RecentItems;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;


/** A menu used to store and display recently used files. */
public class RecentFilesMenu extends JMenu{

	@Serial
	private static final long serialVersionUID = 5949478291911784729L;


	private final RecentItems recentItems;
	private final Consumer<Path> onSelectFile;


	public RecentFilesMenu(final RecentItems recentItems, final Consumer<Path> onSelectFile){
		Objects.requireNonNull(recentItems, "Recent items cannot be null");
		Objects.requireNonNull(onSelectFile, "On select file cannot be null");

		this.recentItems = recentItems;
		this.onSelectFile = onSelectFile;

		addEntriesToMenu();
	}

	public final void addEntry(final String filePath){
		recentItems.push(filePath);

		addEntriesToMenu();
	}

	public final void removeEntry(final String filePath){
		recentItems.remove(filePath);

		addEntriesToMenu();
	}

	public final boolean hasEntries(){
		return (recentItems.size() > 0);
	}

	public final void clear(){
		//clear the existing items
		removeAll();

		recentItems.clear();
	}

	private void addEntriesToMenu(){
		//clear the existing items
		removeAll();

		final ActionListener actionListener = actionEvent -> {
			final String path = actionEvent.getActionCommand();
			recentItems.push(path);

			addEntriesToMenu();

			onSelectFile.accept(Path.of(path));
		};
		final List<String> items = recentItems.getItems();
		int index = 0;
		for(final String item : items){
			final JMenuItem newMenuItem = new JMenuItem(item);
			newMenuItem.setActionCommand(item);
			newMenuItem.addActionListener(actionListener);
			add(newMenuItem, index ++);
		}
	}


	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

}
