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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.Serial;
import java.util.Objects;
import java.util.prefs.Preferences;


public class ReportWarningsAction extends AbstractAction{

	@Serial
	private static final long serialVersionUID = -3894315443438154651L;

	public static final String REPORT_WARNINGS = "report.warnings";


	private final Preferences preferences;


	public ReportWarningsAction(final Preferences preferences){
		super("system.reportWarnings");

		Objects.requireNonNull(preferences);

		this.preferences = preferences;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		preferences.putBoolean(REPORT_WARNINGS, ((AbstractButton)event.getSource()).isSelected());
	}

}
