package unit731.hunlinter.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.Packager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Objects;


public class OpenFileAction extends AbstractAction{

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenFileAction.class);


	private final String fileKey;
	private final Packager packager;


	public OpenFileAction(final String fileKey, final Packager packager){
		super("system.open");

		Objects.requireNonNull(fileKey);
		Objects.requireNonNull(packager);

		this.fileKey = fileKey;
		this.packager = packager;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		try{
			FileHelper.openFileWithChosenEditor(packager.getFile(fileKey));
		}
		catch(final IOException | InterruptedException e){
			LOGGER.warn("Exception while opening sentence exceptions file", e);
		}
	}

}
