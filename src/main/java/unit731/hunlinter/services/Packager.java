package unit731.hunlinter.services;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.workers.exceptions.ProjectNotFoundException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
	//dictionaries spellcheck directory
	private static final String FILENAME_PREFIX_SPELLING = "HunSpellDic_";
	private static final String FILENAME_PREFIX_HYPHENATION = "HyphDic_";
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

	private static final class ConfigurationData{
		final String foldersSeparator;
		final String nodePropertyFile1;
		final String nodePropertyFile2;

		ConfigurationData(final String foldersSeparator, final String nodePropertyFile1, final String nodePropertyFile2){
			this.foldersSeparator = foldersSeparator;
			this.nodePropertyFile1 = nodePropertyFile1;
			this.nodePropertyFile2 = nodePropertyFile2;
		}

		Map<String, File> getDoubleFolders(final String childFolders, final Path basePath, final Path originPath) throws IOException{
			final Map<String, File> folders = new HashMap<>();
			final int splitIndex = childFolders.indexOf(foldersSeparator);
			final String folderAff = childFolders.substring(0, splitIndex + foldersSeparator.length() - 1);
			final File fileAff = absolutizeFolder(folderAff, basePath, originPath);
			folders.put(nodePropertyFile1, fileAff);
			final String folderDic = childFolders.substring(splitIndex + foldersSeparator.length());
			final File fileDic = absolutizeFolder(folderDic, basePath, originPath);
			folders.put(nodePropertyFile2, fileDic);
			return folders;
		}
	}
	private static final Map<String, ConfigurationData> CONFIG_DATA = new HashMap<>();
	static{
		CONFIG_DATA.put(CONFIGURATION_NODE_PROPERTY_SPELLCHECK_AFFIX, new ConfigurationData(".aff ",
			CONFIGURATION_NODE_PROPERTY_SPELLCHECK_AFFIX, CONFIGURATION_NODE_PROPERTY_SPELLCHECK_DICTIONARY));
		CONFIG_DATA.put(CONFIGURATION_NODE_PROPERTY_THESAURUS_DATA, new ConfigurationData(".dat ",
			CONFIGURATION_NODE_PROPERTY_THESAURUS_DATA, CONFIGURATION_NODE_PROPERTY_THESAURUS_INDEX));
	}


	private Path projectPath;
	private Path mainManifestPath;
	private List<File> manifestFiles;
	private List<String> languages;

	private String language;
	private final Map<String, File> configurationFiles = new HashMap<>();


	public void reload(final Path projectPath) throws ProjectNotFoundException, IOException, SAXException{
		Objects.requireNonNull(projectPath);

		this.projectPath = projectPath;

		if(!existDirectory(projectPath))
			throw new ProjectNotFoundException(projectPath, "Folder " + projectPath + " doesn't exists, cannot load project");

		mainManifestPath = Paths.get(projectPath.toString(), FOLDER_META_INF, FILENAME_MANIFEST_XML);
		if(!existFile(mainManifestPath))
			throw new ProjectNotFoundException(projectPath, "No " + FILENAME_MANIFEST_XML + " file found under " + projectPath
				+ ", cannot load project");

		manifestFiles = extractFileEntries(mainManifestPath.toFile()).stream()
			.map(configurationFile -> Paths.get(projectPath.toString(), configurationFile.split(FOLDER_SPLITTER)).toFile())
			.collect(Collectors.toList());

		languages = extractLanguages(manifestFiles);
		if(languages.isEmpty())
			throw new IllegalArgumentException("No language(s) defined");
	}

	public static boolean isProjectFolder(final File file){
		try{
			final Path path = file.toPath();
			return (existDirectory(path)
				&& existFile(Paths.get(path.toString(), FOLDER_META_INF, FILENAME_MANIFEST_XML))
				&& existFile(Paths.get(path.toString(), FILENAME_DESCRIPTION_XML))
			);
		}
		catch(final InvalidPathException ignored){}
		return false;
	}

	public List<String> getAvailableLanguages(){
		return languages;
	}

	public void extractConfigurationFolders(final String language){
		if(!languages.contains(language))
			throw new IllegalArgumentException("Language not present in " + FILENAME_MANIFEST_XML);

		try{
			this.language = language;

			processDictionariesConfigurationFile();
			processPathsConfigurationFile();
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Configuration reading error: {}", e.getMessage());

			LOGGER.error("Something very bad happened while extracting configuration file(s)", e);
		}
	}

	private List<String> extractLanguages(final List<File> configurationFiles) throws IOException, SAXException{
		final Pair<File, Node> pair = findConfiguration(CONFIGURATION_NODE_NAME_SERVICE_MANAGER, configurationFiles);
		final Node parentNode = pair.getRight();
		final List<Node> children = extractChildren(parentNode);
		for(final Node child : children){
			final Node node = XMLManager.extractAttribute(child, CONFIGURATION_NODE_NAME);
			if(node != null && CONFIGURATION_NODE_NAME_DICTIONARIES.equals(node.getNodeValue()))
				return getLanguages(child);
		}
		return Collections.emptyList();
	}

	private List<String> getLanguages(final Node entry){
		final Set<String> languageSets = new HashSet<>();
		final List<Node> children = extractChildren(entry);
		for(final Node child : children)
			if(XMLManager.extractAttributeValue(child, CONFIGURATION_NODE_NAME).startsWith(FILENAME_PREFIX_SPELLING)){
				final String[] locales = extractLocale(child);
				languageSets.addAll(Arrays.asList(locales));
			}
		final List<String> languages = new ArrayList<>(languageSets);
		Collections.sort(languages);
		return Collections.unmodifiableList(languages);
	}

	private void processDictionariesConfigurationFile() throws IOException, SAXException{
		final Pair<File, Node> pair = findConfiguration(CONFIGURATION_NODE_NAME_SERVICE_MANAGER, manifestFiles);
		final File file = pair.getLeft();
		final Node node = pair.getRight();
		if(node == null)
			throw new IllegalArgumentException("Cannot find " + CONFIGURATION_NODE_NAME_SERVICE_MANAGER + " in files: "
				+ manifestFiles.stream().map(File::getName).collect(Collectors.joining(", ", "[", "]")));
		else
			this.configurationFiles.putAll(getFolders(node, mainManifestPath.getParent(), file.toPath().getParent()));
	}

	private void processPathsConfigurationFile() throws IOException, SAXException{
		final Pair<File, Node> pair = findConfiguration(CONFIGURATION_NODE_NAME_PATHS, manifestFiles);
		final File file = pair.getLeft();
		final Node node = pair.getRight();
		if(node != null){
			this.configurationFiles.putAll(getFolders(node, mainManifestPath.getParent(), file.toPath().getParent()));
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

	public void createPackage(final Path projectPath, final String language){
		//package entire folder into a ZIP file
		try{
			final Path autoCorrectOutputPath = packageAutoCorrectFiles(language);
			final Path autoTextOutputPath = packageAutoTextFiles(language);

			packageExtension(projectPath, autoCorrectOutputPath, autoTextOutputPath);

			//remove created sub-packages
			if(autoCorrectOutputPath != null)
				Files.delete(autoCorrectOutputPath);
			if(autoTextOutputPath != null)
				Files.delete(autoTextOutputPath);

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Package created");

			FileHelper.browse(projectPath.toFile());
		}
		catch(final Exception e){
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Package error: {}", e.getMessage());

			LOGGER.error("Something very bad happened while creating package", e);
		}
	}

	private Path packageAutoCorrectFiles(String language) throws IOException{
		File autoCorrectFile = configurationFiles.get(FILENAME_AUTO_CORRECT);
		if(autoCorrectFile == null)
			autoCorrectFile = configurationFiles.get(FILENAME_SENTENCE_EXCEPTIONS);
		if(autoCorrectFile == null)
			autoCorrectFile = configurationFiles.get(FILENAME_WORD_EXCEPTIONS);
		return packageFiles(FILENAME_PREFIX_AUTO_CORRECT + language + EXTENSION_DAT, autoCorrectFile);
	}

	private Path packageAutoTextFiles(final String language) throws IOException{
		final File autoTextFile = configurationFiles.get(CONFIGURATION_NODE_NAME_AUTO_TEXT);
		return packageFiles(FILENAME_PREFIX_AUTO_TEXT + language + EXTENSION_BAU, autoTextFile);
	}

	private Path packageFiles(final String packageFilename, final File file) throws IOException{
		Path outputPath = null;
		if(file != null){
			outputPath = Path.of(file.getParent(), packageFilename);
			ZIPPER.zipDirectory(file.toPath().getParent(), Deflater.BEST_COMPRESSION, outputPath.toFile());
		}
		return outputPath;
	}

	private void packageExtension(Path projectPath, Path autoCorrectOutputPath, Path autoTextOutputPath) throws IOException{
		final File outputFile = Path.of(projectPath.toString(), projectPath.getName(projectPath.getNameCount() - 1)
			+ EXTENSION_ZIP)
			.toFile();
		//exclude all content inside CONFIGURATION_NODE_NAME_AUTO_CORRECT and CONFIGURATION_NODE_NAME_AUTO_TEXT folders
		//that are not autoCorrectOutputFilename or autoTextOutputFilename
		ZIPPER.zipDirectory(projectPath, Deflater.BEST_COMPRESSION, outputFile, autoCorrectOutputPath, autoTextOutputPath);
	}

	public String getLanguage(){
		return language;
	}

	public Path getProjectPath(){
		return projectPath;
	}

	public File getAffixFile(){
		return configurationFiles.get(CONFIGURATION_NODE_PROPERTY_SPELLCHECK_AFFIX);
	}

	public File getDictionaryFile(){
		return configurationFiles.get(CONFIGURATION_NODE_PROPERTY_SPELLCHECK_DICTIONARY);
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
		while(parentPath != null && !existFile(parentPath, FILENAME_DESCRIPTION_XML)
				&& !existFile(parentPath, FILENAME_MANIFEST_JSON))
			parentPath = parentPath.getParent();
		return parentPath;
	}

	private static boolean existDirectory(final Path path){
		return Files.isDirectory(path);
	}

	private static boolean existFile(final Path path){
		return Files.isRegularFile(path);
	}

	private boolean existFile(final Path path, final String filename){
		return Files.isRegularFile(Paths.get(path.toString(), filename));
	}

	private List<String> extractFileEntries(final File manifestFile) throws IOException, SAXException{
		final Document doc = XMLManager.parseXMLDocument(manifestFile);

		final Element rootElement = doc.getDocumentElement();
		if(!MANIFEST_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new IllegalArgumentException("Invalid root element, expected '" + MANIFEST_ROOT_ELEMENT + "', was "
				+ rootElement.getNodeName());

		final List<String> configurationPaths = new ArrayList<>();
		final List<Node> children = extractChildren(rootElement);
		for(final Node child : children){
			final Node mediaType = XMLManager.extractAttribute(child, MANIFEST_FILE_ENTRY_MEDIA_TYPE);
			if(mediaType != null && MANIFEST_MEDIA_TYPE_CONFIGURATION_DATA.equals(mediaType.getNodeValue()))
				configurationPaths.add(XMLManager.extractAttributeValue(child, MANIFEST_FILE_ENTRY_FULL_PATH));
		}
		return configurationPaths;
	}

	private Pair<File, Node> findConfiguration(final String configurationName, final List<File> configurationFiles)
			throws IOException, SAXException{
		for(final File configurationFile : configurationFiles){
			final Document doc = XMLManager.parseXMLDocument(configurationFile);

			final Element rootElement = doc.getDocumentElement();
			if(!CONFIGURATION_ROOT_ELEMENT.equals(rootElement.getNodeName()))
				throw new IllegalArgumentException("Invalid root element, expected '" + CONFIGURATION_ROOT_ELEMENT + "', was "
					+ rootElement.getNodeName());

			final Node foundNode = onNodeNameApply(rootElement, configurationName, Function.identity());
			if(foundNode != null)
				return Pair.of(configurationFile, foundNode);
		}
		return Pair.of(null, null);
	}

	private Map<String, File> getFolders(final Node parentNode, final Path basePath, final Path originPath) throws IOException{
		final Map<String, File> folders = new HashMap<>();
		final List<Node> children = extractChildren(parentNode);
		for(final Node child : children){
			final Node node = XMLManager.extractAttribute(child, CONFIGURATION_NODE_NAME);
			if(node == null)
				continue;

			//extract folder(s)
			final String nodeValue = node.getNodeValue();
			if(CONFIGURATION_NODE_NAME_DICTIONARIES.equals(nodeValue))
				getFoldersForDictionaries(child, basePath, originPath, folders);
			else
				folders.putAll(getFoldersForInternalPaths(child, nodeValue, basePath, originPath));
		}
		return folders;
	}

	private void getFoldersForDictionaries(final Node entry, final Path basePath, final Path originPath,
			final Map<String, File> folders) throws IOException{
		final List<Node> children = extractChildren(entry).stream()
			//restrict to given language
			.filter(child -> ArrayUtils.contains(extractLocale(child), language))
			.collect(Collectors.toList());
		for(final Node child : children){
			final String attributeValue = XMLManager.extractAttributeValue(child, CONFIGURATION_NODE_NAME);
			final String childFolders = extractLocation(child);
			if(attributeValue.startsWith(FILENAME_PREFIX_HYPHENATION)){
				final File file = absolutizeFolder(childFolders, basePath, originPath);
				folders.put(CONFIGURATION_NODE_PROPERTY_HYPHENATION, file);
			}
			else if(attributeValue.startsWith(FILENAME_PREFIX_SPELLING)){
				final Map<String, File> extractedFolders = CONFIG_DATA.get(CONFIGURATION_NODE_PROPERTY_SPELLCHECK_AFFIX)
					.getDoubleFolders(childFolders, basePath, originPath);
				folders.putAll(extractedFolders);
			}
			else if(attributeValue.startsWith(FILENAME_PREFIX_THESAURUS)){
				final Map<String, File> extractedFolders = CONFIG_DATA.get(CONFIGURATION_NODE_PROPERTY_THESAURUS_DATA)
					.getDoubleFolders(childFolders, basePath, originPath);
				folders.putAll(extractedFolders);
			}
		}
	}

	private Map<String, File> getFoldersForInternalPaths(final Node entry, final String nodeValue, final Path basePath,
			final Path originPath) throws IOException{
		final String folder = onNodeNameApply(entry, CONFIGURATION_NODE_NAME_INTERNAL_PATHS, this::extractFolder);
		Objects.requireNonNull(folder);

		final File file = absolutizeFolder(folder, basePath, originPath);
		final Map<String, File> children = new HashMap<>();
		if(CONFIGURATION_NODE_NAME_AUTO_CORRECT.equals(nodeValue)){
			children.put(FILENAME_AUTO_CORRECT, Path.of(file.toString(), FILENAME_AUTO_CORRECT).toFile());
			children.put(FILENAME_SENTENCE_EXCEPTIONS, Path.of(file.toString(), FILENAME_SENTENCE_EXCEPTIONS).toFile());
			children.put(FILENAME_WORD_EXCEPTIONS, Path.of(file.toString(), FILENAME_WORD_EXCEPTIONS).toFile());
		}
		else if(CONFIGURATION_NODE_NAME_AUTO_TEXT.equals(nodeValue))
			children.put(nodeValue, Path.of(file.toString(), FILENAME_AUTO_TEXT).toFile());
		else
			LOGGER.info("Unknown configuration name: {}", nodeValue);
		return children;
	}

	private static File absolutizeFolder(String folder, final Path basePath, final Path originPath) throws IOException{
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

	private String[] extractLocale(final Node parentNode){
		final String locale = extractProperty(parentNode, CONFIGURATION_NODE_NAME_LOCALES);
		return StringUtils.split(locale);
	}

	private String extractProperty(final Node parentNode, final String propertyName){
		final NodeList nodes = parentNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++){
			final Node node = nodes.item(i);
			if(XMLManager.isElement(node, CONFIGURATION_PROPERTY)
					&& propertyName.equals(XMLManager.extractAttributeValue(node, CONFIGURATION_NODE_NAME)))
				return node.getChildNodes().item(1).getFirstChild().getNodeValue();
		}
		return null;
	}

	private String extractFolder(final Node parentNode){
		final List<Node> children = extractChildren(parentNode);
		return (!children.isEmpty()? XMLManager.extractAttributeValue(children.get(0), CONFIGURATION_NODE_NAME): null);
	}

	private <T> T onNodeNameApply(final Node parentNode, final String nodeName, final Function<Node, T> fun){
		final List<Node> children = extractChildren(parentNode);
		for(final Node child : children){
			final Node node = XMLManager.extractAttribute(child, CONFIGURATION_NODE_NAME);
			if(node != null && nodeName.equals(node.getNodeValue()))
				return fun.apply(child);
		}
		return null;
	}

	private List<Node> extractChildren(final Element parentElement){
		return XMLManager.extractChildren(parentElement,
			node -> (node.getNodeType() == Node.ELEMENT_NODE && MANIFEST_FILE_ENTRY.equals(node.getNodeName())));
	}

	private List<Node> extractChildren(final Node parentNode){
		return XMLManager.extractChildren(parentNode,
			node -> XMLManager.isElement(node, CONFIGURATION_NODE));
	}

}
