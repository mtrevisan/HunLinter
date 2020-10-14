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
package unit731.hunlinter.gui;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.Objects;


/**
 * An implementation of {@code FileFilter} that filters hunspell projects.
 *
 * @see FileFilter
 * @see javax.swing.JFileChooser#setFileFilter
 * @see javax.swing.JFileChooser#addChoosableFileFilter
 * @see javax.swing.JFileChooser#getFileFilter
 */
public class ProjectFolderFilter extends FileFilter{

	//description of this filter
	private final String description;


	/**
	 * Creates a {@code ProjectFolderFilter} with the specified description.
	 * <p>
	 * The returned {@code ProjectFolderFilter} will accept all directories.
	 *
	 * @param description	Textual description for the filter
	 * @see #accept
	 */
	public ProjectFolderFilter(final String description){
		Objects.requireNonNull(description, "Description cannot be null");

		this.description = description;
	}

	/**
	 * Tests the specified file, returning true if the file is
	 * accepted, false otherwise. True is returned if the extension
	 * matches one of the file name extensions of this {@code
	 * FileFilter}, or the file is a directory.
	 *
	 * @param f the {@code File} to test
	 * @return true if the file is to be accepted, false otherwise
	 */
	@Override
	public boolean accept(final File f){
		return (f != null && f.isDirectory());
	}


	/**
	 * The description of this filter.
	 *
	 * @return	The description of this filter
	 */
	@Override
	public String getDescription(){
		return description;
	}

}
