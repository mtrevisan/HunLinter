package unit731.hunspeller.parsers.autocorrect;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import unit731.hunspeller.parsers.thesaurus.DuplicationResult;
import unit731.hunspeller.services.XMLParser;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;


public class AutoCorrectParser{

	private static final Logger LOGGER = LoggerFactory.getLogger(AutoCorrectParser.class);

	private static final MessageFormat DUPLICATE_DETECTED = new MessageFormat("Duplicate detected for ''{0}''");

	private static final String AUTO_CORRECT_ROOT_ELEMENT = "block-list:block-list";
	private static final String AUTO_CORRECT_BLOCK = "block-list:block";
	private static final String AUTO_CORRECT_INCORRECT_FORM = "block-list:abbreviated-name";
	private static final String AUTO_CORRECT_CORRECT_FORM = "block-list:name";


	private final List<CorrectionEntry> dictionary = new ArrayList<>();


	/**
	 * Parse the rows out from a `DocumentList.xml` file.
	 *
	 * @param acoPath	The reference to the auto-correct file
	 * @throws IOException	If an I/O error occurs
	 * @throws SAXException	If an parsing error occurs on the `xml` file
	 */
	public void parse(final Path acoPath) throws IOException, SAXException{
		clear();

		final Document doc = XMLParser.parseXMLDocument(acoPath);

		final Element rootElement = doc.getDocumentElement();
		if(!AUTO_CORRECT_ROOT_ELEMENT.equals(rootElement.getNodeName()))
			throw new IllegalArgumentException("Invalid root element, expected '" + AUTO_CORRECT_ROOT_ELEMENT + "', was "
				+ rootElement.getNodeName());

		final NodeList entries = rootElement.getChildNodes();
		for(int i = 0; i < entries.getLength(); i ++){
			final Node entry = entries.item(i);
			if(XMLParser.isElement(entry, AUTO_CORRECT_BLOCK)){
				final Node mediaType = XMLParser.extractAttribute(entry, AUTO_CORRECT_INCORRECT_FORM);
				if(mediaType != null)
					dictionary.add(new CorrectionEntry(mediaType.getNodeValue(),
						XMLParser.extractAttributeValue(entry, AUTO_CORRECT_CORRECT_FORM)));
			}
		}
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
		boolean forceInsertion = false;
		final List<CorrectionEntry> duplicates = extractDuplicates(incorrect, correct);
		if(!duplicates.isEmpty()){
			forceInsertion = duplicatesDiscriminator.get();
			if(!forceInsertion)
				throw new IllegalArgumentException(DUPLICATE_DETECTED.format(
					new Object[]{duplicates.stream().map(CorrectionEntry::toString).collect(Collectors.joining(", "))}));
		}

		if(duplicates.isEmpty() || forceInsertion)
			dictionary.add(new CorrectionEntry(incorrect, correct));

		return new DuplicationResult(duplicates, forceInsertion);
	}

	public void deleteCorrections(final int[] selectedRowIDs){
		final int count = selectedRowIDs.length;
		for(int i = 0; i < count; i ++)
			dictionary.remove(selectedRowIDs[i] - i);
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
			.anyMatch(elem -> elem.getIncorrectForm().equals(incorrect) && elem.getCorrectForm().equals(correct));
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

//	public void setCorrection(final int index, final String text){
	//FIXME
//		dictionary.setMeanings(index, text);
//	}

	public void save(final Path acoPath) throws IOException{
		//FIXME
//		//sort the synonyms
//		dictionary.sort();
//
//		//save index and data files
//		final Charset charset = StandardCharsets.UTF_8;
//		try(final BufferedWriter writer = Files.newBufferedWriter(acoFile.toPath(), charset)){
//			//save charset
//			writer.write(charset.name());
//			writer.write(StringUtils.LF);
//			//save data
//			int idx = charset.name().length() + 1;
//			final int doubleLineTerminatorLength = StringUtils.LF.length() * 2;
//			final List<CorrectionEntry> synonyms = dictionary.getSynonyms();
//			for(CorrectionEntry synonym : synonyms){
//				synonym.saveToIndex(indexWriter, idx);
//
//				int meaningsLength = synonym.saveToData(writer, charset);
//
//				idx += synonym.getSynonym().getBytes(charset).length + meaningsLength + doubleLineTerminatorLength;
//			}
//		}
	}

	public void clear(){
		dictionary.clear();
	}

}
