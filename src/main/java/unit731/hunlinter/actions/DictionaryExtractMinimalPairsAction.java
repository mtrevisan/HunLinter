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

import unit731.hunlinter.gui.GUIHelper;
import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;


public class DictionaryExtractMinimalPairsAction extends AbstractAction{

	private static final long serialVersionUID = -7015649948899909023L;


	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;

	private static final JFileChooser SAVE_RESULT_FILE_CHOOSER;
	static{
		SAVE_RESULT_FILE_CHOOSER = new JFileChooser();
		SAVE_RESULT_FILE_CHOOSER.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
		SAVE_RESULT_FILE_CHOOSER.setFileSelectionMode(JFileChooser.FILES_ONLY);
	}


	public DictionaryExtractMinimalPairsAction(final WorkerManager workerManager, final PropertyChangeListener propertyChangeListener){
		super("dictionary.extractMinimalPairs");

		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		workerManager.createMinimalPairsWorker(
			() -> {
				final int fileChosen = SAVE_RESULT_FILE_CHOOSER.showSaveDialog(parentFrame);
				return (fileChosen == JFileChooser.APPROVE_OPTION? SAVE_RESULT_FILE_CHOOSER.getSelectedFile(): null);
			},
			worker -> {
				setEnabled(false);

				worker.addPropertyChangeListener(propertyChangeListener);
				worker.execute();
			},
			worker -> setEnabled(true)
		);
	}

}
