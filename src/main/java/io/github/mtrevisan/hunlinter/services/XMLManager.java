/**
 * Copyright (c) 2019-2022 Mauro Trevisan
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
package io.github.mtrevisan.hunlinter.services;

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
import javax.xml.transform.Result;
import javax.xml.transform.Source;
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
import java.util.function.Predicate;


public final class XMLManager{

	private static final Logger LOGGER = LoggerFactory.getLogger(XMLManager.class);

	public static final Pair<String, String>[] XML_PROPERTIES_UTF_8 = getXMLProperties(StandardCharsets.UTF_8);
	public static final Pair<String, String>[] XML_PROPERTIES_US_ASCII = getXMLProperties(StandardCharsets.US_ASCII);
	public static final String ROOT_ATTRIBUTE_NAME = "xmlns:block-list";
	@SuppressWarnings("HttpUrlsUsage")
	public static final String ROOT_ATTRIBUTE_VALUE = "http://openoffice.org/2001/block-list";


	private DocumentBuilder documentBuilder;


	public XMLManager(){
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setExpandEntityReferences(false);
		try{
			documentBuilder = factory.newDocumentBuilder();
			documentBuilder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader(StringUtils.EMPTY)));
		}
		catch(final ParserConfigurationException e){
			LOGGER.error("Bad error while creating the XML parser", e);
		}
	}

	public Document parseXMLDocument(final File file) throws SAXException, IOException{
		final Document doc = documentBuilder.parse(file);
		doc.getDocumentElement().normalize();
		return doc;
	}

	public Document newXMLDocumentStandalone(){
		final Document doc = documentBuilder.newDocument();
		//remove `standalone="no"` from XML declaration
		doc.setXmlStandalone(true);
		return doc;
	}

	/** Transform the DOM Object to an XML File. */
	@SafeVarargs
	@SuppressWarnings("OverlyBroadThrowsClause")
	public static void createXML(final File xmlFile, final Document doc, final Pair<String, String>... properties)
			throws TransformerException{
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		if(properties != null)
			for(final Pair<String, String> property : properties)
				transformer.setOutputProperty(property.getKey(), property.getValue());
		final Source domSource = new DOMSource(doc);
		final Result streamResult = new StreamResult(xmlFile);
		transformer.transform(domSource, streamResult);
	}

	public static boolean isElement(final Node entry, final String elementName){
		return (entry.getNodeType() == Node.ELEMENT_NODE && elementName.equals(entry.getNodeName()));
	}

	public static List<Node> extractChildren(final Node parentNode, final Predicate<Node> extractionCondition){
		final ArrayList<Node> children = new ArrayList<>(0);
		if(parentNode != null){
			final NodeList nodes = parentNode.getChildNodes();
			final int length = nodes.getLength();
			children.ensureCapacity(length);
			for(int i = 0; i < length; i ++){
				final Node node = nodes.item(i);
				if(extractionCondition.test(node))
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

	@SuppressWarnings({"unchecked", "HttpUrlsUsage"})
	private static Pair<String, String>[] getXMLProperties(final Charset charset){
		return new Pair[]{
			Pair.of(OutputKeys.VERSION, "1.0"),
			Pair.of(OutputKeys.ENCODING, charset.name()),
			Pair.of(OutputKeys.INDENT, "yes"),
			Pair.of("{http://xml.apache.org/xslt}indent-amount", "3")
		};
	}

}
