package unit731.hunspeller.gui.tags;

import javax.swing.*;
import java.awt.*;


public class JTagPanelExample extends JFrame{

	public JTagPanelExample(){
		setSize(new Dimension(500, 180));
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(layout);
		c.gridx = 0;
		c.gridy = 0;

		JPanel panel = new JTagPanel("type");
		add(panel, c);
		setVisible(true);
	}

	public static void main(String[] args){
		new JTagPanelExample();
	}

}
