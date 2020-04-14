package unit731.hunlinter.parsers.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.XMLManager;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static unit731.hunlinter.services.system.LoopHelper.applyIf;
import static unit731.hunlinter.services.system.LoopHelper.forEach;


/**
 * `SentenceExceptList.xml` – Manages abbreviations that end with a fullstop that should be ignored when determining
 * 	the end of a sentence
 * `WordExceptList.xml` – Manages words that may contain more than 2 leading capital, eg. `CDs`
 */
public class ExceptionsParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionsParser.class);

	public enum TagChangeType{SET, ADD, REMOVE, CLEAR}

	private static final String AUTO_CORRECT_NAMESPACE = "block-list:";
	private static final String WORD_EXCEPTIONS_ROOT_ELEMENT = AUTO_CORRECT_NAMESPACE + "block-list";
	private static final String AUTO_CORRECT_BLOCK = AUTO_CORRECT_NAMESPACE + "block";
	private static final String WORD_EXCEPTIONS_WORD = AUTO_CORRECT_NAMESPACE + "abbreviated-name";


	private final String configurationFilename;
	private final List<String> dictionary = new ArrayList<>();
	private Comparator<String> comparator;


	public ExceptionsParser(final String configurationFilename){
		this.configurationFilename = configurationFilename;
	}

	/**
	 * Parse the rows out from a `SentenceExceptList.xml` or a `WordExceptList.xml` file.
	 *
	 * @param wexFile	The reference to the word exceptions file
	 * @param language	The language (used to sort)
	 * @throws IOException	If an I/O error occurs
	 * @throws SAXException	If an parsing error occurs on the `xml` file
	 */
	public void parse(final File wexFile, final String language) throws IOException, SAXException{
		comparator = BaseBuilder.getComparator(language);

		clear();

		final Document doc = XMLManager.parseXMLDocument(wexFile);

		final Element rootElement = doc.getDocumentElement();
		if(!WORD_EXCEPTIONS_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new LinterException("Invalid root element in file " + configurationFilename
				+ ", expected '" + WORD_EXCEPTIONS_ROOT_ELEMENT + "', was " + rootElement.getNodeName());

		final List<Node> children = XMLManager.extractChildren(rootElement, node -> XMLManager.isElement(node, AUTO_CORRECT_BLOCK));
		for(final Node child : children){
			final Node mediaType = XMLManager.extractAttribute(child, WORD_EXCEPTIONS_WORD);
			if(mediaType != null)
				dictionary.add(mediaType.getNodeValue());
		}
		dictionary.sort(comparator);

		validate();
	}

	private void validate(){
		//check for duplications
		final Map<String, List<String>> map = new HashMap<>();
		forEach(dictionary, s -> map.computeIfAbsent(s, k -> new ArrayList<>(1)).add(s));
		final List<List<String>> duplications = new ArrayList<>();
		applyIf(map.values(),
			s -> s.size() > 1,
			duplications::add);
		for(final List<String> duplication : duplications)
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Duplicated entry in file {}: '{}'", configurationFilename, duplication);
	}

	public List<String> getExceptionsDictionary(){
		return dictionary;
	}

	public int getExceptionsCounter(){
		return dictionary.size();
	}

	public void modify(final TagChangeType changeType, final List<String> tags){
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

	public boolean contains(final String exception){
		return dictionary.contains(exception);
	}

	public void save(final File excFile) throws TransformerException{
		final Document doc = XMLManager.newXMLDocumentStandalone();

		//root element
		final Element root = doc.createElement(WORD_EXCEPTIONS_ROOT_ELEMENT);
		root.setAttribute(XMLManager.ROOT_ATTRIBUTE_NAME, XMLManager.ROOT_ATTRIBUTE_VALUE);
		doc.appendChild(root);

		for(final String exception : dictionary){
			//correction element
			final Element elem = doc.createElement(AUTO_CORRECT_BLOCK);
			elem.setAttribute(WORD_EXCEPTIONS_WORD, exception);
			root.appendChild(elem);
		}

		XMLManager.createXML(excFile, doc, XMLManager.XML_PROPERTIES_UTF_8);
	}

	public void clear(){
		dictionary.clear();
	}

}
