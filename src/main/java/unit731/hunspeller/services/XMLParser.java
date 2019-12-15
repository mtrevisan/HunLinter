package unit731.hunspeller.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public class XMLParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(XMLParser.class);

	@SuppressWarnings("unchecked")
	public static final Pair<String, String>[] XML_PROPERTIES_UTF_8 = new Pair[]{
		Pair.of(OutputKeys.VERSION, "1.0"),
		Pair.of(OutputKeys.ENCODING, StandardCharsets.UTF_8.name()),
		Pair.of(OutputKeys.INDENT, "yes"),
		Pair.of("{http://xml.apache.org/xslt}indent-amount", "3")
	};
	@SuppressWarnings("unchecked")
	public static final Pair<String, String>[] XML_PROPERTIES_US_ASCII = new Pair[]{
		Pair.of(OutputKeys.VERSION, "1.0"),
		Pair.of(OutputKeys.ENCODING, StandardCharsets.US_ASCII.name()),
		Pair.of(OutputKeys.INDENT, "yes"),
		Pair.of("{http://xml.apache.org/xslt}indent-amount", "3")
	};
	public static final String ROOT_ATTRIBUTE_NAME = "xmlns:block-list";
	public static final String ROOT_ATTRIBUTE_VALUE = "http://openoffice.org/2001/block-list";


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


	public static Document parseXMLDocument(final File file) throws SAXException, IOException{
		final Document doc = DOCUMENT_BUILDER.parse(file);
		doc.getDocumentElement().normalize();
		return doc;
	}

	public static Document newXMLDocumentStandalone(){
		final Document doc = DOCUMENT_BUILDER.newDocument();
		//remove `standalone="no"` from XML declaration
		doc.setXmlStandalone(true);
		return doc;
	}

	/** Transform the DOM Object to an XML File */
	public static void createXML(final File xmlFile, final Document doc, final Pair<String, String>... properties)
			throws TransformerException{
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		JavaHelper.nullableToStream(properties)
			.forEach(property -> transformer.setOutputProperty(property.getKey(), property.getValue()));
		final DOMSource domSource = new DOMSource(doc);
		final StreamResult streamResult = new StreamResult(xmlFile);
		transformer.transform(domSource, streamResult);
	}

	public static boolean isElement(final Node entry, final String elementName){
		return (entry.getNodeType() == Node.ELEMENT_NODE && elementName.equals(entry.getNodeName()));
	}

	public static List<Node> extractChildren(final Node parentNode, final Function<Node, Boolean> extrationCondition){
		final List<Node> children = new ArrayList<>();
		if(parentNode != null){
			final NodeList nodes = parentNode.getChildNodes();
			for(int i = 0; i < nodes.getLength(); i ++){
				final Node node = nodes.item(i);
				if(extrationCondition.apply(node))
					children.add(node);
			}
		}
		return children;
	}

	public static Node extractAttribute(final Node entry, final String name){
		return entry.getAttributes().getNamedItem(name);
	}

	public static String extractAttributeValue(final Node entry, final String name){
		return entry.getAttributes().getNamedItem(name).getNodeValue();
	}

}
