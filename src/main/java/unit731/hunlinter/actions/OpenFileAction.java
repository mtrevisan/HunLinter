/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.services.system.FileHelper;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
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

		Objects.requireNonNull(fileSupplier);
		Objects.requireNonNull(packager);

		this.fileSupplier = fileSupplier;
		fileKey = null;
		this.packager = packager;
	}

	public OpenFileAction(final String fileKey, final Packager packager){
		super("system.open");

		Objects.requireNonNull(fileKey);
		Objects.requireNonNull(packager);

		fileSupplier = null;
		this.fileKey = fileKey;
		this.packager = packager;
	}

	@Override
	public void actionPerformed(final ActionEvent event){
		try{
			final File file = (fileSupplier != null? fileSupplier.get(): packager.getFile(fileKey));
			FileHelper.openFileWithChosenEditor(file);
		}
		catch(final IOException | InterruptedException e){
			LOGGER.warn("Exception while opening file", e);
		}
	}

}
