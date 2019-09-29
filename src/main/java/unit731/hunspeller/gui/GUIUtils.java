package unit731.hunspeller.gui;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;

import unit731.hunspeller.JFontChooserDialog;
import unit731.hunspeller.services.PatternHelper;


public class GUIUtils{

	private static final Pattern PATTERN_HTML_CODE = PatternHelper.pattern("</?[^>]+>");

	private static Font currentFont = JFontChooserDialog.getDefaultFont();


	private GUIUtils(){}

	public static Font getCurrentFont(){
		return currentFont;
	}

	public static void setCurrentFont(final Font font, final Component parentFrame){
		if(!font.equals(currentFont)){
			updateComponent(parentFrame, font);

			currentFont = font;
		}
	}

	private static void updateComponent(final Component c, final Font font){
		if(c != null){
			if(c instanceof JComponent)
				((JComponent)c).updateUI();
			if(c instanceof Container){
				final Component[] children = ((Container)c).getComponents();
				if(children != null)
					for(final Component child : children)
						updateComponent(child, font);
			}
			if(c instanceof JEditorPane)
				((JEditorPane)c).putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

			if(c instanceof JTextArea || c instanceof JTextField || c instanceof JTable || c instanceof JWordLabel)
				c.setFont(font);
		}
	}

	/**
	 * Force the escape key to call the same action as pressing the Cancel button.
	 *
	 * @param dialog	Dialog to attach the escape key to
	 */
	public static void addCancelByEscapeKey(JDialog dialog){
		AbstractAction cancelAction = new AbstractAction(){
			private static final long serialVersionUID = -5644390861803492172L;

			@Override
			public void actionPerformed(ActionEvent e){
				dialog.dispose();
			}
		};
		KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false);
		dialog.getRootPane().registerKeyboardAction(cancelAction, escapeKey, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	public static JPopupMenu createCopyingPopupMenu(int iconSize) throws IOException{
		JPopupMenu popupMenu = new JPopupMenu();

		JMenuItem copyMenuItem = new JMenuItem("Copy", 'C');
		BufferedImage img = ImageIO.read(GUIUtils.class.getResourceAsStream("/popup_copy.png"));
		ImageIcon icon = new ImageIcon(img.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));
		copyMenuItem.setIcon(icon);
		copyMenuItem.addActionListener(e -> {
			String textToCopy = null;
			Component c = popupMenu.getInvoker();
			if(c instanceof JTextComponent)
				textToCopy = ((JTextComponent)c).getText();
			else if(c instanceof JLabel)
				textToCopy = ((JLabel)c).getText();

			if(textToCopy != null){
				textToCopy = removeHTMLCode(textToCopy);

				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(new StringSelection(textToCopy), null);
			}
		});
		popupMenu.add(copyMenuItem);

		return popupMenu;
	}

	private static String removeHTMLCode(String text){
		return PatternHelper.clear(text, PATTERN_HTML_CODE);
	}

	/**
	 * Add a popup menu to the specified text fields.
	 *
	 * @param popupMenu	The pop-up to attach to the fields
	 * @param fields	Components for which to add the popup menu
	 */
	public static void addPopupMenu(JPopupMenu popupMenu, JComponent... fields){
		//add mouse listeners to the specified fields
		for(JComponent field : fields){
			field.addMouseListener(new MouseAdapter(){
				@Override
				public void mousePressed(MouseEvent e){
					processMouseEvent(e);
				}

				@Override
				public void mouseReleased(MouseEvent e){
					processMouseEvent(e);
				}

				private void processMouseEvent(MouseEvent e){
					if(e.isPopupTrigger()){
						popupMenu.show(e.getComponent(), e.getX(), e.getY());
						popupMenu.setInvoker(field);
					}
				}
			});
		}
	}

}
