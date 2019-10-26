package unit731.hunspeller.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import unit731.hunspeller.Backbone;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.Deflater;


public class Packager{

	private static final Logger LOGGER = LoggerFactory.getLogger(Packager.class);

	private static final ZipManager ZIPPER = new ZipManager();

	private static final String FOLDER_META_INF = "META-INF";
	private static final String FILENAME_DESCRIPTION_XML = "description.xml";
	private static final String FILENAME_MANIFEST_XML = "manifest.xml";
	private static final String FILENAME_MANIFEST_JSON = "manifest.json";
	private static final String EXTENSION_ZIP = ".zip";
	private static final String EXTENSION_DAT = ".dat";
	private static final String EXTENSION_BAU = ".bau";

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
	private static final String FOLDER_ORIGIN = "%origin%";
	private static final String FOLDER_SPLITTER = "[/\\\\]";
	private static final String FILENAME_PREFIX_AUTO_CORRECT = "acor_";
	private static final String FILENAME_PREFIX_AUTO_TEXT = "atext_";


	private final Map<String, Path> configurationFolders = new HashMap<>();


	public Packager(final File affFile){
		extractConfigurationFolders(affFile);
	}

	private void extractConfigurationFolders(final File affFile){
		final Path basePath = getPackageBaseDirectory(affFile);

		if(basePath != null){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Found base path on folder {}", basePath.toString());

			try{
				final Path manifestPath = Paths.get(basePath.toString(), FOLDER_META_INF, FILENAME_MANIFEST_XML);
				if(existFile(manifestPath)){
					//read FILENAME_MANIFEST_XML into META-INF, collect all manifest:file-entry
					final List<String> fullPaths = extractFileEntries(manifestPath);

					//extract only the Paths configuration file
					final Pair<Path, Node> pair = findPathsConfiguration(manifestPath, fullPaths);
					final Path pathsFolder = pair.getLeft().getParent();
					final Node pathsNode = pair.getRight();
					if(pathsNode != null){
						configurationFolders.putAll(getFolders(pathsNode, manifestPath.getParent(), pathsFolder));
						final Set<String> uniqueFolders = configurationFolders.values().stream()
							.map(Path::toString)
							.collect(Collectors.toSet());
						if(configurationFolders.size() != uniqueFolders.size())
							throw new IllegalArgumentException("Duplicate folders detected, they must be unique: "
								+ StringUtils.join(configurationFolders));
						if(uniqueFolders.stream().anyMatch(String::isEmpty))
							throw new IllegalArgumentException("Empty folders detected, it must be something other than the base folder");
					}
				}
			}
			catch(final Exception e){
				LOGGER.info(Backbone.MARKER_APPLICATION, "Configuration reading error: {}", e.getMessage());

				LOGGER.error("Something very bad happened while extracting configuration file(s)", e);
			}
		}
	}

