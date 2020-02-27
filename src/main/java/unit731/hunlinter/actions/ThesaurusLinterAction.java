package unit731.hunlinter.actions;

import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;


public class ThesaurusLinterAction extends AbstractAction{

	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;


	public ThesaurusLinterAction(final WorkerManager workerManager, final PropertyChangeListener propertyChangeListener){
		super("thesaurus.linter", new ImageIcon(ThesaurusLinterAction.class.getResource("/dictionary_correctness.png")));

		putValue(MNEMONIC_KEY, 'c');
		putValue(SHORT_DESCRIPTION, "Check correctness");

		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		workerManager.createThesaurusLinterWorker(
			worker -> {
				setEnabled(false);

				worker.addPropertyChangeListener(propertyChangeListener);
				worker.execute();
			},
			worker -> setEnabled(true)
		);
	}

}
