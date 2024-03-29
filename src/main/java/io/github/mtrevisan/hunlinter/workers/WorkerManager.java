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
package io.github.mtrevisan.hunlinter.workers;

import io.github.mtrevisan.hunlinter.datastructures.fsa.lookup.DictionaryLookup;
import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.Packager;
import io.github.mtrevisan.hunlinter.services.system.FileHelper;
import io.github.mtrevisan.hunlinter.workers.autocorrect.AutoCorrectLinterFSAWorker;
import io.github.mtrevisan.hunlinter.workers.autocorrect.AutoCorrectLinterWorker;
import io.github.mtrevisan.hunlinter.workers.core.WorkerAbstract;
import io.github.mtrevisan.hunlinter.workers.dictionary.CompoundRulesWorker;
import io.github.mtrevisan.hunlinter.workers.dictionary.DictionaryLinterWorker;
import io.github.mtrevisan.hunlinter.workers.dictionary.DuplicatesWorker;
import io.github.mtrevisan.hunlinter.workers.dictionary.InflectionReader;
import io.github.mtrevisan.hunlinter.workers.dictionary.MinimalPairsWorker;
import io.github.mtrevisan.hunlinter.workers.dictionary.PoSFSAWorker;
import io.github.mtrevisan.hunlinter.workers.dictionary.SorterWorker;
import io.github.mtrevisan.hunlinter.workers.dictionary.WordCountWorker;
import io.github.mtrevisan.hunlinter.workers.dictionary.WordlistFSAWorker;
import io.github.mtrevisan.hunlinter.workers.dictionary.WordlistWorker;
import io.github.mtrevisan.hunlinter.workers.hyphenation.HyphenationLinterWorker;
import io.github.mtrevisan.hunlinter.workers.thesaurus.ThesaurusLinterFSAWorker;
import io.github.mtrevisan.hunlinter.workers.thesaurus.ThesaurusLinterWorker;
import org.slf4j.Logger;

import javax.swing.SwingWorker;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


public class WorkerManager{

	private static final Map<String, WorkerAbstract<?>> WORKERS = new HashMap<>(0);
	private static final Map<String, Consumer<WorkerAbstract<?>>> ON_ENDS = new HashMap<>(0);

	private final Packager packager;
	private final ParserManager parserManager;


	public WorkerManager(final Packager packager, final ParserManager parserManager){
		Objects.requireNonNull(packager, "Packager cannot be null");
		Objects.requireNonNull(parserManager, "Parser manager cannot be null");

		this.packager = packager;
		this.parserManager = parserManager;
	}

	public static void checkForAbortion(final Frame parentFrame){
		for(final Map.Entry<String, WorkerAbstract<?>> workerNameWorker : WORKERS.entrySet()){
			final WorkerAbstract<?> worker = workerNameWorker.getValue();
			if(worker != null && worker.getState() == SwingWorker.StateValue.STARTED){
//				final Runnable resumeTask = () -> setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
				GUIHelper.askUserToAbort(worker, parentFrame, null, null);
			}
		}
	}

	public static void callOnEnd(final String workerName){
		final Consumer<WorkerAbstract<?>> onEnding = ON_ENDS.remove(workerName);
		if(onEnding != null)
			onEnding.accept(WORKERS.get(workerName));

		//release memory
		WORKERS.remove(workerName);
	}


	public final void createProjectLoaderWorker(final Consumer<WorkerAbstract<?>> onStart, final Runnable completed,
			final Consumer<Exception> cancelled){
		final Supplier<WorkerAbstract<?>> creator = () -> new ProjectLoaderWorker(packager, parserManager, completed, cancelled);
		createWorker(ProjectLoaderWorker.WORKER_NAME, creator, onStart, null);
	}

