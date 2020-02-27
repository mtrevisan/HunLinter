package unit731.hunlinter.actions;

import unit731.hunlinter.services.FileHelper;

import javax.swing.*;
import java.awt.event.ActionEvent;


public class OnlineHelpAction extends AbstractAction{

	private static final String URL_ONLINE_HELP = "https://github.com/mtrevisan/HunLinter/blob/master/README.md";


	public OnlineHelpAction(){
		super("system.help", new ImageIcon(OnlineHelpAction.class.getResource("/help_help.png")));
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		FileHelper.browseURL(URL_ONLINE_HELP);
	}

}
