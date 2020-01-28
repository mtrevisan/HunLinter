package unit731.hunlinter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.services.downloader.DownloaderHelper;


/**
 * @see <a href="https://pixabay.com/en/tree-kahl-winter-aesthetic-530324/">Tree logo</a>
 * @see <a href="http://blog.soebes.de/blog/2014/01/02/version-information-into-your-appas-with-maven/">Version informations into your apps with maven</a>
 */
public class HelpDialog extends JDialog{

	private static final long serialVersionUID = -9151942201399886892L;


	public HelpDialog(final Frame parent){
		super(parent, "About", true);

		Objects.requireNonNull(parent);

		initComponents();


		try{
			final BufferedImage img = ImageIO.read(HelpDialog.class.getResourceAsStream("/icon.png"));
			final ImageIcon icon = new ImageIcon(img.getScaledInstance(lblLogo.getHeight(), lblLogo.getHeight(), Image.SCALE_SMOOTH));
			lblLogo.setIcon(icon);
		}
		catch(final IOException ignored){}

		final Map<String, Object> pomProperties = DownloaderHelper.getPOMProperties();
		final String artifactID = (String)pomProperties.get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID);
		final String version = (String)pomProperties.get(DownloaderHelper.PROPERTY_KEY_VERSION);
		final LocalDate buildTimestamp = (LocalDate)pomProperties.get(DownloaderHelper.PROPERTY_KEY_BUILD_TIMESTAMP);

		lblProductNameOut.setText(artifactID);
		lblProductVersionOut.setText(version);
		lblReleaseDateOut.setText(DictionaryParser.DATE_FORMATTER.format(buildTimestamp));
		lblManagedOptionsTextArea.setText(
			"General:\n"
				+ "\tSET, FLAG, COMPLEXPREFIXES, LANG, AF, AM\n\n"
			+ "Suggestions:\n"
				+ "\tREP\n\n"
			+ "Compounding:\n"
				+ "\tCOMPOUNDRULE, COMPOUNDMIN, COMPOUNDFLAG, ONLYINCOMPOUND, COMPOUNDPERMITFLAG, COMPOUNDFORBIDFLAG, COMPOUNDMORESUFFIXES, COMPOUNDWORDMAX, CHECKCOMPOUNDDUP, CHECKCOMPOUNDREP, CHECKCOMPOUNDCASE, CHECKCOMPOUNDTRIPLE, SIMPLIFIEDTRIPLE, FORCEUCASE\n\n"
			+ "Affix creation:\n"
				+ "\tPFX, SFX\n\n"
			+ "Others:\n"
				+ "\tCIRCUMFIX, FORBIDDENWORD, FULLSTRIP, KEEPCASE, ICONV, OCONV, NEEDAFFIX");
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
      lblSupportedOptionsLabel = new javax.swing.JLabel();
      jScrollPane1 = new javax.swing.JScrollPane();
      lblManagedOptionsTextArea = new javax.swing.JTextArea();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setResizable(false);

      lblProductVersion.setLabelFor(lblProductVersionOut);
      lblProductVersion.setText("Product Version:");

      lblProductVersionOut.setText("…");

      lblReleaseDate.setLabelFor(lblReleaseDateOut);
      lblReleaseDate.setText("Release Date:");

      lblReleaseDateOut.setText("…");

      lblCopyrightOut.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
      lblCopyrightOut.setText("…");

      lblProductNameOut.setText("…");

      lblLogo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

      lblSupportedOptionsLabel.setText("Supported options:");

      jScrollPane1.setEnabled(false);

      lblManagedOptionsTextArea.setEditable(false);
      lblManagedOptionsTextArea.setColumns(20);
      lblManagedOptionsTextArea.setFont(new java.awt.Font("Tahoma", Font.PLAIN, 11)); // NOI18N
      lblManagedOptionsTextArea.setLineWrap(true);
      lblManagedOptionsTextArea.setRows(1);
      lblManagedOptionsTextArea.setTabSize(3);
      lblManagedOptionsTextArea.setWrapStyleWord(true);
      jScrollPane1.setViewportView(lblManagedOptionsTextArea);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
               .addComponent(jScrollPane1)
               .addGroup(layout.createSequentialGroup()
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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 116, Short.MAX_VALUE)))
                  .addComponent(lblLogo, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addComponent(lblCopyrightOut, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                  .addComponent(lblSupportedOptionsLabel)
                  .addGap(0, 0, Short.MAX_VALUE)))
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
            .addComponent(lblSupportedOptionsLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(lblCopyrightOut)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents


   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JScrollPane jScrollPane1;
   private javax.swing.JLabel lblCopyrightOut;
   private javax.swing.JLabel lblLogo;
   private javax.swing.JTextArea lblManagedOptionsTextArea;
   private javax.swing.JLabel lblProductNameOut;
   private javax.swing.JLabel lblProductVersion;
   private javax.swing.JLabel lblProductVersionOut;
   private javax.swing.JLabel lblReleaseDate;
   private javax.swing.JLabel lblReleaseDateOut;
   private javax.swing.JLabel lblSupportedOptionsLabel;
   // End of variables declaration//GEN-END:variables
}
