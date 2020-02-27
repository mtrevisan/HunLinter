package unit731.hunlinter.actions;

import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.util.Objects;


public class DictionaryExtractPosFSAAction extends AbstractAction{

	private final ParserManager parserManager;
	private final WorkerManager workerManager;
	private final JFrame parentFrame;
	private final PropertyChangeListener propertyChangeListener;

	private final JFileChooser saveResultFileChooser;


	public DictionaryExtractPosFSAAction(final ParserManager parserManager, final WorkerManager workerManager, final JFrame parentFrame,
			final PropertyChangeListener propertyChangeListener){
		super("dictionary.posFSA");

		putValue(SHORT_DESCRIPTION, "Extract PoS FSA…");

		Objects.requireNonNull(parserManager);
		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(parentFrame);
		Objects.requireNonNull(propertyChangeListener);

		this.parserManager = parserManager;
		this.workerManager = workerManager;
		this.parentFrame = parentFrame;
		this.propertyChangeListener = propertyChangeListener;

		saveResultFileChooser = new JFileChooser();
		saveResultFileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
		saveResultFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		workerManager.createPoSFSAWorker(
			() -> {
				final int fileChosen = saveResultFileChooser.showSaveDialog(parentFrame);
				return (fileChosen == JFileChooser.APPROVE_OPTION? Path.of(saveResultFileChooser.getSelectedFile().getAbsolutePath(),
					parserManager.getAffixData().getLanguage() + ".txt").toFile(): null);
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
