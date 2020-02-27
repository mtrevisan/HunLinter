package unit731.hunlinter.actions;

import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;


public class HyphenationLinterAction extends AbstractAction{

	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;


	public HyphenationLinterAction(final WorkerManager workerManager, final PropertyChangeListener propertyChangeListener){
		super("hyphenation.linter", new ImageIcon(HyphenationLinterAction.class.getResource("/dictionary_correctness.png")));

		putValue(MNEMONIC_KEY, 'c');
		putValue(SHORT_DESCRIPTION, "Check correctness");

		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		workerManager.createHyphenationLinterWorker(
			worker -> {
				setEnabled(false);

				worker.addPropertyChangeListener(propertyChangeListener);
				worker.execute();
			},
			worker -> setEnabled(true)
		);
	}

}
