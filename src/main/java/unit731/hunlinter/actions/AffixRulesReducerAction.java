package unit731.hunlinter.actions;

import unit731.hunlinter.RulesReducerDialog;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.parsers.ParserManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;


public class AffixRulesReducerAction extends AbstractAction{

	private final ParserManager parserManager;


	public AffixRulesReducerAction(final ParserManager parserManager){
		super("affix.rulesReducer");

		Objects.requireNonNull(parserManager);

		this.parserManager = parserManager;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		setEnabled(false);

		final Frame parentFrame = GUIUtils.getParentFrame((JMenuItem)event.getSource());
		final RulesReducerDialog rulesReducerDialog = new RulesReducerDialog(parserManager, parentFrame);
		rulesReducerDialog.setLocationRelativeTo(parentFrame);
		rulesReducerDialog.addWindowListener(new WindowAdapter(){
			@Override
			public void windowDeactivated(final WindowEvent e){
				setEnabled(true);
			}
		});
		rulesReducerDialog.setVisible(true);
	}

}
