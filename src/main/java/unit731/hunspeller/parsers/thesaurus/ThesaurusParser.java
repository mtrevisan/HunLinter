package unit731.hunspeller.parsers.thesaurus;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.SwingWorker;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.interfaces.Resultable;
import unit731.hunspeller.interfaces.Undoable;
import unit731.hunspeller.services.ExceptionService;
import unit731.hunspeller.services.FileService;
import unit731.hunspeller.services.PatternService;
import unit731.hunspeller.services.memento.OriginatorInterface;


@Slf4j
@Getter
public class ThesaurusParser implements OriginatorInterface<ThesaurusParser.Memento>{

	private static final ReentrantLock LOCK_SAVING = new ReentrantLock();

	//NOTE: All members are private and accessible only by Originator
	@AllArgsConstructor
	@NoArgsConstructor(access = AccessLevel.PRIVATE)
	protected static class Memento{

		@JsonProperty
		private ThesaurusDictionary dictionary;

	}

	private final ThesaurusDictionary dictionary = new ThesaurusDictionary();

	private final Undoable undoable;
	private final ThesaurusCaretaker undoCaretaker = new ThesaurusCaretaker();
	private final ThesaurusCaretaker redoCaretaker = new ThesaurusCaretaker();


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
			LOCK_SAVING.lock();

			try{
				publish("Opening Thesaurus file for parsing: " + theFile.getName());
				setProgress(0);

				long readSoFar = 0l;
				long totalSize = theFile.length();

				Charset charset = FileService.determineCharset(theFile.toPath());
				try(BufferedReader br = Files.newBufferedReader(theFile.toPath(), charset)){
					String line = br.readLine();

					while((line = br.readLine()) != null){
						readSoFar += line.length();
						if(!line.isEmpty())
							theParser.dictionary.add(new ThesaurusEntry(line, br));

						setProgress((int)((readSoFar * 100.) / totalSize));
					}
					setProgress(100);
				}

				publish("Finished reading Thesaurus file");
			}
			catch(IOException | IllegalArgumentException e){
				publish(e instanceof ClosedChannelException? "Thesaurus parser thread interrupted": e.getClass().getSimpleName() + ": "
					+ e.getMessage());
			}
			catch(Exception e){
				String message = ExceptionService.getMessage(e, getClass());
				publish(e.getClass().getSimpleName() + ": " + message);
			}
			finally{
				LOCK_SAVING.unlock();
			}

			theParser.dictionary.resetModified();

