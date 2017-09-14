package unit731.hunspeller.parsers.thesaurus;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.interfaces.Undoable;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.PatternService;
import unit731.hunspeller.services.memento.Caretaker;


@Slf4j
@Getter
public class ThesaurusParser{

	private static final Pattern REGEX_PATTERN_LF = PatternService.pattern(StringUtils.LF);


	private final List<ThesaurusEntry> synonyms = new ArrayList<>();
	private boolean modified = false;

	private final Undoable undoable;
	private final Caretaker<ThesaurusEntry> undoCaretaker = new Caretaker<>(ThesaurusEntry.class);
	private final Caretaker<ThesaurusEntry> redoCaretaker = new Caretaker<>(ThesaurusEntry.class);


	public ThesaurusParser(Undoable undoable){
		this.undoable = undoable;
	}

	@AllArgsConstructor
	public static class ParserWorker extends SwingWorker<List<ThesaurusEntry>, String>{

		private final File theFile;
		private final ThesaurusParser theParser;
		private final Runnable postExecution;
		private final Resultable resultable;


		@Override
		protected List<ThesaurusEntry> doInBackground() throws Exception{
			try{
				publish("Opening Thesaurus file: " + theFile.getName());
				setProgress(0);

				long readSoFar = 0l;
				long totalSize = theFile.length();

				Charset charset = FileService.determineCharset(theFile.toPath());
				try(BufferedReader br = Files.newBufferedReader(theFile.toPath(), charset)){
					String line = br.readLine();

					while((line = br.readLine()) != null){
						readSoFar += line.length();
						if(!line.isEmpty()){
							ThesaurusEntry thesaurusIndex = new ThesaurusEntry(line, br);
							theParser.synonyms.add(thesaurusIndex);
						}

						setProgress((int)((readSoFar * 100.) / totalSize));
					}
					setProgress(100);
				}

				publish("Finished reading Thesaurus file");
			}
			catch(IOException | IllegalArgumentException e){
				publish(e.getClass().getSimpleName() + ": " + e.getMessage());
			}
			return theParser.synonyms;
		}

		@Override
		protected void process(List<String> chunks){
			resultable.printResultLine(chunks);
		}

		@Override
		protected void done(){
			if(postExecution != null)
				postExecution.run();
		}
	};

	public int getSynonymsCounter(){
		return synonyms.size();
	}

	/**
	 * @param synonymAndMeanings	The line representing all the synonyms of a word along with their part of speech
	 * @param duplicatesDiscriminator	Function called to ask the user what to do is duplicates are found (return <code>true</code> to force insertion)
	 * @return The duplication result
	 */
	public DuplicationResult insertMeanings(String synonymAndMeanings, Supplier<Boolean> duplicatesDiscriminator){
		String[] partOfSpeechAndMeanings = PatternService.split(synonymAndMeanings, ThesaurusEntry.REGEX_PATTERN_ESCAPED_PIPE, 2);
		String partOfSpeech = StringUtils.strip(partOfSpeechAndMeanings[0]);
		if(!partOfSpeech.startsWith("(") || !partOfSpeech.endsWith(")"))
			throw new IllegalArgumentException("Part of speech is not in parenthesis: " + synonymAndMeanings);

		String[] meanings = PatternService.split(partOfSpeechAndMeanings[1], ThesaurusEntry.REGEX_PATTERN_ESCAPED_PIPE);
		List<String> means = Arrays.stream(meanings)
			.map(String::trim)
			.filter(StringUtils::isNotBlank)
			.distinct()
			.collect(Collectors.toList());
		if(means.size() < 1)
			throw new IllegalArgumentException("Not enough meanings are supplied (at least one should be present): " + synonymAndMeanings);

		DuplicationResult duplicationResult = extractDuplicates(means, partOfSpeech, duplicatesDiscriminator);

		if(duplicationResult.isForcedInsertion() || duplicationResult.getDuplicates().isEmpty()){
			try{
				undoCaretaker.pushMemento(synonyms);

				if(undoable != null)
					undoable.onUndoChange(true);
			}
			catch(IOException ex){
				log.warn("Error while storing a memento", ex);
			}

			for(String meaning : means){
				//insert the new synonym
				StringJoiner sj = new StringJoiner(ThesaurusEntry.PIPE);
				sj.add(partOfSpeech);
				means.stream()
					.filter(m -> !m.equals(meaning))
					.forEachOrdered(sj::add);
				String mm = sj.toString();

				ThesaurusEntry foundSynonym = null;
				for(ThesaurusEntry synonym : this.synonyms)
					if(synonym.getSynonym().equals(meaning)){
						foundSynonym = synonym;
						break;
					}

				MeaningEntry entry = new MeaningEntry(mm);
				if(foundSynonym != null)
					//add to meanings if synonym does exists
					foundSynonym.getMeanings().add(entry);
				else
					//add to list if synonym does not exists
					this.synonyms.add(new ThesaurusEntry(meaning, Arrays.asList(entry)));
			}

			modified = true;
		}

		return duplicationResult;
	}

