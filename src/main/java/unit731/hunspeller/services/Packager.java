package unit731.hunspeller.services;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import unit731.hunspeller.Backbone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.Deflater;


public class Packager{

	private static final Logger LOGGER = LoggerFactory.getLogger(Packager.class);

	private static final ZipManager ZIPPER = new ZipManager();

	private static final String FOLDER_META_INF = "META-INF";
	private static final String FILENAME_DESCRIPTION_XML = "description.xml";
	private static final String FILENAME_MANIFEST_XML = "manifest.xml";
	private static final String FILENAME_MANIFEST_JSON = "manifest.json";
	private static final String EXTENSION_ZIP = ".zip";

	private static final String MANIFEST_ROOT_ELEMENT = "manifest:manifest";
	private static final String MANIFEST_FILE_ENTRY = "manifest:file-entry";
	private static final String MANIFEST_FILE_ENTRY_MEDIA_TYPE = "manifest:media-type";
	private static final String MANIFEST_FILE_ENTRY_FULL_PATH = "manifest:full-path";
	private static final String MANIFEST_MEDIA_TYPE_CONFIGURATION_DATA = "application/vnd.sun.star.configuration-data";
	private static final String CONFIGURATION_ROOT_ELEMENT = "oor:component-data";
	private static final String CONFIGURATION_NODE = "node";
	private static final String CONFIGURATION_NODE_NAME = "oor:name";
	//dictionaries configuration file
	private static final String CONFIGURATION_NODE_NAME_SERVICE_MANAGER = "ServiceManager";
	//autocorrect/autotext configuration file
	private static final String CONFIGURATION_NODE_NAME_PATHS = "Paths";
	private static final String CONFIGURATION_NODE_NAME_AUTO_CORRECT = "AutoCorrect";
	private static final String CONFIGURATION_NODE_NAME_AUTO_TEXT = "AutoText";
	private static final String CONFIGURATION_NODE_NAME_INTERNAL_PATHS = "InternalPaths";


