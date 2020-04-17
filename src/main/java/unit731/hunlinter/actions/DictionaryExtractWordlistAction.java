package unit731.hunlinter.actions;

import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.dictionary.WordlistWorker;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;


public class DictionaryExtractWordlistAction extends AbstractAction{

	private static final long serialVersionUID = 7870582176463311807L;


	private final WordlistWorker.WorkerType type;
	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;

	private final JFileChooser saveResultFileChooser;


	public DictionaryExtractWordlistAction(final WordlistWorker.WorkerType type, final WorkerManager workerManager,
			final PropertyChangeListener propertyChangeListener){
		super("dictionary.extractWordlist",
			new ImageIcon(DictionaryExtractWordlistAction.class.getResource("/dictionary_wordlist.png")));

		Objects.requireNonNull(type);
		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.type = type;
		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;

		saveResultFileChooser = new JFileChooser();
		saveResultFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
		saveResultFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIUtils.getParentFrame((JMenuItem)event.getSource());
		workerManager.createWordlistWorker(
			type,
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
