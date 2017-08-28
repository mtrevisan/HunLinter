package unit731.hunspeller;

import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.POMData;


/**
 * @see <a href="https://pixabay.com/en/tree-kahl-winter-aesthetic-530324/">Tree logo</a>
 */
@Slf4j
public class HelpDialog extends JDialog{

	private static final long serialVersionUID = -9151942201399886892L;


	public HelpDialog(Frame parent){
		super(parent, "About", true);

		Objects.nonNull(parent);

		initComponents();

		addCancelByEscapeKey();

		setLocationRelativeTo(parent);


		try{
			BufferedImage img = ImageIO.read(getClass().getResourceAsStream("/favicon.jpg"));
			ImageIcon icon = new ImageIcon(img.getScaledInstance(lblLogo.getHeight(), lblLogo.getHeight(), Image.SCALE_SMOOTH));
			lblLogo.setIcon(icon);
		}
		catch(IOException e){}

		String artifactID = POMData.getArtifactID();
		String version = POMData.getVersion();
		LocalDate buildTimestamp = POMData.getBuildTimestamp();

		lblProductNameOut.setText(artifactID);
		lblProductVersionOut.setText(version);
		lblReleaseDateOut.setText(buildTimestamp.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)));
		lblCopyrightOut.setText("Copyright (c) 2017 Mauro Trevisan");
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      lblProductVersion = new javax.swing.JLabel();
      lblProductVersionOut = new javax.swing.JLabel();
      lblReleaseDate = new javax.swing.JLabel();
      lblReleaseDateOut = new javax.swing.JLabel();
      lblCopyrightOut = new javax.swing.JLabel();
      lblProductNameOut = new javax.swing.JLabel();
      lblLogo = new javax.swing.JLabel();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      lblProductVersion.setLabelFor(lblProductVersionOut);
      lblProductVersion.setText("Product Version:");

      lblProductVersionOut.setText("...");

      lblReleaseDate.setLabelFor(lblReleaseDateOut);
      lblReleaseDate.setText("Release Date:");

      lblReleaseDateOut.setText("...");

      lblCopyrightOut.setText("...");

      lblProductNameOut.setText("...");

      lblLogo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(lblLogo, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lblReleaseDate)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(lblReleaseDateOut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addComponent(lblProductNameOut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lblProductVersion)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(lblProductVersionOut, javax.swing.GroupLayout.DEFAULT_SIZE, 151, Short.MAX_VALUE))
               .addComponent(lblCopyrightOut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lblProductNameOut)
                  .addGap(18, 18, 18)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(lblProductVersion)
                     .addComponent(lblProductVersionOut))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(lblReleaseDate)
                     .addComponent(lblReleaseDateOut))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(lblCopyrightOut))
               .addComponent(lblLogo, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	/** Force the escape key to call the same action as pressing the Cancel button. */
	private void addCancelByEscapeKey(){
		AbstractAction cancelAction = new AbstractAction(){
			private static final long serialVersionUID = -5644390861803492172L;

			@Override
			public void actionPerformed(ActionEvent e){
				dispose();
			}
		};
		KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		getRootPane().registerKeyboardAction(cancelAction, escapeKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	public static void main(String args[]){
		//<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
		try{
			String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex){
			log.error(null, ex);
		}
		//</editor-fold>

		java.awt.EventQueue.invokeLater(() -> {
			try{
				HelpDialog dialog = new HelpDialog(new javax.swing.JFrame());
				dialog.addWindowListener(new java.awt.event.WindowAdapter(){
					@Override
					public void windowClosing(java.awt.event.WindowEvent e){
						System.exit(0);
					}
				});
				dialog.setVisible(true);
			}
			catch(IllegalArgumentException ex){
				log.error(null, ex);
			}
		});
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLabel lblCopyrightOut;
   private javax.swing.JLabel lblLogo;
   private javax.swing.JLabel lblProductNameOut;
   private javax.swing.JLabel lblProductVersion;
   private javax.swing.JLabel lblProductVersionOut;
   private javax.swing.JLabel lblReleaseDate;
   private javax.swing.JLabel lblReleaseDateOut;
   // End of variables declaration//GEN-END:variables
}
