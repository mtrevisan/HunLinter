package unit731.hunlinter.workers;

import org.slf4j.Logger;
import unit731.hunlinter.gui.GUIHelper;
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
				GUIHelper.askUserToAbort(worker, parentFrame, null, null);
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


	public void createProjectLoaderWorker(final Consumer<WorkerAbstract<?>> onStart, final Runnable completed,
			final Consumer<Exception> cancelled){
		final Supplier<WorkerAbstract<?>> creator = () -> new ProjectLoaderWorker(packager, parserManager, completed, cancelled);
		createWorker(ProjectLoaderWorker.WORKER_NAME, creator, onStart, null);
	}

	public void createDictionaryLinterWorker(final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> new DictionaryLinterWorker(parserManager);
		createWorker(DictionaryLinterWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public void createWordCountWorker(final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> new WordCountWorker(parserManager);
		createWorker(WordCountWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public void createDuplicatesWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Function<File, WorkerAbstract<?>> creator = outputFile -> new DuplicatesWorker(parserManager, outputFile);
		createWorker(DuplicatesWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public void createSorterWorker(final Supplier<Integer> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Function<Integer, WorkerAbstract<?>> creator = selectedRow ->
			new SorterWorker(packager.getDictionaryFile(), parserManager, selectedRow);
		createWorker(SorterWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public void createThesaurusLinterWorker(final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> new ThesaurusLinterWorker(parserManager.getTheParser());
		createWorker(ThesaurusLinterWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public void createDictionaryStatistics(final Supplier<Boolean> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> {
			final Boolean performHyphenationStatistics = preStart.get();
			return new StatisticsWorker(parserManager, performHyphenationStatistics, parentFrame);
		};
		createWorker(StatisticsWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public void createWordlistWorker(final WordlistWorker.WorkerType type, final Supplier<File> preStart,
			final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		final Function<File, WorkerAbstract<?>> creator = outputFile -> new WordlistWorker(parserManager, type, outputFile);
		createWorker(WordlistWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public void createWordlistFSAWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Function<File, WorkerAbstract<?>> creator = outputFile -> new WordlistFSAWorker(parserManager, outputFile);
		createWorker(WordlistFSAWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public void createPoSFSAWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Function<File, WorkerAbstract<?>> creator = outputFile -> new PoSFSAWorker(parserManager, outputFile);
		createWorker(PoSFSAWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public void createMinimalPairsWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Function<File, WorkerAbstract<?>> creator = outputFile -> new MinimalPairsWorker(parserManager, outputFile);
		createWorker(MinimalPairsWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public void createHyphenationLinterWorker(final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> new HyphenationLinterWorker(parserManager);
		createWorker(HyphenationLinterWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public void createCompoundRulesWorker(final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<List<Inflection>> onComplete, final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> {
			final AffixParser affParser = parserManager.getAffParser();
			final AffixData affixData = affParser.getAffixData();
			final String compoundFlag = affixData.getCompoundFlag();
			final List<Inflection> compounds = new ArrayList<>();
			final BiConsumer<Inflection, Integer> inflectionReader = (inflection, row) -> {
				if(!inflection.distributeByCompoundRule(affixData).isEmpty() || inflection.hasContinuationFlag(compoundFlag))
					compounds.add(inflection);
			};
			final Runnable completed = () -> onComplete.accept(compounds);
			return new CompoundRulesWorker(parserManager, inflectionReader, completed);
		};
		createWorker(CompoundRulesWorker.WORKER_NAME, creator, onStart, onEnd);
	}


	private void createWorker(final String workerName, final Supplier<WorkerAbstract<?>> creator,
			final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			worker = creator.get();
			WORKERS.put(workerName, worker);
			ON_ENDS.put(workerName, onEnd);

			onStart.accept(worker);
		}
	}

	private <T> void createWorker(final String workerName, final Function<T, WorkerAbstract<?>> creator,
			final Supplier<T> preStart, final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			final T param = preStart.get();
			if(param != null){
				worker = creator.apply(param);
				WORKERS.put(workerName, worker);
				ON_ENDS.put(workerName, onEnd);

				onStart.accept(worker);
			}
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
