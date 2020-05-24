package unit731.hunlinter.actions;

import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;


public class DictionaryWordCountAction extends AbstractAction{

	private static final long serialVersionUID = 4648025847847837059L;


	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;


	public DictionaryWordCountAction(final WorkerManager workerManager, final PropertyChangeListener propertyChangeListener){
		super("dictionary.wordCount",
			new ImageIcon(DictionaryWordCountAction.class.getResource("/dictionary_count.png")));

		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		workerManager.createWordCountWorker(
			worker -> {
				setEnabled(false);

				worker.addPropertyChangeListener(propertyChangeListener);
				worker.execute();
			},
			worker -> setEnabled(true)
		);
	}

}
