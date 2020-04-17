package unit731.hunlinter.actions;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.prefs.Preferences;


public class CheckUpdateOnStartupAction extends AbstractAction{

	private static final long serialVersionUID = 2319044845856106299L;

	private final static String UPDATE_STARTUP_CHECK = "update.startupCheck";


	private final Preferences preferences;


	public CheckUpdateOnStartupAction(final Preferences preferences){
		super("system.checkUpdateOnStartup");

		Objects.requireNonNull(preferences);

		this.preferences = preferences;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		preferences.putBoolean(UPDATE_STARTUP_CHECK, ((JCheckBoxMenuItem)event.getSource()).isSelected());
	}

}
