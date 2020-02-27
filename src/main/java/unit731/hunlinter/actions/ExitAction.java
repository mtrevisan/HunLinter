package unit731.hunlinter.actions;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;


public class ExitAction extends AbstractAction{

	private final JFrame parentFrame;


	public ExitAction(final JFrame parentFrame){
		super("system.exit", new ImageIcon(ExitAction.class.getResource("/file_exit.png")));

		Objects.requireNonNull(parentFrame);

		this.parentFrame = parentFrame;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		parentFrame.dispose();

		System.exit(0);
	}

}
