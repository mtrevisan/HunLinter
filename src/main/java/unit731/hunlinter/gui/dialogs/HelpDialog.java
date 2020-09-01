/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.gui.dialogs;

import unit731.hunlinter.gui.GUIHelper;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.services.downloader.DownloaderHelper;
import unit731.hunlinter.services.system.FileHelper;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDate;


/**
 * @see <a href="https://pixabay.com/en/tree-kahl-winter-aesthetic-530324/">Tree logo</a>
 * @see <a href="http://blog.soebes.de/blog/2014/01/02/version-information-into-your-appas-with-maven/">Version informations into your apps with maven</a>
 */
public class HelpDialog extends JDialog{

	private static final long serialVersionUID = -9151942201399886892L;


	public HelpDialog(final Frame parent){
		super(parent, "About", true);

		initComponents();


		try{
			final BufferedImage img = ImageIO.read(HelpDialog.class.getResourceAsStream("/icon.png"));
			final Icon icon = new ImageIcon(img.getScaledInstance(logo.getHeight(), logo.getHeight(), Image.SCALE_SMOOTH));
			logo.setIcon(icon);
		}
		catch(final IOException ignored){}

		final String artifactID = (String)DownloaderHelper.APPLICATION_PROPERTIES.get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID);
		final String version = (String)DownloaderHelper.APPLICATION_PROPERTIES.get(DownloaderHelper.PROPERTY_KEY_VERSION);
		final LocalDate buildTimestamp = (LocalDate)DownloaderHelper.APPLICATION_PROPERTIES.get(DownloaderHelper.PROPERTY_KEY_BUILD_TIMESTAMP);

		productNameValue.setText(artifactID);
		productVersionValue.setText(version);
		releaseDateValue.setText(DictionaryParser.DATE_FORMATTER.format(buildTimestamp));
		managedOptionsTextArea.setText(
			"General:\n"
				+ "\tSET, FLAG, COMPLEXPREFIXES, LANG, AF, AM\n\n"
			+ "Suggestions:\n"
				+ "\tTRY (only read), NOSUGGEST (only read), REP, MAP (only read)\n\n"
			+ "Compounding:\n"
				+ "\tBREAK (only read), COMPOUNDRULE, COMPOUNDMIN, COMPOUNDFLAG, ONLYINCOMPOUND, COMPOUNDPERMITFLAG, COMPOUNDFORBIDFLAG, COMPOUNDMORESUFFIXES, COMPOUNDWORDMAX, CHECKCOMPOUNDDUP, CHECKCOMPOUNDREP, CHECKCOMPOUNDCASE, CHECKCOMPOUNDTRIPLE, SIMPLIFIEDTRIPLE, FORCEUCASE\n\n"
			+ "Affix creation:\n"
				+ "\tPFX, SFX\n\n"
			+ "Others:\n"
				+ "\tCIRCUMFIX, FORBIDDENWORD, FULLSTRIP, KEEPCASE, ICONV, OCONV, NEEDAFFIX");
		managedOptionsTextArea.setCaretPosition(0);
		copyright.setText("Copyright © 2019-" + DictionaryParser.YEAR_FORMATTER.format(LocalDate.now()) + " Mauro Trevisan");
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      productNameValue = new javax.swing.JLabel();
      productVersion = new javax.swing.JLabel();
      productVersionValue = new javax.swing.JLabel();
      releaseDate = new javax.swing.JLabel();
      releaseDateValue = new javax.swing.JLabel();
      copyright = new javax.swing.JLabel();
      authorLabel = new javax.swing.JLabel();
      authorLabelValue = new javax.swing.JLabel();
      homePageLabel = new javax.swing.JLabel();
      homePageLabelValue = new javax.swing.JLabel();
      logo = new javax.swing.JLabel();
      supportedOptionsLabel = new javax.swing.JLabel();
      managedOptionsScrollPane = new javax.swing.JScrollPane();
      managedOptionsTextArea = new javax.swing.JTextArea();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setResizable(false);

      productNameValue.setText("…");

      productVersion.setLabelFor(productVersionValue);
      productVersion.setText("Product Version:");

