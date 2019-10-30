package unit731.hunspeller.parsers.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.services.XMLParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * `SentenceExceptList.xml` – Manages abbreviations that end with a fullstop that should be ignored when determining
 * 	the end of a sentence
 * `WordExceptList.xml` – Manages words that may contain more than 2 leading capital, eg. `CDs`
 */
public class ExceptionsParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionsParser.class);

	private static final String AUTO_CORRECT_NAMESPACE = "block-list:";
	private static final String WORD_EXCEPTIONS_ROOT_ELEMENT = AUTO_CORRECT_NAMESPACE + "block-list";
	private static final String AUTO_CORRECT_BLOCK = AUTO_CORRECT_NAMESPACE + "block";
	private static final String WORD_EXCEPTIONS_WORD = AUTO_CORRECT_NAMESPACE + "abbreviated-name";


	private final String configurationFilename;
	private final List<String> dictionary = new ArrayList<>();


	public ExceptionsParser(final String configurationFilename){
		this.configurationFilename = configurationFilename;
	}

	/**
	 * Parse the rows out from a `SentenceExceptList.xml` or a `WordExceptList.xml` file.
	 *
	 * @param wexFile	The reference to the word exceptions file
	 * @throws IOException	If an I/O error occurs
	 * @throws SAXException	If an parsing error occurs on the `xml` file
	 */
	public void parse(final File wexFile) throws IOException, SAXException{
		clear();

		final Document doc = XMLParser.parseXMLDocument(wexFile);

		final Element rootElement = doc.getDocumentElement();
		if(!WORD_EXCEPTIONS_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new IllegalArgumentException("Invalid root element in file " + configurationFilename
				+ ", expected '" + WORD_EXCEPTIONS_ROOT_ELEMENT + "', was " + rootElement.getNodeName());

		final List<Node> children = XMLParser.extractChildren(rootElement, node -> XMLParser.isElement(node, AUTO_CORRECT_BLOCK));
		for(final Node child : children){
			final Node mediaType = XMLParser.extractAttribute(child, WORD_EXCEPTIONS_WORD);
			if(mediaType != null)
				dictionary.add(mediaType.getNodeValue());
		}

		validate();
	}

	private void validate(){
		//check for duplications
		final List<List<String>> duplications = dictionary.stream()
			.collect(Collectors.groupingBy(Function.identity()))
			.values().stream()
			.filter(list -> list.size() > 1)
			.collect(Collectors.toList());
		for(final List<String> duplication : duplications)
			LOGGER.info(Backbone.MARKER_APPLICATION, "Duplicated entry in file {}: '{}'", configurationFilename, duplication);
	}

	public List<String> getExceptionsDictionary(){
		return dictionary;
	}

	public int getExceptionsCounter(){
		return dictionary.size();
	}

	public void clear(){
		dictionary.clear();
	}

}
