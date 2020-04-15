package unit731.hunlinter.workers;

import org.slf4j.Logger;
import unit731.hunlinter.gui.GUIUtils;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.system.FileHelper;
import unit731.hunlinter.services.Packager;
import unit731.hunlinter.workers.core.WorkerAbstract;
import unit731.hunlinter.workers.dictionary.CompoundRulesWorker;
import unit731.hunlinter.workers.dictionary.DictionaryLinterWorker;
import unit731.hunlinter.workers.dictionary.DuplicatesWorker;
import unit731.hunlinter.workers.dictionary.MinimalPairsWorker;
import unit731.hunlinter.workers.dictionary.PoSFSAWorker;
import unit731.hunlinter.workers.dictionary.SorterWorker;
import unit731.hunlinter.workers.dictionary.WordCountWorker;
import unit731.hunlinter.workers.dictionary.WordlistFSAWorker;
import unit731.hunlinter.workers.dictionary.WordlistWorker;
import unit731.hunlinter.workers.hyphenation.HyphenationLinterWorker;
import unit731.hunlinter.workers.thesaurus.ThesaurusLinterWorker;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class WorkerManager{

	private static final Map<String, WorkerAbstract<?>> WORKERS = new HashMap<>();
	private static final Map<String, Consumer<WorkerAbstract<?>>> ON_ENDS = new HashMap<>();

	private final Packager packager;
	private final ParserManager parserManager;
	private final Frame parentFrame;


	public WorkerManager(final Packager packager, final ParserManager parserManager, final Frame parentFrame){
		Objects.requireNonNull(packager);
		Objects.requireNonNull(parserManager);
		Objects.requireNonNull(parentFrame);

		this.packager = packager;
		this.parserManager = parserManager;
		this.parentFrame = parentFrame;
	}

	public void checkForAbortion(){
		for(final Map.Entry<String, WorkerAbstract<?>> workerNameWorker : WORKERS.entrySet()){
			final WorkerAbstract<?> worker = workerNameWorker.getValue();
			if(worker != null && worker.getState() == SwingWorker.StateValue.STARTED){
//				final Runnable resumeTask = () -> setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
				GUIUtils.askUserToAbort(worker, parentFrame, null, null);
			}
		}
	}

	public void callOnEnd(final String workerName){
		final Consumer<WorkerAbstract<?>> onEnding = ON_ENDS.remove(workerName);
		if(onEnding != null)
			onEnding.accept(WORKERS.get(workerName));
		//release memory
		WORKERS.remove(workerName);
	}

	public void createProjectLoaderWorker(final Consumer<WorkerAbstract<?>> onStart, final Runnable completed, final Consumer<Exception> cancelled){
		final String workerName = ProjectLoaderWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			worker = new ProjectLoaderWorker(packager, parserManager, completed, cancelled);
			WORKERS.put(workerName, worker);

			onStart.accept(worker);
		}
	}

	public void createDictionaryLinterWorker(final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = DictionaryLinterWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			worker = new DictionaryLinterWorker(parserManager.getDicParser(), parserManager.getChecker(),
				parserManager.getWordGenerator());
			WORKERS.put(workerName, worker);
			ON_ENDS.put(workerName, onEnd);

			onStart.accept(worker);
		}
	}

	public void createWordCountWorker(final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = WordCountWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			worker = new WordCountWorker(parserManager.getAffParser().getAffixData().getLanguage(),
				parserManager.getDicParser(), parserManager.getWordGenerator());
			WORKERS.put(workerName, worker);
			ON_ENDS.put(workerName, onEnd);

			onStart.accept(worker);
		}
	}

	public void createDuplicatesWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = DuplicatesWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			final File outputFile = preStart.get();
			if(outputFile != null){
				worker = new DuplicatesWorker(parserManager.getAffixData().getLanguage(), parserManager.getDicParser(),
					parserManager.getWordGenerator(), outputFile);
				WORKERS.put(workerName, worker);
				ON_ENDS.put(workerName, onEnd);

				onStart.accept(worker);
			}
		}
	}

	public void createSorterWorker(final Supplier<Integer> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = SorterWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			final Integer selectedRow = preStart.get();
			if(selectedRow != null){
				worker = new SorterWorker(packager, parserManager, selectedRow);
				WORKERS.put(workerName, worker);
				ON_ENDS.put(workerName, onEnd);

				onStart.accept(worker);
			}
		}
	}

	public void createThesaurusLinterWorker(final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = ThesaurusLinterWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			worker = new ThesaurusLinterWorker(parserManager.getTheParser());
			WORKERS.put(workerName, worker);
			ON_ENDS.put(workerName, onEnd);

			onStart.accept(worker);
		}
	}

	public void createDictionaryStatistics(final Supplier<Boolean> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = StatisticsWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			final Boolean performHyphenationStatistics = preStart.get();
			worker = new StatisticsWorker(parserManager.getAffParser(), parserManager.getDicParser(),
				(performHyphenationStatistics? parserManager.getHyphenator(): null), parserManager.getWordGenerator(), parentFrame);
			WORKERS.put(workerName, worker);
			ON_ENDS.put(workerName, onEnd);

			onStart.accept(worker);
		}
	}

	public void createWordlistWorker(final WordlistWorker.WorkerType type, final Supplier<File> preStart,
			final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = WordlistWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			final File outputFile = preStart.get();
			if(outputFile != null){
				worker = new WordlistWorker(parserManager.getDicParser(), parserManager.getWordGenerator(), type,
					outputFile);
				WORKERS.put(workerName, worker);
				ON_ENDS.put(workerName, onEnd);

				onStart.accept(worker);
			}
		}
	}

	public void createWordlistFSAWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = WordlistFSAWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			final File outputFile = preStart.get();
			if(outputFile != null){
				worker = new WordlistFSAWorker(parserManager.getAffParser().getAffixData(), parserManager.getDicParser(),
					parserManager.getWordGenerator(), outputFile);
				WORKERS.put(workerName, worker);
				ON_ENDS.put(workerName, onEnd);

				onStart.accept(worker);
			}
		}
	}

	public void createPoSFSAWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = PoSFSAWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			final File outputFile = preStart.get();
			if(outputFile != null){
				worker = new PoSFSAWorker(parserManager.getAffParser().getAffixData(), parserManager.getDicParser(),
					parserManager.getWordGenerator(), outputFile);
				WORKERS.put(workerName, worker);
				ON_ENDS.put(workerName, onEnd);

				onStart.accept(worker);
			}
		}
	}

	public void createMinimalPairsWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = MinimalPairsWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			final File outputFile = preStart.get();
			if(outputFile != null){
				worker = new MinimalPairsWorker(parserManager.getAffixData().getLanguage(), parserManager.getDicParser(),
					parserManager.getChecker(), parserManager.getWordGenerator(), outputFile);
				WORKERS.put(workerName, worker);
				ON_ENDS.put(workerName, onEnd);

				onStart.accept(worker);
			}
		}
	}

	public void createHyphenationLinterWorker(final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = HyphenationLinterWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			worker = new HyphenationLinterWorker(parserManager.getAffParser().getAffixData().getLanguage(),
				parserManager.getDicParser(), parserManager.getHyphenator(), parserManager.getWordGenerator());
			WORKERS.put(workerName, worker);
			ON_ENDS.put(workerName, onEnd);

			onStart.accept(worker);
		}
	}

	public void createCompoundRulesWorker(final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<List<Inflection>> onComplete, final Consumer<WorkerAbstract<?>> onEnd){
		final String workerName = CompoundRulesWorker.WORKER_NAME;
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			final AffixParser affParser = parserManager.getAffParser();
			final AffixData affixData = affParser.getAffixData();
			final String compoundFlag = affixData.getCompoundFlag();
			final List<Inflection> compounds = new ArrayList<>();
			final BiConsumer<Inflection, Integer> inflectionReader = (inflection, row) -> {
				if(!inflection.distributeByCompoundRule(affixData).isEmpty() || inflection.hasContinuationFlag(compoundFlag))
					compounds.add(inflection);
			};
			final Runnable completed = () -> onComplete.accept(compounds);
			worker = new CompoundRulesWorker(parserManager.getDicParser(), parserManager.getWordGenerator(),
				inflectionReader, completed);
			WORKERS.put(workerName, worker);
			ON_ENDS.put(workerName, onEnd);

			onStart.accept(worker);
		}
	}


	public static Function<File, Void> openFileStep(final Logger logger){
		return file -> {
			try{
				FileHelper.openFileWithChosenEditor(file);
			}
			catch(final IOException | InterruptedException e){
				logger.warn("Exception while opening file {}", file.getName(), e);
			}

			return null;
		};
	}

	public static Function<File, Void> openFolderStep(final Logger logger){
		return file -> {
			try{
				FileHelper.browse(file);
			}
			catch(final IOException | InterruptedException e){
				logger.warn("Exception while opening folder {}", file.getParent(), e);
			}

			return null;
		};
	}

}
