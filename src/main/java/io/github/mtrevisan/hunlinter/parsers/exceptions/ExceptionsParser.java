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
package io.github.mtrevisan.hunlinter.parsers.exceptions;

import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.services.XMLManager;
import io.github.mtrevisan.hunlinter.services.eventbus.EventBusService;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterWarning;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;


/**
 * `SentenceExceptList.xml` – Manages abbreviations that end with a fullstop that should be ignored when determining
 * 	the end of a sentence
 * `WordExceptList.xml` – Manages words that may contain more than 2 leading capital, e.g. `CDs`
 */
public class ExceptionsParser{

	private static final String DUPLICATED_ENTRY = "Duplicated entry in file {}: `{}`";
	private static final String INVALID_ROOT = "Invalid root element in file {}, expected `{}`, was `{}`";

	public enum TagChangeType{SET, ADD, REMOVE, CLEAR}

	private static final String AUTO_CORRECT_NAMESPACE = "block-list:";
	private static final String WORD_EXCEPTIONS_ROOT_ELEMENT = AUTO_CORRECT_NAMESPACE + "block-list";
	private static final String AUTO_CORRECT_BLOCK = AUTO_CORRECT_NAMESPACE + "block";
	private static final String WORD_EXCEPTIONS_WORD = AUTO_CORRECT_NAMESPACE + "abbreviated-name";


	private final String configurationFilename;
	private final List<String> dictionary = new ArrayList<>(0);
	private Comparator<String> comparator;

	private final XMLManager xmlManager = new XMLManager();


	public ExceptionsParser(final String configurationFilename){
		this.configurationFilename = configurationFilename;
	}

	/**
	 * Parse the rows out from a `SentenceExceptList.xml` or a `WordExceptList.xml` file.
	 *
	 * @param wexFile	The reference to the word exceptions file.
	 * @param language	The language (used to sort).
	 * @throws IOException	If an I/O error occurs.
	 * @throws SAXException	If a parsing error occurs on the `xml` file.
	 */
	public final void parse(final File wexFile, final String language) throws IOException, SAXException{
		comparator = BaseBuilder.getComparator(language);

		clear();

		final Document doc = xmlManager.parseXMLDocument(wexFile);

		final Element rootElement = doc.getDocumentElement();
		if(!WORD_EXCEPTIONS_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new LinterException(INVALID_ROOT, configurationFilename, WORD_EXCEPTIONS_ROOT_ELEMENT, rootElement.getNodeName());

		final List<Node> children = XMLManager.extractChildren(rootElement, node -> XMLManager.isElement(node, AUTO_CORRECT_BLOCK));
		for(int i = 0; i < children.size(); i ++){
			final Node mediaType = XMLManager.extractAttribute(children.get(i), WORD_EXCEPTIONS_WORD);
			if(mediaType != null)
				dictionary.add(mediaType.getNodeValue());
		}
		dictionary.sort(comparator);

		validate();
	}

	private void validate(){
		//check for duplications
		int index = 0;
		final Collection<String> map = new HashSet<>(dictionary.size());
		for(int i = 0; i < dictionary.size(); i ++){
			final String exception = dictionary.get(i);
			if(!map.add(exception))
				EventBusService.publish(new LinterWarning(DUPLICATED_ENTRY, configurationFilename, exception)
					.withIndex(index));

			index ++;
		}
	}

	public final List<String> getExceptionsDictionary(){
		return dictionary;
	}

	public final int getExceptionsCounter(){
		return dictionary.size();
	}

	public final void modify(final TagChangeType changeType, final Collection<String> tags){
		switch(changeType){
			case ADD -> {
				dictionary.addAll(tags);
				dictionary.sort(comparator);
			}
			case REMOVE -> dictionary.removeAll(tags);
			case SET -> {
				dictionary.clear();
				dictionary.addAll(tags);
			}
		}
	}

	public final boolean contains(final String exception){
		return dictionary.contains(exception);
	}

	public final void save(final File excFile) throws TransformerException{
		final Document doc = xmlManager.newXMLDocumentStandalone();

		//root element
		final Element root = doc.createElement(WORD_EXCEPTIONS_ROOT_ELEMENT);
		root.setAttribute(XMLManager.ROOT_ATTRIBUTE_NAME, XMLManager.ROOT_ATTRIBUTE_VALUE);
		doc.appendChild(root);

		for(int i = 0; i < dictionary.size(); i ++){
			//correction element
			final Element elem = doc.createElement(AUTO_CORRECT_BLOCK);
			elem.setAttribute(WORD_EXCEPTIONS_WORD, dictionary.get(i));
			root.appendChild(elem);
		}

		XMLManager.createXML(excFile, doc, XMLManager.XML_PROPERTIES_UTF_8);
	}

	public final void clear(){
		dictionary.clear();
	}

}
