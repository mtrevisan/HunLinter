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
package io.github.mtrevisan.hunlinter.actions;

import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.WorkerManager;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.MenuSelectionManager;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Future;


public class DictionaryExtractWordlistFSAAction extends AbstractAction{

	@Serial
	private static final long serialVersionUID = -471619630768881123L;


	private final ParserManager parserManager;
	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;

	private final Future<JFileChooser> futureSaveResultFileChooser;


	public DictionaryExtractWordlistFSAAction(final ParserManager parserManager, final WorkerManager workerManager,
			final PropertyChangeListener propertyChangeListener){
		super("dictionary.wordlistFSA");

		Objects.requireNonNull(parserManager, "Parser manager cannot be null");
		Objects.requireNonNull(workerManager, "Worker manager cannot be null");
		Objects.requireNonNull(propertyChangeListener, "Property change listener cannot be null");

		this.parserManager = parserManager;
		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;

		futureSaveResultFileChooser = JavaHelper.executeFuture(() -> {
			final JFileChooser saveResultFileChooser = new JFileChooser();
			saveResultFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			return saveResultFileChooser;
		});
	}

	@Override
	public final void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		workerManager.createWordlistFSAWorker(
			() -> {
				final JFileChooser saveResultFileChooser = JavaHelper.waitForFuture(futureSaveResultFileChooser);
				final int fileChosen = saveResultFileChooser.showSaveDialog(parentFrame);
				return (fileChosen == JFileChooser.APPROVE_OPTION? Path.of(saveResultFileChooser.getSelectedFile().getAbsolutePath(),
					parserManager.getLanguage() + "-wordlist.dict").toFile(): null);
			},
			worker -> {
				setEnabled(false);

				worker.addPropertyChangeListener(propertyChangeListener);
				worker.execute();
			},
			worker -> {
				//change color of progress bar to reflect an error
				if(worker.isCancelled())
					propertyChangeListener.propertyChange(worker.propertyChangeEventWorkerCancelled);

				setEnabled(true);
			}
		);
	}


	@Override
	@SuppressWarnings("NewExceptionWithoutArguments")
	protected final Object clone() throws CloneNotSupportedException{
		throw new CloneNotSupportedException();
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
