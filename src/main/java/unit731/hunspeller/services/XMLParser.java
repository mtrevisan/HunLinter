package unit731.hunspeller.services;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;


public class XMLParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(XMLParser.class);


	private static DocumentBuilder DOCUMENT_BUILDER;
	static{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setExpandEntityReferences(false);
		try{
			DOCUMENT_BUILDER = factory.newDocumentBuilder();
			DOCUMENT_BUILDER.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader(StringUtils.EMPTY)));
		}
		catch(ParserConfigurationException e){
			LOGGER.error("Bad error while creating the XML parser", e);
		}
	}


	public static Document parseXMLDocument(final Path manifestPath) throws SAXException, IOException{
		final Document doc = DOCUMENT_BUILDER.parse(manifestPath.toFile());
		doc.getDocumentElement().normalize();
		return doc;
	}

	public static boolean isElement(final Node entry, final String elementName){
		return (entry.getNodeType() == Node.ELEMENT_NODE && elementName.equals(entry.getNodeName()));
	}

	public static Node extractAttribute(final Node entry, final String name){
		return entry.getAttributes().getNamedItem(name);
	}

	public static String extractAttributeValue(final Node entry, final String name){
		return entry.getAttributes().getNamedItem(name).getNodeValue();
	}

}
