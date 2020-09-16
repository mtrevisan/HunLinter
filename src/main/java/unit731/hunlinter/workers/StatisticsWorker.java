/**
 * Copyright (c) 2019-2020 Mauro Trevisan
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
package unit731.hunlinter.workers;

import unit731.hunlinter.gui.GUIHelper;
import unit731.hunlinter.gui.dialogs.DictionaryStatisticsDialog;
import unit731.hunlinter.languages.BaseBuilder;
import unit731.hunlinter.languages.Orthography;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.affix.AffixParser;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.DictionaryStatistics;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.hyphenation.Hyphenation;
import unit731.hunlinter.parsers.hyphenation.HyphenatorInterface;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import java.awt.Frame;
import java.nio.charset.Charset;
import java.nio.file.Path;
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

		Objects.requireNonNull(affParser);
		Objects.requireNonNull(wordGenerator);


		final AffixData affixData = affParser.getAffixData();
		final String language = affixData.getLanguage();
		dicStatistics = new DictionaryStatistics(language, affixData.getCharset());
		orthography = BaseBuilder.getOrthography(language);

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = DictionaryEntry.createFromDictionaryLine(indexData.getData(), affixData);
			if(!dicEntry.hasPartOfSpeech(POS_UNIT_OF_MEASURE)){
				final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

				for(final Inflection inflection : inflections){
					//collect statistics
					final String word = inflection.getWord();
					final String[] subwords = (hyphenator != null? hyphenator.splitIntoCompounds(word): null);
					if(subwords == null || subwords.length == 0)
						dicStatistics.addData(word);
					else
						for(final String subword : subwords){
							final Hyphenation hyph = hyphenator.hyphenate(orthography.markDefaultStress(subword));
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

			final Path dicPath = dicParser.getDicFile().toPath();
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
