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

import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.ProjectNotFoundException;
import org.apache.commons.io.FilenameUtils;
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class Packager{

	private static final Logger LOGGER = LoggerFactory.getLogger(Packager.class);

	private static final String SLASH = "/";

	private static final Pattern LANGUAGE_SAMPLE_EXTRACTOR = RegexHelper.pattern("(?:TRY |FX [^ ]+ )([^\r\n\\d]+)[\r\n]+");

	public static final String KEY_FILE_AFFIX = "file.affix";
	public static final String KEY_FILE_DICTIONARY = "file.dictionary";
	public static final String KEY_FILE_HYPHENATION = "file.hyphenation";
	public static final String KEY_FILE_THESAURUS_DATA = "file.thesaurus.data";
	public static final String KEY_FILE_THESAURUS_INDEX = "file.thesaurus.index";
	public static final String KEY_FILE_AUTO_CORRECT = "file.auto.correct";
	public static final String KEY_FILE_SENTENCE_EXCEPTIONS = "file.sentence.exceptions";
	public static final String KEY_FILE_WORD_EXCEPTIONS = "file.word.exceptions";
	public static final String KEY_FILE_AUTO_TEXT = "file.auto.text";

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
	private static final String CONFIGURATION_NODE_NAME_INTERNAL_PATHS = "InternalPaths";
	private static final String FOLDER_ORIGIN = "%origin%";
	private static final Pattern FOLDER_SPLITTER = RegexHelper.pattern("[/\\\\]");
	private static final String FILENAME_PREFIX_AUTO_CORRECT = "acor_";
	private static final String FILENAME_PREFIX_AUTO_TEXT = "atext_";
	private static final String FILE_EXTENSION_AFFIX = ".aff";
	private static final String FILE_EXTENSION_DICTIONARY = ".dic";

	private static final Pattern CUT_PATTERN = Pattern.compile("(\\s*#|^/).*$");

	private static final Map<String, String> KEY_FILE_MAPPER = Map.of(
		KEY_FILE_AFFIX, CONFIGURATION_NODE_PROPERTY_SPELLCHECK_AFFIX,
		KEY_FILE_DICTIONARY, CONFIGURATION_NODE_PROPERTY_SPELLCHECK_DICTIONARY,
		KEY_FILE_HYPHENATION, CONFIGURATION_NODE_PROPERTY_HYPHENATION,
		KEY_FILE_THESAURUS_DATA, CONFIGURATION_NODE_PROPERTY_THESAURUS_DATA,
		KEY_FILE_THESAURUS_INDEX, CONFIGURATION_NODE_PROPERTY_THESAURUS_INDEX,
		KEY_FILE_AUTO_CORRECT, FILENAME_AUTO_CORRECT,
		KEY_FILE_SENTENCE_EXCEPTIONS, FILENAME_SENTENCE_EXCEPTIONS,
		KEY_FILE_WORD_EXCEPTIONS, FILENAME_WORD_EXCEPTIONS,
		KEY_FILE_AUTO_TEXT, CONFIGURATION_NODE_NAME_AUTO_TEXT);

	private record ConfigurationData(String foldersSeparator, String nodePropertyFile1, String nodePropertyFile2){
		Map<String, File> getDoubleFolders(final String childFolders, final Path basePath, final Path originPath)
				throws IOException{
			final Map<String, File> folders = new HashMap<>(2);
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
	private static final Map<String, ConfigurationData> CONFIG_DATA = Map.of(
		CONFIGURATION_NODE_PROPERTY_SPELLCHECK_AFFIX, new ConfigurationData(".aff ",
			CONFIGURATION_NODE_PROPERTY_SPELLCHECK_AFFIX, CONFIGURATION_NODE_PROPERTY_SPELLCHECK_DICTIONARY),
		CONFIGURATION_NODE_PROPERTY_THESAURUS_DATA, new ConfigurationData(".dat ",
			CONFIGURATION_NODE_PROPERTY_THESAURUS_DATA, CONFIGURATION_NODE_PROPERTY_THESAURUS_INDEX)
	);


	private Path projectPath;
	private Path mainManifestPath;
	private Path autoCorrectPath;
	private Path autoTextPath;
	private final Collection<File> manifestFiles = new ArrayList<>(0);
	private List<String> languages;

	private String language;
	private final Map<String, Object> configurationFiles = new HashMap<>(0);

	private final XMLManager xmlManager = new XMLManager();


	public final void reload(final Path projectPath) throws ProjectNotFoundException, IOException, SAXException{
		Objects.requireNonNull(projectPath, "Project path cannot be null");

		this.projectPath = projectPath;

		clear();

		if(!isDirectoryExisting(projectPath))
			throw new ProjectNotFoundException(projectPath, "Folder " + projectPath
				+ " doesn't exists, cannot load project");

		mainManifestPath = Paths.get(projectPath.toString(), FOLDER_META_INF, FILENAME_MANIFEST_XML);
		if(!isFileExisting(mainManifestPath))
			throw new ProjectNotFoundException(projectPath, "No " + FILENAME_MANIFEST_XML + " file found under "
				+ projectPath + ", cannot load project");

		final List<String> collection = extractFileEntries(mainManifestPath.toFile());
		for(final String configurationFile : collection)
			manifestFiles.add(Paths.get(projectPath.toString(),
				RegexHelper.split(configurationFile, FOLDER_SPLITTER)).toFile());

		languages = extractLanguages(manifestFiles);
		if(languages.isEmpty())
			throw new IllegalArgumentException("No language(s) defined");
	}

	private void clear(){
		manifestFiles.clear();
		configurationFiles.clear();
	}

	public static boolean isProjectFolder(final File file){
		try{
			final Path path = file.toPath();
			return (isDirectoryExisting(path)
				&& isFileExisting(Paths.get(path.toString(), FOLDER_META_INF, FILENAME_MANIFEST_XML))
				&& isFileExisting(Paths.get(path.toString(), FILENAME_DESCRIPTION_XML))
			);
		}
		catch(final InvalidPathException ignored){}
		return false;
	}

	public final List<String> getLanguages(){
		try{
			final Map<Integer, List<String>> allFolders = new HashMap<>();
			for(final String language : languages){
				final Map<String, Object> folders = processDictionariesConfigurationFile(language);
				final Map<String, Object> folders2 = extractPathsConfigurationFile(language);
				if(folders2 != null)
					folders.putAll(folders2);

				for(final Map.Entry<String, Object> entry : folders.entrySet()){
					final Object value = entry.getValue();
					final int hash = (value instanceof File[] array? Arrays.hashCode(array): Objects.hash(value));
					allFolders.computeIfAbsent(hash, k -> new ArrayList<>(1))
						.add(language);
				}
			}

			//check that all keys have the same number of strings in the value
			int size = -1;
			boolean sameSize = true;
			for(final List<String> value : allFolders.values()){
				if(size == -1)
					size = value.size();
				else if(size != value.size()){
					sameSize = false;
					break;
				}
			}

			if(sameSize){
				String shortest = null;
				//peek one (they are all equals)
				final List<String> values = allFolders.values().iterator()
					.next();
				//extract the shortest language code
				for(final String value : values)
					if(shortest == null || value.length() < shortest.length())
						shortest = value;
				return (shortest != null? List.of(shortest): Collections.emptyList());
			}
			else
				return languages;
		}
		catch(final Exception ignored){
			return Collections.emptyList();
		}
	}

	public final void extractConfigurationFolders(final String language){
		if(!languages.contains(language))
			throw new IllegalArgumentException("Language not present in " + FILENAME_MANIFEST_XML);

		try{
			this.language = language;

			Map<String, Object> folders = processDictionariesConfigurationFile(language);
			configurationFiles.putAll(folders);
			folders = extractPathsConfigurationFile(language);
			if(folders != null)
				configurationFiles.putAll(folders);

			//extract all .dat in autocorr folder
			if(autoCorrectPath != null){
				try(final Stream<Path> stream = Files.list(autoCorrectPath)){
					stream.filter(file -> !Files.isDirectory(file))
						.filter(path -> path.getFileName().toString().endsWith(EXTENSION_DAT))
						.forEach(path -> ZipManager.unzipFile(path.toFile(), autoCorrectPath));
				}
			}
			//extract all .bau in autotext folder
			if(autoTextPath != null){
				try(final Stream<Path> stream = Files.list(autoTextPath)){
					stream.filter(file -> !Files.isDirectory(file))
						.filter(path -> path.getFileName().toString().endsWith(EXTENSION_BAU))
						.forEach(path -> {
							final Path outputPath = Path.of(autoTextPath.toString(),
								FilenameUtils.getBaseName(path.toFile().getName()));
							ZipManager.unzipFile(path.toFile(), outputPath);
						});
				}
			}
		}
		catch(final SAXException | IOException e){
			LOGGER.error(ParserManager.MARKER_APPLICATION, "Configuration reading error: {}", e.getMessage());

			LOGGER.error("Something very bad happened while extracting configuration file(s)", e);
		}
	}

	private List<String> extractLanguages(final Iterable<File> configurationFiles) throws IOException, SAXException{
		final Pair<Path, Node> pair = findConfiguration(CONFIGURATION_NODE_NAME_SERVICE_MANAGER, configurationFiles);
		final Node parentNode = pair.getRight();
		final List<Node> children = extractChildren(parentNode);
		for(final Node child : children){
			final Node node = XMLManager.extractAttribute(child, CONFIGURATION_NODE_NAME);
			if(node != null && CONFIGURATION_NODE_NAME_DICTIONARIES.equals(node.getNodeValue()))
				return getLanguages(child);
		}
		return Collections.emptyList();
	}

	private static List<String> getLanguages(final Node entry){
		final Set<String> languageSets = new HashSet<>(0);
		final List<Node> children = extractChildren(entry);
		for(final Node child : children)
			if(XMLManager.extractAttributeValue(child, CONFIGURATION_NODE_NAME).startsWith(FILENAME_PREFIX_SPELLING)){
				final String[] locales = extractLocale(child);
				languageSets.addAll(Arrays.asList(locales));
			}
		final List<String> langs = new ArrayList<>(languageSets);
		Collections.sort(langs);
		return Collections.unmodifiableList(langs);
	}

	private Map<String, Object> processDictionariesConfigurationFile(final String language) throws IOException,
			SAXException{
		final Pair<Path, Node> pair = findConfiguration(CONFIGURATION_NODE_NAME_SERVICE_MANAGER, manifestFiles);
		final Path path = pair.getLeft();
		final Node node = pair.getRight();
		if(node == null)
			throw new IllegalArgumentException("Cannot find " + CONFIGURATION_NODE_NAME_SERVICE_MANAGER + " in files: "
				+ manifestFiles.stream().map(File::getName).collect(Collectors.joining(", ", "[", "]")));

		return getFolders(language, node, mainManifestPath.getParent(), path.getParent());
	}

	private Map<String, Object> extractPathsConfigurationFile(final String language) throws IOException, SAXException{
		Map<String, Object> folders = null;
		final Pair<Path, Node> pair = findConfiguration(CONFIGURATION_NODE_NAME_PATHS, manifestFiles);
		final Path path = pair.getLeft();
		final Node node = pair.getRight();
		if(node != null){
			folders = getFolders(language, node, mainManifestPath.getParent(), path.getParent());
			final Collection<Object> collection = folders.values();
			final Collection<String> uniqueFolders = new HashSet<>(collection.size());
			for(final Object f : collection)
				uniqueFolders.add(f.toString());
			if(folders.size() != uniqueFolders.size())
				throw new IllegalArgumentException("Duplicate folders detected, they must be unique: "
					+ StringUtils.join(folders));
			for(final String folder : uniqueFolders)
				if(folder.isEmpty())
					throw new IllegalArgumentException("Empty folders detected, it must be something other than the base folder");
		}
		return folders;
	}

	public final void createPackage(final Path projectPath){
		//package the entire folder into a ZIP file
		try{
			final Path projectFolder = projectPath.toRealPath(LinkOption.NOFOLLOW_LINKS);
			final Path outputPath = Path.of(projectFolder.toString(),
					FilenameUtils.getBaseName(projectFolder.toString()) + EXTENSION_ZIP)
				.toAbsolutePath()
				.normalize();

			packageExtension(projectFolder, outputPath);

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Package created");

			FileHelper.browse(projectPath.toFile());
		}
		catch(final InterruptedException | IOException e){
			LOGGER.error(ParserManager.MARKER_APPLICATION, "Package error: {}", e.getMessage());

			LOGGER.error("Something very bad happened while creating package", e);
		}
	}

	private void packageExtension(final Path baseFolder, final Path outputPath) throws IOException{
		final Path autoCorrectPath = getAutoCorrectPath();
		final Set<Path> autoTextPaths = getAutoTextPaths();

		//routes to skip in the main walk
		final Set<Path> skipPaths = new HashSet<>(3);
		if(autoCorrectPath != null)
			skipPaths.add(autoCorrectPath);
		if(autoTextPaths != null)
			skipPaths.addAll(autoTextPaths);

		try(final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outputPath.toFile())))){
			zos.setLevel(Deflater.BEST_COMPRESSION);

			extracted(baseFolder, skipPaths, zos, outputPath);


			//add autocorrect:
			if(autoCorrectPath != null){
				final String packageFilename = FILENAME_PREFIX_AUTO_CORRECT + language + EXTENSION_DAT;
				final Path autoCorrectOutputPath = Path.of(autoCorrectPath.toString(), packageFilename);
				final byte[] nested = buildNestedZip(autoCorrectPath, autoCorrectOutputPath);
				final String entryName = relativeEntryName(baseFolder, autoCorrectOutputPath);
				zos.putNextEntry(new ZipEntry(entryName));
				zos.write(nested);
				zos.closeEntry();
			}

			//add autotext:
			if(autoTextPaths != null)
				for(final Path path : autoTextPaths){
					final String packageFilename = FILENAME_PREFIX_AUTO_TEXT + language + EXTENSION_BAU;
					final Path autoTextOutputPath = Path.of(path.toString(), packageFilename);
					final byte[] nested = buildNestedZip(path, autoTextOutputPath);
					final String entryName = relativeEntryName(baseFolder, autoTextOutputPath);
					zos.putNextEntry(new ZipEntry(entryName));
					zos.write(nested);
					zos.closeEntry();
				}
		}
		catch(final RuntimeException rte){
			if(rte.getCause() instanceof IOException)
				throw (IOException)rte.getCause();

			throw rte;
		}
	}

	private Path getAutoCorrectPath(){
		File autoCorrectFile = (File)configurationFiles.get(FILENAME_AUTO_CORRECT);
		if(autoCorrectFile == null)
			autoCorrectFile = (File)configurationFiles.get(FILENAME_SENTENCE_EXCEPTIONS);
		if(autoCorrectFile == null)
			autoCorrectFile = (File)configurationFiles.get(FILENAME_WORD_EXCEPTIONS);
		return (autoCorrectFile != null? autoCorrectFile.toPath().getParent(): null);
	}

	private Set<Path> getAutoTextPaths(){
		final File[] autoTextFiles = (File[])configurationFiles.get(CONFIGURATION_NODE_NAME_AUTO_TEXT);
		if(autoTextFiles != null){
			final Set<Path> set = new HashSet<>(autoTextFiles.length);
			for(final File file : autoTextFiles){
				final File inputFolder = new File(FilenameUtils.removeExtension(file.getAbsolutePath()));
				set.add(inputFolder.toPath().getParent());
			}
			return set;
		}
		return null;
	}

	/**
	 * Builds a nested ZIP archive from the contents of the specified directory.
	 * <p>
	 * This method traverses the directory tree, adds directories and files to the ZIP archive, and applies specific
	 * logic, such as stripping comments from certain file types.
	 * </p>
	 *
	 * @param dir	The root directory to be archived into a nested ZIP. Must be a valid, readable path.
	 * @param outputPath	Name of the output ZIP file. Must be a valid, writable path.
	 * @return	A byte array containing the resulting ZIP archive.
	 * @throws IOException	If an I/O error occurs while reading the directory or writing the ZIP archive.
	 */
	private static byte[] buildNestedZip(final Path dir, final Path outputPath) throws IOException{
		final ByteArrayOutputStream baos = new ByteArrayOutputStream(64 * 1024);
		try(final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(baos))){
			zos.setLevel(Deflater.BEST_COMPRESSION);

			final Path baseFolder = dir.toRealPath(LinkOption.NOFOLLOW_LINKS);
			final Set<Path> skipPaths = new HashSet<>(0);

			extracted(baseFolder, skipPaths, zos, outputPath);
		}
		catch(final IOException ioe){
			LOGGER.error("Cannot package folder `{}`", dir.toFile().getName(), ioe);

			throw ioe;
		}
		return baos.toByteArray();
	}

	private static void extracted(final Path baseFolder, final Set<Path> skipPaths, final ZipOutputStream zos,
			final Path outputPath) throws IOException{
		Files.walkFileTree(baseFolder, new SimpleFileVisitor<>(){
			@Override
			public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
					throws IOException{
				//skip special folders
				if(isSamePath(dir, skipPaths))
					return FileVisitResult.SKIP_SUBTREE;

				String entryName = relativeEntryName(baseFolder, dir);
				if(entryName != null){
					if(!entryName.endsWith(SLASH))
						entryName = entryName + SLASH;
					zos.putNextEntry(new ZipEntry(entryName));

					zos.closeEntry();
				}

				return FileVisitResult.CONTINUE;
			}

			private static boolean isSamePath(final Path a, final Set<Path> b){
				return (a != null && b != null && equalsPath(a, b));
			}

			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException{
				//skip output file
				if(equalsPath(file, outputPath))
					return FileVisitResult.CONTINUE;

				final String entryName = relativeEntryName(baseFolder, file);
				if(entryName != null){
					zos.putNextEntry(new ZipEntry(entryName));

					final String nameOnly = file.getFileName()
						.toString();
					if(shouldStrip(nameOnly))
						copyStrippingComments(file, zos);
					else
						copyBinary(file, zos);

					zos.closeEntry();
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private static boolean equalsPath(final Path a, final Path b){
		try{
			return a.toRealPath()
				.equals(b.toRealPath());
		}
		catch(final IOException ignored){
			return a.toAbsolutePath().normalize()
				.equals(b.toAbsolutePath().normalize());
		}
	}

	private static boolean equalsPath(final Path a, final Set<Path> pathsB){
		try{
			final Path realPathA = a.toRealPath();
			for(final Path pathB : pathsB)
				if(pathB.toRealPath().equals(realPathA))
					return true;
		}
		catch(final IOException ignored){
			final Path absolutePathA = a.toAbsolutePath()
				.normalize();
			for(final Path pathB : pathsB)
				if(pathB.toAbsolutePath().normalize().equals(absolutePathA))
					return true;
		}
		return false;
	}

	private static String relativeEntryName(final Path base, final Path path){
		final String relativeName = base.relativize(path)
			.toString()
			.replace('\\', '/');
		return (relativeName.isEmpty()? null: relativeName);
	}

	private static boolean shouldStrip(final String fileName){
		return (fileName.endsWith(FILE_EXTENSION_AFFIX) || fileName.endsWith(FILE_EXTENSION_DICTIONARY));
	}

	private static void copyStrippingComments(final Path path, final ZipOutputStream out) throws IOException{
		try(final BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
			String line;
			while((line = br.readLine()) != null){
				line = stripComment(line);
				if(line != null){
					final byte[] bytes = (line + '\n').getBytes(StandardCharsets.UTF_8);
					out.write(bytes);
				}
			}
		}
	}

	private static void copyBinary(final Path path, final ZipOutputStream out) throws IOException{
		try(final InputStream in = new BufferedInputStream(new FileInputStream(path.toFile()))){
			in.transferTo(out);
		}
	}

	public static String stripComment(final String line){
		final String out = CUT_PATTERN.matcher(line)
			.replaceFirst(StringUtils.EMPTY);
		return (out.trim().isEmpty()? null: out);
	}

	public final String getLanguage(){
		return language;
	}

	/**
	 * Extracts a sample from an affix file
	 *
	 * @return	A sample text
	 *
	 * @see AffixData#getSampleText()
	 */
	public final String getSampleText(){
		String sampleText = "The quick brown fox jumps over the lazy dog\n0123456789";
		final File affFile = getAffixFile();
		if(affFile != null){
			try{
				final Path affPath = affFile.toPath();
				final Charset charset = FileHelper.determineCharset(affPath, -1);
				final CharSequence content = Files.readString(affPath, charset);
				final List<String> extractions = RegexHelper.extract(content, LANGUAGE_SAMPLE_EXTRACTOR, 10);
				sampleText = String.join(StringUtils.EMPTY, String.join(StringUtils.EMPTY, extractions).chars()
					.mapToObj(Character::toString)
					.collect(Collectors.toSet()));
			}
			catch(final IOException ignored){}
		}
		return sampleText;
	}

	public final Path getProjectPath(){
		return projectPath;
	}

	public final File getFile(final String key){
		return (File)configurationFiles.get(KEY_FILE_MAPPER.get(key));
	}

	public final File[] getFiles(final String key){
		return (File[])configurationFiles.get(KEY_FILE_MAPPER.get(key));
	}

	public final File getAffixFile(){
		return getFile(KEY_FILE_AFFIX);
	}

	public final File getDictionaryFile(){
		return getFile(KEY_FILE_DICTIONARY);
	}

	public final File getHyphenationFile(){
		return getFile(KEY_FILE_HYPHENATION);
	}

	public final File getThesaurusDataFile(){
		return getFile(KEY_FILE_THESAURUS_DATA);
	}

	public final File getThesaurusIndexFile(){
		return getFile(KEY_FILE_THESAURUS_INDEX);
	}

	public final File getAutoCorrectFile(){
		return getFile(KEY_FILE_AUTO_CORRECT);
	}

	public final File getSentenceExceptionsFile(){
		return getFile(KEY_FILE_SENTENCE_EXCEPTIONS);
	}

	public final File getWordExceptionsFile(){
		return getFile(KEY_FILE_WORD_EXCEPTIONS);
	}

	public final File[] getAutoTextFiles(){
		return getFiles(KEY_FILE_AUTO_TEXT);
	}

	/** Go up directories until description.xml or manifest.json is found. */
	private static Path getPackageBaseDirectory(final File affFile){
		Path parentPath = affFile.toPath()
			.getParent();
		while(parentPath != null && !isFileExisting(parentPath, FILENAME_DESCRIPTION_XML)
				&& !isFileExisting(parentPath, FILENAME_MANIFEST_JSON))
			parentPath = parentPath.getParent();
		return parentPath;
	}

	private static boolean isDirectoryExisting(final Path path){
		return Files.isDirectory(path);
	}

	private static boolean isFileExisting(final Path path){
		return Files.isRegularFile(path);
	}

	private static boolean isFileExisting(final Path path, final String filename){
		return Files.isRegularFile(Paths.get(path.toString(), filename));
	}

	private List<String> extractFileEntries(final File manifestFile) throws IOException, SAXException{
		final Document doc = xmlManager.parseXMLDocument(manifestFile);

		final Element rootElement = doc.getDocumentElement();
		if(!MANIFEST_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new IllegalArgumentException("Invalid root element, expected '" + MANIFEST_ROOT_ELEMENT + "', was "
				+ rootElement.getNodeName());

		final List<Node> children = extractChildren(rootElement);
		final ArrayList<String> configurationPaths = new ArrayList<>(children.size());
		for(final Node child : children){
			final Node mediaType = XMLManager.extractAttribute(child, MANIFEST_FILE_ENTRY_MEDIA_TYPE);
			if(mediaType != null && MANIFEST_MEDIA_TYPE_CONFIGURATION_DATA.equals(mediaType.getNodeValue()))
				configurationPaths.add(XMLManager.extractAttributeValue(child, MANIFEST_FILE_ENTRY_FULL_PATH));
		}
		return configurationPaths;
	}

	private Pair<Path, Node> findConfiguration(final String configurationName, final Iterable<File> configurationFiles)
			throws IOException, SAXException{
		for(final File configurationFile : configurationFiles){
			final Document doc = xmlManager.parseXMLDocument(configurationFile);

			final Element rootElement = doc.getDocumentElement();
			if(!CONFIGURATION_ROOT_ELEMENT.equals(rootElement.getNodeName()))
				throw new IllegalArgumentException("Invalid root element, expected '" + CONFIGURATION_ROOT_ELEMENT
					+ "', was " + rootElement.getNodeName());

			final Node foundNode = onNodeNameApply(rootElement, configurationName, Function.identity());
			if(foundNode != null)
				return Pair.of(configurationFile.toPath(), foundNode);
		}
		return Pair.of(null, null);
	}

	private Map<String, Object> getFolders(final String language, final Node parentNode, final Path basePath,
			final Path originPath) throws IOException{
		final Map<String, Object> folders = new HashMap<>(0);
		final List<Node> children = extractChildren(parentNode);
		for(final Node child : children){
			final Node node = XMLManager.extractAttribute(child, CONFIGURATION_NODE_NAME);
			if(node == null)
				continue;

			//extract folder(s)
			final String nodeValue = node.getNodeValue();
			if(CONFIGURATION_NODE_NAME_DICTIONARIES.equals(nodeValue))
				getFoldersForDictionaries(language, child, basePath, originPath, folders);
			else
				folders.putAll(getFoldersForInternalPaths(child, nodeValue, basePath, originPath));
		}
		return folders;
	}

	private void getFoldersForDictionaries(final String language, final Node entry, final Path basePath,
			final Path originPath, final Map<String, Object> folders) throws IOException{
		//restrict to given language
		final List<Node> children = extractChildren(entry);
		children.removeIf(node -> !ArrayUtils.contains(extractLocale(node), language));
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

	private Map<String, Object> getFoldersForInternalPaths(final Node entry, final String nodeValue, final Path basePath,
			final Path originPath) throws IOException{
		final String folder = onNodeNameApply(entry, CONFIGURATION_NODE_NAME_INTERNAL_PATHS, Packager::extractFolder);
		Objects.requireNonNull(folder, "Folder cannot be null");

		final File file = absolutizeFolder(folder, basePath, originPath);
		final Map<String, Object> children = new HashMap<>(3);
		if(CONFIGURATION_NODE_NAME_AUTO_CORRECT.equals(nodeValue)){
			autoCorrectPath = file.toPath();

			children.put(FILENAME_AUTO_CORRECT, Path.of(file.toString(), FILENAME_AUTO_CORRECT).toFile());
			children.put(FILENAME_SENTENCE_EXCEPTIONS, Path.of(file.toString(), FILENAME_SENTENCE_EXCEPTIONS).toFile());
			children.put(FILENAME_WORD_EXCEPTIONS, Path.of(file.toString(), FILENAME_WORD_EXCEPTIONS).toFile());
		}
		else if(CONFIGURATION_NODE_NAME_AUTO_TEXT.equals(nodeValue)){
			autoTextPath = file.toPath();

			final Set<File> autotextFiles = new HashSet<>(0);
			final File[] autotextFolders = file.listFiles((dir, name) -> new File(dir, name).isDirectory());
			if(autotextFolders != null)
				autotextFiles.addAll(Arrays.asList(autotextFolders));
			final File[] ff = file.listFiles((dir, name) -> name.endsWith(EXTENSION_BAU));
			if(ff != null)
				for(final File f : ff)
					autotextFiles.add(new File(FilenameUtils.removeExtension(f.getAbsolutePath())));
			children.put(CONFIGURATION_NODE_NAME_AUTO_TEXT, autotextFiles.toArray(new File[0]));
		}
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
		final Path truePath = Path.of(currentParentPath.toString(), RegexHelper.split(folder, FOLDER_SPLITTER));
		return Path.of(truePath.toFile().getCanonicalPath())
			.toFile();
	}

	private static String extractLocation(final Node parentNode){
		return extractProperty(parentNode, CONFIGURATION_NODE_NAME_LOCATIONS);
	}

	private static String[] extractLocale(final Node parentNode){
		final String locale = extractProperty(parentNode, CONFIGURATION_NODE_NAME_LOCALES);
		return StringUtils.split(locale);
	}

	private static String extractProperty(final Node parentNode, final String propertyName){
		final NodeList nodes = parentNode.getChildNodes();
		for(int i = 0; i < nodes.getLength(); i ++){
			final Node node = nodes.item(i);
			if(XMLManager.isElement(node, CONFIGURATION_PROPERTY)
					&& propertyName.equals(XMLManager.extractAttributeValue(node, CONFIGURATION_NODE_NAME)))
				return node.getChildNodes().item(1).getFirstChild().getNodeValue();
		}
		return null;
	}

	private static String extractFolder(final Node parentNode){
		final List<Node> children = extractChildren(parentNode);
		return (!children.isEmpty()? XMLManager.extractAttributeValue(children.getFirst(), CONFIGURATION_NODE_NAME): null);
	}

	private static <T> T onNodeNameApply(final Node parentNode, final String nodeName, final Function<Node, T> fun){
		final List<Node> children = extractChildren(parentNode);
		for(final Node child : children){
			final Node node = XMLManager.extractAttribute(child, CONFIGURATION_NODE_NAME);
			if(node != null && nodeName.equals(node.getNodeValue()))
				return fun.apply(child);
		}
		return null;
	}

	private static List<Node> extractChildren(final Element parentElement){
		return XMLManager.extractChildren(parentElement,
			node -> (node.getNodeType() == Node.ELEMENT_NODE && MANIFEST_FILE_ENTRY.equals(node.getNodeName())));
	}

	private static List<Node> extractChildren(final Node parentNode){
		return XMLManager.extractChildren(parentNode,
			node -> XMLManager.isElement(node, CONFIGURATION_NODE));
	}

}
