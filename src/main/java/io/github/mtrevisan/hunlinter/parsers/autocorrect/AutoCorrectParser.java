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
package io.github.mtrevisan.hunlinter.parsers.autocorrect;

import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenationParser;
import io.github.mtrevisan.hunlinter.parsers.thesaurus.DuplicationResult;
import io.github.mtrevisan.hunlinter.services.RegexHelper;
import io.github.mtrevisan.hunlinter.services.XMLManager;
import io.github.mtrevisan.hunlinter.services.eventbus.EventBusService;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterWarning;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;


/** Manages pairs of mistyped words and their correct spelling. */
public class AutoCorrectParser{

	private static final String QUOTATION_MARK = "\"";

	private static final String BAD_QUOTE = "{} form cannot contain apostrophes or double quotes: `{}`";
	private static final String DUPLICATED_ENTRY = "Duplicated entry in auto-correct file: `{}` -> `{}`";
	private static final String INVALID_ROOT = "Invalid root element, expected `{}`, was `{}`";

	private static final String AUTO_CORRECT_NAMESPACE = "block-list:";
	private static final String AUTO_CORRECT_ROOT_ELEMENT = AUTO_CORRECT_NAMESPACE + "block-list";
	private static final String AUTO_CORRECT_BLOCK = AUTO_CORRECT_NAMESPACE + "block";
	private static final String AUTO_CORRECT_INCORRECT_FORM = AUTO_CORRECT_NAMESPACE + "abbreviated-name";
	private static final String AUTO_CORRECT_CORRECT_FORM = AUTO_CORRECT_NAMESPACE + "name";


	private final List<CorrectionEntry> dictionary = new ArrayList<>(0);

	private final XMLManager xmlManager = new XMLManager();


