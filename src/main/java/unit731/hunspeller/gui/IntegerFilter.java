package unit731.hunspeller.gui;

import org.apache.commons.lang3.StringUtils;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;


public class IntegerFilter extends DocumentFilter{

	@Override
	public void insertString(final FilterBypass fb, final int offset, final String text, final AttributeSet attr) throws BadLocationException{
		final Document doc = fb.getDocument();
		final StringBuffer sb = new StringBuffer();
		sb.append(doc.getText(0, doc.getLength()));
		sb.insert(offset, text);

		final String newText = sb.toString();
		if(validInput(newText))
			super.insertString(fb, offset, text, attr);
	}

	@Override
	public void replace(final FilterBypass fb, final int offset, final int length, final String text, final AttributeSet attrs) throws BadLocationException{
		final Document doc = fb.getDocument();
		final StringBuffer sb = new StringBuffer();
		sb.append(doc.getText(0, doc.getLength()));
		sb.replace(offset, offset + length, text);

		final String newText = sb.toString();
		if(validInput(newText))
			super.replace(fb, offset, length, text, attrs);
	}

	@Override
	public void remove(final FilterBypass fb, final int offset, final int length) throws BadLocationException{
		final Document doc = fb.getDocument();
		final StringBuffer sb = new StringBuffer();
		sb.append(doc.getText(0, doc.getLength()));
		sb.delete(offset, offset + length);

		final String newText = sb.toString();
		if(validInput(newText))
			super.remove(fb, offset, length);
	}

	private boolean validInput(final String text){
		try{
			if(StringUtils.isNotBlank(text))
				Integer.parseInt(text);
			return true;
		}
		catch(final NumberFormatException e){
			return false;
		}
	}

}
