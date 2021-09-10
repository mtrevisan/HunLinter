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
package io.github.mtrevisan.hunlinter.gui.panes;

import io.github.mtrevisan.hunlinter.MainFrame;
import io.github.mtrevisan.hunlinter.actions.OpenFileAction;
import io.github.mtrevisan.hunlinter.gui.FontHelper;
import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.gui.dialogs.HyphenationOptionsDialog;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.languages.Orthography;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.Hyphenation;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationOptionsParser;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.services.Packager;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.eventbus.EventHandler;
import io.github.mtrevisan.hunlinter.services.system.Debouncer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPopupMenu;
import java.awt.Font;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;


public class HyphenationLayeredPane extends JLayeredPane{

	@Serial
	private static final long serialVersionUID = 67463311214897950L;

	private static final Logger LOGGER = LoggerFactory.getLogger(HyphenationLayeredPane.class);

	private static final Pattern PATTERN_POINTS_AND_NUMBERS_AND_EQUALS_AND_MINUS = RegexHelper.pattern("[.\\d=-]");

	private static final int DEBOUNCER_INTERVAL = 600;

	private static final String SYLLABATION_PREFIX = "<html>";
	private static final String SYLLABATION_SUFFIX = "</html>";


	private final Debouncer<HyphenationLayeredPane> debouncer = new Debouncer<>(this::hyphenate, DEBOUNCER_INTERVAL);
	private final Debouncer<HyphenationLayeredPane> addRuleDebouncer = new Debouncer<>(this::hyphenateAddRule, DEBOUNCER_INTERVAL);

	private final Packager packager;
	private final ParserManager parserManager;
	private final JFrame parentFrame;

	private String formerHyphenationText;


