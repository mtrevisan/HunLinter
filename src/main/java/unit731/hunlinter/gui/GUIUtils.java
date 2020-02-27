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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import unit731.hunlinter.FontChooserDialog;
import unit731.hunlinter.workers.core.WorkerAbstract;
import unit731.hunlinter.services.system.JavaHelper;
import unit731.hunlinter.services.PatternHelper;


public class GUIUtils{

	private static final String CLIENT_PROPERTY_KEY_FONTABLE = "fontable";

	private static final Pattern PATTERN_HTML_CODE = PatternHelper.pattern("</?[^>]+>");

	private static final String GRAPHEME_I = "i";
	private static final String GRAPHEME_M = "m";
	private static final FontRenderContext FRC = new FontRenderContext(null, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
		RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);

	private static final String KEY_UNDO = "Undo";
	private static final String KEY_REDO = "Redo";

	private static String languageSample;
	private static final List<String> familyNamesAll = new ArrayList<>();
	private static final List<String> familyNamesMonospaced = new ArrayList<>();
	private static Font currentFont = FontChooserDialog.getDefaultFont();


	private GUIUtils(){}

	public static Font chooseBestFont(final String languageSample){
		Font bestFont = currentFont;
		if(!canCurrentFontDisplay(languageSample)){
			//check to see if the error can be visualized, if not, change the font to one that can
			extractFonts(languageSample);

			final List<String> list = (familyNamesMonospaced.isEmpty()? familyNamesAll: familyNamesMonospaced);
			double width = 0.;
			for(final String elem : list){
				final Font currentFont = new Font(elem, Font.PLAIN, GUIUtils.currentFont.getSize());
				final Rectangle2D bounds = getStringBounds(currentFont, languageSample);
				final double w = bounds.getWidth();
				if(w > width){
					bestFont = currentFont;
					width = w;
				}
			}
		}
		return getDefaultHeightFont(bestFont);
	}

	public static Font getDefaultHeightFont(final Font font){
		final Rectangle2D bounds = getStringBounds(font, "I");
		return font.deriveFont((float)Math.round(font.getSize() * 17.f / bounds.getHeight()));
	}

	private static Rectangle2D getStringBounds(final Font font, final String text){
		return font.getStringBounds(text, FRC);
	}

	public static void extractFonts(final String languageSample){
		Objects.requireNonNull(languageSample);

		if(!languageSample.equals(GUIUtils.languageSample)){
			GUIUtils.languageSample = languageSample;

			familyNamesAll.clear();
			familyNamesMonospaced.clear();

			final String[] familyNames = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getAvailableFontFamilyNames();
			for(final String familyName : familyNames){
				final Font font = new Font(familyName, Font.PLAIN, 20);
				if(font.canDisplayUpTo(languageSample) < 0){
					familyNamesAll.add(familyName);
					if(isMonospaced(font))
						familyNamesMonospaced.add(familyName);
				}
			}
		}
	}

	public static boolean canCurrentFontDisplay(final String languageSample){
		return (currentFont.canDisplayUpTo(languageSample) < 0);
	}

	private static boolean isMonospaced(final Font font){
		final double iWidth = font.getStringBounds(GRAPHEME_I, FRC).getWidth();
		final double mWidth = font.getStringBounds(GRAPHEME_M, FRC).getWidth();
		return (Math.abs(iWidth - mWidth) <= 1);
	}

	public static List<String> getFamilyNamesAll(){
		return familyNamesAll;
	}

	public static List<String> getFamilyNamesMonospaced(){
		return familyNamesMonospaced;
	}

	public static Font getCurrentFont(){
		return currentFont;
	}

	public static void addFontableProperty(final JComponent... components){
		JavaHelper.nullableToStream(components)
			.forEach(component -> component.putClientProperty(CLIENT_PROPERTY_KEY_FONTABLE, true));
	}

	public static void setCurrentFont(final Font font, final Component... parentFrames){
		if(!font.equals(currentFont)){
			currentFont = font;

			JavaHelper.nullableToStream(parentFrames)
				.forEach(parentFrame -> updateComponent(parentFrame, font));
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
				Arrays.stream(((Container)comp).getComponents())
					.forEach(stack::push);
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
		return PatternHelper.clear(text, PATTERN_HTML_CODE);
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


	public static void askUserToAbort(final WorkerAbstract<?, ?> worker, final Component parentComponent, final Runnable onAbort,
			final Runnable resumeTask){
		Objects.requireNonNull(parentComponent);

		worker.pause();

		final Object[] options = {"Abort", "Cancel"};
		final int answer = JOptionPane.showOptionDialog(parentComponent,
			"Do you really want to abort the " + worker.getWorkerData().getWorkerName() + " task?", "Warning!",
			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
		if(answer == JOptionPane.YES_OPTION){
			worker.cancel();

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


	public static int setTabbedPaneEnable(final JTabbedPane tabbedPane, final Component component, final boolean enabled){
		final int index = tabbedPane.indexOfComponent(component);
		tabbedPane.setEnabledAt(index, enabled);
		return index;
	}

}
