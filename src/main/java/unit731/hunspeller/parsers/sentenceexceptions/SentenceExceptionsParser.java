package unit731.hunspeller.parsers.sentenceexceptions;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.services.XMLParser;

import javax.xml.transform.OutputKeys;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


/** Manages abbreviations that end with a fullstop that should be ignored when determining the end of a sentence */
public class SentenceExceptionsParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(SentenceExceptionsParser.class);

	private static final String AUTO_CORRECT_NAMESPACE = "block-list:";
	private static final String WORD_EXCEPTIONS_ROOT_ELEMENT = AUTO_CORRECT_NAMESPACE + "block-list";
	private static final String AUTO_CORRECT_BLOCK = AUTO_CORRECT_NAMESPACE + "block";
	private static final String WORD_EXCEPTIONS_WORD = AUTO_CORRECT_NAMESPACE + "abbreviated-name";

	@SuppressWarnings("unchecked")
	private static final Pair<String, String>[] XML_PROPERTIES = new Pair[]{
		Pair.of(OutputKeys.VERSION, "1.0"),
		Pair.of(OutputKeys.ENCODING, StandardCharsets.UTF_8.name())
	};


	private final List<String> dictionary = new ArrayList<>();


	/**
	 * Parse the rows out from a `WordExceptList.xml` file.
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
			throw new IllegalArgumentException("Invalid root element, expected '" + WORD_EXCEPTIONS_ROOT_ELEMENT + "', was "
				+ rootElement.getNodeName());

		final NodeList entries = rootElement.getChildNodes();
		for(int i = 0; i < entries.getLength(); i ++){
			final Node entry = entries.item(i);
			if(XMLParser.isElement(entry, AUTO_CORRECT_BLOCK)){
				final Node mediaType = XMLParser.extractAttribute(entry, WORD_EXCEPTIONS_WORD);
				if(mediaType != null)
					dictionary.add(mediaType.getNodeValue());
			}
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
			LOGGER.info(Backbone.MARKER_APPLICATION, "Duplicated entry in word exceptions file: '{}'", duplication);
	}

	public List<String> getSentenceExceptionsDictionary(){
		return dictionary;
	}

	public int getSentenceExceptionsCounter(){
		return dictionary.size();
	}

	public void clear(){
		dictionary.clear();
	}

}
