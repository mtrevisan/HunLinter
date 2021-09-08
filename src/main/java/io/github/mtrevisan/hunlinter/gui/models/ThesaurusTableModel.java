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

import io.github.mtrevisan.hunlinter.parsers.thesaurus.ThesaurusEntry;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.text.MessageFormat;
import java.util.List;


public class ThesaurusTableModel extends AbstractTableModel{

	@Serial
	private static final long serialVersionUID = -2584004821296780108L;

	private static final String[] COLUMN_NAMES = {"Definition", "Synonyms"};

	public static final String TAG_NEW_LINE = "<br>";
	private static final ThreadLocal<MessageFormat> TAG = JavaHelper.createMessageFormat("<html><body style=\"'white-space:nowrap'\">{0}</body></html>");


	private List<ThesaurusEntry> synonyms;


	public ThesaurusEntry getSynonymsAt(final int index){
		return synonyms.get(index);
	}

	public void setSynonyms(final List<ThesaurusEntry> synonyms){
		this.synonyms = synonyms;

		fireTableDataChanged();
	}

	@Override
	public int getRowCount(){
		return (synonyms != null? synonyms.size(): 0);
	}

	@Override
	public int getColumnCount(){
		return COLUMN_NAMES.length;
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex){
		if(synonyms == null || synonyms.size() <= rowIndex)
			return null;

		final ThesaurusEntry thesaurus = synonyms.get(rowIndex);
		return switch(columnIndex){
			case 0 -> thesaurus.getDefinition();
			case 1 -> TAG.get().format(new Object[]{thesaurus.joinSynonyms(TAG_NEW_LINE)});
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
