package unit731.hunlinter.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import unit731.hunlinter.services.system.LoopHelper;

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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;


public class XMLManager{

	private static final Logger LOGGER = LoggerFactory.getLogger(XMLManager.class);

	public static final Pair<String, String>[] XML_PROPERTIES_UTF_8 = getXMLProperties(StandardCharsets.UTF_8);
	public static final Pair<String, String>[] XML_PROPERTIES_US_ASCII = getXMLProperties(StandardCharsets.US_ASCII);
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

	/* Transform the DOM Object to an XML File */
	public static void createXML(final File xmlFile, final Document doc, final Pair<String, String>... properties)
			throws TransformerException{
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		LoopHelper.forEach(properties, property -> transformer.setOutputProperty(property.getKey(), property.getValue()));
		final DOMSource domSource = new DOMSource(doc);
		final StreamResult streamResult = new StreamResult(xmlFile);
		transformer.transform(domSource, streamResult);
	}

	public static boolean isElement(final Node entry, final String elementName){
		return (entry.getNodeType() == Node.ELEMENT_NODE && elementName.equals(entry.getNodeName()));
	}

	public static List<Node> extractChildren(final Node parentNode, final Function<Node, Boolean> extractionCondition){
		final ArrayList<Node> children = new ArrayList<>();
		if(parentNode != null){
			final NodeList nodes = parentNode.getChildNodes();
			children.ensureCapacity(nodes.getLength());
			IntStream.range(0, nodes.getLength())
				.mapToObj(nodes::item)
				.filter(extractionCondition::apply)
				.forEach(children::add);
		}
		return children;
	}

	public static Node extractAttribute(final Node entry, final String name){
		return entry.getAttributes().getNamedItem(name);
	}

	public static String extractAttributeValue(final Node entry, final String name){
		return entry.getAttributes().getNamedItem(name).getNodeValue();
	}

	@SuppressWarnings("unchecked")
	private static Pair<String, String>[] getXMLProperties(final Charset charset){
		return new Pair[]{
			Pair.of(OutputKeys.VERSION, "1.0"),
			Pair.of(OutputKeys.ENCODING, charset.name()),
			Pair.of(OutputKeys.INDENT, "yes"),
			Pair.of("{http://xml.apache.org/xslt}indent-amount", "3")
		};
	}

}
