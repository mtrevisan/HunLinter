package unit731.hunlinter.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.DictionarySortDialog;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.workers.WorkerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Objects;


public class DictionarySorterAction extends AbstractAction{

	private static final long serialVersionUID = -3875908108517717837L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DictionarySorterAction.class);


	private final ParserManager parserManager;
	private final WorkerManager workerManager;
	private final PropertyChangeListener propertyChangeListener;


	public DictionarySorterAction(final ParserManager parserManager, final WorkerManager workerManager,
			final PropertyChangeListener propertyChangeListener){
		super("dictionary.sorter", new ImageIcon(DictionarySorterAction.class.getResource("/dictionary_sort.png")));

		Objects.requireNonNull(parserManager);
		Objects.requireNonNull(workerManager);
		Objects.requireNonNull(propertyChangeListener);

		this.parserManager = parserManager;
		this.workerManager = workerManager;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		try{
			final Frame parentFrame = GUIUtils.getParentFrame((JMenuItem)event.getSource());
			final DictionarySortDialog dialog = new DictionarySortDialog(parserManager.getDicParser(), parentFrame);

			dialog.loadLines(parserManager.getDictionaryLines(), 0);

			GUIUtils.addCancelByEscapeKey(dialog);
			dialog.setLocationRelativeTo(parentFrame);
			dialog.addListSelectionListener(evt -> {
				if(evt.getValueIsAdjusting())
					workerManager.createSorterWorker(
						() -> {
							final int selectedRow = dialog.getSelectedIndex();
							return (parserManager.getDicParser().isInBoundary(selectedRow)? selectedRow: null);
						},
						worker -> {
							dialog.setDictionaryEnabled(false);

							parserManager.stopFileListener();

							worker.addPropertyChangeListener(propertyChangeListener);
							worker.execute();
						},
						worker -> {
							parserManager.startFileListener();

							dialog.setDictionaryEnabled(true);
						}
					);
			});
			dialog.setVisible(true);
		}
		catch(final IOException e){
			LOGGER.error("Something very bad happened while sorting the dictionary", e);
		}
	}

}
