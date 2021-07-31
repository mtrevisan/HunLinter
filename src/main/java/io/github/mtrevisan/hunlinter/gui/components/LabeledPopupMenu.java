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
	public void setLabel(final String text){
		if(label == null)
			return;

		final String oldValue = label.getText();
		label.setText(composeTitle(text));
		firePropertyChange("label", oldValue, label);
		if(accessibleContext != null)
			accessibleContext.firePropertyChange(AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY, oldValue, label);

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