	public void createPackage(final File affFile, final String language){
		final Path basePath = getPackageBaseDirectory(affFile);

		//package entire folder with ZIP
		if(basePath != null){
			LOGGER.info(Backbone.MARKER_APPLICATION, "Found base path on folder {}", basePath.toString());

			try{
				Path autoCorrectOutputPath = null;
				final Path autoCorrectPath = configurationFolders.get(CONFIGURATION_NODE_NAME_AUTO_CORRECT);
				if(autoCorrectPath != null){
					//zip directory into .dat
					final String autoCorrectOutputFilename = autoCorrectPath.toString() + File.separator
						+ FILENAME_PREFIX_AUTO_CORRECT + language + EXTENSION_DAT;
					autoCorrectOutputPath = Path.of(autoCorrectOutputFilename);
					ZIPPER.zipDirectory(autoCorrectPath.toFile(), Deflater.BEST_COMPRESSION, autoCorrectOutputFilename);
				}
				Path autoTextOutputPath = null;
				final Path autoTextPath = configurationFolders.get(CONFIGURATION_NODE_NAME_AUTO_TEXT);
				if(autoTextPath != null){
					//zip directory into .bau
					final String autoTextOutputFilename = autoTextPath.toString() + File.separator + FILENAME_PREFIX_AUTO_TEXT
						+ language + EXTENSION_BAU;
					autoTextOutputPath = Path.of(autoTextOutputFilename);
					ZIPPER.zipDirectory(autoTextPath.toFile(), Deflater.BEST_COMPRESSION, autoTextOutputFilename);
				}

				final String outputFilename = basePath.toString() + File.separator + basePath.getName(basePath.getNameCount() - 1)
					+ EXTENSION_ZIP;
				//exclude all content inside CONFIGURATION_NODE_NAME_AUTO_CORRECT and CONFIGURATION_NODE_NAME_AUTO_TEXT folders
				//that are not autoCorrectOutputFilename or autoTextOutputFilename
				ZIPPER.zipDirectory(basePath.toFile(), Deflater.BEST_COMPRESSION, outputFilename,
					autoCorrectOutputPath, autoTextOutputPath);

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

	public Path getAutoCorrectPath(){
		final Path path = configurationFolders.get(CONFIGURATION_NODE_NAME_AUTO_CORRECT);
		return (path != null? Path.of(path.toString(), Backbone.FILENAME_AUTO_CORRECT): null);
	}

	public Path getAutoTextPath(){
		final Path path = configurationFolders.get(CONFIGURATION_NODE_NAME_AUTO_TEXT);
		return (path != null? Path.of(path.toString(), Backbone.FILENAME_AUTO_TEXT): null);
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
		final Document doc = XMLParser.parseXMLDocument(manifestPath);

		final Element rootElement = doc.getDocumentElement();
		if(!MANIFEST_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new IllegalArgumentException("Invalid root element, expected '" + MANIFEST_ROOT_ELEMENT + "', was "
				+ rootElement.getNodeName());

		final List<String> fullPaths = new ArrayList<>();
		final NodeList entries = rootElement.getChildNodes();
		for(int i = 0; i < entries.getLength(); i ++){
			final Node entry = entries.item(i);
			if(entry.getNodeType() == Node.ELEMENT_NODE && MANIFEST_FILE_ENTRY.equals(entry.getNodeName())){
				final Node mediaType = XMLParser.extractAttribute(entry, MANIFEST_FILE_ENTRY_MEDIA_TYPE);
				if(mediaType != null && MANIFEST_MEDIA_TYPE_CONFIGURATION_DATA.equals(mediaType.getNodeValue()))
					fullPaths.add(XMLParser.extractAttributeValue(entry, MANIFEST_FILE_ENTRY_FULL_PATH));
			}
		}
		return fullPaths;
	}

	private Pair<Path, Node> findPathsConfiguration(final Path manifestPath, final List<String> configurationFiles) throws IOException, SAXException{
		for(final String configurationFile : configurationFiles){
			final Path configurationPath = Paths.get(manifestPath.getParent().getParent().toString(),
				configurationFile.split(FOLDER_SPLITTER));

			final Document doc = XMLParser.parseXMLDocument(configurationPath);

			final Element rootElement = doc.getDocumentElement();
			if(!CONFIGURATION_ROOT_ELEMENT.equals(rootElement.getNodeName()))
				throw new IllegalArgumentException("Invalid root element, expected '" + CONFIGURATION_ROOT_ELEMENT + "', was "
					+ rootElement.getNodeName());

			final Node foundNode = onNodeNameApply(rootElement, CONFIGURATION_NODE_NAME_PATHS, Function.identity());
			if(foundNode != null)
				return Pair.of(configurationPath, foundNode);
		}
		return null;
	}

	private Map<String, Path> getFolders(final Node parentNode, final Path basePath, final Path originPath) throws IOException{
		final Map<String, Path> children = new HashMap<>();
		final NodeList nodes = parentNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++){
			final Node entry = nodes.item(i);
			if(XMLParser.isElement(entry, CONFIGURATION_NODE)){
				final Node node = XMLParser.extractAttribute(entry, CONFIGURATION_NODE_NAME);
				if(node != null){
					//extract folder
					Path currentParentPath = basePath;
					String folder = onNodeNameApply(entry, CONFIGURATION_NODE_NAME_INTERNAL_PATHS, this::extractFolder);
					if(folder.startsWith(FOLDER_ORIGIN)){
						folder = folder.substring(FOLDER_ORIGIN.length());
						currentParentPath = originPath;
					}
					final Path truePath = Path.of(currentParentPath.toString(), folder.split(FOLDER_SPLITTER));
					final Path path = Path.of(truePath.toFile().getCanonicalPath());
					children.put(node.getNodeValue(), path);
				}
			}
		}
		return children;
	}

	private <T> T onNodeNameApply(final Node parentNode, final String nodeName, final Function<Node, T> fun){
		final NodeList nodes = parentNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++){
			final Node entry = nodes.item(i);
			if(XMLParser.isElement(entry, CONFIGURATION_NODE)){
				final Node node = XMLParser.extractAttribute(entry, CONFIGURATION_NODE_NAME);
				if(node != null && nodeName.equals(node.getNodeValue()))
					return fun.apply(entry);
			}
		}
		return null;
	}

	private String extractFolder(final Node parentNode){
		final NodeList nodes = parentNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++){
			final Node node = nodes.item(i);
			if(XMLParser.isElement(node, CONFIGURATION_NODE))
				return XMLParser.extractAttributeValue(node, CONFIGURATION_NODE_NAME);
		}
		return null;
	}

}
