package unit731.hunlinter.actions;

import unit731.hunlinter.gui.GUIHelper;
import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;


public class DictionaryExtractDuplicatesAction extends AbstractAction{

	private static final long serialVersionUID = 30252862022089504L;


	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;

	private static final JFileChooser SAVE_RESULT_FILE_CHOOSER;
	static{
		SAVE_RESULT_FILE_CHOOSER = new JFileChooser();
		SAVE_RESULT_FILE_CHOOSER.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
		SAVE_RESULT_FILE_CHOOSER.setFileSelectionMode(JFileChooser.FILES_ONLY);
	}


	public DictionaryExtractDuplicatesAction(final WorkerManager workerManager, final PropertyChangeListener propertyChangeListener){
		super("dictionary.extractDuplicates",
			new ImageIcon(DictionaryExtractDuplicatesAction.class.getResource("/dictionary_duplicates.png")));

		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		workerManager.createDuplicatesWorker(
			() -> {
				final int fileChosen = SAVE_RESULT_FILE_CHOOSER.showSaveDialog(parentFrame);
				return (fileChosen == JFileChooser.APPROVE_OPTION? SAVE_RESULT_FILE_CHOOSER.getSelectedFile(): null);
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
