package unit731.hunspeller.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionListener;
import lombok.Getter;


public class DictionarySortDialog{

	private final JList<String> list;
	@Getter private final JDialog dialog;


	public DictionarySortDialog(String title, String message){
		Objects.nonNull(title);
		Objects.nonNull(message);

		list = new JList<>();
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		JLabel label = new JLabel(message);
		panel.add(label, BorderLayout.NORTH);
		JScrollPane scrollPane = new JScrollPane(list);
		scrollPane.setViewportBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
		scrollPane.setBackground(Color.WHITE);
		panel.add(scrollPane, BorderLayout.CENTER);

		JOptionPane optionPane = new JOptionPane(panel);
		optionPane.setOptions(new Object[]{});

		dialog = optionPane.createDialog(title);
	}

	public void show(){
		dialog.setVisible(true);
	}

	public void hide(){
		dialog.setVisible(false);
	}

	public void setCellRenderer(ListCellRenderer<String> renderer){
		list.setCellRenderer(renderer);
	}

	public void addListSelectionListener(ListSelectionListener listener){
		list.addListSelectionListener(listener);
	}

	public void setListData(String[] listData){
		list.setListData(listData);
	}

	public int getSelectedIndex(){
		return list.getSelectedIndex();
	}

}
