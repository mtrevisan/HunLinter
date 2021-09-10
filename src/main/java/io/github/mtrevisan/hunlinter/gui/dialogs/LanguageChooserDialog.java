/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.gui.dialogs;

import javax.swing.DefaultListModel;
import java.awt.Frame;
import java.io.Serial;
import java.util.Objects;
import java.util.function.Consumer;


public class LanguageChooserDialog extends javax.swing.JDialog{

	@Serial
	private static final long serialVersionUID = 1343230759230486883L;

	private final Consumer<String> onSelection;
	private boolean languageChosen;


	public LanguageChooserDialog(final Iterable<String> availableLanguages, final Consumer<String> onSelection, final Frame parent){
		super(parent, "Language chooser", true);

		Objects.requireNonNull(onSelection, "On selection cannot be null");

		initComponents();

		final DefaultListModel<String> model = new DefaultListModel<>();
		for(final String language : availableLanguages)
			model.addElement(language);
		languageList.setModel(model);

		this.onSelection = onSelection;
	}

	/**
	 * This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      languageScrollPane = new javax.swing.JScrollPane();
      languageList = new javax.swing.JList<>();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
      setResizable(false);

      languageList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
      languageList.addListSelectionListener(this::languageListValueChanged);
      languageScrollPane.setViewportView(languageList);

      final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(languageScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 130, Short.MAX_VALUE)
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(languageScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 85, Short.MAX_VALUE)
            .addContainerGap())
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void languageListValueChanged(final javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_languageListValueChanged
		languageChosen = true;
		onSelection.accept(languageList.getSelectedValue());

		dispose();
   }//GEN-LAST:event_languageListValueChanged

	public final boolean isLanguageChosen(){
   	return languageChosen;
	}


   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JList<String> languageList;
   private javax.swing.JScrollPane languageScrollPane;
   // End of variables declaration//GEN-END:variables

}
