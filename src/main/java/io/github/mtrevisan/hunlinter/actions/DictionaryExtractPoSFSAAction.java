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
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.WorkerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.Serial;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.FutureTask;


public class DictionaryExtractPoSFSAAction extends AbstractAction{

	@Serial
	private static final long serialVersionUID = -3884387265563163063L;


	private final ParserManager parserManager;
	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;

	private final FutureTask<JFileChooser> futureSaveResultFileChooser;


	public DictionaryExtractPoSFSAAction(final ParserManager parserManager, final WorkerManager workerManager,
			final PropertyChangeListener propertyChangeListener){
		super("dictionary.posFSA");

		Objects.requireNonNull(parserManager, "Parser manager cannot be null");
		Objects.requireNonNull(workerManager, "Worker manager cannot be null");
		Objects.requireNonNull(propertyChangeListener, "Property change listener cannot be null");

		this.parserManager = parserManager;
		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;

		futureSaveResultFileChooser = JavaHelper.createFuture(() -> {
			final JFileChooser saveResultFileChooser = new JFileChooser();
			saveResultFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			return saveResultFileChooser;
		});
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		workerManager.createPoSFSAWorker(
			() -> {
				final JFileChooser saveResultFileChooser = JavaHelper.waitForFuture(futureSaveResultFileChooser);
				final int fileChosen = saveResultFileChooser.showSaveDialog(parentFrame);
				return (fileChosen == JFileChooser.APPROVE_OPTION? Path.of(saveResultFileChooser.getSelectedFile().getAbsolutePath(),
					parserManager.getLanguage() + "-PoS.dict").toFile(): null);
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
