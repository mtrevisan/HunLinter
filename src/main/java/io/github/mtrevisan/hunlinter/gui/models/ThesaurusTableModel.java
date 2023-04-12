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
package io.github.mtrevisan.hunlinter.gui.models;

import io.github.mtrevisan.hunlinter.parsers.thesaurus.ThesaurusEntry;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.table.AbstractTableModel;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.List;


public class ThesaurusTableModel extends AbstractTableModel{

	@Serial
	private static final long serialVersionUID = -2584004821296780108L;

	private static final String[] COLUMN_NAMES = {"Definition", "Synonyms"};

	private static final String COMMA = ",";
	private static final String PIPE = "|";
	/** Adds a zero-width space to let wrapping occurs after commas. */
	private static final String ZERO_WIDTH_SPACE = "&#8203;";
	private static final String WRAPPABLE_COMMA = COMMA + ZERO_WIDTH_SPACE;
	private static final String COLUMN = ":";
	private static final String WRAPPABLE_COLUMN = COLUMN + ZERO_WIDTH_SPACE;

	public static final String TAG_NEW_LINE = "<br>";
//	private static final String TAG = "<html><body style=\"'white-space:nowrap;overflow:hidden;text-overflow:ellipsis'\">{}</body></html>";
	private static final String TAG = "<html>{}</html>";


	private List<ThesaurusEntry> synonyms;


	public final ThesaurusEntry getSynonymsAt(final int index){
		return synonyms.get(index);
	}

	public final void setSynonyms(final List<ThesaurusEntry> synonyms){
		this.synonyms = synonyms;

		fireTableDataChanged();
	}

	@Override
	public final int getRowCount(){
		return (synonyms != null? synonyms.size(): 0);
	}

	@Override
	public final int getColumnCount(){
		return COLUMN_NAMES.length;
	}

	@Override
	public final Object getValueAt(final int rowIndex, final int columnIndex){
		if(synonyms == null || synonyms.size() <= rowIndex)
			return null;

		final ThesaurusEntry thesaurus = synonyms.get(rowIndex);
		return switch(columnIndex){
			case 0 -> thesaurus.getDefinition();
			case 1 -> {
				String temp = thesaurus.joinSynonyms(TAG_NEW_LINE);
				temp = StringUtils.replace(temp, PIPE, WRAPPABLE_COMMA);
				temp = StringUtils.replace(temp, ")" + WRAPPABLE_COMMA, ") ");
				yield JavaHelper.textFormat(TAG, temp);
			}
			default -> null;
		};
	}

	@Override
	public final String getColumnName(final int column){
		return COLUMN_NAMES[column];
	}

	public String getDefinition(final int rowIndex){
		return (String)getValueAt(rowIndex, 0);
	}

	public String getSynonyms(final int rowIndex){
		String temp = (String)getValueAt(rowIndex, 1);
		temp = StringUtils.replace(temp, WRAPPABLE_COMMA, COMMA);
		temp = StringUtils.replace(temp, ") ", COLUMN);
		return StringUtils.replace(temp, "(", StringUtils.EMPTY);
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
