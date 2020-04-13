package unit731.hunlinter.actions;

import unit731.hunlinter.services.system.FileHelper;

import javax.swing.*;
import java.awt.event.ActionEvent;


public class IssueReporterAction extends AbstractAction{

	private static final String URL_REPORT_ISSUE = "https://github.com/mtrevisan/HunLinter/issues";


	public IssueReporterAction(){
		super("system.issue", new ImageIcon(IssueReporterAction.class.getResource("/help_issue.png")));
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		FileHelper.browseURL(URL_REPORT_ISSUE);
	}

}