	public final void createDictionaryLinterWorker(final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> new DictionaryLinterWorker(parserManager);
		createWorker(DictionaryLinterWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public final void createWordCountWorker(final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> new WordCountWorker(parserManager);
		createWorker(WordCountWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public final void createDuplicatesWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Function<File, WorkerAbstract<?>> creator = outputFile -> new DuplicatesWorker(parserManager, outputFile);
		createWorker(DuplicatesWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public final void createSorterWorker(final Supplier<Integer> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Function<Integer, WorkerAbstract<?>> creator = selectedRow ->
			new SorterWorker(packager.getDictionaryFile(), parserManager, selectedRow);
		createWorker(SorterWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public final void createThesaurusLinterWorker(final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> new ThesaurusLinterWorker(parserManager.getTheParser(), parserManager.getLanguage(),
			parserManager.getDicParser(), parserManager.getWordGenerator());
		createWorker(ThesaurusLinterWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public final void createThesaurusLinterFSAWorker(final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd,
			final DictionaryLookup dictionaryLookup){
		final Supplier<WorkerAbstract<?>> creator = () -> new ThesaurusLinterFSAWorker(parserManager.getTheParser(),
			parserManager.getDicParser(), parserManager.getWordGenerator(), dictionaryLookup);
		createWorker(ThesaurusLinterFSAWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public final void createAutoCorrectLinterWorker(final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> new AutoCorrectLinterWorker(parserManager.getAcoParser(),
			parserManager.getLanguage(), parserManager.getDicParser(), parserManager.getWordGenerator());
		createWorker(AutoCorrectLinterWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public final void createAutoCorrectLinterFSAWorker(final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd,
			final DictionaryLookup dictionaryLookup){
		final Supplier<WorkerAbstract<?>> creator = () -> new AutoCorrectLinterFSAWorker(parserManager.getAcoParser(),
			parserManager.getDicParser(), parserManager.getWordGenerator(), dictionaryLookup);
		createWorker(AutoCorrectLinterWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public final void createDictionaryStatistics(final BooleanSupplier preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd, final Frame parentFrame){
		final Supplier<WorkerAbstract<?>> creator = () -> {
			final boolean performHyphenationStatistics = preStart.getAsBoolean();
			return new StatisticsWorker(parserManager, performHyphenationStatistics, parentFrame);
		};
		createWorker(StatisticsWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public final void createWordlistWorker(final WordlistWorker.WorkerType type, final Supplier<File> preStart,
			final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		final Function<File, WorkerAbstract<?>> creator = outputFile -> new WordlistWorker(parserManager, type, outputFile);
		createWorker(WordlistWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public final void createWordlistFSAWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Function<File, WorkerAbstract<?>> creator = outputFile -> new WordlistFSAWorker(parserManager, outputFile);
		createWorker(WordlistFSAWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public final void createPoSFSAWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Function<File, WorkerAbstract<?>> creator = outputFile -> new PoSFSAWorker(parserManager, outputFile);
		createWorker(PoSFSAWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public final void createMinimalPairsWorker(final Supplier<File> preStart, final Consumer<WorkerAbstract<?>> onStart,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Function<File, WorkerAbstract<?>> creator = outputFile -> new MinimalPairsWorker(parserManager, outputFile);
		createWorker(MinimalPairsWorker.WORKER_NAME, creator, preStart, onStart, onEnd);
	}

	public final void createHyphenationLinterWorker(final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> new HyphenationLinterWorker(parserManager);
		createWorker(HyphenationLinterWorker.WORKER_NAME, creator, onStart, onEnd);
	}

	public final void createCompoundRulesWorker(final Consumer<WorkerAbstract<?>> onStart, final Consumer<List<Inflection>> onComplete,
			final Consumer<WorkerAbstract<?>> onEnd){
		final Supplier<WorkerAbstract<?>> creator = () -> {
			final AffixParser affParser = parserManager.getAffParser();
			final AffixData affixData = affParser.getAffixData();
			final String compoundFlag = affixData.getCompoundFlag();
			final List<Inflection> compounds = Collections.synchronizedList(new ArrayList<>(0));
			final InflectionReader inflectionReader = (inflection, row) -> {
				if(!inflection.distributeByCompoundRule(affixData).isEmpty() || inflection.hasContinuationFlag(compoundFlag))
					compounds.add(inflection);
			};
			final Runnable completed = () -> onComplete.accept(compounds);
			return new CompoundRulesWorker(parserManager, inflectionReader, completed);
		};
		createWorker(CompoundRulesWorker.WORKER_NAME, creator, onStart, onEnd);
	}


	private static void createWorker(final String workerName, final Supplier<WorkerAbstract<?>> creator,
			final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
		WorkerAbstract<?> worker = WORKERS.get(workerName);
		if(worker == null || worker.isDone()){
			worker = creator.get();
			WORKERS.put(workerName, worker);
			ON_ENDS.put(workerName, onEnd);

			onStart.accept(worker);
		}
	}

	private static <T> void createWorker(final String workerName, final Function<T, WorkerAbstract<?>> creator, final Supplier<T> preStart,
			final Consumer<WorkerAbstract<?>> onStart, final Consumer<WorkerAbstract<?>> onEnd){
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
