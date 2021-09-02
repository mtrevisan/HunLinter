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
package io.github.mtrevisan.hunlinter.services.filelistener;


public interface FileListener{

	/** Starts the service. This method blocks until the service has completely started. */
	void start();

	/** Stops the service. This method blocks until the service has completely shut down. */
	void stop();

	/**
	 * Notifies the implementation of <em>this</em> interface that {@code dirPath}
	 * should be monitored for file system events. If the changed file matches any
	 * of the {@code globPatterns}, {@code listener} should be notified.
	 *
	 * @param listener	The listener.
	 * @param patterns	Zero or more file patterns to be matched against file names.
	 *							If none provided, matches <em>any</em> file.
	 */
	void register(FileChangeListener listener, String... patterns);

	/** Removes all listeners. */
	void unregisterAll();

}
