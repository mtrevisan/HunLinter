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
package unit731.hunlinter.datastructures.fsa.stemming;

import unit731.hunlinter.datastructures.fsa.FSA;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * A dictionary combines {@link FSA} automaton and {@link DictionaryMetadata}
 * describing the way terms are encoded in the automaton.
 *
 * <p>
 * A dictionary consists of two files:
 * <ul>
 * <li>an actual compressed FSA file,
 * <li>{@link DictionaryMetadata}, describing the way terms are encoded.
 * </ul>
 *
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public class Dictionary{

	/**
	 * {@link FSA} automaton with the compiled dictionary data.
	 */
	public final FSA fsa;

	/**
	 * Metadata associated with the dictionary.
	 */
	public final DictionaryMetadata metadata;

	/**
	 * It is strongly recommended to use static methods in this class for
	 * reading dictionaries.
	 *
	 * @param fsa	An instantiated {@link FSA} instance.
	 * @param metadata	A map of attributes describing the compression format and other settings not contained in the FSA automaton. For an
	 *		explanation of available attributes and their possible values, see {@link DictionaryMetadata}.
	 */
	public Dictionary(final FSA fsa, final DictionaryMetadata metadata){
		this.fsa = fsa;
		this.metadata = metadata;
	}

	/**
	 * Attempts to load a dictionary using the path to the FSA file and the
	 * expected metadata extension.
	 *
	 * @param location The location of the dictionary file (<code>*.dict</code>).
	 * @return An instantiated dictionary.
	 * @throws IOException if an I/O error occurs.
	 */
	public static Dictionary read(final Path location) throws IOException{
		final Path metadata = DictionaryMetadata.getExpectedMetadataLocation(location);

		try(final InputStream fsaStream = Files.newInputStream(location); final InputStream metadataStream = Files.newInputStream(metadata)){
			return read(fsaStream, metadataStream);
		}
	}

	/**
	 * Attempts to load a dictionary using the URL to the FSA file and the
	 * expected metadata extension.
	 *
	 * @param dictURL The URL pointing to the dictionary file (<code>*.dict</code>).
	 * @return An instantiated dictionary.
	 * @throws IOException if an I/O error occurs.
	 */
	public static Dictionary read(final URL dictURL) throws IOException{
		final URL expectedMetadataURL;
		try{
			final String external = dictURL.toExternalForm();
			expectedMetadataURL = new URL(DictionaryMetadata.getExpectedMetadataFileName(external));
		}
		catch(final MalformedURLException e){
			throw new IOException("Couldn't construct relative feature map URL for: " + dictURL, e);
		}

		try(final InputStream fsaStream = dictURL.openStream(); final InputStream metadataStream = expectedMetadataURL.openStream()){
			return read(fsaStream, metadataStream);
		}
	}

	/**
	 * Attempts to load a dictionary from opened streams of FSA dictionary data
	 * and associated metadata. Input streams are not closed automatically.
	 *
	 * @param fsaStream	The stream with FSA data
	 * @param metadataStream	The stream with metadata
	 * @return	An instantiated {@link Dictionary}.
	 * @throws IOException	If an I/O error occurs.
	 */
	public static Dictionary read(final InputStream fsaStream, final InputStream metadataStream) throws IOException{
		return new Dictionary(FSA.read(fsaStream), DictionaryMetadata.read(metadataStream));
	}

}
