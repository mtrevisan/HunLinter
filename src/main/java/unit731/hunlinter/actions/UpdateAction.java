package unit731.hunlinter.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.FileDownloaderDialog;
import unit731.hunlinter.gui.GUIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;


public class UpdateAction extends AbstractAction{

	private static final long serialVersionUID = -624514803595503205L;

	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAction.class);


	public UpdateAction(){
		super("system.update");
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIUtils.getParentFrame((JMenuItem)event.getSource());
		try{
			final FileDownloaderDialog dialog = new FileDownloaderDialog(parentFrame);
			GUIUtils.addCancelByEscapeKey(dialog, new AbstractAction(){
				private static final long serialVersionUID = -5644390861803492172L;

				@Override
				public void actionPerformed(ActionEvent e){
					dialog.interrupt();

					dialog.dispose();
				}
			});
			dialog.setLocationRelativeTo(parentFrame);
			dialog.setVisible(true);
		}
		catch(final NoRouteToHostException | UnknownHostException e){
			final String message = "Connection failed.\r\nPlease check network connection and try again.";
			LOGGER.warn(message);

			JOptionPane.showMessageDialog(parentFrame, message, "Application update",
				JOptionPane.WARNING_MESSAGE);
		}
		catch(final Exception e){
			final String message = e.getMessage();
			LOGGER.info(message);

			JOptionPane.showMessageDialog(parentFrame, message, "Application update",
				JOptionPane.INFORMATION_MESSAGE);
		}
	}

}
