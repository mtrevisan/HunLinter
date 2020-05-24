package unit731.hunlinter.actions;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.gui.FontHelper;
import unit731.hunlinter.gui.dialogs.FontChooserDialog;
import unit731.hunlinter.gui.GUIHelper;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.services.Packager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.prefs.Preferences;


public class SelectFontAction extends AbstractAction{

	private static final long serialVersionUID = -4735745104118440213L;

	private static final String FONT_FAMILY_NAME_PREFIX = "font.familyName.";
	private static final String FONT_SIZE_PREFIX = "font.size.";


	private final Packager packager;
	private final ParserManager parserManager;
	private final Preferences preferences;


	public SelectFontAction(final Packager packager, final ParserManager parserManager, final Preferences preferences){
		super("system.font", new ImageIcon(SelectFontAction.class.getResource("/file_font.png")));

		Objects.requireNonNull(packager);
		Objects.requireNonNull(parserManager);
		Objects.requireNonNull(preferences);

		this.packager = packager;
		this.parserManager = parserManager;
		this.preferences = preferences;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Supplier<String> sampleExtractor = () -> {
			final AffixData affixData = parserManager.getAffixData();
			String sampleText = affixData.getSampleText();
			if(StringUtils.isBlank(sampleText))
				sampleText = packager.getSampleText();
			return sampleText;
		};
		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		final Consumer<Font> onSelection = font -> {
			FontHelper.setCurrentFont(font, parentFrame);

			final String language = parserManager.getLanguage();
			preferences.put(FONT_FAMILY_NAME_PREFIX + language, font.getFamily());
			preferences.put(FONT_SIZE_PREFIX + language, Integer.toString(font.getSize()));
		};
		final FontChooserDialog dialog = new FontChooserDialog(sampleExtractor, onSelection, parentFrame);
		GUIHelper.addCancelByEscapeKey(dialog);
		dialog.setLocationRelativeTo(parentFrame);
		dialog.setVisible(true);
	}

}