	public HyphenationLayeredPane(final Packager packager, final ParserManager parserManager, final JFrame parentFrame){
		Objects.requireNonNull(packager, "Packager cannot be null");
		Objects.requireNonNull(parserManager, "Parser manager cannot be null");
		Objects.requireNonNull(parentFrame, "Parent frame cannot be null");

		this.packager = packager;
		this.parserManager = parserManager;
		this.parentFrame = parentFrame;


		initComponents();


		//add "fontable" property
		FontHelper.addFontableProperty(wordTextField, addRuleTextField, syllabationValueLabel, rulesValueLabel, addRuleSyllabationValueLabel);

		GUIHelper.addUndoManager(wordTextField, addRuleTextField);

		try{
			final JPopupMenu copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIHelper.createPopupCopyMenu(copyPopupMenu, GUIHelper::copyCallback));
			GUIHelper.addPopupMenu(copyPopupMenu, syllabationValueLabel, rulesValueLabel, addRuleSyllabationValueLabel);
		}
		catch(final IOException ignored){}
	}

   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      wordLabel = new javax.swing.JLabel();
      wordTextField = new javax.swing.JTextField();
      syllabationLabel = new javax.swing.JLabel();
      syllabationValueLabel = new javax.swing.JLabel();
      syllabesCountLabel = new javax.swing.JLabel();
      syllabesCountValueLabel = new javax.swing.JLabel();
      rulesLabel = new javax.swing.JLabel();
      rulesValueLabel = new javax.swing.JLabel();
      addRuleLabel = new javax.swing.JLabel();
      addRuleTextField = new javax.swing.JTextField();
      addRuleLevelComboBox = new javax.swing.JComboBox<>();
      addRuleButton = new javax.swing.JButton();
      addRuleSyllabationLabel = new javax.swing.JLabel();
      addRuleSyllabationValueLabel = new javax.swing.JLabel();
      addRuleSyllabesCountLabel = new javax.swing.JLabel();
      addRuleSyllabesCountValueLabel = new javax.swing.JLabel();
      optionsButton = new javax.swing.JButton();
      openHypButton = new javax.swing.JButton();

      wordLabel.setText("Word:");

		final Font currentFont = FontHelper.getCurrentFont();

		wordTextField.setFont(currentFont);
      wordTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(final java.awt.event.KeyEvent evt) {
            wordTextFieldKeyReleased(evt);
         }
      });

      syllabationLabel.setText("Syllabation:");
      syllabationLabel.setPreferredSize(new java.awt.Dimension(58, 17));

      syllabationValueLabel.setFont(currentFont);
      syllabationValueLabel.setText("…");
      syllabationValueLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      syllabesCountLabel.setText("Syllabes:");

      syllabesCountValueLabel.setFont(currentFont);
      syllabesCountValueLabel.setText("…");

      rulesLabel.setText("Rules:");
      rulesLabel.setPreferredSize(new java.awt.Dimension(31, 17));

      rulesValueLabel.setFont(currentFont);
      rulesValueLabel.setText("…");
      rulesValueLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      addRuleLabel.setText("Add rule:");

      addRuleTextField.setFont(currentFont);
      addRuleTextField.setEnabled(false);
      addRuleTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(final java.awt.event.KeyEvent evt) {
            addRuleTextFieldKeyReleased(evt);
         }
      });

      addRuleLevelComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Non compound", "Compound" }));
      addRuleLevelComboBox.setEnabled(false);
      addRuleLevelComboBox.addActionListener(this::addRuleLevelComboBoxActionPerformed);

      addRuleButton.setMnemonic('A');
      addRuleButton.setText("Add rule");
      addRuleButton.setEnabled(false);
      addRuleButton.addActionListener(this::addRuleButtonActionPerformed);

      addRuleSyllabationLabel.setText("New syllabation:");
      addRuleSyllabationLabel.setPreferredSize(new java.awt.Dimension(81, 17));

      addRuleSyllabationValueLabel.setFont(currentFont);
      addRuleSyllabationValueLabel.setText("…");

      addRuleSyllabesCountLabel.setText("New syllabes:");

      addRuleSyllabesCountValueLabel.setFont(currentFont);
      addRuleSyllabesCountValueLabel.setText("…");

      optionsButton.setText("Options");
      optionsButton.addActionListener(this::optionsButtonActionPerformed);

      openHypButton.setAction(new OpenFileAction(Packager.KEY_FILE_HYPHENATION, packager));
      openHypButton.setText("Open Hyphenation");
      openHypButton.setEnabled(false);

      setLayer(wordLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(wordTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(syllabationLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(syllabationValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(syllabesCountLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(syllabesCountValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(rulesLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(rulesValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(addRuleLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(addRuleTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(addRuleLevelComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(addRuleButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(addRuleSyllabationLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(addRuleSyllabationValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(addRuleSyllabesCountLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(addRuleSyllabesCountValueLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(optionsButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openHypButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      final javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(wordLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(wordTextField))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGap(0, 0, Short.MAX_VALUE)
                  .addComponent(syllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(syllabationValueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 845, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(addRuleLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(addRuleTextField)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(addRuleLevelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addGap(18, 18, 18)
                  .addComponent(addRuleButton))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(addRuleSyllabesCountLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(addRuleSyllabesCountValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(addRuleSyllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(addRuleSyllabationValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addComponent(rulesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(rulesValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(optionsButton)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(openHypButton))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(syllabesCountLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(syllabesCountValueLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(wordLabel)
               .addComponent(wordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(syllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(syllabationValueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(syllabesCountLabel)
               .addComponent(syllabesCountValueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(rulesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(rulesValueLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(addRuleLabel)
               .addComponent(addRuleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(addRuleButton)
               .addComponent(addRuleLevelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(addRuleSyllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(addRuleSyllabationValueLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(addRuleSyllabesCountLabel)
               .addComponent(addRuleSyllabesCountValueLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 48, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(optionsButton)
               .addComponent(openHypButton))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void wordTextFieldKeyReleased(final java.awt.event.KeyEvent evt) {//GEN-FIRST:event_wordTextFieldKeyReleased
      debouncer.call(this);
   }//GEN-LAST:event_wordTextFieldKeyReleased

   private void addRuleTextFieldKeyReleased(final java.awt.event.KeyEvent evt) {//GEN-FIRST:event_addRuleTextFieldKeyReleased
      addRuleDebouncer.call(this);
   }//GEN-LAST:event_addRuleTextFieldKeyReleased

   private void addRuleLevelComboBoxActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRuleLevelComboBoxActionPerformed
      addRuleDebouncer.call(this);
   }//GEN-LAST:event_addRuleLevelComboBoxActionPerformed

   private void addRuleButtonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addRuleButtonActionPerformed
      final  String newRule = addRuleTextField.getText();
      final HyphenationParser.Level level = HyphenationParser.Level.values()[addRuleLevelComboBox.getSelectedIndex()];
      final String foundRule = parserManager.addHyphenationRule(newRule.toLowerCase(Locale.ROOT), level);
      if(foundRule == null){
         try{
            parserManager.storeHyphenationFile();

            if(wordTextField.getText() != null){
               formerHyphenationText = null;

               hyphenate();
            }

            addRuleLevelComboBox.setEnabled(false);
            addRuleButton.setEnabled(false);
            addRuleTextField.setText(null);
            addRuleSyllabationValueLabel.setText(null);
            addRuleSyllabesCountValueLabel.setText(null);
         }
         catch(final IOException ioe){
            LOGGER.error("Something very bad happened while adding a rule to the hyphenation file", ioe);
         }
      }
      else{
         addRuleTextField.requestFocusInWindow();

         LOGGER.info(ParserManager.MARKER_APPLICATION, "Duplicated rule found ({}), cannot insert {}", foundRule, newRule);
      }
   }//GEN-LAST:event_addRuleButtonActionPerformed

   private void optionsButtonActionPerformed(final java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optionsButtonActionPerformed
      final Consumer<HyphenationOptionsParser> acceptButtonAction = (options) -> {
         try{
            parserManager.getHypParser().setOptions(options);

            parserManager.storeHyphenationFile();
         }
         catch(final IOException ioe){
            LOGGER.info(ParserManager.MARKER_APPLICATION, ioe.getMessage());
         }
      };
      final HyphenationOptionsDialog dialog = new HyphenationOptionsDialog(parserManager.getHypParser().getOptions(),
         acceptButtonAction, parentFrame);
      GUIHelper.addCancelByEscapeKey(dialog);
      dialog.setLocationRelativeTo(this);
      dialog.setVisible(true);
   }//GEN-LAST:event_optionsButtonActionPerformed

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void initialize(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_INITIALIZE)
			return;

		openHypButton.setEnabled(packager.getHyphenationFile() != null);
	}

	@EventHandler
	@SuppressWarnings({"unused", "NumberEquality"})
	public final void clear(final Integer actionCommand){
		if(actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_ALL && actionCommand != MainFrame.ACTION_COMMAND_GUI_CLEAR_HYPHENATION)
			return;

		openHypButton.setEnabled(false);

		formerHyphenationText = null;

		wordTextField.setText(null);
		syllabationValueLabel.setText(null);
		syllabesCountValueLabel.setText(null);
		rulesValueLabel.setText(null);
		addRuleTextField.setText(null);
		addRuleLevelComboBox.setEnabled(false);
		addRuleButton.setEnabled(false);
		addRuleSyllabationValueLabel.setText(null);
		addRuleSyllabesCountValueLabel.setText(null);
	}

	private void hyphenate(){
		final String language = parserManager.getLanguage();
		final Orthography orthography = BaseBuilder.getOrthography(language);
		String text = orthography.correctOrthography(wordTextField.getText().trim());
		if(formerHyphenationText != null && formerHyphenationText.equals(text))
			return;
		formerHyphenationText = text;

		String count = null;
		java.util.List<String> rules = new ArrayList<>(0);
		if(StringUtils.isNotBlank(text)){
			final Hyphenation hyphenation = parserManager.getHyphenator().hyphenate(text);

			final Supplier<StringJoiner> sj = () -> new StringJoiner(HyphenationParser.SOFT_HYPHEN, SYLLABATION_PREFIX, SYLLABATION_SUFFIX);
			final Function<String, String> errorFormatter = syllabe -> "<b style=\"color:red\">" + syllabe + "</b>";
			text = orthography.formatHyphenation(hyphenation.getSyllabes(), sj.get(), errorFormatter)
				.toString();
			count = Long.toString(hyphenation.countSyllabes());
			rules = hyphenation.getRules();

			addRuleTextField.setEnabled(true);
		}
		else{
			text = null;

			addRuleTextField.setEnabled(false);
		}

		syllabationValueLabel.setText(text);
		syllabesCountValueLabel.setText(count);
		rulesValueLabel.setText(StringUtils.join(rules, StringUtils.SPACE));

		addRuleTextField.setText(null);
		addRuleSyllabationValueLabel.setText(null);
		addRuleSyllabesCountValueLabel.setText(null);
	}

	private void hyphenateAddRule(){
		final String language = parserManager.getLanguage();
		final Orthography orthography = BaseBuilder.getOrthography(language);
		String addedRuleText = orthography.correctOrthography(wordTextField.getText());
		final String addedRule = orthography.correctOrthography(addRuleTextField.getText().toLowerCase(Locale.ROOT));
		final HyphenationParser.Level level = HyphenationParser.Level.values()[addRuleLevelComboBox.getSelectedIndex()];
		String addedRuleCount = null;
		if(StringUtils.isNotBlank(addedRule)){
			final boolean alreadyHasRule = parserManager.hasHyphenationRule(addedRule, level);
			boolean ruleMatchesText = false;
			boolean hyphenationChanged = false;
			boolean correctHyphenation = false;
			if(!alreadyHasRule){
				ruleMatchesText = addedRuleText.contains(RegexHelper.clear(addedRule,
					PATTERN_POINTS_AND_NUMBERS_AND_EQUALS_AND_MINUS));

				if(ruleMatchesText){
					final Hyphenation hyphenation = parserManager.getHyphenator().hyphenate(addedRuleText);
					final Hyphenation addedRuleHyphenation = parserManager.getHyphenator().hyphenate(addedRuleText, addedRule,
						level);

					final Supplier<StringJoiner> sj = () -> new StringJoiner(HyphenationParser.SOFT_HYPHEN, "<html>",
						"</html>");
					final Function<String, String> errorFormatter = syllabe -> "<b style=\"color:red\">" + syllabe + "</b>";
					final String text = orthography.formatHyphenation(hyphenation.getSyllabes(), sj.get(), errorFormatter)
						.toString();
					addedRuleText = orthography.formatHyphenation(addedRuleHyphenation.getSyllabes(), sj.get(), errorFormatter)
						.toString();
					addedRuleCount = Long.toString(addedRuleHyphenation.countSyllabes());

					hyphenationChanged = !text.equals(addedRuleText);
					correctHyphenation = !orthography.hasSyllabationErrors(addedRuleHyphenation.getSyllabes());
				}
			}

			if(alreadyHasRule || !ruleMatchesText)
				addedRuleText = null;
			addRuleLevelComboBox.setEnabled(ruleMatchesText);
			addRuleButton.setEnabled(ruleMatchesText && hyphenationChanged && correctHyphenation);
		}
		else{
			addedRuleText = null;

			addRuleTextField.setText(null);
			addRuleLevelComboBox.setEnabled(false);
			addRuleButton.setEnabled(false);
			addRuleSyllabationValueLabel.setText(null);
			addRuleSyllabesCountValueLabel.setText(null);
		}

		addRuleSyllabationValueLabel.setText(addedRuleText);
		addRuleSyllabesCountValueLabel.setText(addedRuleCount);
	}


	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}


   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JButton addRuleButton;
   private javax.swing.JLabel addRuleLabel;
   private javax.swing.JComboBox<String> addRuleLevelComboBox;
   private javax.swing.JLabel addRuleSyllabationLabel;
   private javax.swing.JLabel addRuleSyllabationValueLabel;
   private javax.swing.JLabel addRuleSyllabesCountLabel;
   private javax.swing.JLabel addRuleSyllabesCountValueLabel;
   private javax.swing.JTextField addRuleTextField;
   private javax.swing.JButton openHypButton;
   private javax.swing.JButton optionsButton;
   private javax.swing.JLabel rulesLabel;
   private javax.swing.JLabel rulesValueLabel;
   private javax.swing.JLabel syllabationLabel;
   private javax.swing.JLabel syllabationValueLabel;
   private javax.swing.JLabel syllabesCountLabel;
   private javax.swing.JLabel syllabesCountValueLabel;
   private javax.swing.JLabel wordLabel;
   private javax.swing.JTextField wordTextField;
   // End of variables declaration//GEN-END:variables
}
