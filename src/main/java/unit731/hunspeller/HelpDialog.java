package unit731.hunspeller;

import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.parsers.dictionary.DictionaryParser;


/**
 * @see <a href="https://pixabay.com/en/tree-kahl-winter-aesthetic-530324/">Tree logo</a>
 * @see <a href="http://blog.soebes.de/blog/2014/01/02/version-information-into-your-appas-with-maven/">Version informations into your apps with maven</a>
 */
@Slf4j
public class HelpDialog extends JDialog{

	private static final long serialVersionUID = -9151942201399886892L;


	public HelpDialog(Frame parent){
		super(parent, "About", true);

		Objects.requireNonNull(parent);

		initComponents();

		addCancelByEscapeKey();


		try{
			BufferedImage img = ImageIO.read(HelpDialog.class.getResourceAsStream("/favicon.jpg"));
			ImageIcon icon = new ImageIcon(img.getScaledInstance(lblLogo.getHeight(), lblLogo.getHeight(), Image.SCALE_SMOOTH));
			lblLogo.setIcon(icon);
		}
		catch(IOException e){}

		String artifactID = null;
		String version = null;
		LocalDate buildTimestamp = null;
		try(InputStream versionInfoStream = HelpDialog.class.getResourceAsStream("/version.properties")){
			Properties prop = new Properties();
			prop.load(versionInfoStream);

			artifactID = prop.getProperty("artifactId");
			version = prop.getProperty("version");
			buildTimestamp = LocalDate.parse(prop.getProperty("buildTimestamp"));
		}
		catch(IOException e){}

		lblProductNameOut.setText(artifactID);
		lblProductVersionOut.setText(version);
		lblReleaseDateOut.setText(DictionaryParser.DATE_FORMATTER.format(buildTimestamp));
		lblManagedOptionsTextArea.setText(
			"General:\n"
			+ "\tSET, FLAG, COMPLEXPREFIXES, LANG, AF, AM\n"
			+ "Compounding:\n"
			+ "\tCOMPOUNDRULE, COMPOUNDMIN, COMPOUNDFLAG, ONLYINCOMPOUND, CIRCUMFIX, CHECKCOMPOUNDTRIPLE, SIMPLIFIEDTRIPLE\n"
			+ "Affix creation:\n"
			+ "\tPFX, SFX\n"
			+ "Others:\n"
			+ "\tFULLSTRIP, KEEPCASE, NEEDAFFIX, ICONV, OCONV");
		lblManagedOptionsTextArea.setCaretPosition(0);
		lblCopyrightOut.setText("Copyright © " + DictionaryParser.YEAR_FORMATTER.format(LocalDate.now()) + " Mauro Trevisan");
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
      lblManagedOptionsLabel = new javax.swing.JLabel();
      jScrollPane1 = new javax.swing.JScrollPane();
      lblManagedOptionsTextArea = new javax.swing.JTextArea();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      lblProductVersion.setLabelFor(lblProductVersionOut);
      lblProductVersion.setText("Product Version:");

      lblProductVersionOut.setText("...");

      lblReleaseDate.setLabelFor(lblReleaseDateOut);
      lblReleaseDate.setText("Release Date:");

      lblReleaseDateOut.setText("...");

      lblCopyrightOut.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
      lblCopyrightOut.setText("...");

      lblProductNameOut.setText("...");

      lblLogo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

      lblManagedOptionsLabel.setText("Managed options:");

      jScrollPane1.setEnabled(false);

      lblManagedOptionsTextArea.setEditable(false);
      lblManagedOptionsTextArea.setColumns(20);
      lblManagedOptionsTextArea.setFont(new java.awt.Font("Tahoma", 0, 11)); // NOI18N
      lblManagedOptionsTextArea.setRows(5);
      lblManagedOptionsTextArea.setTabSize(3);
      jScrollPane1.setViewportView(lblManagedOptionsTextArea);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(lblProductNameOut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(144, 144, 144))
                     .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                           .addGroup(layout.createSequentialGroup()
                              .addComponent(lblReleaseDate)
                              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                              .addComponent(lblReleaseDateOut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                           .addGroup(layout.createSequentialGroup()
                              .addComponent(lblProductVersion)
                              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                              .addComponent(lblProductVersionOut, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                  .addComponent(lblLogo, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addComponent(lblCopyrightOut, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lblManagedOptionsLabel)
                  .addGap(0, 0, Short.MAX_VALUE))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGap(0, 0, Short.MAX_VALUE)
                  .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 412, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(lblProductNameOut)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(lblProductVersion)
                     .addComponent(lblProductVersionOut))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(lblReleaseDate)
                     .addComponent(lblReleaseDateOut))
                  .addGap(75, 75, 75))
               .addComponent(lblLogo, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(lblManagedOptionsLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 126, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(lblCopyrightOut)
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
		catch(ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e){
			log.error(null, e);
		}
		//</editor-fold>

		java.awt.EventQueue.invokeLater(() -> {
			try{
				javax.swing.JFrame parent = new javax.swing.JFrame();
				HelpDialog dialog = new HelpDialog(parent);
				dialog.setLocationRelativeTo(parent);
				dialog.addWindowListener(new java.awt.event.WindowAdapter(){
					@Override
					public void windowClosing(java.awt.event.WindowEvent e){
						System.exit(0);
					}
				});
				dialog.setVisible(true);
			}
			catch(IllegalArgumentException e){
				log.error(null, e);
			}
		});
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JScrollPane jScrollPane1;
   private javax.swing.JLabel lblCopyrightOut;
   private javax.swing.JLabel lblLogo;
   private javax.swing.JLabel lblManagedOptionsLabel;
   private javax.swing.JTextArea lblManagedOptionsTextArea;
   private javax.swing.JLabel lblProductNameOut;
   private javax.swing.JLabel lblProductVersion;
   private javax.swing.JLabel lblProductVersionOut;
   private javax.swing.JLabel lblReleaseDate;
   private javax.swing.JLabel lblReleaseDateOut;
   // End of variables declaration//GEN-END:variables
}
