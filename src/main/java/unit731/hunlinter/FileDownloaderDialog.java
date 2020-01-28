package unit731.hunlinter;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import javax.swing.*;

import unit731.hunlinter.services.downloader.DownloadListenerInterface;
import unit731.hunlinter.services.downloader.DownloadTask;


public class FileDownloaderDialog extends JDialog implements PropertyChangeListener{

	public FileDownloaderDialog(final String downloadURL, final String saveFilePath, final DownloadListenerInterface listener, final Frame parent){
		super(parent, "File downloader", true);

		Objects.requireNonNull(downloadURL);
		Objects.requireNonNull(saveFilePath);

		initComponents();


		try{
			fileProgressBar.setValue(0);

			final DownloadTask task = new DownloadTask(downloadURL, saveFilePath, listener);
			task.addPropertyChangeListener(this);
			task.execute();
		}
		catch(final Exception e){
			JOptionPane.showMessageDialog(this,
				"Error executing upload task: " + e.getMessage(), "Error",
				JOptionPane.ERROR_MESSAGE);
		}
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      fileProgressBar = new javax.swing.JProgressBar();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(fileProgressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 527, Short.MAX_VALUE)
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap(308, Short.MAX_VALUE)
            .addComponent(fileProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addGap(56, 56, 56))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

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
	// End of variables declaration//GEN-END:variables

}
