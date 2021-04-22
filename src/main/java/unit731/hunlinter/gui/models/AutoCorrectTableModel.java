/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.gui.models;

import unit731.hunlinter.parsers.autocorrect.CorrectionEntry;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.List;


public class AutoCorrectTableModel extends AbstractTableModel{

	@Serial
	private static final long serialVersionUID = -5235919012141465022L;

	private static final String[] COLUMN_NAMES = {"Incorrect form", "Correct form"};


	private List<CorrectionEntry> corrections;


	public void setCorrections(final List<CorrectionEntry> corrections){
		this.corrections = corrections;

		fireTableDataChanged();
	}

	@Override
	public int getRowCount(){
		return (corrections != null? corrections.size(): 0);
	}

	@Override
	public int getColumnCount(){
		return COLUMN_NAMES.length;
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex){
		if(corrections == null || corrections.size() <= rowIndex)
			return null;

		final CorrectionEntry correction = corrections.get(rowIndex);
		return switch(columnIndex){
			case 0 -> correction.getIncorrectForm();
			case 1 -> correction.getCorrectForm();
			default -> null;
		};
	}

	@Override
	public String getColumnName(final int column){
		return COLUMN_NAMES[column];
	}


	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws IOException{
		throw new NotSerializableException(getClass().getName());
	}

}
