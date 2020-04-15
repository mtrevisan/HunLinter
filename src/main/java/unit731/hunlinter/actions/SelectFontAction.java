package unit731.hunlinter.actions;

import unit731.hunlinter.FontChooserDialog;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.parsers.ParserManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.prefs.Preferences;


public class SelectFontAction extends AbstractAction{

	private final static String FONT_FAMILY_NAME_PREFIX = "font.familyName.";
	private final static String FONT_SIZE_PREFIX = "font.size.";


	private final ParserManager parserManager;
	private final Preferences preferences;


	public SelectFontAction(final ParserManager parserManager, final Preferences preferences){
		super("system.font", new ImageIcon(SelectFontAction.class.getResource("/file_font.png")));

		Objects.requireNonNull(parserManager);
		Objects.requireNonNull(preferences);

		this.parserManager = parserManager;
		this.preferences = preferences;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIUtils.getParentFrame((JMenuItem)event.getSource());
		Consumer<Font> onSelection = font -> {
			GUIUtils.setCurrentFont(font, parentFrame);

			final String language = parserManager.getLanguage();
			preferences.put(FONT_FAMILY_NAME_PREFIX + language, font.getFamily());
			preferences.put(FONT_SIZE_PREFIX + language, Integer.toString(font.getSize()));
		};
		FontChooserDialog dialog = new FontChooserDialog(parserManager.getAffixData(), GUIUtils.getCurrentFont(), onSelection,
			parentFrame);
		GUIUtils.addCancelByEscapeKey(dialog);
		dialog.setLocationRelativeTo(parentFrame);
		dialog.setVisible(true);
	}

}
