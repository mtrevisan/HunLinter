package unit731.hunlinter;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.swing.*;

import org.json.simple.JSONObject;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.downloader.DownloadListenerInterface;
import unit731.hunlinter.services.downloader.DownloadTask;
import unit731.hunlinter.services.downloader.DownloaderHelper;


public class FileDownloaderDialog extends JDialog implements PropertyChangeListener, DownloadListenerInterface{

	private JSONObject remoteObject;
	private String localPath;


	public FileDownloaderDialog(final String repositoryURL, final Frame parent){
		super(parent, "File downloader", true);

		initComponents();


		try{
			fileProgressBar.setValue(0);

			remoteObject = DownloaderHelper.extractLastVersion(repositoryURL);
			final String remoteURL = (String)remoteObject.getOrDefault("download_url", null);
			final String filename = (String)remoteObject.getOrDefault("name", null);
			localPath = System.getProperty("user.home") + "/Downloads/" + filename;

			final DownloadTask task = new DownloadTask(localPath, remoteURL, this);
			task.addPropertyChangeListener(this);
			task.execute();
		}
		catch(final Exception e){
			JOptionPane.showMessageDialog(this, "Error executing upload task: " + e.getMessage(), "Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      statusLabel = new javax.swing.JLabel();
      fileProgressBar = new javax.swing.JProgressBar();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      statusLabel.setText("…");

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(fileProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(statusLabel)
                  .addGap(0, 0, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap(283, Short.MAX_VALUE)
            .addComponent(statusLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(fileProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(56, 56, 56))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	@Override
	public void startCheckUpdates() throws Exception{
		statusLabel.setText("Check for updates…");
	}

	@Override
	public void startDownloads() throws Exception{
		statusLabel.setText("Begin downloading…");
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

		try{
			FileHelper.openFolder(new File(localPath));
		}
		catch(final Exception ignored){}
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
   private javax.swing.JProgressBar fileProgressBar;
   private javax.swing.JLabel statusLabel;
   // End of variables declaration//GEN-END:variables

}