      productVersionValue.setText("…");

      releaseDate.setLabelFor(releaseDateValue);
      releaseDate.setText("Release Date:");

      releaseDateValue.setText("…");

      copyright.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
      copyright.setText("…");

      authorLabel.setText("Author:");

      authorLabelValue.setText("<html><a href=#>Mauro Trevisan</a></html>");
      authorLabelValue.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
      authorLabelValue.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            authorLabelValueMouseClicked(evt);
         }
      });

      homePageLabel.setText("Home page:");

      homePageLabelValue.setText("<html><a href=#>https://github.com/mtrevisan/HunLinter</a></html>");
      homePageLabelValue.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
      homePageLabelValue.addMouseListener(new java.awt.event.MouseAdapter() {
         public void mouseClicked(java.awt.event.MouseEvent evt) {
            homePageLabelValueMouseClicked(evt);
         }
      });

      logo.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

      supportedOptionsLabel.setText("Supported options:");

      managedOptionsScrollPane.setEnabled(false);

      managedOptionsTextArea.setEditable(false);
      managedOptionsTextArea.setColumns(20);
      managedOptionsTextArea.setFont(new java.awt.Font("Tahoma", 0, 11)); // NOI18N
      managedOptionsTextArea.setLineWrap(true);
      managedOptionsTextArea.setRows(1);
      managedOptionsTextArea.setTabSize(3);
      managedOptionsTextArea.setWrapStyleWord(true);
      managedOptionsScrollPane.setViewportView(managedOptionsTextArea);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
               .addComponent(managedOptionsScrollPane)
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(productNameValue, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(144, 144, 144))
                     .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                           .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                              .addGroup(layout.createSequentialGroup()
                                 .addComponent(releaseDate)
                                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                 .addComponent(releaseDateValue, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                              .addGroup(layout.createSequentialGroup()
                                 .addComponent(productVersion)
                                 .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                 .addComponent(productVersionValue, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)))
                           .addGroup(layout.createSequentialGroup()
                              .addComponent(authorLabel)
                              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                              .addComponent(authorLabelValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                           .addGroup(layout.createSequentialGroup()
                              .addComponent(homePageLabel)
                              .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                              .addComponent(homePageLabelValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE)))
                  .addComponent(logo, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addComponent(copyright, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                  .addComponent(supportedOptionsLabel)
                  .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(productNameValue)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(productVersion)
                     .addComponent(productVersionValue))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(releaseDate)
                     .addComponent(releaseDateValue))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(authorLabel)
                     .addComponent(authorLabelValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                     .addComponent(homePageLabel)
                     .addComponent(homePageLabelValue, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                  .addGap(30, 30, 30))
               .addComponent(logo, javax.swing.GroupLayout.PREFERRED_SIZE, 134, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(supportedOptionsLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(managedOptionsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(18, 18, 18)
            .addComponent(copyright)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	private void authorLabelValueMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_authorLabelValueMouseClicked
		FileHelper.sendEmail("mailto:851903%2Bmtrevisan@users.noreply.github.com?subject=HunLinter%20request");
	}//GEN-LAST:event_authorLabelValueMouseClicked

   private void homePageLabelValueMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_homePageLabelValueMouseClicked
		FileHelper.browseURL(GUIHelper.removeHTMLCode(homePageLabelValue.getText()));
   }//GEN-LAST:event_homePageLabelValueMouseClicked


   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLabel authorLabel;
   private javax.swing.JLabel authorLabelValue;
   private javax.swing.JLabel copyright;
   private javax.swing.JLabel homePageLabel;
   private javax.swing.JLabel homePageLabelValue;
   private javax.swing.JLabel logo;
   private javax.swing.JScrollPane managedOptionsScrollPane;
   private javax.swing.JTextArea managedOptionsTextArea;
   private javax.swing.JLabel productNameValue;
   private javax.swing.JLabel productVersion;
   private javax.swing.JLabel productVersionValue;
   private javax.swing.JLabel releaseDate;
   private javax.swing.JLabel releaseDateValue;
   private javax.swing.JLabel supportedOptionsLabel;
   // End of variables declaration//GEN-END:variables
}
