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

import io.github.mtrevisan.hunlinter.datastructures.fsa.lookup.DictionaryLookup;
import io.github.mtrevisan.hunlinter.gui.panes.ThesaurusLayeredPane;
import io.github.mtrevisan.hunlinter.workers.WorkerManager;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.MenuSelectionManager;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Objects;


public class ThesaurusLinterFSAAction extends AbstractAction{

	@Serial
	private static final long serialVersionUID = 2607687961029515520L;

	private final WorkerManager workerManager;
	private final ThesaurusLayeredPane theLayeredPane;
	private final PropertyChangeListener propertyChangeListener;


	@SuppressWarnings("ConstantConditions")
	public ThesaurusLinterFSAAction(final WorkerManager workerManager, final ThesaurusLayeredPane theLayeredPane,
			final PropertyChangeListener propertyChangeListener){
		super("thesaurus.linter.fsa",
			new ImageIcon(ThesaurusLinterFSAAction.class.getResource("/dictionary_correctness.png")));

		Objects.requireNonNull(workerManager, "Worker manager cannot be null");
		Objects.requireNonNull(theLayeredPane, "Thesaurus layered pane cannot be null");
		Objects.requireNonNull(propertyChangeListener, "Property change listener cannot be null");

		this.workerManager = workerManager;
		this.theLayeredPane = theLayeredPane;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public final void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		theLayeredPane.openDictionaryFSAActionPerformed(null);
		final DictionaryLookup dictionaryLookup = theLayeredPane.getDictionaryLookup();

		if(dictionaryLookup != null)
			workerManager.createThesaurusLinterFSAWorker(
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
				},
				dictionaryLookup
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
