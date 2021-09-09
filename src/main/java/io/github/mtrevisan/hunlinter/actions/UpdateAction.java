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
package io.github.mtrevisan.hunlinter.actions;

import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.gui.dialogs.FileDownloaderDialog;
import io.github.mtrevisan.hunlinter.services.downloader.VersionException;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;


public class UpdateAction extends AbstractAction{

	@Serial
	private static final long serialVersionUID = -624514803595503205L;

	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAction.class);


	public UpdateAction(){
		super("system.update");
	}

	@Override
	public final void actionPerformed(final ActionEvent event){
		MenuSelectionManager.defaultManager().clearSelectedPath();

		final Frame parentFrame = GUIHelper.getParentFrame((JMenuItem)event.getSource());
		try{
			final FileDownloaderDialog dialog = new FileDownloaderDialog(parentFrame);
			GUIHelper.addCancelByEscapeKey(dialog, new CancelAction(dialog));
			dialog.setLocationRelativeTo(parentFrame);
			dialog.setVisible(true);
		}
		catch(final NoRouteToHostException | UnknownHostException e){
			final String message = "Connection failed.\r\nPlease check network connection and try again.";
			LOGGER.warn(message);

			JOptionPane.showMessageDialog(parentFrame, message, "Application update",
				JOptionPane.WARNING_MESSAGE);
		}
		catch(final IOException | ParseException | VersionException e){
			final String message = e.getMessage();
			LOGGER.info(message);

			JOptionPane.showMessageDialog(parentFrame, message, "Application update",
				JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private static final class CancelAction extends AbstractAction{
		@Serial
		private static final long serialVersionUID = -5644390861803492172L;
		private final FileDownloaderDialog dialog;

		private CancelAction(FileDownloaderDialog dialog){this.dialog = dialog;}

		@Override
		public void actionPerformed(final ActionEvent e){
			dialog.interrupt();

			dialog.dispose();
		}


		@Override
		protected Object clone() throws CloneNotSupportedException{
			throw new CloneNotSupportedException();
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
	}


	@Override
	protected final Object clone() throws CloneNotSupportedException{
		throw new CloneNotSupportedException();
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

}
