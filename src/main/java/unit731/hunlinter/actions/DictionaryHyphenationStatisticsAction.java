package unit731.hunlinter.actions;

import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;


public class DictionaryHyphenationStatisticsAction extends AbstractAction{

	private final boolean performHyphenationStatistics;
	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;


	public DictionaryHyphenationStatisticsAction(final boolean performHyphenationStatistics, final WorkerManager workerManager,
			final PropertyChangeListener propertyChangeListener){
		super("dictionary.statistics",
			new ImageIcon(DictionarySorterAction.class.getResource("/dictionary_statistics.png")));

		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.performHyphenationStatistics = performHyphenationStatistics;
		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		workerManager.createDictionaryStatistics(
			() -> performHyphenationStatistics,
			worker -> {
				setEnabled(false);

				worker.addPropertyChangeListener(propertyChangeListener);
				worker.execute();
			},
			worker -> setEnabled(true)
		);
	}

}
