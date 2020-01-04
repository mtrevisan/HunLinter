package unit731.hunlinter.gui;

import java.awt.Component;
import java.awt.Font;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.function.Function;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;


public class DictionarySortCellRenderer extends JLabel implements ListCellRenderer<String>{

	private static final long serialVersionUID = -6904206237491328151L;

	private static final Watercolors[] COLORS = Watercolors.values();
	private static final int COLORS_SIZE = COLORS.length;


	private final Function<Integer, Integer> boundaryIndex;
	private final Font font;


	public DictionarySortCellRenderer(final Function<Integer, Integer> boundaryIndex, final Font font){
		Objects.requireNonNull(boundaryIndex);
		Objects.requireNonNull(font);

		this.boundaryIndex = boundaryIndex;
		this.font = font;
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends String> list, String value, int lineIndex,
			boolean isSelected, boolean cellHasFocus){
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
