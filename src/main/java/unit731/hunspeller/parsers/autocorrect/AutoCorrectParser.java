package unit731.hunspeller.parsers.autocorrect;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.thesaurus.DuplicationResult;
import unit731.hunspeller.parsers.workers.exceptions.HunspellException;
import unit731.hunspeller.services.XMLParser;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/** Manages pairs of mistyped words and their correct spelling */
public class AutoCorrectParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCorrectParser.class);

	private static final MessageFormat BAD_INCORRECT_QUOTE = new MessageFormat("Incorrect form cannot contain apostrophes or double quotes: ''{0}''");
	private static final MessageFormat BAD_CORRECT_QUOTE = new MessageFormat("Correct form cannot contain apostrophes or double quotes: ''{0}''");
	private static final MessageFormat DUPLICATE_DETECTED = new MessageFormat("Duplicate detected for ''{0}''");

	private static final String AUTO_CORRECT_NAMESPACE = "block-list:";
	private static final String AUTO_CORRECT_ROOT_ELEMENT = AUTO_CORRECT_NAMESPACE + "block-list";
	private static final String AUTO_CORRECT_BLOCK = AUTO_CORRECT_NAMESPACE + "block";
	private static final String AUTO_CORRECT_INCORRECT_FORM = AUTO_CORRECT_NAMESPACE + "abbreviated-name";
	private static final String AUTO_CORRECT_CORRECT_FORM = AUTO_CORRECT_NAMESPACE + "name";


	private final List<CorrectionEntry> dictionary = new ArrayList<>();


	/**
	 * Parse the rows out from a `DocumentList.xml` file.
	 *
	 * @param acoFile	The reference to the auto–correct file
	 * @throws IOException	If an I/O error occurs
	 * @throws SAXException	If an parsing error occurs on the `xml` file
	 */
	public void parse(final File acoFile) throws IOException, SAXException{
		clear();

		final Document doc = XMLParser.parseXMLDocument(acoFile);

		final Element rootElement = doc.getDocumentElement();
		if(!AUTO_CORRECT_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new HunspellException("Invalid root element, expected '" + AUTO_CORRECT_ROOT_ELEMENT + "', was "
				+ rootElement.getNodeName());

		final List<Node> children = XMLParser.extractChildren(rootElement, node -> XMLParser.isElement(node, AUTO_CORRECT_BLOCK));
		for(final Node child : children){
			final Node mediaType = XMLParser.extractAttribute(child, AUTO_CORRECT_INCORRECT_FORM);
			if(mediaType != null){
				final CorrectionEntry correctionEntry = new CorrectionEntry(mediaType.getNodeValue(),
					XMLParser.extractAttributeValue(child, AUTO_CORRECT_CORRECT_FORM));
				dictionary.add(correctionEntry);
			}
		}

		validate();
	}

	private void validate(){
		//check for duplications
		final List<List<CorrectionEntry>> duplications = dictionary.stream()
			.collect(Collectors.groupingBy(CorrectionEntry::getIncorrectForm))
			.values().stream()
			.filter(list -> list.size() > 1)
			.collect(Collectors.toList());
		for(final List<CorrectionEntry> duplication : duplications)
			LOGGER.info(Backbone.MARKER_APPLICATION, "Duplicated entry in auto–correct file: incorrect form '{}', correct forms '{}'",
				duplication.get(0).getIncorrectForm(),
				duplication.stream().map(CorrectionEntry::getCorrectForm).collect(Collectors.toList()));
	}

	public List<CorrectionEntry> getCorrectionsDictionary(){
		return dictionary;
	}

	public int getCorrectionsCounter(){
		return dictionary.size();
	}

	public void setCorrection(final int index, final String incorrect, final String correct){
		dictionary.set(index, new CorrectionEntry(incorrect, correct));
	}

	/**
	 * @param incorrect	The incorrect form
	 * @param correct	The correct form
	 * @param duplicatesDiscriminator	Function called to ask the user what to do if duplicates are found
	 * 	(return <code>true</code> to force insertion)
	 * @return The duplication result
	 */
	public DuplicationResult<CorrectionEntry> insertCorrection(final String incorrect, final String correct,
			final Supplier<Boolean> duplicatesDiscriminator){
		if(incorrect.contains("'") || incorrect.contains("\""))
			throw new HunspellException(BAD_INCORRECT_QUOTE.format(new Object[]{incorrect}));
		if(correct.contains("'") || correct.contains("\""))
			throw new HunspellException(BAD_CORRECT_QUOTE.format(new Object[]{incorrect}));

		boolean forceInsertion = false;
		final List<CorrectionEntry> duplicates = extractDuplicates(incorrect, correct);
		if(!duplicates.isEmpty()){
			forceInsertion = duplicatesDiscriminator.get();
			if(!forceInsertion)
				throw new HunspellException(DUPLICATE_DETECTED.format(
					new Object[]{duplicates.stream().map(CorrectionEntry::toString).collect(Collectors.joining(", "))}));
		}

		if(duplicates.isEmpty() || forceInsertion)
			dictionary.add(new CorrectionEntry(incorrect, correct));

		return new DuplicationResult<>(duplicates, forceInsertion);
	}

	public void deleteCorrections(final int[] selectedRowIDs){
		IntStream.range(0, selectedRowIDs.length)
			.map(idx -> selectedRowIDs[idx] - idx)
			.forEach(dictionary::remove);
	}

	/** Find if there is a duplicate with the same incorrect and correct forms */
	private List<CorrectionEntry> extractDuplicates(final String incorrect, final String correct){
		return dictionary.stream()
			.filter(correction -> correction.getIncorrectForm().equals(incorrect) && correction.getCorrectForm().equals(correct))
			.collect(Collectors.toList());
	}

	/** Find if there is a duplicate with the same incorrect and correct forms */
	public boolean isAlreadyContained(final String incorrect, final String correct){
		return dictionary.stream()
			.anyMatch(elem -> !incorrect.isEmpty() && !correct.isEmpty()
				&& elem.getIncorrectForm().equals(incorrect) && elem.getCorrectForm().equals(correct));
	}

	public static Pair<String, String> extractComponentsForFilter(final String incorrect, final String correct){
		return Pair.of(clearFilter(incorrect), clearFilter(correct));
	}

	private static String clearFilter(final String text){
		//escape special characters
		return Matcher.quoteReplacement(StringUtils.strip(text));
	}

	public static Pair<String, String> prepareTextForFilter(final String incorrect, String correct){
		//extract part of speech if present
		final String incorrectFilter = (!incorrect.isEmpty()? incorrect: ".+");
		final String correctFilter = (!correct.isEmpty()? correct: ".+");

		//compose filter regexp
		return Pair.of(incorrectFilter, correctFilter);
	}

	public void save(final File acoFile) throws TransformerException{
		final Document doc = XMLParser.newXMLDocumentStandalone();

		//root element
		final Element root = doc.createElement(AUTO_CORRECT_ROOT_ELEMENT);
		root.setAttribute(XMLParser.ROOT_ATTRIBUTE_NAME, XMLParser.ROOT_ATTRIBUTE_VALUE);
		doc.appendChild(root);

		for(final CorrectionEntry correction : dictionary){
			//correction element
			final Element elem = doc.createElement(AUTO_CORRECT_BLOCK);
			elem.setAttribute(AUTO_CORRECT_INCORRECT_FORM, correction.getIncorrectForm());
			elem.setAttribute(AUTO_CORRECT_CORRECT_FORM, correction.getCorrectForm());
			root.appendChild(elem);
		}

		XMLParser.createXML(acoFile, doc, XMLParser.XML_PROPERTIES);
	}

	public void clear(){
		dictionary.clear();
	}

}