	/**
	 * Parse the rows out from a `DocumentList.xml` file.
	 *
	 * @param acoFile	The reference to the auto-correct file.
	 * @throws IOException	If an I/O error occurs.
	 * @throws SAXException	If a parsing error occurs on the `xml` file.
	 */
	public final void parse(final File acoFile) throws IOException, SAXException{
		clear();

		final Document doc = xmlManager.parseXMLDocument(acoFile);

		final Element rootElement = doc.getDocumentElement();
		if(!AUTO_CORRECT_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new LinterException(INVALID_ROOT, AUTO_CORRECT_ROOT_ELEMENT, rootElement.getNodeName());

		final List<Node> children = XMLManager.extractChildren(rootElement, node -> XMLManager.isElement(node, AUTO_CORRECT_BLOCK));
		for(int i = 0; i < children.size(); i ++){
			final Node child = children.get(i);
			final Node mediaType = XMLManager.extractAttribute(child, AUTO_CORRECT_INCORRECT_FORM);
			if(mediaType != null){
				final CorrectionEntry correctionEntry = new CorrectionEntry(mediaType.getNodeValue(),
					XMLManager.extractAttributeValue(child, AUTO_CORRECT_CORRECT_FORM));
				dictionary.add(correctionEntry);
			}
		}

		validate();
	}

	private void validate(){
		//check for duplications
		int index = 0;
		final Collection<String> map = new HashSet<>(dictionary.size());
		for(int i = 0; i < dictionary.size(); i ++){
			final CorrectionEntry entry = dictionary.get(i);
			if(!map.add(entry.getIncorrectForm()))
				EventBusService.publish(new LinterWarning(DUPLICATED_ENTRY, entry.getIncorrectForm(), entry.getCorrectForm())
					.withIndex(index));

			index ++;
		}
	}

	public final List<CorrectionEntry> getCorrectionsDictionary(){
		return dictionary;
	}

	public final int getCorrectionsCounter(){
		return dictionary.size();
	}

	public final void setCorrection(final int index, final String incorrect, final String correct){
		dictionary.set(index, new CorrectionEntry(incorrect, correct));
	}

	/**
	 * @param incorrect	The incorrect form
	 * @param correct	The correct form
	 * @param duplicatesDiscriminator	Function called to ask the user what to do if duplicates are found
	 * 	(return {@code true} to force insertion)
	 * @return The duplication result
	 */
	public final DuplicationResult<CorrectionEntry> insertCorrection(final String incorrect, final String correct,
			final BooleanSupplier duplicatesDiscriminator){
		if(incorrect.contains(HyphenationParser.APOSTROPHE) || incorrect.contains(QUOTATION_MARK))
			throw new LinterException(BAD_QUOTE, "Incorrect", incorrect);
		if(correct.contains(HyphenationParser.APOSTROPHE) || correct.contains(QUOTATION_MARK))
			throw new LinterException(BAD_QUOTE, "Correct", correct);

		final List<CorrectionEntry> duplicates = extractDuplicates(incorrect, correct);
		final boolean forceInsertion = (duplicates.isEmpty() || duplicatesDiscriminator.getAsBoolean());
		if(forceInsertion)
			dictionary.add(new CorrectionEntry(incorrect, correct));

		return new DuplicationResult<>(duplicates, forceInsertion);
	}

	public final void deleteCorrection(final int selectedRowID){
		dictionary.remove(selectedRowID);
	}

	/** Find if there is a duplicate with the same incorrect and correct forms. */
	private List<CorrectionEntry> extractDuplicates(final String incorrect, final String correct){
		final List<CorrectionEntry> duplicates = new ArrayList<>(dictionary.size());
		for(int i = 0; i < dictionary.size(); i ++){
			final CorrectionEntry correction = dictionary.get(i);
			if(correction.getIncorrectForm().equals(incorrect) && correction.getCorrectForm().equals(correct))
				duplicates.add(correction);
		}
		return duplicates;
	}

	/** Find if there is a duplicate with the same incorrect and correct forms. */
	public final boolean contains(final String incorrect, final String correct){
		for(int i = 0; i < dictionary.size(); i ++){
			final CorrectionEntry elem = dictionary.get(i);
			if(!incorrect.isEmpty() && !correct.isEmpty()
					&& elem.getIncorrectForm().equals(incorrect) && elem.getCorrectForm().equals(correct))
				return true;
		}
		return false;
	}

	public static Pair<String, String> extractComponentsForFilter(final String incorrect, final String correct){
		return Pair.of(clearFilter(incorrect), clearFilter(correct));
	}

	private static String clearFilter(final String text){
		//escape special characters
		return RegexHelper.quoteReplacement(text.trim());
	}

	public static Pair<String, String> prepareTextForFilter(final String incorrect, final String correct){
		//extract part-of-speech if present
		final String incorrectFilter = (!incorrect.isEmpty()? incorrect: ".+");
		final String correctFilter = (!correct.isEmpty()? correct: ".+");

		//compose filter regexp
		return Pair.of(incorrectFilter, correctFilter);
	}

	public final void save(final File acoFile) throws TransformerException{
		final Document doc = xmlManager.newXMLDocumentStandalone();

		//root element
		final Element root = doc.createElement(AUTO_CORRECT_ROOT_ELEMENT);
		root.setAttribute(XMLManager.ROOT_ATTRIBUTE_NAME, XMLManager.ROOT_ATTRIBUTE_VALUE);
		doc.appendChild(root);

		for(int i = 0; i < dictionary.size(); i ++){
			final CorrectionEntry correction = dictionary.get(i);
			//correction element
			final Element elem = doc.createElement(AUTO_CORRECT_BLOCK);
			elem.setAttribute(AUTO_CORRECT_INCORRECT_FORM, correction.getIncorrectForm());
			elem.setAttribute(AUTO_CORRECT_CORRECT_FORM, correction.getCorrectForm());
			root.appendChild(elem);
		}

		XMLManager.createXML(acoFile, doc, XMLManager.XML_PROPERTIES_US_ASCII);
	}

	public final void clear(){
		dictionary.clear();
	}

}
