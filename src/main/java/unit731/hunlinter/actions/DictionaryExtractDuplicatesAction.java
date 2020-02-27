package unit731.hunlinter.actions;

import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;


public class DictionaryExtractDuplicatesAction extends AbstractAction{

	private final WorkerManager workerManager;
	private final JFrame parentFrame;
	private final PropertyChangeListener propertyChangeListener;

	private final JFileChooser saveResultFileChooser;


	public DictionaryExtractDuplicatesAction(final WorkerManager workerManager, final JFrame parentFrame, final PropertyChangeListener propertyChangeListener){
		super("dictionary.extractDuplicates", new ImageIcon(DictionaryExtractDuplicatesAction.class.getResource("/dictionary_duplicates.png")));

		putValue(MNEMONIC_KEY, 'd');
		putValue(SHORT_DESCRIPTION, "Extract duplicates…");

		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(parentFrame);
		Objects.requireNonNull(propertyChangeListener);

		this.workerManager = workerManager;
		this.parentFrame = parentFrame;
		this.propertyChangeListener = propertyChangeListener;

		saveResultFileChooser = new JFileChooser();
		saveResultFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
		saveResultFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		workerManager.createDuplicatesWorker(
			() -> {
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
