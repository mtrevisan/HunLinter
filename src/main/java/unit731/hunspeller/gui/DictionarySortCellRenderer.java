package unit731.hunspeller.gui;

import java.awt.Component;
import java.io.IOException;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.parsers.affix.AffixParser;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;
import unit731.hunspeller.parsers.dictionary.Watercolors;


@AllArgsConstructor
@Slf4j
public class DictionarySortCellRenderer extends JLabel implements ListCellRenderer<String>{

	private static final long serialVersionUID = -6904206237491328151L;

	private static final Watercolors[] COLORS = Watercolors.values();
	private static final int COLORS_SIZE = COLORS.length;


	@NonNull
	private final AffixParser affParser;
	@NonNull
	private final DictionaryParser dicParser;


	@Override
	public Component getListCellRendererComponent(JList<? extends String> list, String value, int lineIndex, boolean isSelected, boolean cellHasFocus){
		try{
			dicParser.calculateDictionaryBoundaries(affParser);

			int boundaryIndex = dicParser.getBoundaryIndex(lineIndex);
			if(boundaryIndex >= 0){
				Watercolors watercolor = COLORS[boundaryIndex % COLORS_SIZE];

				setOpaque(true);
				setBackground(watercolor.getColor());
			}
			else
				setOpaque(false);

			setText(value);
		}
		catch(IOException e){
			log.error(null, e);
		}

		return this;
	}

}
