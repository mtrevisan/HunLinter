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
package io.github.mtrevisan.hunlinter.gui;

import io.github.mtrevisan.hunlinter.parsers.vos.AffixEntry;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.core.WorkerAbstract;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.regex.Pattern;


public final class GUIHelper{

	private static final Pattern PATTERN_HTML_CODE = RegexHelper.pattern("</?[^>]+>");

	private static final String KEY_UNDO = "Undo";
	private static final String KEY_REDO = "Redo";


	private GUIHelper(){}


	/**
	 * Force the escape key to call the same action as pressing the Cancel button.
	 *
	 * @param dialog	Dialog to attach the escape key to
	 */
	public static void addCancelByEscapeKey(final JDialog dialog){
		addCancelByEscapeKey(dialog, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -5644390861803492172L;

			@Override
			public void actionPerformed(final ActionEvent e){
				dialog.dispose();
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
		});
	}

	/**
	 * Force the escape key to call the same action as pressing the Cancel button.
	 *
	 * @param dialog	Dialog to attach the escape key to
	 * @param cancelAction	Action to be performed on cancel
	 */
	public static void addCancelByEscapeKey(final JDialog dialog, final ActionListener cancelAction){
		final KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		dialog.getRootPane()
			.registerKeyboardAction(cancelAction, escapeKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}


	public static JMenuItem createPopupMenuItem(final String text, final char mnemonic, final String iconURL, final JPopupMenu popupMenu,
			final Consumer<Component> fnCallback) throws IOException{
		final MyMenuItem menuItem = new MyMenuItem(text, mnemonic);
		if(iconURL != null){
			@SuppressWarnings("ConstantConditions")
			final BufferedImage img = ImageIO.read(GUIHelper.class.getResourceAsStream(iconURL));
			menuItem.setIcon(img);
		}
		menuItem.addActionListener(e -> fnCallback.accept(popupMenu.getInvoker()));
		return menuItem;
	}

	private static final class MyMenuItem extends JMenuItem{
		private BufferedImage icon;

		private MyMenuItem(final String text, final int mnemonic){
			super(text, mnemonic);
		}

		public void setIcon(final BufferedImage icon){
			this.icon = icon;
		}

		@Override
		public void paintComponent(final Graphics g){
			super.paintComponent(g);

			final int height = getHeight();
			final int iconSize = height * 17 / 26;
			final int offset = (height - iconSize) >> 1;
			g.drawImage(icon, offset, offset, iconSize, iconSize,this);
		}
	}

	public static JMenuItem createPopupMenuItem(final String text, final boolean selected, final JPopupMenu popupMenu,
			final Consumer<Component> fnCallback){
		final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text, selected);
		menuItem.addActionListener(e -> fnCallback.accept(popupMenu.getInvoker()));
		return menuItem;
	}

	public static JMenuItem createPopupMergeMenu(final JPopupMenu popupMenu, final Consumer<Component> fnMerge)
			throws IOException{
		return createPopupMenuItem("Merge", 'M', "/popup_add.png", popupMenu, fnMerge);
	}

	public static JMenuItem createPopupCopyMenu(final JPopupMenu popupMenu, final Consumer<Component> fnCopy)
			throws IOException{
		return createPopupMenuItem("Copy", 'C', "/popup_copy.png", popupMenu, fnCopy);
	}

	public static JMenuItem createPopupExportTableMenu(final JPopupMenu popupMenu, final Consumer<Component> fnExport)
			throws IOException{
		return createPopupMenuItem("Export", 'E', "/popup_export.png", popupMenu, fnExport);
	}

	public static JMenuItem createPopupRemoveMenu(final JPopupMenu popupMenu, final Consumer<Component> fnDelete)
			throws IOException{
		return createPopupMenuItem("Remove", 'R', "/popup_delete.png", popupMenu, fnDelete);
	}

	public static JMenuItem createCheckBoxMenu(final String text, final boolean selected, final JPopupMenu popupMenu,
			final Consumer<Component> fnCallback){
		return createPopupMenuItem(text, selected, popupMenu, fnCallback);
	}

	public static void copyCallback(final Component invoker){
		String textToCopy = null;
		if(invoker instanceof JTextComponent)
			textToCopy = ((JTextComponent)invoker).getText();
		else if(invoker instanceof JLabel)
			textToCopy = ((JLabel)invoker).getText();
		else if(invoker instanceof JCopyableTable){
			final int selectedRow = ((JTable)invoker).convertRowIndexToModel(((JTable)invoker).getSelectedRow());
			textToCopy = ((JCopyableTable)invoker).getValueAtRow(selectedRow);
		}

		if(textToCopy != null)
			copyToClipboard(textToCopy);
	}

	public static void exportTableCallback(final Component invoker){
		if(invoker instanceof JCopyableTable){
			final StringJoiner sj = new StringJoiner(StringUtils.LF);
			final int rows = ((JTable)invoker).getModel().getRowCount();
			for(int row = 0; row < rows; row ++){
				final String textToCopy = ((JCopyableTable)invoker).getValueAtRow(row);
				sj.add(textToCopy);
			}
			if(sj.length() > 0)
				copyToClipboard(sj.toString());
		}
		else
			copyCallback(invoker);
	}

	public static void copyToClipboard(final JCopyableTable table){
		final int selectedRow = table.convertRowIndexToModel(table.getSelectedRow());
		final String textToCopy = table.getValueAtRow(selectedRow);
		copyToClipboard(textToCopy);
	}

