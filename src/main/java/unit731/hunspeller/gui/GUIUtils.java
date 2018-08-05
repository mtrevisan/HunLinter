package unit731.hunspeller.gui;

import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.regex.Matcher;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import unit731.hunspeller.services.PatternService;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GUIUtils{

	private static final Matcher MATCHER_HTML_CODE = PatternService.matcher("</?[^>]+>");


	public static JPopupMenu createCopyingPopupMenu(int iconSize){
		JPopupMenu popupMenu = new JPopupMenu();

		JMenuItem copyMenuItem = new JMenuItem("Copy", 'C');
		try{
			BufferedImage img = ImageIO.read(GUIUtils.class.getResourceAsStream("/popup_copy.png"));
			ImageIcon icon = new ImageIcon(img.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH));
			copyMenuItem.setIcon(icon);
		}
		catch(IOException e){}
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
		return PatternService.clear(text, MATCHER_HTML_CODE);
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
