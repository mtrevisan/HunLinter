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
import java.util.Collections;
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
	private static final String CONFIGURATION_PROPERTY = "prop";
	private static final String CONFIGURATION_NODE = "node";
	private static final String CONFIGURATION_NODE_NAME = "oor:name";
	private static final String CONFIGURATION_LANGUAGE = "LANGUAGE";
	//dictionaries spellcheck directory
	private static final String FILENAME_PREFIX_SPELLING = "HunSpellDic_";
	private static final String FILENAME_PREFIX_HYPHENATION = "HypDic_";
	private static final String FILENAME_PREFIX_THESAURUS = "ThesDic_";
	private static final String CONFIGURATION_NODE_PROPERTY_SPELLCHECK_AFFIX = "DICT_SPELL_AFF";
	private static final String CONFIGURATION_NODE_PROPERTY_SPELLCHECK_DICTIONARY = "DICT_SPELL_DIC";
	//dictionaries hyphenation file
	private static final String CONFIGURATION_NODE_PROPERTY_HYPHENATION = "DICT_HYPH";
	//dictionaries thesaurus directory
	private static final String CONFIGURATION_NODE_PROPERTY_THESAURUS_DATA = "DICT_THES_DAT";
	private static final String CONFIGURATION_NODE_PROPERTY_THESAURUS_INDEX = "DICT_THES_IDX";
	//dictionaries configuration file
	private static final String CONFIGURATION_NODE_NAME_SERVICE_MANAGER = "ServiceManager";
	private static final String CONFIGURATION_NODE_NAME_DICTIONARIES = "Dictionaries";
	private static final String CONFIGURATION_NODE_NAME_LOCATIONS = "Locations";
	private static final String CONFIGURATION_NODE_NAME_LOCALES = "Locales";
	//autocorrect/autotext configuration file
	private static final String CONFIGURATION_NODE_NAME_PATHS = "Paths";
	private static final String CONFIGURATION_NODE_NAME_AUTO_CORRECT = "AutoCorrect";
	private static final String FILENAME_AUTO_CORRECT = "DocumentList.xml";
	public static final String FILENAME_SENTENCE_EXCEPTIONS = "SentenceExceptList.xml";
	public static final String FILENAME_WORD_EXCEPTIONS = "WordExceptList.xml";
	private static final String CONFIGURATION_NODE_NAME_AUTO_TEXT = "AutoText";
	private static final String FILENAME_AUTO_TEXT = "BlockList.xml";
	private static final String CONFIGURATION_NODE_NAME_INTERNAL_PATHS = "InternalPaths";
	private static final String FOLDER_ORIGIN = "%origin%";
	private static final String FOLDER_SPLITTER = "[/\\\\]";
	private static final String FILENAME_PREFIX_AUTO_CORRECT = "acor_";
	private static final String FILENAME_PREFIX_AUTO_TEXT = "atext_";
	private static final String SPELLCHECK_FOLDERS_SEPARATOR = ".aff ";
	private static final String THESAURUS_FOLDERS_SEPARATOR = ".dat ";


	private String language;
	private final Map<String, File> configurationFiles = new HashMap<>();


	public Packager(final File affFile){
		extractConfigurationFolders(affFile);
	}

	private void extractConfigurationFolders(final File affFile){
		final Path basePath = getPackageBaseDirectory(affFile);

		if(basePath != null){
			LOGGER.trace("Found base path on folder {}", basePath.toString());

			try{
				final File manifestFile = Paths.get(basePath.toString(), FOLDER_META_INF, FILENAME_MANIFEST_XML)
					.toFile();
				if(existFile(manifestFile)){
					final List<File> fullFiles = extractFileEntries(manifestFile).stream()
						.map(configurationFile -> Paths.get(basePath.toString(), configurationFile.split(FOLDER_SPLITTER)).toFile())
						.collect(Collectors.toList());

					final Map<String, File> languages = extractLanguages(manifestFile, fullFiles);
					if(languages.isEmpty())
						throw new IllegalArgumentException("No language(s) defined");
					final Map<File, String> affFiles = invert(languages);
					language = affFiles.get(affFile);
					if(language == null)
						throw new IllegalArgumentException("Language not present in " + FILENAME_MANIFEST_XML);

					processDictionariesConfigurationFile(manifestFile, fullFiles);
					processPathsConfigurationFile(manifestFile, fullFiles);
				}
			}
			catch(final Exception e){
				LOGGER.info(Backbone.MARKER_APPLICATION, "Configuration reading error: {}", e.getMessage());

				LOGGER.error("Something very bad happened while extracting configuration file(s)", e);
			}
		}
	}

	private static <V, K> Map<V, K> invert(final Map<K, V> map){
		return map.entrySet().stream()
			.collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
	}

	private Map<String, File> extractLanguages(final File manifestFile, final List<File> configurationFiles) throws IOException, SAXException{
		final Pair<File, Node> pair = findConfiguration(CONFIGURATION_NODE_NAME_SERVICE_MANAGER, configurationFiles);
		final File file = pair.getLeft();
		final Node parentNode = pair.getRight();
		if(parentNode != null){
			final Path basePath = manifestFile.toPath().getParent();
			final Path originPath = file.toPath().getParent();
			final NodeList nodes = parentNode.getChildNodes();
			for(int i = 0; i < nodes.getLength(); i ++){
				final Node entry = nodes.item(i);
				if(!XMLParser.isElement(entry, CONFIGURATION_NODE))
					continue;

				final Node node = XMLParser.extractAttribute(entry, CONFIGURATION_NODE_NAME);
				if(node != null && CONFIGURATION_NODE_NAME_DICTIONARIES.equals(node.getNodeValue())){
					return getLanguages(entry, basePath, originPath);
				}
			}
		}
		return Collections.emptyMap();
	}

	private Map<String, File> getLanguages(final Node entry, final Path basePath, final Path originPath) throws IOException{
		final Map<String, File> languages = new HashMap<>();
		final NodeList subNodes = entry.getChildNodes();
		for(int i = 0; i < subNodes.getLength(); i ++){
			final Node subEntry = subNodes.item(i);
			if(XMLParser.isElement(subEntry, CONFIGURATION_NODE) && XMLParser.extractAttributeValue(subEntry, CONFIGURATION_NODE_NAME).startsWith(FILENAME_PREFIX_SPELLING)){
				final String language = extractLocale(subEntry);
				final String folder = extractLocation(subEntry);
				final int splitIndex = folder.indexOf(SPELLCHECK_FOLDERS_SEPARATOR);
				final String folderAff = folder.substring(0, splitIndex + SPELLCHECK_FOLDERS_SEPARATOR.length() - 1);
				final File affFile = absolutizeFolder(folderAff, basePath, originPath);
				languages.put(language, affFile);
			}
		}
		return languages;
	}

	private void processDictionariesConfigurationFile(final File manifestFile, final List<File> configurationFiles) throws IOException, SAXException{
		final Pair<File, Node> pair = findConfiguration(CONFIGURATION_NODE_NAME_SERVICE_MANAGER, configurationFiles);
		final File file = pair.getLeft();
		final Node node = pair.getRight();
		if(node != null)
			this.configurationFiles.putAll(getFolders(node, manifestFile.toPath().getParent(), file.toPath().getParent()));
	}

	private void processPathsConfigurationFile(final File manifestFile, final List<File> configurationFiles)
			throws IOException, SAXException{
		final Pair<File, Node> pair = findConfiguration(CONFIGURATION_NODE_NAME_PATHS, configurationFiles);
		final File file = pair.getLeft();
		final Node node = pair.getRight();
		if(node != null){
			this.configurationFiles.putAll(getFolders(node, manifestFile.toPath().getParent(), file.toPath().getParent()));
			final Set<String> uniqueFolders = this.configurationFiles.values().stream()
				.map(File::toString)
				.collect(Collectors.toSet());
			if(this.configurationFiles.size() != uniqueFolders.size())
				throw new IllegalArgumentException("Duplicate folders detected, they must be unique: "
					+ StringUtils.join(this.configurationFiles));
			if(uniqueFolders.stream().anyMatch(String::isEmpty))
				throw new IllegalArgumentException("Empty folders detected, it must be something other than the base folder");
		}
	}

	public void createPackage(final File affFile, final String language){
		final Path basePath = getPackageBaseDirectory(affFile);

		//package entire folder into a ZIP file
		if(basePath != null){
			try{
				Path autoCorrectOutputPath = null;
				File autoCorrectFile = configurationFiles.get(FILENAME_AUTO_CORRECT);
				if(autoCorrectFile == null)
					autoCorrectFile = configurationFiles.get(FILENAME_SENTENCE_EXCEPTIONS);
				if(autoCorrectFile == null)
					autoCorrectFile = configurationFiles.get(FILENAME_WORD_EXCEPTIONS);
				if(autoCorrectFile != null){
					//zip directory into .dat
					autoCorrectOutputPath = Path.of(autoCorrectFile.getParent(),
						FILENAME_PREFIX_AUTO_CORRECT + language + EXTENSION_DAT);
					ZIPPER.zipDirectory(autoCorrectFile.toPath().getParent(), Deflater.BEST_COMPRESSION, autoCorrectOutputPath.toFile());
				}
				Path autoTextOutputPath = null;
				final File autoTextFile = configurationFiles.get(CONFIGURATION_NODE_NAME_AUTO_TEXT);
				if(autoTextFile != null){
					//zip directory into .bau
					autoTextOutputPath = Path.of(autoTextFile.getParent(),
						FILENAME_PREFIX_AUTO_TEXT + language + EXTENSION_BAU);
					ZIPPER.zipDirectory(autoTextFile.toPath().getParent(), Deflater.BEST_COMPRESSION, autoTextOutputPath.toFile());
				}

				final File outputFile = new File(basePath.toString() + File.separator
					+ basePath.getName(basePath.getNameCount() - 1) + EXTENSION_ZIP);
				//exclude all content inside CONFIGURATION_NODE_NAME_AUTO_CORRECT and CONFIGURATION_NODE_NAME_AUTO_TEXT folders
				//that are not autoCorrectOutputFilename or autoTextOutputFilename
				ZIPPER.zipDirectory(basePath, Deflater.BEST_COMPRESSION, outputFile, autoCorrectOutputPath, autoTextOutputPath);

				//remove created sub-packages
				if(autoCorrectOutputPath != null)
					Files.delete(autoCorrectOutputPath);
				if(autoTextOutputPath != null)
					Files.delete(autoTextOutputPath);

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

	public String getLanguage(){
		return language;
	}

	public File getHyphenationFile(){
		return configurationFiles.get(CONFIGURATION_NODE_PROPERTY_HYPHENATION);
	}

	public File getThesaurusDataFile(){
		return configurationFiles.get(CONFIGURATION_NODE_PROPERTY_THESAURUS_DATA);
	}

	public File getThesaurusIndexFile(){
		return configurationFiles.get(CONFIGURATION_NODE_PROPERTY_THESAURUS_INDEX);
	}

	public File getAutoCorrectFile(){
		return configurationFiles.get(FILENAME_AUTO_CORRECT);
	}

	public File getSentenceExceptionsFile(){
		return configurationFiles.get(FILENAME_SENTENCE_EXCEPTIONS);
	}

	public File getWordExceptionsFile(){
		return configurationFiles.get(FILENAME_WORD_EXCEPTIONS);
	}

	public File getAutoTextFile(){
		return configurationFiles.get(CONFIGURATION_NODE_NAME_AUTO_TEXT);
	}

	/** Go up directories until description.xml or manifest.json is found */
	private Path getPackageBaseDirectory(final File affFile){
		Path parentPath = affFile.toPath().getParent();
		while(parentPath != null && !existFile(parentPath, FILENAME_DESCRIPTION_XML) && !existFile(parentPath, FILENAME_MANIFEST_JSON))
			parentPath = parentPath.getParent();
		return parentPath;
	}

	private boolean existFile(final File file){
		return Files.isRegularFile(file.toPath());
	}

	private boolean existFile(final Path path, final String filename){
		return Files.isRegularFile(Paths.get(path.toString(), filename));
	}

	private List<String> extractFileEntries(final File manifestFile) throws IOException, SAXException{
		final Document doc = XMLParser.parseXMLDocument(manifestFile);

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

	private Pair<File, Node> findConfiguration(final String configurationName,
			final List<File> configurationFiles) throws IOException, SAXException{
		for(final File configurationFile : configurationFiles){
			final Document doc = XMLParser.parseXMLDocument(configurationFile);

			final Element rootElement = doc.getDocumentElement();
			if(!CONFIGURATION_ROOT_ELEMENT.equals(rootElement.getNodeName()))
				throw new IllegalArgumentException("Invalid root element, expected '" + CONFIGURATION_ROOT_ELEMENT + "', was "
					+ rootElement.getNodeName());

			final Node foundNode = onNodeNameApply(rootElement, configurationName, Function.identity());
			if(foundNode != null)
				return Pair.of(configurationFile, foundNode);
		}
		return null;
	}

	private Map<String, File> getFolders(final Node parentNode, final Path basePath, final Path originPath) throws IOException{
		final Map<String, File> children = new HashMap<>();
		final NodeList nodes = parentNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++){
			final Node entry = nodes.item(i);
			if(!XMLParser.isElement(entry, CONFIGURATION_NODE))
				continue;

			final Node node = XMLParser.extractAttribute(entry, CONFIGURATION_NODE_NAME);
			if(node != null){
				//extract folder(s)
				if(CONFIGURATION_NODE_NAME_DICTIONARIES.equals(node.getNodeValue()))
					getFoldersForDictionaries(entry, basePath, originPath, children);
				else{
					final String nodeValue = node.getNodeValue();
					getFoldersForInternalPaths(entry, nodeValue, basePath, originPath, children);
				}
			}
		}
		return children;
	}

	private void getFoldersForDictionaries(final Node entry, final Path basePath, final Path originPath, final Map<String, File> children) throws IOException{
		final NodeList subNodes = entry.getChildNodes();
		for(int j = 0; j < subNodes.getLength(); j ++){
			final Node subEntry = subNodes.item(j);
			if(!XMLParser.isElement(subEntry, CONFIGURATION_NODE))
				continue;

			//restrict to given language
			final String locale = extractLocale(subEntry);
			if(!locale.equals(language))
				continue;

			if(XMLParser.extractAttributeValue(subEntry, CONFIGURATION_NODE_NAME).startsWith(FILENAME_PREFIX_SPELLING)){
				final String folders = extractLocation(subEntry);
				final int splitIndex = folders.indexOf(SPELLCHECK_FOLDERS_SEPARATOR);
				final String folderAff = folders.substring(0, splitIndex + SPELLCHECK_FOLDERS_SEPARATOR.length() - 1);
				final File fileAff = absolutizeFolder(folderAff, basePath, originPath);
				children.put(CONFIGURATION_NODE_PROPERTY_SPELLCHECK_AFFIX, fileAff);
				final String folderDic = folders.substring(splitIndex + SPELLCHECK_FOLDERS_SEPARATOR.length());
				final File fileDic = absolutizeFolder(folderDic, basePath, originPath);
				children.put(CONFIGURATION_NODE_PROPERTY_SPELLCHECK_DICTIONARY, fileDic);
			}
			else if(XMLParser.extractAttributeValue(subEntry, CONFIGURATION_NODE_NAME).startsWith(FILENAME_PREFIX_HYPHENATION)){
				final String folder = extractLocation(subEntry);
				final File file = absolutizeFolder(folder, basePath, originPath);
				children.put(CONFIGURATION_NODE_PROPERTY_HYPHENATION, file);
			}
			else if(XMLParser.extractAttributeValue(subEntry, CONFIGURATION_NODE_NAME).startsWith(FILENAME_PREFIX_THESAURUS)){
				final String folders = extractLocation(subEntry);
				final int splitIndex = folders.indexOf(THESAURUS_FOLDERS_SEPARATOR);
				final String folderDat = folders.substring(0, splitIndex + THESAURUS_FOLDERS_SEPARATOR.length() - 1);
				final File fileDat = absolutizeFolder(folderDat, basePath, originPath);
				children.put(CONFIGURATION_NODE_PROPERTY_THESAURUS_DATA, fileDat);
				final String folderIdx = folders.substring(splitIndex + THESAURUS_FOLDERS_SEPARATOR.length());
				final File fileIdx = absolutizeFolder(folderIdx, basePath, originPath);
				children.put(CONFIGURATION_NODE_PROPERTY_THESAURUS_INDEX, fileIdx);
			}
		}
	}

	private void getFoldersForInternalPaths(final Node entry, final String nodeValue, final Path basePath, final Path originPath, final Map<String, File> children) throws IOException{
		final String folder = onNodeNameApply(entry, CONFIGURATION_NODE_NAME_INTERNAL_PATHS, this::extractFolder);
		final File file = absolutizeFolder(folder, basePath, originPath);
		if(CONFIGURATION_NODE_NAME_AUTO_CORRECT.equals(nodeValue)){
			children.put(FILENAME_AUTO_CORRECT, Path.of(file.toString(), FILENAME_AUTO_CORRECT).toFile());
			children.put(FILENAME_SENTENCE_EXCEPTIONS, Path.of(file.toString(), FILENAME_SENTENCE_EXCEPTIONS).toFile());
			children.put(FILENAME_WORD_EXCEPTIONS, Path.of(file.toString(), FILENAME_WORD_EXCEPTIONS).toFile());
		}
		else if(CONFIGURATION_NODE_NAME_AUTO_TEXT.equals(nodeValue))
			children.put(nodeValue, Path.of(file.toString(), FILENAME_AUTO_TEXT).toFile());
		else
			LOGGER.info("Unknown configuration name: {}", nodeValue);
	}

	private File absolutizeFolder(String folder, final Path basePath, final Path originPath) throws IOException{
		Path currentParentPath = basePath;
		if(folder.startsWith(FOLDER_ORIGIN)){
			folder = folder.substring(FOLDER_ORIGIN.length());
			currentParentPath = originPath;
		}
		final Path truePath = Path.of(currentParentPath.toString(), folder.split(FOLDER_SPLITTER));
		return Path.of(truePath.toFile().getCanonicalPath())
			.toFile();
	}

	private String extractLocation(final Node parentNode){
		return extractProperty(parentNode, CONFIGURATION_NODE_NAME_LOCATIONS);
	}

	private String extractLocale(final Node parentNode){
		return extractProperty(parentNode, CONFIGURATION_NODE_NAME_LOCALES);
	}

	private String extractProperty(final Node parentNode, final String propertyName){
		final NodeList nodes = parentNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++){
			final Node node = nodes.item(i);
			if(XMLParser.isElement(node, CONFIGURATION_PROPERTY)
					&& propertyName.equals(XMLParser.extractAttributeValue(node, CONFIGURATION_NODE_NAME)))
				return node.getChildNodes().item(1).getFirstChild().getNodeValue();
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

	private <T> T onNodeNameApply(final Node parentNode, final String nodeName, final Function<Node, T> fun){
		final NodeList nodes = parentNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++){
			final Node entry = nodes.item(i);
			if(!XMLParser.isElement(entry, CONFIGURATION_NODE))
				continue;

			final Node node = XMLParser.extractAttribute(entry, CONFIGURATION_NODE_NAME);
			if(node != null && nodeName.equals(node.getNodeValue()))
				return fun.apply(entry);
		}
		return null;
	}

}