			return theParser.getSynonymsDictionary();
		}

		@Override
		protected void process(List<String> chunks){
			resultable.printResultLine(chunks);
		}

		@Override
		protected void done(){
			if(postExecution != null)
				postExecution.run();

//System.out.println(com.carrotsearch.sizeof.RamUsageEstimator.sizeOfAll(theParser.synonyms));
//6 035 792 B
		}
	};

	public int getSynonymsCounter(){
		LOCK_SAVING.lock();

		try{
			return dictionary.size();
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	public boolean isDictionaryModified(){
		LOCK_SAVING.lock();

		try{
			return dictionary.isModified();
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	public List<ThesaurusEntry> getSynonymsDictionary(){
		LOCK_SAVING.lock();

		try{
			return dictionary.getSynonyms();
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	/**
	 * @param synonymAndMeanings			The line representing all the synonyms of a word along with their part of speech
	 * @param duplicatesDiscriminator	Function called to ask the user what to do is duplicates are found (return <code>true</code> to force
	 *												insertion)
	 * @return The duplication result
	 */
	public DuplicationResult insertMeanings(String synonymAndMeanings, Supplier<Boolean> duplicatesDiscriminator){
		LOCK_SAVING.lock();

		try{
			String[] partOfSpeechAndMeanings = PatternService.split(synonymAndMeanings, ThesaurusEntry.REGEX_PATTERN_ESCAPED_PIPE, 2);
			String partOfSpeech = StringUtils.strip(partOfSpeechAndMeanings[0]);
			if(!partOfSpeech.startsWith("(") || !partOfSpeech.endsWith(")"))
				throw new IllegalArgumentException("Part of speech is not in parenthesis: " + synonymAndMeanings);

			String[] means = PatternService.split(partOfSpeechAndMeanings[1], ThesaurusEntry.REGEX_PATTERN_ESCAPED_PIPE);
			List<String> meanings = Arrays.stream(means)
				.map(String::trim)
				.filter(StringUtils::isNotBlank)
				.distinct()
				.collect(Collectors.toList());
			if(meanings.size() < 1)
				throw new IllegalArgumentException("Not enough meanings are supplied (at least one should be present): " + synonymAndMeanings);

			DuplicationResult duplicationResult = extractDuplicates(meanings, partOfSpeech, duplicatesDiscriminator);

			if(duplicationResult.isForcedInsertion() || duplicationResult.getDuplicates().isEmpty()){
				try{
					undoCaretaker.pushMemento(createMemento());

					if(undoable != null)
						undoable.onUndoChange(true);
				}
				catch(IOException ex){
					log.warn("Error while storing a memento", ex);
				}

				dictionary.add(partOfSpeech, meanings);
			}

			return duplicationResult;
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	/** Find if there is a duplicate with the same part of speech */
	private DuplicationResult extractDuplicates(List<String> means, String partOfSpeech, Supplier<Boolean> duplicatesDiscriminator)
			throws IllegalArgumentException{
		LOCK_SAVING.lock();

		try{
			boolean forcedInsertion = false;
			List<ThesaurusEntry> duplicates = new ArrayList<>();
			try{
				List<ThesaurusEntry> synonyms = dictionary.getSynonyms();
				for(String meaning : means)
					for(ThesaurusEntry synonym : synonyms)
						if(synonym.getSynonym().equals(meaning)){
							long countSamePartOfSpeech = 0;
							List<MeaningEntry> meanings = synonym.getMeanings();
							for(MeaningEntry m : meanings)
								if(m.getPartOfSpeech().equals(partOfSpeech))
									countSamePartOfSpeech ++;
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
		finally{
			LOCK_SAVING.unlock();
		}
	}

	public void setMeanings(int index, List<MeaningEntry> meanings, String text){
		LOCK_SAVING.lock();

		try{
			undoCaretaker.pushMemento(createMemento());

			dictionary.setMeanings(index, meanings, text);

			if(undoable != null)
				undoable.onUndoChange(true);
		}
		catch(IOException e){
			try{
				undoCaretaker.popMemento();
			}
			catch(IOException ioe){
				log.warn("Error while removing a memento", ioe);
			}

			log.warn("Error while storing a memento", e);
		}
		catch(Exception e){
			try{
				undoCaretaker.popMemento();
			}
			catch(IOException ioe){
				log.warn("Error while removing a memento", ioe);
			}

			log.warn("Error while modifying the meanings", e);
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	public void deleteMeanings(int[] selectedRowIDs){
		LOCK_SAVING.lock();

		try{
			int count = selectedRowIDs.length;
			if(count > 0){
				try{
					undoCaretaker.pushMemento(createMemento());

					if(undoable != null)
						undoable.onUndoChange(true);
				}
				catch(IOException ex){
					log.warn("Error while storing a memento", ex);
				}

				for(int i = 0; i < count; i ++)
					dictionary.remove(selectedRowIDs[i] - i);
			}
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	public List<String> extractDuplicates(){
		LOCK_SAVING.lock();

		try{
			return dictionary.extractDuplicates();
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	public void save(File theIndexFile, File theDataFile) throws IOException{
		LOCK_SAVING.lock();

		try{
			//sort the synonyms
			dictionary.sort();

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
				indexWriter.write(Integer.toString(dictionary.size()));
				indexWriter.write(StringUtils.LF);
				//save charset
				dataWriter.write(charset.name());
				dataWriter.write(StringUtils.LF);
				//save data
				int idx = charset.name().length() + 1;
				List<ThesaurusEntry> synonyms = dictionary.getSynonyms();
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

				dictionary.resetModified();
			}
		}
		finally{
			LOCK_SAVING.unlock();
		}
	}

	public void clear(){
		LOCK_SAVING.lock();

		try{
			dictionary.clear();
		}
		finally{
			LOCK_SAVING.unlock();
		}
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
			redoCaretaker.pushMemento(createMemento());

			Memento memento = undoCaretaker.popMemento();
			if(undoable != null){
				undoable.onUndoChange(canUndo());
				undoable.onRedoChange(true);
			}

			restoreMemento(memento);

			restored = true;
		}
		return restored;
	}

	public boolean restoreNextSnapshot() throws IOException{
		boolean restored = false;
		if(canRedo()){
			undoCaretaker.pushMemento(createMemento());

			Memento memento = redoCaretaker.popMemento();
			if(undoable != null){
				undoable.onUndoChange(true);
				undoable.onRedoChange(canRedo());
			}

			restoreMemento(memento);

			restored = true;
		}
		return restored;
	}

	@Override
	public Memento createMemento(){
		return new Memento(dictionary);
	}

	@Override
	public void restoreMemento(Memento memento){
		dictionary.restore(memento.dictionary);
	}

}
