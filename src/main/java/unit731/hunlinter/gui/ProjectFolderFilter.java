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
		Objects.requireNonNull(description);

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
	public boolean accept(final File f){
		return (f != null && f.isDirectory());
	}


	/**
	 * The description of this filter.
	 *
	 * @return	The description of this filter
	 */
	public String getDescription() {
		return description;
	}

}
