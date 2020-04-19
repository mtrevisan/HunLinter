package unit731.hunlinter.gui;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunlinter.gui.dialogs.FontChooserDialog;
import unit731.hunlinter.parsers.vos.AffixEntry;
import unit731.hunlinter.workers.core.WorkerAbstract;
import unit731.hunlinter.services.RegexHelper;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


public class GUIUtils{

	private static final String CLIENT_PROPERTY_KEY_FONTABLE = "fontable";

	private static final Pattern PATTERN_HTML_CODE = RegexHelper.pattern("</?[^>]+>");

	private static final String GRAPHEME_I = "i";
	private static final String GRAPHEME_M = "m";
	private static final FontRenderContext FRC = new FontRenderContext(null, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
		RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);

	private static final String KEY_UNDO = "Undo";
	private static final String KEY_REDO = "Redo";


	private static String LANGUAGE_SAMPLE;
	private static final List<String> FAMILY_NAMES_ALL = new ArrayList<>();
	private static final List<String> FAMILY_NAMES_MONOSPACED = new ArrayList<>();
	private static Font CURRENT_FONT = FontChooserDialog.getDefaultFont();

	private static final class WidthFontPair{
		private final double width;
		private final Font font;

		public static WidthFontPair of(final double width, final Font font){
			return new WidthFontPair(width, font);
		}

		private WidthFontPair(final double width, final Font font){
			this.width = width;
			this.font = font;
		}

		public double getWidth(){
			return width;
		}

		public Font getFont(){
			return font;
		}

		@Override
		public String toString(){
			return width + ": " + font.getName();
		}

		@Override
		public boolean equals(final Object obj){
			if(obj == this)
				return true;
			if(obj == null || obj.getClass() != getClass())
				return false;

			final WidthFontPair rhs = (WidthFontPair)obj;
			return new EqualsBuilder()
				.append(font, rhs.font)
				.isEquals();
		}

		@Override
		public int hashCode(){
			return new HashCodeBuilder()
				.append(font)
				.toHashCode();
		}
	}


	private GUIUtils(){}

	public static Font chooseBestFont(final String languageSample){
		Font bestFont = CURRENT_FONT;
		if(!canCurrentFontDisplay(languageSample)){
			//check to see if the error can be visualized, if not, change the font to one that can
			extractFonts(languageSample);

			final Function<String, WidthFontPair> widthFontPair = fontName -> {
				final Font currentFont = new Font(fontName, Font.PLAIN, GUIUtils.CURRENT_FONT.getSize());
				final double w = getStringBounds(currentFont, languageSample)
					.getWidth();
				return WidthFontPair.of(w, currentFont);
			};
			final List<String> fontNames = (FAMILY_NAMES_MONOSPACED.isEmpty()? FAMILY_NAMES_ALL: FAMILY_NAMES_MONOSPACED);

			WidthFontPair bestPair = null;
			for(final String fontName : fontNames){
				final WidthFontPair doubleFontPair = widthFontPair.apply(fontName);
				if(bestPair == null || doubleFontPair.getWidth() > bestPair.getWidth())
					bestPair = doubleFontPair;
			}
			bestFont = (bestPair != null? bestPair.getFont(): CURRENT_FONT);
		}
		return getDefaultHeightFont(bestFont);
	}

	public static Font getDefaultHeightFont(final Font font){
		final Rectangle2D bounds = getStringBounds(font, "I");
		return font.deriveFont(Math.round(font.getSize() * 17.f / bounds.getHeight()));
	}

	private static Rectangle2D getStringBounds(final Font font, final String text){
		return font.getStringBounds(text, FRC);
	}

	public static void extractFonts(final String languageSample){
		Objects.requireNonNull(languageSample);

		if(!languageSample.equals(GUIUtils.LANGUAGE_SAMPLE)){
			GUIUtils.LANGUAGE_SAMPLE = languageSample;

			FAMILY_NAMES_ALL.clear();
			FAMILY_NAMES_MONOSPACED.clear();

			final String[] familyNames = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames();
			for(final String familyName : familyNames){
				final Font font = new Font(familyName, Font.PLAIN, 20);
				if(font.canDisplayUpTo(languageSample) < 0){
					FAMILY_NAMES_ALL.add(familyName);
					if(isMonospaced(font))
						FAMILY_NAMES_MONOSPACED.add(familyName);
				}
			}
		}
	}

	public static boolean canCurrentFontDisplay(final String languageSample){
		return (CURRENT_FONT.canDisplayUpTo(languageSample) < 0);
	}

	private static boolean isMonospaced(final Font font){
		final double iWidth = font.getStringBounds(GRAPHEME_I, FRC).getWidth();
		final double mWidth = font.getStringBounds(GRAPHEME_M, FRC).getWidth();
		return (Math.abs(iWidth - mWidth) <= 1);
	}

	public static List<String> getFamilyNamesAll(){
		return FAMILY_NAMES_ALL;
	}

	public static List<String> getFamilyNamesMonospaced(){
		return FAMILY_NAMES_MONOSPACED;
	}

	public static Font getCurrentFont(){
		return CURRENT_FONT;
	}

	public static void addFontableProperty(final JComponent... components){
		forEach(components, component -> component.putClientProperty(CLIENT_PROPERTY_KEY_FONTABLE, true));
	}

	public static void setCurrentFont(final Font font, final Component... parentFrames){
		if(!font.equals(CURRENT_FONT)){
			CURRENT_FONT = font;

			forEach(parentFrames, parentFrame -> updateComponent(parentFrame, font));
		}
	}

