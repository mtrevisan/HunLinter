/**
 * Copyright (c) 2019-2021 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.actions;

import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.gui.dialogs.FontChooserDialog;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.services.Packager;
import org.apache.commons.lang3.StringUtils;
import io.github.mtrevisan.hunlinter.gui.FontHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.prefs.Preferences;


public class SelectFontAction extends AbstractAction{

	@Serial
	private static final long serialVersionUID = -4735745104118440213L;

	private static final String FONT_FAMILY_NAME_PREFIX = "font.familyName.";
	private static final String FONT_SIZE_PREFIX = "font.size.";


	private final Packager packager;
	private final ParserManager parserManager;
	private final Preferences preferences;


	public SelectFontAction(final Packager packager, final ParserManager parserManager, final Preferences preferences){
		super("system.font", new ImageIcon(SelectFontAction.class.getResource("/file_font.png")));

		Objects.requireNonNull(packager, "Packager cannot be null");
		Objects.requireNonNull(parserManager, "Parser manager cannot be null");
		Objects.requireNonNull(preferences, "Preferences cannot be null");

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
