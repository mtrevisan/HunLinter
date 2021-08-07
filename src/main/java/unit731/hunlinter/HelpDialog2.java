/*
 * Created by JFormDesigner on Tue Feb 11 12:34:53 CET 2020
 */

package unit731.hunlinter;

import javax.swing.*;
import java.awt.*;


/**
 * @author unknown
 */
public class HelpDialog2 extends JDialog {
	public HelpDialog2(final Window owner) {
		super(owner);
		initComponents();
	}

	private void initComponents() {
		// JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
		// Generated using JFormDesigner Evaluation license - unknown
		productNameValue = new JLabel();
		productVersion = new JLabel();
		releaseDate = new JLabel();
		authorLabel = new JLabel();
		label1 = new JLabel();
		supportedOptionsLabel = new JLabel();
		managedOptionsScrollPane = new JScrollPane();
		managedOptionsTextArea = new JTextArea();
		copyright = new JLabel();

		//======== this ========
		final var contentPane = getContentPane();

		//---- productNameValue ----
		productNameValue.setText("…");

		//---- productVersion ----
		productVersion.setText("Product Version:");

		//---- releaseDate ----
		releaseDate.setText("Release Date:");

		//---- authorLabel ----
		authorLabel.setText("Author:");

		//---- label1 ----
		label1.setText("Home page:");

		//---- supportedOptionsLabel ----
		supportedOptionsLabel.setText("Supported options:");

		//======== managedOptionsScrollPane ========
		{

			//---- managedOptionsTextArea ----
			managedOptionsTextArea.setTabSize(3);
			managedOptionsTextArea.setWrapStyleWord(true);
			managedOptionsTextArea.setLineWrap(true);
			managedOptionsTextArea.setColumns(20);
			managedOptionsTextArea.setEditable(false);
			managedOptionsScrollPane.setViewportView(managedOptionsTextArea);
		}

		//---- copyright ----
		copyright.setText("…");
		copyright.setHorizontalAlignment(SwingConstants.RIGHT);

		final GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout.setHorizontalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup()
					.addContainerGap(438, Short.MAX_VALUE)
					.addComponent(copyright)
					.addContainerGap())
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addContainerGap()
					.addGroup(contentPaneLayout.createParallelGroup()
						.addComponent(managedOptionsScrollPane, GroupLayout.DEFAULT_SIZE, 447, Short.MAX_VALUE)
						.addGroup(contentPaneLayout.createSequentialGroup()
							.addGroup(contentPaneLayout.createParallelGroup()
								.addComponent(productNameValue, GroupLayout.PREFERRED_SIZE, 124, GroupLayout.PREFERRED_SIZE)
								.addComponent(productVersion)
								.addComponent(releaseDate)
								.addComponent(label1)
								.addComponent(supportedOptionsLabel)
								.addComponent(authorLabel))
							.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
		);
		contentPaneLayout.setVerticalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addContainerGap()
					.addComponent(productNameValue)
					.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
					.addComponent(productVersion)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(releaseDate)
					.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
					.addComponent(authorLabel)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(label1)
					.addGap(48, 48, 48)
					.addComponent(supportedOptionsLabel)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(managedOptionsScrollPane, GroupLayout.DEFAULT_SIZE, 98, Short.MAX_VALUE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(copyright)
					.addContainerGap())
		);
		pack();
		setLocationRelativeTo(getOwner());
		// JFormDesigner - End of component initialization  //GEN-END:initComponents
	}

	// JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - unknown
	private JLabel productNameValue;
	private JLabel productVersion;
	private JLabel releaseDate;
	private JLabel authorLabel;
	private JLabel label1;
	private JLabel supportedOptionsLabel;
	private JScrollPane managedOptionsScrollPane;
	private JTextArea managedOptionsTextArea;
	private JLabel copyright;
	// JFormDesigner - End of variables declaration  //GEN-END:variables
}
