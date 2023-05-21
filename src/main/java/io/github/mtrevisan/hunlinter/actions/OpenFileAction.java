/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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

import io.github.mtrevisan.hunlinter.services.Packager;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Objects;
import java.util.function.Supplier;


public class OpenFileAction extends AbstractAction{

	@Serial
	private static final long serialVersionUID = 5234230587473648899L;

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenFileAction.class);


	private final Supplier<File> fileSupplier;
	private final String fileKey;
	private final Packager packager;


	public OpenFileAction(final Supplier<File> fileSupplier, final Packager packager){
		super("system.open");

		Objects.requireNonNull(fileSupplier, "File supplier cannot be null");
		Objects.requireNonNull(packager, "Packager cannot be null");

		this.fileSupplier = fileSupplier;
		fileKey = null;
		this.packager = packager;
	}

	public OpenFileAction(final String fileKey, final Packager packager){
		super("system.open");

		Objects.requireNonNull(fileKey, "File key cannot be null");
		Objects.requireNonNull(packager, "Packager cannot be null");

		fileSupplier = null;
		this.fileKey = fileKey;
		this.packager = packager;
	}

	@Override
	public final void actionPerformed(final ActionEvent event){
		try{
			final File file = (fileSupplier != null? fileSupplier.get(): packager.getFile(fileKey));
			FileHelper.openFileWithChosenEditor(file);
		}
		catch(final IOException | InterruptedException e){
			LOGGER.warn("Exception while opening file", e);
		}
	}


	@Override
	@SuppressWarnings("NewExceptionWithoutArguments")
	protected final Object clone() throws CloneNotSupportedException{
		throw new CloneNotSupportedException();
	}

	@SuppressWarnings("unused")
	@Serial
	private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

	@SuppressWarnings("unused")
	@Serial
	private void readObject(final ObjectInputStream is) throws NotSerializableException{
		throw new NotSerializableException(getClass().getName());
	}

}