	private static void updateComponent(final Component component, final Font font){
		final Stack<Component> stack = new Stack<>();
		stack.push(component);
		while(!stack.isEmpty()){
			final Component comp = stack.pop();

			if(comp instanceof JEditorPane)
				((JEditorPane)comp).putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			if((comp instanceof JComponent) && ((JComponent)comp).getClientProperty(CLIENT_PROPERTY_KEY_FONTABLE) == Boolean.TRUE)
				comp.setFont(font);

			if(comp instanceof Container)
				forEach(((Container)comp).getComponents(), stack::push);
		}
	}


	/**
	 * Force the escape key to call the same action as pressing the Cancel button.
	 *
	 * @param dialog	Dialog to attach the escape key to
	 */
	public static void addCancelByEscapeKey(final JDialog dialog){
		addCancelByEscapeKey(dialog, new AbstractAction(){
			private static final long serialVersionUID = -5644390861803492172L;

			@Override
			public void actionPerformed(ActionEvent e){
				dialog.dispose();
			}
		});
	}

	/**
	 * Force the escape key to call the same action as pressing the Cancel button.
	 *
	 * @param dialog	Dialog to attach the escape key to
	 * @param cancelAction	Action to be performed on cancel
	 */
	public static void addCancelByEscapeKey(final JDialog dialog, final AbstractAction cancelAction){
		final KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		dialog.getRootPane()
			.registerKeyboardAction(cancelAction, escapeKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}


	public static JMenuItem createPopupMenu(final String text, final char mnemonic, final String iconURL, final int iconSize,
			final JPopupMenu popupMenu, final Consumer<Component> fnCallback) throws IOException{
		final JMenuItem menuItem = new JMenuItem(text, mnemonic);
		final BufferedImage img = ImageIO.read(GUIUtils.class.getResourceAsStream(iconURL));
		final ImageIcon icon = new ImageIcon(img.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));
		menuItem.setIcon(icon);
		menuItem.addActionListener(e -> fnCallback.accept(popupMenu.getInvoker()));
		return menuItem;
	}

	public static JMenuItem createPopupMergeMenu(final int iconSize, final JPopupMenu popupMenu, final Consumer<Component> fnMerge)
			throws IOException{
		return createPopupMenu("Merge", 'M', "/popup_add.png", iconSize, popupMenu, fnMerge);
	}

	public static JMenuItem createPopupCopyMenu(final int iconSize, final JPopupMenu popupMenu, final Consumer<Component> fnCopy)
			throws IOException{
		return createPopupMenu("Copy", 'C', "/popup_copy.png", iconSize, popupMenu, fnCopy);
	}

	public static JMenuItem createPopupRemoveMenu(final int iconSize, final JPopupMenu popupMenu, final Consumer<Component> fnDelete)
			throws IOException{
		return createPopupMenu("Remove", 'R', "/popup_delete.png", iconSize, popupMenu, fnDelete);
	}

	public static void copyCallback(final Component invoker){
		String textToCopy = null;
		if(invoker instanceof JTextComponent)
			textToCopy = ((JTextComponent)invoker).getText();
		else if(invoker instanceof JLabel)
			textToCopy = ((JLabel)invoker).getText();
		else if(invoker instanceof JCopyableTable){
			final int selectedRow = ((JCopyableTable)invoker).convertRowIndexToModel(((JCopyableTable)invoker).getSelectedRow());
			textToCopy = ((JCopyableTable)invoker).getValueAtRow(selectedRow);
		}

		if(textToCopy != null)
			copyToClipboard(textToCopy);
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


	public static String removeHTMLCode(final String text){
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
							final int selectedRow = ((JCopyableTable)field).rowAtPoint(e.getPoint());
							((JCopyableTable)field).setRowSelectionInterval(selectedRow, selectedRow);
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
		Objects.requireNonNull(parentComponent);

		worker.pause();

		final Object[] options = {"Abort", "Cancel"};
		final int answer = JOptionPane.showOptionDialog(parentComponent,
			"Do you really want to abort the " + worker.getWorkerData().getWorkerName() + " task?", "Warning!",
			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		if(answer == JOptionPane.YES_OPTION){
			worker.cancel();

			System.gc();

			Optional.ofNullable(onAbort)
				.ifPresent(Runnable::run);
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
			private static final long serialVersionUID = -6536021676834946105L;

			@Override
			public void actionPerformed(ActionEvent evt){
				if(undo.canUndo()){
					try{
						undo.undo();
					}
					catch(final CannotUndoException ignored){}
				}
			}
		});
		//create a redo action and add it to the text component
		actionMap.put(KEY_REDO, new AbstractAction(KEY_REDO){
			private static final long serialVersionUID = -6536021676834946105L;

			@Override
			public void actionPerformed(ActionEvent evt){
				if(undo.canRedo()){
					try{
						undo.redo();
					}
					catch(final CannotRedoException ignored){}
				}
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
		dicSorter.setComparator(0, comparator);
		dicSorter.setComparator(1, comparator);
		if(table.getColumnModel().getColumnCount() > 2){
			dicSorter.setComparator(2, comparatorAffix);
			dicSorter.setComparator(3, comparatorAffix);
			dicSorter.setComparator(4, comparatorAffix);
		}
		table.setRowSorter(dicSorter);
	}


	//Extract parent frame from menu item
	public static Frame getParentFrame(final JMenuItem menuItem){
		final JPopupMenu popupMenu = (JPopupMenu)menuItem.getParent();
		final JComponent menu = (JComponent)popupMenu.getInvoker();
		return (Frame)menu.getTopLevelAncestor();
	}

}
