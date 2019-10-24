package unit731.hunspeller;

import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Objects;
import javax.swing.JDialog;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.gui.GUIUtils;


public class ThesaurusDuplicatesDialog extends JDialog{

	private static final long serialVersionUID = 5718588727397261977L;


	public ThesaurusDuplicatesDialog(List<String> duplicates, Frame parent){
		super(parent, "Thesaurus duplicates", true);

		Objects.requireNonNull(duplicates);
		Objects.requireNonNull(parent);

		initComponents();

		duplicatesTextArea.setFont(GUIUtils.getCurrentFont());

		String content = String.join(StringUtils.LF, duplicates);
		duplicatesTextArea.setText(content);
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      mainScrollPane = new javax.swing.JScrollPane();
      duplicatesTextArea = new javax.swing.JTextArea();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      duplicatesTextArea.setEditable(false);
      duplicatesTextArea.setColumns(20);
      duplicatesTextArea.setLineWrap(true);
      duplicatesTextArea.setRows(1);
      duplicatesTextArea.setWrapStyleWord(true);
      mainScrollPane.setViewportView(duplicatesTextArea);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(mainScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(mainScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

	@SuppressWarnings("unused")
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JTextArea duplicatesTextArea;
   private javax.swing.JScrollPane mainScrollPane;
   // End of variables declaration//GEN-END:variables
}
