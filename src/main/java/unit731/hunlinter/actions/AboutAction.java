package unit731.hunlinter.actions;

import unit731.hunlinter.HelpDialog;
import unit731.hunlinter.gui.GUIUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;


public class AboutAction extends AbstractAction{

	private final JFrame parentFrame;


	public AboutAction(final JFrame parentFrame){
		super("menu.about", new ImageIcon(AboutAction.class.getResource("/help_about.png")));

		Objects.requireNonNull(parentFrame);

		this.parentFrame = parentFrame;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final HelpDialog dialog = new HelpDialog(parentFrame);
		GUIUtils.addCancelByEscapeKey(dialog);
		dialog.setLocationRelativeTo(parentFrame);
		dialog.setVisible(true);
	}

}
