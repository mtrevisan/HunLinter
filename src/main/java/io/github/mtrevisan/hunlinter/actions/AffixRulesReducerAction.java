/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.actions;

import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.gui.dialogs.RulesReducerDialog;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.services.eventbus.EventBusService;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.MenuSelectionManager;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Objects;


public class AffixRulesReducerAction extends AbstractAction{

	@Serial
	private static final long serialVersionUID = 2384472624481589969L;


	private final ParserManager parserManager;


	public AffixRulesReducerAction(final ParserManager parserManager){
		super("affix.rulesReducer");

		Objects.requireNonNull(parserManager, "Parser manager cannot be null");

		this.parserManager = parserManager;
	}

	@Override
	public final void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		setEnabled(false);

		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		final RulesReducerDialog rulesReducerDialog = new RulesReducerDialog(parserManager, parentFrame);
		EventBusService.subscribe(rulesReducerDialog);
		rulesReducerDialog.setLocationRelativeTo(parentFrame);
		rulesReducerDialog.addWindowListener(new WindowAdapter(){
			@Override
			public void windowDeactivated(final WindowEvent e){
				setEnabled(true);
			}
		});
		rulesReducerDialog.setVisible(true);
	}


	@Override
	@SuppressWarnings("NewExceptionWithoutArguments")
	protected final Object clone() throws CloneNotSupportedException{
		throw new CloneNotSupportedException();
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

}
