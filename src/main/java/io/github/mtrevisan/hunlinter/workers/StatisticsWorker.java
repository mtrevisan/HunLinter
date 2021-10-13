/**
 * Copyright (c) 2019-2021 Mauro Trevisan
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

import io.github.mtrevisan.hunlinter.gui.GUIHelper;
import io.github.mtrevisan.hunlinter.gui.dialogs.DictionaryStatisticsDialog;
import io.github.mtrevisan.hunlinter.languages.BaseBuilder;
import io.github.mtrevisan.hunlinter.languages.Orthography;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixParser;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryStatistics;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.enums.MorphologicalTag;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.Hyphenation;
import io.github.mtrevisan.hunlinter.parsers.hyphenation.HyphenatorInterface;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntryFactory;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDictionary;

import java.awt.Frame;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;


public class StatisticsWorker extends WorkerDictionary{

	public static final String WORKER_NAME = "Statistics";

	private static final String POS_UNIT_OF_MEASURE = MorphologicalTag.PART_OF_SPEECH.attachValue("unit_of_measure");

	private final DictionaryStatistics dicStatistics;
	private final Orthography orthography;


	public StatisticsWorker(final ParserManager parserManager, final boolean performHyphenationStatistics, final Frame parent){
		this(parserManager.getAffParser(), parserManager.getDicParser(),
			(performHyphenationStatistics? parserManager.getHyphenator(): null), parserManager.getWordGenerator(), parent);
	}

	public StatisticsWorker(final AffixParser affParser, final DictionaryParser dicParser, final HyphenatorInterface hyphenator,
			final WordGenerator wordGenerator, final Frame parent){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(affParser, "Affix parser cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");


		final AffixData affixData = affParser.getAffixData();
		final DictionaryEntryFactory dictionaryEntryFactory = new DictionaryEntryFactory(affixData);
		final String language = affixData.getLanguage();
		dicStatistics = new DictionaryStatistics(language, affixData.getCharset());
		orthography = BaseBuilder.getOrthography(language);

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = dictionaryEntryFactory.createFromDictionaryLine(indexData.getData());
			if(!dicEntry.hasPartOfSpeech(POS_UNIT_OF_MEASURE)){
				final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry);

				for(int i = 0; i < inflections.size(); i ++){
					//collect statistics
					final String word = inflections.get(i).getWord();
					final List<String> subwords = (hyphenator != null? hyphenator.splitIntoCompounds(word): null);
					if(subwords == null || subwords.isEmpty())
						dicStatistics.addData(word);
					else
						for(int j = 0; j < subwords.size(); j ++){
							final Hyphenation hyph = hyphenator.hyphenate(orthography.markDefaultStress(subwords.get(j)));
							dicStatistics.addData(word, hyph);
						}
				}
			}
		};
		final Consumer<Exception> cancelled = exception -> dicStatistics.close();

		getWorkerData()
			.withDataCancelledCallback(cancelled);

		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing("Execute " + workerData.getWorkerName());

			final Path dicPath = dicParser.getDicFile()
				.toPath();
			final Charset charset = dicParser.getCharset();
			processLines(dicPath, charset, lineProcessor);

			finalizeProcessing("Successfully processed " + workerData.getWorkerName());

			return null;
		};
		final Function<Void, Void> step2 = ignored -> {
			dicStatistics.close();

			//show statistics window
			final DictionaryStatisticsDialog dialog = new DictionaryStatisticsDialog(dicStatistics, parent);
			GUIHelper.addCancelByEscapeKey(dialog);
			dialog.setLocationRelativeTo(parent);
			dialog.setVisible(true);

			return null;
		};
		setProcessor(step1.andThen(step2));
	}

}
