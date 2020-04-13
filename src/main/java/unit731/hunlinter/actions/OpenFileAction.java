package unit731.hunlinter.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.services.system.FileHelper;
import unit731.hunlinter.services.Packager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;


public class OpenFileAction extends AbstractAction{

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenFileAction.class);


	private final Supplier<File> fileSupplier;
	private final String fileKey;
	private final Packager packager;


	public OpenFileAction(final Supplier<File> fileSupplier, final Packager packager){
		super("system.open");

		Objects.requireNonNull(fileSupplier);
		Objects.requireNonNull(packager);

		this.fileSupplier = fileSupplier;
		fileKey = null;
		this.packager = packager;
	}

	public OpenFileAction(final String fileKey, final Packager packager){
		super("system.open");

		Objects.requireNonNull(fileKey);
		Objects.requireNonNull(packager);

		fileSupplier = null;
		this.fileKey = fileKey;
		this.packager = packager;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		try{
			final File file = (fileSupplier != null? fileSupplier.get(): packager.getFile(fileKey));
			FileHelper.openFileWithChosenEditor(file);
		}
		catch(final IOException | InterruptedException e){
			LOGGER.warn("Exception while opening file", e);
		}
	}

}
