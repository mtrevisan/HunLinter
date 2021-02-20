/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.gui.renderers;

import unit731.hunlinter.gui.Watercolors;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import java.awt.Font;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.function.Function;


public class DictionarySortCellRenderer extends JLabel implements ListCellRenderer<String>{

	private static final long serialVersionUID = -6904206237491328151L;

	private static final Watercolors[] COLORS = Watercolors.values();
	private static final int COLORS_SIZE = COLORS.length;


	private final Function<Integer, Integer> boundaryIndex;
	private final Font font;


	public DictionarySortCellRenderer(final Function<Integer, Integer> boundaryIndex, final Font font){
		Objects.requireNonNull(boundaryIndex, "Boundary index cannot be null");
		Objects.requireNonNull(font, "Font cannot be null");

		this.boundaryIndex = boundaryIndex;
		this.font = font;
	}

	@Override
	public Component getListCellRendererComponent(final JList<? extends String> list, final String value, final int lineIndex,
			final boolean isSelected, final boolean cellHasFocus){
		final int index = boundaryIndex.apply(lineIndex);
		if(index >= 0){
			final Watercolors watercolor = COLORS[index % COLORS_SIZE];

			setOpaque(true);
			setBackground(watercolor.getColor());
		}
		else
			setOpaque(false);

		setText(value);
		setFont(font);

		return this;
	}


	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

}
