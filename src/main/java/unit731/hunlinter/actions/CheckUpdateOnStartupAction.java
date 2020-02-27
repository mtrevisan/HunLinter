package unit731.hunlinter.actions;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.prefs.Preferences;


public class CheckUpdateOnStartupAction extends AbstractAction{

	private final static String UPDATE_STARTUP_CHECK = "update.startupCheck";


	private final JCheckBoxMenuItem item;
	private final Preferences preferences;


	public CheckUpdateOnStartupAction(final JCheckBoxMenuItem item, final Preferences preferences){
		super("system.checkUpdateOnStartup");

		Objects.requireNonNull(item);
		Objects.requireNonNull(preferences);

		this.item = item;
		this.preferences = preferences;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		preferences.putBoolean(UPDATE_STARTUP_CHECK, item.isSelected());
	}

}
