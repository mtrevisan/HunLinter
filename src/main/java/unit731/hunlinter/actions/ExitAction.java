package unit731.hunlinter.actions;

import unit731.hunlinter.gui.GUIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


public class ExitAction extends AbstractAction{

	public ExitAction(){
		super("system.exit", new ImageIcon(ExitAction.class.getResource("/file_exit.png")));
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		final Frame parentFrame = GUIUtils.getParentFrame((JMenuItem)event.getSource());
		parentFrame.dispose();

		System.exit(0);
	}

}
