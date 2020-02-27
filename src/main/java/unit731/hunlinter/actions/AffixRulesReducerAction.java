package unit731.hunlinter.actions;

import unit731.hunlinter.RulesReducerDialog;
import unit731.hunlinter.parsers.ParserManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;


public class AffixRulesReducerAction extends AbstractAction{

	private final ParserManager parserManager;
	private final JFrame parentFrame;


	public AffixRulesReducerAction(final ParserManager parserManager, final JFrame parentFrame){
		super("affix.rulesReducer");

		putValue(SHORT_DESCRIPTION, "Rules reducer…");

		Objects.requireNonNull(parserManager);
		Objects.requireNonNull(parentFrame);

		this.parserManager = parserManager;
		this.parentFrame = parentFrame;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		setEnabled(false);

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
