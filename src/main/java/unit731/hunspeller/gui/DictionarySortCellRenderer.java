package unit731.hunspeller.gui;

import java.awt.Component;
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


	public DictionarySortCellRenderer(Function<Integer, Integer> boundaryIndex){
		Objects.requireNonNull(boundaryIndex);

		this.boundaryIndex = boundaryIndex;
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends String> list, String value, int lineIndex, boolean isSelected,
			boolean cellHasFocus){
		int index = boundaryIndex.apply(lineIndex);
		if(index >= 0){
			Watercolors watercolor = COLORS[index % COLORS_SIZE];

			setOpaque(true);
			setBackground(watercolor.getColor());
		}
		else
			setOpaque(false);

		setText(value);

		return this;
	}

	private void writeObject(ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(DictionarySortCellRenderer.class.getName());
	}

	private void readObject(ObjectInputStream is) throws IOException{
		throw new NotSerializableException(DictionarySortCellRenderer.class.getName());
	}

}
