package unit731.hunlinter.actions;

import unit731.hunlinter.gui.GUIHelper;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.util.Objects;


public class DictionaryExtractPoSFSAAction extends AbstractAction{

	private static final long serialVersionUID = -3884387265563163063L;


	private final ParserManager parserManager;
	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;

	private final JFileChooser saveResultFileChooser;


	public DictionaryExtractPoSFSAAction(final ParserManager parserManager, final WorkerManager workerManager,
													 final PropertyChangeListener propertyChangeListener){
		super("dictionary.posFSA");

		Objects.requireNonNull(parserManager);
		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.parserManager = parserManager;
		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;

		saveResultFileChooser = new JFileChooser();
		saveResultFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		workerManager.createPoSFSAWorker(
			() -> {
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
