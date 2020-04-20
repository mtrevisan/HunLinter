package unit731.hunlinter.actions;

import unit731.hunlinter.gui.GUIHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


public class ExitAction extends AbstractAction{

	private static final long serialVersionUID = -3856496810694201902L;


	public ExitAction(){
		super("system.exit", new ImageIcon(ExitAction.class.getResource("/file_exit.png")));
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		parentFrame.dispose();

		System.exit(0);
	}

}
