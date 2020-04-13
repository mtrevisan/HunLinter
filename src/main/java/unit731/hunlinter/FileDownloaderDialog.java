package unit731.hunlinter;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunlinter.services.system.FileHelper;
import unit731.hunlinter.services.semanticversioning.Version;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.services.downloader.DownloadListenerInterface;
import unit731.hunlinter.services.downloader.DownloadTask;
import unit731.hunlinter.services.downloader.DownloaderHelper;
import unit731.hunlinter.services.downloader.GITFileData;


public class FileDownloaderDialog extends JDialog implements PropertyChangeListener, DownloadListenerInterface{

	private final String localPath;
	private final GITFileData remoteObject;

	private DownloadTask task;


	public FileDownloaderDialog(final Frame parent) throws Exception{
		super(parent, "File downloader", true);

		initComponents();


		fileProgressBar.setValue(0);
		downloadButton.setEnabled(true);

		final List<Pair<Version, String>> newerVersions = DownloaderHelper.extractNewerVersions();
		remoteObject = DownloaderHelper.extractVersionData(newerVersions.get(0).getKey());

		//copy to default download folder
		localPath = System.getProperty("user.home") + "/Downloads/" + remoteObject.name;

		final Map<String, Object> pomProperties = DownloaderHelper.getApplicationProperties();
		currentVersionLabel.setText((String)pomProperties.get(DownloaderHelper.PROPERTY_KEY_VERSION));
		newVersionLabel.setText(remoteObject.version.toString());
		downloadSizeLabel.setText(StringHelper.byteCountToHumanReadable(remoteObject.size));
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      versionAvailableLabel = new javax.swing.JLabel();
      currentVersionPreLabel = new javax.swing.JLabel();
      currentVersionLabel = new javax.swing.JLabel();
      newVersionPreLabel = new javax.swing.JLabel();
      newVersionLabel = new javax.swing.JLabel();
      downloadSizePreLabel = new javax.swing.JLabel();
      downloadSizeLabel = new javax.swing.JLabel();
      statusLabel = new javax.swing.JLabel();
      fileProgressBar = new javax.swing.JProgressBar();
      whatsNewButton = new javax.swing.JButton();
      downloadButton = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      versionAvailableLabel.setText("A new version of " + DownloaderHelper.getApplicationProperties().get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID) + " is available.");

      currentVersionPreLabel.setText("Current version:");

      currentVersionLabel.setText("…");

      newVersionPreLabel.setText("New version:");

      newVersionLabel.setText("…");

      downloadSizePreLabel.setText("Total download size:");

      downloadSizeLabel.setText("…");

      statusLabel.setText(" ");

