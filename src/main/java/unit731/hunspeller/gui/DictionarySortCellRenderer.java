package unit731.hunspeller.gui;

import java.awt.Component;
import java.util.function.Function;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.parsers.dictionary.dtos.Watercolors;


@AllArgsConstructor
@Slf4j
public class DictionarySortCellRenderer extends JLabel implements ListCellRenderer<String>{

	private static final long serialVersionUID = -6904206237491328151L;

	private static final Watercolors[] COLORS = Watercolors.values();
	private static final int COLORS_SIZE = COLORS.length;


	@NonNull
	private final Function<Integer, Integer> getBoundaryIndex;


	@Override
	public Component getListCellRendererComponent(JList<? extends String> list, String value, int lineIndex, boolean isSelected, boolean cellHasFocus){
		int boundaryIndex = getBoundaryIndex.apply(lineIndex);
		if(boundaryIndex >= 0){
			Watercolors watercolor = COLORS[boundaryIndex % COLORS_SIZE];

			setOpaque(true);
			setBackground(watercolor.getColor());
		}
		else
			setOpaque(false);

		setText(value);

		return this;
	}

}
