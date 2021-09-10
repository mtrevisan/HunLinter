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
package io.github.mtrevisan.hunlinter.gui.models;

import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;

import javax.swing.table.AbstractTableModel;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.List;


public class CompoundTableModel extends AbstractTableModel implements HunLinterTableModelInterface<Inflection>{

	@Serial
	private static final long serialVersionUID = -7276635232728680738L;

	private static final String[] COLUMN_NAMES = {"Inflection", "Morphological fields"};


	private List<Inflection> inflections;


	@Override
	public final void setInflections(final List<Inflection> inflections){
		this.inflections = inflections;

		fireTableDataChanged();
	}

	@Override
	public final int getRowCount(){
		return (inflections != null? inflections.size(): 0);
	}

	@Override
	public final int getColumnCount(){
		return COLUMN_NAMES.length;
	}

	@Override
	public final Object getValueAt(final int rowIndex, final int columnIndex){
		if(inflections == null || inflections.size() <= rowIndex)
			return null;

		final Inflection inflection = inflections.get(rowIndex);
		return switch(columnIndex){
			case 0 -> inflection.getWord();
			case 1 -> inflection.getMorphologicalFields();
			default -> null;
		};
	}

	@Override
	public final String getColumnName(final int column){
		return COLUMN_NAMES[column];
	}

	@Override
	public final void clear(){
		setInflections(null);
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
