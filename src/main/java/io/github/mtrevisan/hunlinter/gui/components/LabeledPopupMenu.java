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
package io.github.mtrevisan.hunlinter.gui.components;

import org.apache.commons.text.StringEscapeUtils;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;


public class LabeledPopupMenu extends JPopupMenu{

	private final JLabel label;


	public LabeledPopupMenu(){
		label = null;
	}

	public LabeledPopupMenu(final String label){
		this.label = new JLabel(composeTitle(label));
		final Font font = this.label.getFont();
		this.label.setFont(font.deriveFont(font.getSize() + 1.f));
		this.label.setHorizontalAlignment(SwingConstants.CENTER);
		add(this.label);
		addSeparator();
	}

	@Override
	public void setLabel(final String label){
		if(this.label == null)
			return;

		final String oldValue = this.label.getText();
		this.label.setText(composeTitle(label));
		firePropertyChange("label", oldValue, this.label);
		if(accessibleContext != null)
			accessibleContext.firePropertyChange(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY, oldValue, this.label);

		invalidate();
		repaint();
	}

	private String composeTitle(final String text){
		return "<html><b>" + StringEscapeUtils.escapeHtml4(text) + "</b></html>";
	}

	@Override
	public String getLabel(){
		return label.getText();
	}

}

