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
package unit731.hunlinter.actions;

import unit731.hunlinter.gui.dialogs.DictionarySortDialog;
import unit731.hunlinter.gui.GUIHelper;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;


public class DictionarySorterAction extends AbstractAction{

	private static final long serialVersionUID = -3875908108517717837L;


	private final ParserManager parserManager;
	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;


	public DictionarySorterAction(final ParserManager parserManager, final WorkerManager workerManager,
			final PropertyChangeListener propertyChangeListener){
		super("dictionary.sorter", new ImageIcon(DictionarySorterAction.class.getResource("/dictionary_sort.png")));

		Objects.requireNonNull(parserManager);
		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.parserManager = parserManager;
		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		final DictionarySortDialog dialog = new DictionarySortDialog(parserManager, parentFrame);

		GUIHelper.addCancelByEscapeKey(dialog);
		dialog.setLocationRelativeTo(parentFrame);
		dialog.addListSelectionListener(evt -> {
			if(evt.getValueIsAdjusting())
				workerManager.createSorterWorker(
					() -> {
						final int selectedRow = dialog.getSelectedIndex();
						return (parserManager.getDicParser().isInBoundary(selectedRow)? selectedRow: null);
					},
					worker -> {
						dialog.setDictionaryEnabled(false);

						parserManager.stopFileListener();

						worker.addPropertyChangeListener(propertyChangeListener);
						worker.execute();
					},
					worker -> {
						parserManager.startFileListener();

						dialog.setDictionaryEnabled(true);
					}
				);
		});
		dialog.setVisible(true);
	}

}
