package unit731.hunlinter.actions;

import unit731.hunlinter.HelpDialog;
import unit731.hunlinter.gui.GUIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


public class AboutAction extends AbstractAction{

	private static final long serialVersionUID = 4363575204925273954L;


	public AboutAction(){
		super("system.about", new ImageIcon(AboutAction.class.getResource("/help_about.png")));
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIUtils.getParentFrame((JMenuItem)event.getSource());
		final HelpDialog dialog = new HelpDialog(parentFrame);
		GUIUtils.addCancelByEscapeKey(dialog);
		dialog.setLocationRelativeTo(parentFrame);
		dialog.setVisible(true);
	}

}
