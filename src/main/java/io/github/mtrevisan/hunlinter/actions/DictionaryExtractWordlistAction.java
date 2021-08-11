/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.actions;

import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.WorkerManager;
import io.github.mtrevisan.hunlinter.workers.dictionary.WordlistWorker;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.Serial;
import java.util.Objects;
import java.util.concurrent.FutureTask;


public class DictionaryExtractWordlistAction extends AbstractAction{

	@Serial
	private static final long serialVersionUID = 7870582176463311807L;


	private final WordlistWorker.WorkerType type;
	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;

	private final FutureTask<JFileChooser> futureSaveResultFileChooser;


	public DictionaryExtractWordlistAction(final WordlistWorker.WorkerType type, final WorkerManager workerManager,
			final PropertyChangeListener propertyChangeListener){
		super("dictionary.extractWordlist",
			new ImageIcon(DictionaryExtractWordlistAction.class.getResource("/dictionary_wordlist.png")));

		Objects.requireNonNull(type, "Type cannot be null");
		Objects.requireNonNull(workerManager, "Worker manager cannot be null");
		Objects.requireNonNull(propertyChangeListener, "Property change listener cannot be null");

		this.type = type;
		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;

		futureSaveResultFileChooser = JavaHelper.createFuture(() -> {
			final JFileChooser saveResultFileChooser = new JFileChooser();
			saveResultFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
			saveResultFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			return saveResultFileChooser;
		});
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		workerManager.createWordlistWorker(
			type,
			() -> {
				final JFileChooser saveResultFileChooser = JavaHelper.waitForFuture(futureSaveResultFileChooser);
				final int fileChosen = saveResultFileChooser.showSaveDialog(parentFrame);
				return (fileChosen == JFileChooser.APPROVE_OPTION? saveResultFileChooser.getSelectedFile(): null);
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
