/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.actions;

import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.gui.dialogs.LanguageChooserDialog;
import io.github.mtrevisan.hunlinter.parsers.exceptions.WorkerException;
import io.github.mtrevisan.hunlinter.services.Packager;
import io.github.mtrevisan.hunlinter.services.downloader.DownloaderHelper;
import io.github.mtrevisan.hunlinter.workers.WorkerManager;
import io.github.mtrevisan.hunlinter.workers.exceptions.LanguageNotChosenException;
import io.github.mtrevisan.hunlinter.workers.exceptions.ProjectNotFoundException;
import org.xml.sax.SAXException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


public class ProjectLoaderAction extends AbstractAction{

	@Serial
	private static final long serialVersionUID = 2457698626251961045L;


	private final Path projectPath;
	private final Packager packager;
	private final WorkerManager workerManager;
	private final Consumer<Font> initialize;
	private final Runnable completed;
	private final Consumer<Exception> cancelled;
	private final PropertyChangeListener propertyChangeListener;


	public ProjectLoaderAction(final Path projectPath, final Packager packager, final WorkerManager workerManager,
			final Consumer<Font> initialize, final Runnable completed, final Consumer<Exception> cancelled,
			final PropertyChangeListener propertyChangeListener){
		super("project.load");

		Objects.requireNonNull(projectPath, "Project path cannot be null");
		Objects.requireNonNull(packager, "Packager cannot be null");
		Objects.requireNonNull(workerManager, "Worker manager cannot be null");
		Objects.requireNonNull(propertyChangeListener, "Property change listener cannot be null");

		this.projectPath = projectPath;
		this.packager = packager;
		this.workerManager = workerManager;
		this.initialize = initialize;
		this.completed = completed;
		this.cancelled = cancelled;
		this.propertyChangeListener = propertyChangeListener;
	}

	@Override
	public final void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		workerManager.createProjectLoaderWorker(worker -> {
			try{
				packager.reload(projectPath);

				final List<String> availableLanguages = packager.getLanguages();
				final AtomicReference<String> language = new AtomicReference<>(availableLanguages.get(0));
				if(availableLanguages.size() > 1){
					//choose between available languages
					final Consumer<String> onSelection = language::set;
					final LanguageChooserDialog dialog = new LanguageChooserDialog(availableLanguages, onSelection, parentFrame);
					GUIHelper.addCancelByEscapeKey(dialog);
					dialog.setLocationRelativeTo(parentFrame);
					dialog.setVisible(true);

					if(!dialog.isLanguageChosen())
						throw new LanguageNotChosenException("Language not chosen loading " + projectPath);
				}
				//load appropriate files based on current language
				packager.extractConfigurationFolders(language.get());

				parentFrame.setTitle(DownloaderHelper.APPLICATION_PROPERTIES.get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID) + " : " + packager.getLanguage());

				//choose one font (in case of reading errors)
				final String sampleText = packager.getSampleText();
				final Font temporaryFont = FontHelper.chooseBestFont(sampleText);
				if(initialize != null)
					initialize.accept(temporaryFont);

				worker.addPropertyChangeListener(propertyChangeListener);
				worker.execute();
			}
			catch(final IOException | SAXException | ProjectNotFoundException | LanguageNotChosenException e){
				worker.cancel();

				throw new WorkerException(e);
			}
		}, completed, cancelled);
	}


	@Override
	@SuppressWarnings("NewExceptionWithoutArguments")
	protected final Object clone() throws CloneNotSupportedException{
		throw new CloneNotSupportedException();
	}

	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

}
