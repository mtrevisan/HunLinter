package unit731.hunlinter.parsers.autocorrect;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.thesaurus.DuplicationResult;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.XMLManager;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static unit731.hunlinter.services.system.LoopHelper.applyIf;
import static unit731.hunlinter.services.system.LoopHelper.forEach;
import static unit731.hunlinter.services.system.LoopHelper.match;


/** Manages pairs of mistyped words and their correct spelling */
public class AutoCorrectParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCorrectParser.class);

	private static final MessageFormat BAD_QUOTE = new MessageFormat("{0} form cannot contain apostrophes or double quotes: ''{1}''");

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

		final Document doc = XMLManager.parseXMLDocument(acoFile);

		final Element rootElement = doc.getDocumentElement();
		if(!AUTO_CORRECT_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new LinterException("Invalid root element, expected '" + AUTO_CORRECT_ROOT_ELEMENT + "', was "
				+ rootElement.getNodeName());

		final List<Node> children = XMLManager.extractChildren(rootElement, node -> XMLManager.isElement(node, AUTO_CORRECT_BLOCK));
		for(final Node child : children){
			final Node mediaType = XMLManager.extractAttribute(child, AUTO_CORRECT_INCORRECT_FORM);
			if(mediaType != null){
				final CorrectionEntry correctionEntry = new CorrectionEntry(mediaType.getNodeValue(),
					XMLManager.extractAttributeValue(child, AUTO_CORRECT_CORRECT_FORM));
				dictionary.add(correctionEntry);
			}
		}

		validate();
	}

	private void validate(){
		//check for duplications
		final Map<String, List<CorrectionEntry>> map = new HashMap<>();
		forEach(dictionary,
			correctionEntry -> map.computeIfAbsent(correctionEntry.getIncorrectForm(), k -> new ArrayList<>(1)).add(correctionEntry));
		final List<List<CorrectionEntry>> duplications = new ArrayList<>(map.size());
		applyIf(map.values(),
			list -> list.size() > 1,
			duplications::add);
		for(final List<CorrectionEntry> duplication : duplications)
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Duplicated entry in auto–correct file: incorrect form '{}', correct forms '{}'",
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
			throw new LinterException(BAD_QUOTE.format(new Object[]{"Incorrect", incorrect}));
		if(correct.contains("'") || correct.contains("\""))
			throw new LinterException(BAD_QUOTE.format(new Object[]{"Correct", correct}));

		final List<CorrectionEntry> duplicates = extractDuplicates(incorrect, correct);
		final boolean forceInsertion = (duplicates.isEmpty() || duplicatesDiscriminator.get());
		if(forceInsertion)
			dictionary.add(new CorrectionEntry(incorrect, correct));

		return new DuplicationResult<>(duplicates, forceInsertion);
	}

	public void deleteCorrection(final int selectedRowID){
		dictionary.remove(selectedRowID);
	}

	/* Find if there is a duplicate with the same incorrect and correct forms */
	private List<CorrectionEntry> extractDuplicates(final String incorrect, final String correct){
		final ArrayList<CorrectionEntry> duplicates = new ArrayList<>(dictionary.size());
		applyIf(dictionary,
			correction -> correction.getIncorrectForm().equals(incorrect) && correction.getCorrectForm().equals(correct),
			duplicates::add);
		duplicates.trimToSize();
		return duplicates;
	}

	/* Find if there is a duplicate with the same incorrect and correct forms */
	public boolean contains(final String incorrect, final String correct){
		return (match(dictionary,
			elem -> !incorrect.isEmpty() && !correct.isEmpty()
				&& elem.getIncorrectForm().equals(incorrect) && elem.getCorrectForm().equals(correct)) != null);
	}

	public static Pair<String, String> extractComponentsForFilter(final String incorrect, final String correct){
		return Pair.of(clearFilter(incorrect), clearFilter(correct));
	}

	private static String clearFilter(final String text){
		//escape special characters
		return Matcher.quoteReplacement(text.trim());
	}

	public static Pair<String, String> prepareTextForFilter(final String incorrect, String correct){
		//extract part–of–speech if present
		final String incorrectFilter = (!incorrect.isEmpty()? incorrect: ".+");
		final String correctFilter = (!correct.isEmpty()? correct: ".+");

		//compose filter regexp
		return Pair.of(incorrectFilter, correctFilter);
	}

	public void save(final File acoFile) throws TransformerException{
		final Document doc = XMLManager.newXMLDocumentStandalone();

		//root element
		final Element root = doc.createElement(AUTO_CORRECT_ROOT_ELEMENT);
		root.setAttribute(XMLManager.ROOT_ATTRIBUTE_NAME, XMLManager.ROOT_ATTRIBUTE_VALUE);
		doc.appendChild(root);

		for(final CorrectionEntry correction : dictionary){
			//correction element
			final Element elem = doc.createElement(AUTO_CORRECT_BLOCK);
			elem.setAttribute(AUTO_CORRECT_INCORRECT_FORM, correction.getIncorrectForm());
			elem.setAttribute(AUTO_CORRECT_CORRECT_FORM, correction.getCorrectForm());
			root.appendChild(elem);
		}

		XMLManager.createXML(acoFile, doc, XMLManager.XML_PROPERTIES_US_ASCII);
	}

	public void clear(){
		dictionary.clear();
	}

}