      whatsNewButton.setText("What's new");
      whatsNewButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            whatsNewButtonActionPerformed(evt);
         }
      });

      downloadButton.setText("Download");
      downloadButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            downloadButtonActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(fileProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                     .addComponent(statusLabel)
                     .addComponent(versionAvailableLabel)
                     .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                           .addComponent(currentVersionPreLabel)
                           .addComponent(newVersionPreLabel))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                           .addComponent(newVersionLabel)
                           .addComponent(currentVersionLabel)))
                     .addGroup(layout.createSequentialGroup()
                        .addComponent(downloadSizePreLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(downloadSizeLabel)))
                  .addGap(0, 0, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addGap(0, 0, Short.MAX_VALUE)
                  .addComponent(whatsNewButton)
                  .addGap(18, 18, 18)
                  .addComponent(downloadButton)
                  .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(versionAvailableLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(currentVersionPreLabel)
               .addComponent(currentVersionLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(newVersionLabel)
               .addComponent(newVersionPreLabel))
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(downloadSizePreLabel)
               .addComponent(downloadSizeLabel))
            .addGap(18, 18, 18)
            .addComponent(statusLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(fileProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(downloadButton)
               .addComponent(whatsNewButton))
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	private void whatsNewButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_whatsNewButtonActionPerformed
		try{
			final List<Pair<Version, String>> whatsNew = DownloaderHelper.extractNewerVersions();
			final String message = whatsNew.stream()
				.map(line -> "<b>Version " + line.getKey()
					+ Arrays.stream(StringUtils.split(line.getValue(), "\r\n"))
						.map(change -> "<li>" + StringUtils.removeStart(change, "- ") + "</li>")
						.collect(Collectors.joining(StringUtils.EMPTY, "</b><ul>", "</ul>"))
				)
				.collect(Collectors.joining("<br><br>"));

			//create a text area
			final JEditorPane textArea = new JEditorPane("text/html", "<html>" + message + "</html>");
			textArea.setEditable(false);
			//wrap a scrollpane around it
			final JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setPreferredSize(new Dimension(450, 150));
			textArea.setCaretPosition(0);

			JOptionPane.showMessageDialog(this, scrollPane, "What's new", JOptionPane.INFORMATION_MESSAGE);
		}
		catch(final Exception ignored){}
	}//GEN-LAST:event_whatsNewButtonActionPerformed

   private void downloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadButtonActionPerformed
		downloadButton.setEnabled(false);

		task = new DownloadTask(localPath, remoteObject, this);
		task.addPropertyChangeListener(this);
		task.execute();
   }//GEN-LAST:event_downloadButtonActionPerformed

	public void interrupt(){
		if(task != null && !task.isCancelled())
			task.cancelTask();

		stopped();
	}

	@Override
	public void startCheckUpdates(){
		statusLabel.setText("Check for updates…");
	}

	@Override
	public void startDownloads(final GITFileData fileData){
		statusLabel.setText("Begin downloading version " + fileData.version + "…");
	}

	@Override
	public void validatingFile(final GITFileData fileData, final String localPath){
		statusLabel.setText("Validating download…");

		try{
			final byte[] content = DownloaderHelper.readFileContent(localPath);
			DownloaderHelper.validate(content, remoteObject);
		}
		catch(final Exception e){
			statusLabel.setText(e.getMessage());
		}
	}

	@Override
	public void stopped(){
		statusLabel.setText("Update stopped");

		downloadButton.setEnabled(true);
	}

	@Override
	public void succeeded(){
		statusLabel.setText("File has been downloaded and verified successfully!");

		try{
			final Path fileToMove = Path.of(localPath);
			final String destinationFolder = FilenameUtils.getFullPath(
				HunLinterFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			final Path destinationPath = Path.of(
				(destinationFolder.startsWith("/")? destinationFolder.substring(1): destinationFolder),
				FilenameUtils.getBaseName(localPath) + "." + FilenameUtils.getExtension(localPath));
			FileHelper.moveFile(fileToMove, destinationPath);

			//exit current jar and start new one
			JavaHelper.closeAndStartAnotherApplication(destinationPath.toString());
		}
		catch(final Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void failed(final Exception e){
		statusLabel.setText("Error downloading file: " + e.getMessage());

		downloadButton.setEnabled(true);
	}

	@Override
	public void propertyChange(final PropertyChangeEvent evt){
		if(evt.getPropertyName().equals("progress"))
			fileProgressBar.setValue((Integer)evt.getNewValue());
	}

	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}


   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JLabel currentVersionLabel;
   private javax.swing.JLabel currentVersionPreLabel;
   private javax.swing.JButton downloadButton;
   private javax.swing.JLabel downloadSizeLabel;
   private javax.swing.JLabel downloadSizePreLabel;
   private javax.swing.JProgressBar fileProgressBar;
   private javax.swing.JLabel newVersionLabel;
   private javax.swing.JLabel newVersionPreLabel;
   private javax.swing.JLabel statusLabel;
   private javax.swing.JLabel versionAvailableLabel;
   private javax.swing.JButton whatsNewButton;
   // End of variables declaration//GEN-END:variables

}
