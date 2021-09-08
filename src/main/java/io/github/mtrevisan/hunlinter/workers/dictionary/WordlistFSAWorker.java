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
package io.github.mtrevisan.hunlinter.workers.dictionary;

import io.github.mtrevisan.hunlinter.datastructures.fsa.FSAAbstract;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.FSABuilder;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.LexicographicalComparator;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.MetadataBuilder;
import io.github.mtrevisan.hunlinter.datastructures.fsa.serializers.CFSASerializer;
import io.github.mtrevisan.hunlinter.datastructures.fsa.serializers.FSASerializerInterface;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.WorkerManager;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;


public class WordlistFSAWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(WordlistFSAWorker.class);

	public static final String WORKER_NAME = "Wordlist FSA Extractor";


	public WordlistFSAWorker(final ParserManager parserManager, final File outputFile){
		this(parserManager.getAffixData(), parserManager.getDicParser(), parserManager.getWordGenerator(), parserManager.getLanguage(),
			outputFile);
	}

	public WordlistFSAWorker(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final String language, final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(affixData, "Affix data cannot be null");
		Objects.requireNonNull(dicParser, "Dictionary parser cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");
		Objects.requireNonNull(language, "Language cannot be null");
		Objects.requireNonNull(outputFile, "Output file cannot be null");


		final Charset charset = dicParser.getCharset();
		try{
			final Path metadataPath = MetadataBuilder.getMetadataPath(outputFile);
			if(!metadataPath.toFile().exists())
				MetadataBuilder.createWordlistInfo(affixData, "none", metadataPath, charset);
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}


		final ByteArrayList encodings = new ByteArrayList(1_000_000.f);
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final String line = indexData.getData();
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry);

			for(final Inflection inflection : inflections){
				final byte[] assembled = StringHelper.getRawBytes(inflection.getWord().toLowerCase(Locale.ROOT));
				encodings.add(assembled);
			}

			sleepOnPause();
		};
		final FSABuilder builder = new FSABuilder();

		final Function<Void, ByteArrayList> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/4)");

			final Path dicPath = dicParser.getDicFile()
				.toPath();
			processLines(dicPath, charset, lineProcessor);

			return encodings;
		};
		final Function<ByteArrayList, ByteArrayList> step2 = list -> {
			resetProcessing("Sorting (step 2/4)");

			//sort list
			list.parallelSort(LexicographicalComparator.lexicographicalComparator());

			return list;
		};
		final Function<ByteArrayList, FSAAbstract> step3 = list -> {
			resetProcessing("Creating FSA (step 3/4)");

			getWorkerData()
				.withNoHeader()
				.withSequentialProcessing();

			int progress = 0;
			int progressIndex = 0;
			final int progressStep = (int)Math.ceil(list.size() / 100.f);
			for(int index = 0; index < list.size(); index ++){
				final byte[] encoding = list.get(index);
				builder.add(encoding);

				if(++ progress % progressStep == 0)
					setProgress(++ progressIndex, 100);

				sleepOnPause();
			}

			//release memory
			list.clear();

			return builder.complete();
		};
		final Path outputPath = outputFile.toPath();
		final Function<FSAAbstract, File> step4 = fsa -> {
			resetProcessing("Compressing FSA (step 4/4)");

			final FSASerializerInterface serializer = new CFSASerializer();
			try(final ByteArrayOutputStream os = new ByteArrayOutputStream()){
				serializer.serialize(fsa, os, percent -> {
					setProgress(percent, 100);

					sleepOnPause();
				});

				Files.write(outputPath, os.toByteArray());

				finalizeProcessing("Successfully processed " + workerData.getWorkerName() + ": " + outputFile.getAbsolutePath());

				return outputFile;
			}
			catch(final Exception e){
				throw new RuntimeException(e.getMessage());
			}
		};
		final Function<File, Void> step5 = WorkerManager.openFolderStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4).andThen(step5));
	}

}