	private static DocumentBuilder DOCUMENT_BUILDER;
	static{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setValidating(true);
		try{
			DOCUMENT_BUILDER = factory.newDocumentBuilder();
			DOCUMENT_BUILDER.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader(StringUtils.EMPTY)));
		}
		catch(ParserConfigurationException e){
			LOGGER.error("Bad error while creating the XML parser, cannot create package", e);
		}
	}


	public void createPackage(final File affFile){
		final Path basePath = getPackageBaseDirectory(affFile);

		//package entire folder with ZIP
		if(basePath != null){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Found base path on folder {}", basePath.toString());

			try{
				final Path manifestPath = Paths.get(basePath.toString(), FOLDER_META_INF, FILENAME_MANIFEST_XML);
				if(existFile(manifestPath)){
					//read FILENAME_MANIFEST_XML into META-INF, collect all manifest:file-entry
					final List<String> fullPaths = extractFileEntries(manifestPath);

					//extract only the Paths configuration file
					final Node pathsNode = findPathsConfiguration(manifestPath, fullPaths);
					if(pathsNode != null){
						final Map<String, String> folders = getChildren(pathsNode);
						final String autoCorrectFolder = folders.get(CONFIGURATION_NODE_NAME_AUTO_CORRECT);
						final String autoTextFolder = folders.get(CONFIGURATION_NODE_NAME_AUTO_TEXT);
						//TODO
						//for each sub-node
							//search for node oor:name=CONFIGURATION_NODE_NAME_AUTO_CORRECT
								//zip directory into .dat
							//search for node oor:name=CONFIGURATION_NODE_NAME_AUTO_TEXT
								//zip directory into .bau

						System.out.println();
					}
				}

				//TODO exclude all content inside AutoCorrect and AutoText folders that does not ends in .dat or .bau
				final String outputFilename = basePath.toString() + File.separator + basePath.getName(basePath.getNameCount() - 1) + EXTENSION_ZIP;
				ZIPPER.zipDirectory(basePath.toFile(), Deflater.BEST_COMPRESSION, outputFilename);

				LOGGER.info(Backbone.MARKER_APPLICATION, "Package created");

				//open directory
				if(Desktop.isDesktopSupported())
					Desktop.getDesktop().open(new File(basePath.toString()));
			}
			catch(final Exception e){
				LOGGER.info(Backbone.MARKER_APPLICATION, "Package error: {}", e.getMessage());

				LOGGER.error("Something very bad happened while creating package", e);
			}
		}
	}

	/** Go up directories until description.xml or manifest.json is found */
	private Path getPackageBaseDirectory(final File affFile){
		Path parentPath = affFile.toPath().getParent();
		while(parentPath != null && !existFile(parentPath, FILENAME_DESCRIPTION_XML) && !existFile(parentPath, FILENAME_MANIFEST_JSON))
			parentPath = parentPath.getParent();
		return parentPath;
	}

	private boolean existFile(final Path path){
		return Files.isRegularFile(path);
	}

	private boolean existFile(final Path path, final String filename){
		return Files.isRegularFile(Paths.get(path.toString(), filename));
	}

	private List<String> extractFileEntries(final Path manifestPath) throws IOException, SAXException{
		final Document doc = parseXMLDocument(manifestPath);

		final Element rootElement = doc.getDocumentElement();
		if(!MANIFEST_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new IllegalArgumentException("Invalid root element, expected '" + MANIFEST_ROOT_ELEMENT + "', was " + rootElement.getNodeName());

		final List<String> fullPaths = new ArrayList<>();
		final NodeList entries = rootElement.getChildNodes();
		for(int i = 0; i < entries.getLength(); i ++){
			final Node entry = entries.item(i);
			if(entry.getNodeType() == Node.ELEMENT_NODE && MANIFEST_FILE_ENTRY.equals(entry.getNodeName())){
				final Node mediaType = entry.getAttributes().getNamedItem(MANIFEST_FILE_ENTRY_MEDIA_TYPE);
				if(mediaType != null && MANIFEST_MEDIA_TYPE_CONFIGURATION_DATA.equals(mediaType.getNodeValue()))
					fullPaths.add(entry.getAttributes().getNamedItem(MANIFEST_FILE_ENTRY_FULL_PATH).getNodeValue());
			}
		}
		return fullPaths;
	}

	private Node findPathsConfiguration(final Path manifestPath, final List<String> configurationFiles) throws IOException, SAXException{
		for(final String configurationFile : configurationFiles){
			final Path configurationPath = Paths.get(manifestPath.getParent().getParent().toString(), configurationFile.split("[/\\\\]"));

			final Document doc = parseXMLDocument(configurationPath);

			final Element rootElement = doc.getDocumentElement();
			if(!CONFIGURATION_ROOT_ELEMENT.equals(rootElement.getNodeName()))
				throw new IllegalArgumentException("Invalid root element, expected '" + CONFIGURATION_ROOT_ELEMENT + "', was " + rootElement.getNodeName());

			final Node foundNode = onNodeNameApply(rootElement, CONFIGURATION_NODE_NAME_PATHS, Function.identity());
			if(foundNode != null)
				return foundNode;
		}
		return null;
	}

	private <T> T onNodeNameApply(final Node parentNode, final String nodeName, final Function<Node, T> fun){
		final NodeList nodes = parentNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++){
			final Node entry = nodes.item(i);
			if(isNode(entry)){
				final Node node = entry.getAttributes().getNamedItem(CONFIGURATION_NODE_NAME);
				if(node != null && nodeName.equals(node.getNodeValue()))
					return fun.apply(entry);
			}
		}
		return null;
	}

	private Map<String, String> getChildren(final Node parentNode){
		final Map<String, String> children = new HashMap<>();
		final NodeList nodes = parentNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++){
			final Node entry = nodes.item(i);
			if(isNode(entry)){
				final Node node = entry.getAttributes().getNamedItem(CONFIGURATION_NODE_NAME);
				if(node != null){
					//extract folder
					final String folder = onNodeNameApply(entry, CONFIGURATION_NODE_NAME_INTERNAL_PATHS, this::extractFolder);

					children.put(node.getNodeValue(), folder);
				}
			}
		}
		return children;
	}

	private String extractFolder(final Node parentNode){
		final NodeList nodes = parentNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++){
			final Node entry = nodes.item(i);
			if(isNode(entry))
				return entry.getAttributes().getNamedItem(CONFIGURATION_NODE_NAME).getNodeValue();
		}
		return null;
	}

	private Document parseXMLDocument(Path manifestPath) throws SAXException, IOException{
		final Document doc = DOCUMENT_BUILDER.parse(manifestPath.toFile());
		doc.getDocumentElement().normalize();
		return doc;
	}

	private boolean isNode(final Node entry){
		return (entry.getNodeType() == Node.ELEMENT_NODE && CONFIGURATION_NODE.equals(entry.getNodeName()));
	}

}
