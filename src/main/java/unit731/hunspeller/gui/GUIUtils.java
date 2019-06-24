package unit731.hunspeller.gui;

import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.JTextComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.workers.core.WorkerDictionaryBase;
import unit731.hunspeller.services.PatternHelper;


public class GUIUtils{

	private static final Logger LOGGER = LoggerFactory.getLogger(GUIUtils.class);

	private static final Pattern PATTERN_HTML_CODE = PatternHelper.pattern("</?[^>]+>");


	private GUIUtils(){}

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

	public static void askUserToAbort(WorkerDictionaryBase worker, Component parentComponent, Runnable cancelTask, Runnable resumeTask, Runnable notRunningTask){
		if(worker != null && worker.getState() == SwingWorker.StateValue.STARTED){
			Objects.requireNonNull(parentComponent);
			Objects.requireNonNull(cancelTask);
			Objects.requireNonNull(resumeTask);

			worker.pause();

			Object[] options = {"Abort", "Cancel"};
			int answer = JOptionPane.showOptionDialog(parentComponent, "Do you really want to abort the " + worker.getWorkerName() + " task?", "Warning!",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
			if(answer == JOptionPane.YES_OPTION){
				worker.cancel();

				cancelTask.run();

				LOGGER.info(Backbone.MARKER_APPLICATION, worker.getWorkerName() + " aborted");
			}
			else if(answer == JOptionPane.NO_OPTION || answer == JOptionPane.CLOSED_OPTION){
				worker.resume();

				resumeTask.run();
			}
		}
		else if(notRunningTask != null)
			notRunningTask.run();
	}

}
