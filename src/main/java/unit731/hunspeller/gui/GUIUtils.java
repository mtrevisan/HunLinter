package unit731.hunspeller.gui;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;

import unit731.hunspeller.JFontChooserDialog;
import unit731.hunspeller.services.PatternHelper;


public class GUIUtils{

	private static final Pattern PATTERN_HTML_CODE = PatternHelper.pattern("</?[^>]+>");

	private static final String GRAPHEME_I = "i";
	private static final String GRAPHEME_M = "m";
	private static final FontRenderContext FRC = new FontRenderContext(null, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
		RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);

	private static String languageSample;
	private static final List<String> familyNamesAll = new ArrayList<>();
	private static final List<String> familyNamesMonospaced = new ArrayList<>();
	private static Font currentFont = JFontChooserDialog.getDefaultFont();


	private GUIUtils(){}

	public static Font chooseBestFont(final String languageSample){
		Font bestFont = currentFont;
		float height = 0.f;
		if(!canCurrentFondDisplay(languageSample) && familyNamesAll.isEmpty()){
			//check to see if the error can be visualized, if not, change the font to one that can
			extractFonts(languageSample);
			final List<String> list = (!familyNamesMonospaced.isEmpty()? familyNamesMonospaced: familyNamesAll);
			double width = 0.;
			for(final String elem : list){
				final Font currentFont = new Font(elem, Font.PLAIN, GUIUtils.currentFont.getSize());
				final Rectangle2D bounds = currentFont.getStringBounds(languageSample, FRC);
				final double w = bounds.getWidth();
				if(w > width){
					bestFont = currentFont;
					width = w;
					height = (float)bounds.getHeight();
				}
			}
		}
		return (height != 0.f? bestFont.deriveFont(bestFont.getSize() * 17.f / height): bestFont);
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

	public static boolean canCurrentFondDisplay(final String languageSample){
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

	public static void setCurrentFont(final Font font, final Component... parentFrames){
		if(!font.equals(currentFont)){
			for(final Component parentFrame : parentFrames)
				updateComponent(parentFrame, font);

			currentFont = font;
		}
	}

	private static void updateComponent(final Component component, final Font font){
		if(component != null){
			if(component instanceof JComponent)
				((JComponent)component).updateUI();
			if(component instanceof Container){
				final Component[] children = ((Container)component).getComponents();
				if(children != null)
					for(final Component child : children)
						updateComponent(child, font);
			}
			if(component instanceof JEditorPane)
				((JEditorPane)component).putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

			if(component instanceof JTextArea || component instanceof JComboBox || component instanceof JTextField
					|| component instanceof JTable || component instanceof JWordLabel)
				component.setFont(font);
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