	/** Find if there is a duplicate with the same part of speech */
	private DuplicationResult extractDuplicates(List<String> means, String partOfSpeech, Supplier<Boolean> duplicatesDiscriminator) throws IllegalArgumentException{
		boolean forcedInsertion = false;
		List<ThesaurusEntry> duplicates = new ArrayList<>();
		try{
			for(String meaning : means)
				for(ThesaurusEntry synonym : this.synonyms)
					if(synonym.getSynonym().equals(meaning)){
						long countSamePartOfSpeech = synonym.getMeanings().stream()
							.map(MeaningEntry::getPartOfSpeech)
							.filter(pos -> pos.equals(partOfSpeech))
							.count();
						if(countSamePartOfSpeech > 0l)
							throw new IllegalArgumentException("Duplicate detected for " + meaning);
					}
		}
		catch(IllegalArgumentException e){
			if(!duplicatesDiscriminator.get())
				throw e;
			
			duplicates.clear();
			forcedInsertion = true;
		}
		return new DuplicationResult(duplicates, forcedInsertion);
	}

	public void setMeanings(ThesaurusEntry synonym, List<MeaningEntry> meanings, String text){
		try{
			undoCaretaker.pushMemento(synonyms);

			if(StringUtils.isNotBlank(text)){
				String[] lines = PatternService.split(text, REGEX_PATTERN_LF);
				meanings.clear();
				for(String line : lines)
					meanings.add(new MeaningEntry(line));
			}

			if(undoable != null)
				undoable.onUndoChange(true);
		}
		catch(IOException ex){
			log.warn("Error while storing a memento", ex);
		}

		synonym.setMeanings(meanings);

		modified = true;
	}

	public void deleteMeanings(int[] selectedRowIDs){
		int count = selectedRowIDs.length;
		if(count > 0){
			try{
				undoCaretaker.pushMemento(synonyms);

				if(undoable != null)
					undoable.onUndoChange(true);
			}
			catch(IOException ex){
				log.warn("Error while storing a memento", ex);
			}

			for(int i = 0; i < count; i ++)
				synonyms.remove(selectedRowIDs[i] - i);

			modified = true;
		}
	}

	public List<String> extractDuplicates(){
		Set<String> allItems = new HashSet<>();
		List<String> duplicatedSynonyms = synonyms.stream()
			.map(ThesaurusEntry::getSynonym)
			.filter(s -> !allItems.add(s))
			.collect(Collectors.toList());
		return duplicatedSynonyms;
	}

	public void save(File theIndexFile, File theDataFile) throws IOException{
		//sort the synonyms
		synonyms.sort((s0, s1) -> s0.compareTo(s1));

		//save index and data files
		Charset charset = StandardCharsets.UTF_8;
		try(
				BufferedWriter indexWriter = Files.newBufferedWriter(theIndexFile.toPath(), charset);
				BufferedWriter dataWriter = Files.newBufferedWriter(theDataFile.toPath(), charset);
				){
			//save charset
			indexWriter.write(charset.name());
			indexWriter.write(StringUtils.LF);
			//save counter
			indexWriter.write(Integer.toString(synonyms.size()));
			indexWriter.write(StringUtils.LF);
			//save charset
			dataWriter.write(charset.name());
			dataWriter.write(StringUtils.LF);
			//save data
			int idx = charset.name().length() + 1;
			for(ThesaurusEntry synonym : synonyms){
				String syn = synonym.getSynonym();
				indexWriter.write(syn);
				indexWriter.write(ThesaurusEntry.PIPE);
				indexWriter.write(Integer.toString(idx));
				indexWriter.write(StringUtils.LF);

				int meaningsCount = synonym.getMeanings().size();
				dataWriter.write(syn);
				dataWriter.write(ThesaurusEntry.PIPE);
				dataWriter.write(Integer.toString(meaningsCount));
				dataWriter.write(StringUtils.LF);
				List<MeaningEntry> meanings = synonym.getMeanings();
				int meaningsLength = 1;
				for(MeaningEntry meaning : meanings){
					dataWriter.write(meaning.toString());
					dataWriter.write(StringUtils.LF);

					meaningsLength += meaning.toString().getBytes(charset).length + 1;
				}

				idx += syn.getBytes(charset).length + meaningsLength + 2;
			}

			modified = false;
		}
	}

	public void clear(){
		synonyms.clear();
	}

	public boolean canUndo(){
		return undoCaretaker.canUndo();
	}

	public boolean canRedo(){
		return redoCaretaker.canUndo();
	}

	public boolean restorePreviousSnapshot() throws IOException{
		boolean restored = false;
		if(canUndo()){
			redoCaretaker.pushMemento(synonyms);
			List<ThesaurusEntry> poppedSynonyms = undoCaretaker.popMemento();
			if(undoable != null){
				undoable.onUndoChange(canUndo());
				undoable.onRedoChange(true);
			}

			synonyms.clear();
			synonyms.addAll(poppedSynonyms);

			restored = true;
		}
		return restored;
	}

	public boolean restoreNextSnapshot() throws IOException{
		boolean restored = false;
		if(canRedo()){
			undoCaretaker.pushMemento(synonyms);
			List<ThesaurusEntry> poppedSynonyms = redoCaretaker.popMemento();
			if(undoable != null){
				undoable.onUndoChange(true);
				undoable.onRedoChange(canRedo());
			}

			synonyms.clear();
			synonyms.addAll(poppedSynonyms);

			restored = true;
		}
		return restored;
	}

}
