package unit731.hunlinter;

import java.awt.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.swing.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.actions.OpenFileAction;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;
import unit731.hunlinter.parsers.hyphenation.HyphenationOptionsParser;
import unit731.hunlinter.parsers.hyphenation.HyphenationParser;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.system.Debouncer;


public class HyphenationLayeredPane extends JLayeredPane{

	private static final Logger LOGGER = LoggerFactory.getLogger(HyphenationLayeredPane.class);

	private static final Pattern PATTERN_POINTS_AND_NUMBERS_AND_EQUALS_AND_MINUS = PatternHelper.pattern("[.\\d=-]");

	private static final int DEBOUNCER_INTERVAL = 600;


	private final Debouncer<HyphenationLayeredPane> debouncer = new Debouncer<>(this::hyphenate, DEBOUNCER_INTERVAL);
	private final Debouncer<HyphenationLayeredPane> addRuleDebouncer = new Debouncer<>(this::hyphenateAddRule, DEBOUNCER_INTERVAL);

	private final Packager packager;
	private final ParserManager parserManager;
	private final JFrame parentFrame;

	private String formerHyphenationText;


	public HyphenationLayeredPane(final Packager packager, final ParserManager parserManager, final JFrame parentFrame){
		Objects.requireNonNull(packager);
		Objects.requireNonNull(parserManager);
		Objects.requireNonNull(parentFrame);

		this.packager = packager;
		this.parserManager = parserManager;
		this.parentFrame = parentFrame;


		initComponents();


		//add "fontable" property
		GUIUtils.addFontableProperty(hypWordTextField, hypAddRuleTextField, hypSyllabationOutputLabel, hypRulesOutputLabel, hypAddRuleSyllabationOutputLabel);

		GUIUtils.addUndoManager(hypWordTextField, hypAddRuleTextField);

		try{
			//FIXME
//			final int iconSize = hypRulesOutputLabel.getHeight();
//			final int iconSize = dicTotalProductionsValueLabel.getHeight();
final int iconSize = 17;
			final JPopupMenu copyPopupMenu = new JPopupMenu();
			copyPopupMenu.add(GUIUtils.createPopupCopyMenu(iconSize, copyPopupMenu, GUIUtils::copyCallback));
			GUIUtils.addPopupMenu(copyPopupMenu, hypSyllabationOutputLabel, hypRulesOutputLabel, hypAddRuleSyllabationOutputLabel);
		}
		catch(final IOException ignored){}
	}

	@SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      hypWordLabel = new javax.swing.JLabel();
      hypWordTextField = new javax.swing.JTextField();
      hypSyllabationLabel = new javax.swing.JLabel();
      hypSyllabationOutputLabel = new javax.swing.JLabel();
      hypSyllabesCountLabel = new javax.swing.JLabel();
      hypSyllabesCountOutputLabel = new javax.swing.JLabel();
      hypRulesLabel = new javax.swing.JLabel();
      hypRulesOutputLabel = new javax.swing.JLabel();
      hypAddRuleLabel = new javax.swing.JLabel();
      hypAddRuleTextField = new javax.swing.JTextField();
      hypAddRuleLevelComboBox = new javax.swing.JComboBox<>();
      hypAddRuleButton = new javax.swing.JButton();
      hypAddRuleSyllabationLabel = new javax.swing.JLabel();
      hypAddRuleSyllabationOutputLabel = new javax.swing.JLabel();
      hypAddRuleSyllabesCountLabel = new javax.swing.JLabel();
      hypAddRuleSyllabesCountOutputLabel = new javax.swing.JLabel();
      optionsButton = new javax.swing.JButton();
      openHypButton = new javax.swing.JButton();

      hypWordLabel.setText("Word:");

      hypWordTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            hypWordTextFieldKeyReleased(evt);
         }
      });

      hypSyllabationLabel.setText("Syllabation:");
      hypSyllabationLabel.setPreferredSize(new java.awt.Dimension(58, 17));

      hypSyllabationOutputLabel.setText("…");
      hypSyllabationOutputLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      hypSyllabesCountLabel.setText("Syllabes:");

      hypSyllabesCountOutputLabel.setText("…");

      hypRulesLabel.setText("Rules:");
      hypRulesLabel.setPreferredSize(new java.awt.Dimension(31, 17));

      hypRulesOutputLabel.setText("…");
      hypRulesOutputLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      hypAddRuleLabel.setText("Add rule:");

      hypAddRuleTextField.setEnabled(false);
      hypAddRuleTextField.addKeyListener(new java.awt.event.KeyAdapter() {
         public void keyReleased(java.awt.event.KeyEvent evt) {
            hypAddRuleTextFieldKeyReleased(evt);
         }
      });

      hypAddRuleLevelComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Non compound", "Compound" }));
      hypAddRuleLevelComboBox.setEnabled(false);
      hypAddRuleLevelComboBox.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hypAddRuleLevelComboBoxActionPerformed(evt);
         }
      });

      hypAddRuleButton.setMnemonic('A');
      hypAddRuleButton.setText("Add rule");
      hypAddRuleButton.setEnabled(false);
      hypAddRuleButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            hypAddRuleButtonActionPerformed(evt);
         }
      });

      hypAddRuleSyllabationLabel.setText("New syllabation:");
      hypAddRuleSyllabationLabel.setPreferredSize(new java.awt.Dimension(81, 17));

      hypAddRuleSyllabationOutputLabel.setText("…");
      hypAddRuleSyllabationOutputLabel.setPreferredSize(new java.awt.Dimension(9, 17));

      hypAddRuleSyllabesCountLabel.setText("New syllabes:");

      hypAddRuleSyllabesCountOutputLabel.setText("…");

      optionsButton.setText("Options");
      optionsButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            optionsButtonActionPerformed(evt);
         }
      });

      openHypButton.setAction(new OpenFileAction(Packager.KEY_FILE_HYPHENATION, packager));
      openHypButton.setText("Open Hyphenation");
      openHypButton.setEnabled(false);

      setLayer(hypWordLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypWordTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypSyllabationLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypSyllabationOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypSyllabesCountLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypSyllabesCountOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypRulesLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypRulesOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypAddRuleLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypAddRuleTextField, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypAddRuleLevelComboBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypAddRuleButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypAddRuleSyllabationLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypAddRuleSyllabationOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypAddRuleSyllabesCountLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(hypAddRuleSyllabesCountOutputLabel, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(optionsButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
      setLayer(openHypButton, javax.swing.JLayeredPane.DEFAULT_LAYER);

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(hypWordLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypWordTextField))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addGap(0, 0, Short.MAX_VALUE)
                  .addComponent(hypSyllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypSyllabationOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 845, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(hypAddRuleLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypAddRuleTextField)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(hypAddRuleLevelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addGap(18, 18, 18)
                  .addComponent(hypAddRuleButton))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(hypAddRuleSyllabesCountLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypAddRuleSyllabesCountOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(hypAddRuleSyllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypAddRuleSyllabationOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                  .addComponent(hypRulesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypRulesOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(optionsButton)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                  .addComponent(openHypButton))
               .addGroup(layout.createSequentialGroup()
                  .addComponent(hypSyllabesCountLabel)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                  .addComponent(hypSyllabesCountOutputLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
            .addContainerGap())
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypWordLabel)
               .addComponent(hypWordTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypSyllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(hypSyllabationOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addComponent(hypSyllabesCountLabel)
               .addComponent(hypSyllabesCountOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypRulesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(hypRulesOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGap(18, 18, 18)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleLabel)
               .addComponent(hypAddRuleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(hypAddRuleButton)
               .addComponent(hypAddRuleLevelComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleSyllabationLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addComponent(hypAddRuleSyllabationOutputLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(hypAddRuleSyllabesCountLabel)
               .addComponent(hypAddRuleSyllabesCountOutputLabel))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 48, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
               .addComponent(optionsButton)
               .addComponent(openHypButton))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void hypWordTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_hypWordTextFieldKeyReleased
      debouncer.call(this);
   }//GEN-LAST:event_hypWordTextFieldKeyReleased

   private void hypAddRuleTextFieldKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_hypAddRuleTextFieldKeyReleased
      addRuleDebouncer.call(this);
   }//GEN-LAST:event_hypAddRuleTextFieldKeyReleased

   private void hypAddRuleLevelComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypAddRuleLevelComboBoxActionPerformed
      addRuleDebouncer.call(this);
   }//GEN-LAST:event_hypAddRuleLevelComboBoxActionPerformed

   private void hypAddRuleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hypAddRuleButtonActionPerformed
      final  String newRule = hypAddRuleTextField.getText();
      final HyphenationParser.Level level = HyphenationParser.Level.values()[hypAddRuleLevelComboBox.getSelectedIndex()];
      final String foundRule = parserManager.addHyphenationRule(newRule.toLowerCase(Locale.ROOT), level);
      if(foundRule == null){
         try{
            parserManager.storeHyphenationFile();

            if(hypWordTextField.getText() != null){
               formerHyphenationText = null;
               hyphenate();
            }

            hypAddRuleLevelComboBox.setEnabled(false);
            hypAddRuleButton.setEnabled(false);
            hypAddRuleTextField.setText(null);
            hypAddRuleSyllabationOutputLabel.setText(null);
            hypAddRuleSyllabesCountOutputLabel.setText(null);
         }
         catch(final IOException e){
            LOGGER.error("Something very bad happened while adding a rule to the hyphenation file", e);
         }
      }
      else{
         hypAddRuleTextField.requestFocusInWindow();

         LOGGER.info(ParserManager.MARKER_APPLICATION, "Duplicated rule found ({}), cannot insert {}", foundRule, newRule);
      }
   }//GEN-LAST:event_hypAddRuleButtonActionPerformed

   private void optionsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_optionsButtonActionPerformed
      final Consumer<HyphenationOptionsParser> acceptButtonAction = (options) -> {
         try{
            parserManager.getHypParser().setOptions(options);

            parserManager.storeHyphenationFile();
         }
         catch(Exception ex){
            LOGGER.info(ParserManager.MARKER_APPLICATION, ex.getMessage());
         }
      };
      final HyphenationOptionsDialog dialog = new HyphenationOptionsDialog(parserManager.getHypParser().getOptions(),
         acceptButtonAction, parentFrame);
      GUIUtils.addCancelByEscapeKey(dialog);
      dialog.setLocationRelativeTo(this);
      dialog.setVisible(true);
   }//GEN-LAST:event_optionsButtonActionPerformed

	public void initialize(){
		openHypButton.setEnabled(packager.getHyphenationFile() != null);
	}

	public void setCurrentFont(){
		final Font currentFont = GUIUtils.getCurrentFont();
		hypWordTextField.setFont(currentFont);
		hypSyllabationOutputLabel.setFont(currentFont);
		hypAddRuleTextField.setFont(currentFont);
		hypAddRuleSyllabationOutputLabel.setFont(currentFont);
	}

	public void clear(){
		openHypButton.setEnabled(false);

		formerHyphenationText = null;

		hypWordTextField.setText(null);
		hypSyllabationOutputLabel.setText(null);
		hypSyllabesCountOutputLabel.setText(null);
		hypRulesOutputLabel.setText(null);
		hypAddRuleTextField.setText(null);
		hypAddRuleLevelComboBox.setEnabled(false);
		hypAddRuleButton.setEnabled(false);
		hypAddRuleSyllabationOutputLabel.setText(null);
		hypAddRuleSyllabesCountOutputLabel.setText(null);
	}

	private void hyphenate(){
		final String language = parserManager.getAffixData().getLanguage();
		final Orthography orthography = BaseBuilder.getOrthography(language);
		String text = orthography.correctOrthography(hypWordTextField.getText());
		if(formerHyphenationText != null && formerHyphenationText.equals(text))
			return;
		formerHyphenationText = text;

		String count = null;
		List<String> rules = Collections.emptyList();
		if(StringUtils.isNotBlank(text)){
			final Hyphenation hyphenation = parserManager.getHyphenator().hyphenate(text);

			final Supplier<StringJoiner> sj = () -> new StringJoiner(HyphenationParser.SOFT_HYPHEN, "<html>",
				"</html>");
			final Function<String, String> errorFormatter = syllabe -> "<b style=\"color:red\">" + syllabe + "</b>";
			text = orthography.formatHyphenation(hyphenation.getSyllabes(), sj.get(), errorFormatter)
				.toString();
			count = Long.toString(hyphenation.countSyllabes());
			rules = hyphenation.getRules();

			hypAddRuleTextField.setEnabled(true);
		}
		else{
			text = null;

			hypAddRuleTextField.setEnabled(false);
		}

		hypSyllabationOutputLabel.setText(text);
		hypSyllabesCountOutputLabel.setText(count);
		hypRulesOutputLabel.setText(StringUtils.join(rules, StringUtils.SPACE));

		hypAddRuleTextField.setText(null);
		hypAddRuleSyllabationOutputLabel.setText(null);
		hypAddRuleSyllabesCountOutputLabel.setText(null);
	}

	private void hyphenateAddRule(){
		final String language = parserManager.getAffixData().getLanguage();
		final Orthography orthography = BaseBuilder.getOrthography(language);
		String addedRuleText = orthography.correctOrthography(hypWordTextField.getText());
		final String addedRule = orthography.correctOrthography(hypAddRuleTextField.getText().toLowerCase(Locale.ROOT));
		final HyphenationParser.Level level = HyphenationParser.Level.values()[hypAddRuleLevelComboBox.getSelectedIndex()];
		String addedRuleCount = null;
		if(StringUtils.isNotBlank(addedRule)){
			final boolean alreadyHasRule = parserManager.hasHyphenationRule(addedRule, level);
			boolean ruleMatchesText = false;
			boolean hyphenationChanged = false;
			boolean correctHyphenation = false;
			if(!alreadyHasRule){
				ruleMatchesText = addedRuleText.contains(PatternHelper.clear(addedRule,
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
			hypAddRuleLevelComboBox.setEnabled(ruleMatchesText);
			hypAddRuleButton.setEnabled(ruleMatchesText && hyphenationChanged && correctHyphenation);
		}
		else{
			addedRuleText = null;

			hypAddRuleTextField.setText(null);
			hypAddRuleLevelComboBox.setEnabled(false);
			hypAddRuleButton.setEnabled(false);
			hypAddRuleSyllabationOutputLabel.setText(null);
			hypAddRuleSyllabesCountOutputLabel.setText(null);
		}

		hypAddRuleSyllabationOutputLabel.setText(addedRuleText);
		hypAddRuleSyllabesCountOutputLabel.setText(addedRuleCount);
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
   private javax.swing.JButton hypAddRuleButton;
   private javax.swing.JLabel hypAddRuleLabel;
   private javax.swing.JComboBox<String> hypAddRuleLevelComboBox;
   private javax.swing.JLabel hypAddRuleSyllabationLabel;
   private javax.swing.JLabel hypAddRuleSyllabationOutputLabel;
   private javax.swing.JLabel hypAddRuleSyllabesCountLabel;
   private javax.swing.JLabel hypAddRuleSyllabesCountOutputLabel;
   private javax.swing.JTextField hypAddRuleTextField;
   private javax.swing.JLabel hypRulesLabel;
   private javax.swing.JLabel hypRulesOutputLabel;
   private javax.swing.JLabel hypSyllabationLabel;
   private javax.swing.JLabel hypSyllabationOutputLabel;
   private javax.swing.JLabel hypSyllabesCountLabel;
   private javax.swing.JLabel hypSyllabesCountOutputLabel;
   private javax.swing.JLabel hypWordLabel;
   private javax.swing.JTextField hypWordTextField;
   private javax.swing.JButton openHypButton;
   private javax.swing.JButton optionsButton;
   // End of variables declaration//GEN-END:variables
}
