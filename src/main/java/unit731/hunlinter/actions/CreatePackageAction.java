package unit731.hunlinter.actions;

import unit731.hunlinter.parsers.ParserManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;


public class CreatePackageAction extends AbstractAction{

	private final ParserManager parserManager;


	public CreatePackageAction(final ParserManager parserManager){
		super("menu.package", new ImageIcon(CreatePackageAction.class.getResource("/file_package.png")));

		Objects.requireNonNull(parserManager);

		this.parserManager = parserManager;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		parserManager.createPackage();
	}

}
