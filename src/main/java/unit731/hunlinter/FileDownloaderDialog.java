package unit731.hunlinter;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileSystemException;
import java.util.Map;
import javax.swing.*;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.StringHelper;
import unit731.hunlinter.services.downloader.DownloadListenerInterface;
import unit731.hunlinter.services.downloader.DownloadTask;
import unit731.hunlinter.services.downloader.DownloaderHelper;


public class FileDownloaderDialog extends JDialog implements PropertyChangeListener, DownloadListenerInterface{

	private JSONObject remoteObject;
	private String localPath;

	private String remoteURL;


	public FileDownloaderDialog(final String repositoryURL, final Frame parent) throws Exception{
		super(parent, "File downloader", true);

		initComponents();


		fileProgressBar.setValue(0);
		downloadButton.setEnabled(true);

		remoteObject = DownloaderHelper.extractLastVersion(repositoryURL);
		remoteURL = (String)remoteObject.getOrDefault("download_url", null);
		final String filename = (String)remoteObject.getOrDefault("name", null);
		localPath = System.getProperty("user.home") + "/Downloads/" + filename;

		final Map<String, Object> pomProperties = DownloaderHelper.getPOMProperties();
		currentVersionLabel.setText((String)pomProperties.get(DownloaderHelper.PROPERTY_KEY_VERSION));
		newVersionLabel.setText(DownloaderHelper.extractVersion(filename));
		downloadSizeLabel.setText(StringHelper.byteCountToHumanReadable((Long)remoteObject.getOrDefault("size", null)));
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
      downloadButton = new javax.swing.JButton();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      versionAvailableLabel.setText("A new version of " + DownloaderHelper.getPOMProperties().get(DownloaderHelper.PROPERTY_KEY_ARTIFACT_ID) + " is available.");

      currentVersionPreLabel.setText("Current version:");

      currentVersionLabel.setText("…");

      newVersionPreLabel.setText("New version:");

      newVersionLabel.setText("…");

      downloadSizePreLabel.setText("Total download size:");

      downloadSizeLabel.setText("…");

      statusLabel.setText(" ");

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
            .addComponent(downloadButton)
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void downloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadButtonActionPerformed
		final DownloadTask task = new DownloadTask(localPath, remoteURL, this);
		task.addPropertyChangeListener(this);
		task.execute();
   }//GEN-LAST:event_downloadButtonActionPerformed

	@Override
	public void startCheckUpdates() throws Exception{
		statusLabel.setText("Check for updates…");
	}

	@Override
	public void startDownloads(final String version) throws Exception{
		statusLabel.setText("Begin downloading version " + version + "…");
	}

	@Override
	public void validatingFile(final String localPath){
		statusLabel.setText("Validating download…");

		try{
			DownloaderHelper.validate(localPath, remoteObject);
		}
		catch(final Exception e){
			statusLabel.setText(e.getMessage());
		}
	}

	@Override
	public void stopped(){
		statusLabel.setText("Update stopped");
	}

	@Override
	public void succeeded(){
		statusLabel.setText("File has been downloaded successfully!");
		downloadButton.setEnabled(false);

		//TODO copy file to current location
		try{
			final File fileToMove = new File(localPath);
			String currentlyRunningApplication = HunLinterFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
//currentlyRunningApplication = "D:/Mauro/HunLinter/target/Hunspeller-1.9.1.jar";
			String filename = FilenameUtils.getBaseName(localPath);
			final String currentVersion = DownloaderHelper.extractVersion(filename + "." + FilenameUtils.getExtension(localPath));
			if(currentVersion != null)
				filename = filename.substring(0, filename.length() - currentVersion.length() - 1);
			currentlyRunningApplication = FilenameUtils.getFullPath(currentlyRunningApplication) + filename + "." + FilenameUtils.getExtension(currentlyRunningApplication);
			final boolean moved = fileToMove.renameTo(new File(currentlyRunningApplication));
			if(!moved)
				throw new FileSystemException(currentlyRunningApplication);
		}
		catch(final Exception e){
			e.printStackTrace();
		}

		//TODO restart application
	}

	@Override
	public void failed(final Exception e){
		statusLabel.setText("Error downloading file: " + e.getMessage());
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
   // End of variables declaration//GEN-END:variables

}