	public static void copyToClipboard(String textToCopy){
		textToCopy = removeHTMLCode(textToCopy);

		final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(textToCopy), null);
	}


	public static String removeHTMLCode(final CharSequence text){
		return RegexHelper.clear(text, PATTERN_HTML_CODE);
	}

	/**
	 * Add a popup menu to the specified text fields.
	 *
	 * @param popupMenu	The pop-up to attach to the fields
	 * @param fields	Components for which to add the popup menu
	 */
	public static void addPopupMenu(final JPopupMenu popupMenu, final JComponent... fields){
		//add mouse listeners to the specified fields
		for(final JComponent field : fields){
			field.addMouseListener(new MouseAdapter(){
				@Override
				public void mouseClicked(final MouseEvent e){
					processMouseEvent(e);
				}

				@Override
				public void mouseReleased(final MouseEvent e){
					processMouseEvent(e);
				}

				private void processMouseEvent(final MouseEvent e){
					if(e.isPopupTrigger()){
						//select row
						if(field instanceof JCopyableTable){
							final int selectedRow = ((JTable)field).rowAtPoint(e.getPoint());
							((JTable)field).setRowSelectionInterval(selectedRow, selectedRow);
						}

						popupMenu.show(e.getComponent(), e.getX(), e.getY());
						popupMenu.setInvoker(field);
					}
				}
			});
		}
	}


	public static void askUserToAbort(final WorkerAbstract<?> worker, final Component parentComponent, final Runnable onAbort,
			final Runnable resumeTask){
		Objects.requireNonNull(parentComponent, "Parent component cannot be null");

		worker.pause();

		final Object[] options = {"Abort", "Cancel"};
		final int answer = JOptionPane.showOptionDialog(parentComponent,
			"Do you really want to abort the " + worker.getWorkerData().getWorkerName() + " task?", "Warning!",
			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		if(answer == JOptionPane.YES_OPTION){
			Optional.ofNullable(onAbort)
				.ifPresent(Runnable::run);

			worker.cancel();
		}
		else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION){
			worker.resume();

			Optional.ofNullable(resumeTask)
				.ifPresent(Runnable::run);
		}
	}


	public static void addUndoManager(final JTextComponent... fields){
		for(final JTextComponent field : fields)
			addUndoManager(field);
	}

	public static void addUndoManager(final JTextComponent field){
		final UndoManager undo = new UndoManager();

		field.getDocument().addUndoableEditListener(evt -> undo.addEdit(evt.getEdit()));

		final ActionMap actionMap = field.getActionMap();
		//create an undo action and add it to the text component
		actionMap.put(KEY_UNDO, new AbstractAction(KEY_UNDO){
			@Serial
			private static final long serialVersionUID = -6536021676834946105L;

			@Override
			public void actionPerformed(final ActionEvent evt){
				if(undo.canUndo()){
					try{
						undo.undo();
					}
					catch(final CannotUndoException ignored){}
				}
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
		});
		//create a redo action and add it to the text component
		actionMap.put(KEY_REDO, new AbstractAction(KEY_REDO){
			@Serial
			private static final long serialVersionUID = -6536021676834946105L;

			@Override
			public void actionPerformed(final ActionEvent evt){
				if(undo.canRedo()){
					try{
						undo.redo();
					}
					catch(final CannotRedoException ignored){}
				}
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
		});

		final InputMap inputMap = field.getInputMap();
		//bind the undo action to Ctrl-Z
		inputMap.put(KeyStroke.getKeyStroke('Z', InputEvent.CTRL_DOWN_MASK), KEY_UNDO);
		//bind the redo action to Ctrl-Y
		inputMap.put(KeyStroke.getKeyStroke('Y', InputEvent.CTRL_DOWN_MASK), KEY_REDO);
	}


	public static void addSorterToTable(final JTable table, final Comparator<String> comparator,
			final Comparator<AffixEntry> comparatorAffix){
		final TableRowSorter<TableModel> dicSorter = new AscendingDescendingUnsortedTableRowSorter<>(table.getModel());
		//inflection
		dicSorter.setComparator(0, comparator);
		//morphological fields
		dicSorter.setComparator(1, comparator);
		if(table.getColumnModel().getColumnCount() > 2){
			//applied rule 1
			dicSorter.setComparator(2, comparatorAffix);
			//applied rule 2
			dicSorter.setComparator(3, comparatorAffix);
			//applied rule 3
			dicSorter.setComparator(4, comparatorAffix);
		}
		table.setRowSorter(dicSorter);
	}

	public static void addScrollToFirstRow(final JTable table){
		final KeyStroke homeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0, false);
		table.registerKeyboardAction(event -> {
			final int index = table.convertRowIndexToView(0);
			table.changeSelection(index, 0, false, false);
		}, homeKeyStroke, JComponent.WHEN_FOCUSED);
	}

	public static void addScrollToLastRow(final JTable table){
		final KeyStroke endKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_END, 0, false);
		table.registerKeyboardAction(event -> {
			final int index = table.convertRowIndexToView(table.getModel().getRowCount() - 1);
			table.changeSelection(index, 0, false, false);

			//hack (repeat command)
			JavaHelper.delayedRun(() -> table.changeSelection(index, 0, false, false), 10);
		}, endKeyStroke, JComponent.WHEN_FOCUSED);
	}


	//Extract parent frame from menu item
	public static Frame getParentFrame(final JMenuItem menuItem){
		final JPopupMenu popupMenu = (JPopupMenu)menuItem.getParent();
		final JComponent menu = (JComponent)popupMenu.getInvoker();
		return (Frame)menu.getTopLevelAncestor();
	}

}
