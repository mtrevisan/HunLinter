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

import io.github.mtrevisan.hunlinter.datastructures.SetHelper;
import io.github.mtrevisan.hunlinter.datastructures.fsa.FSAAbstract;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.FSABuilder;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.LexicographicalComparator;
import io.github.mtrevisan.hunlinter.datastructures.fsa.builders.MetadataBuilder;
import io.github.mtrevisan.hunlinter.datastructures.fsa.serializers.CFSASerializer;
import io.github.mtrevisan.hunlinter.datastructures.fsa.serializers.FSASerializerInterface;
import io.github.mtrevisan.hunlinter.datastructures.fsa.stemming.DictionaryMetadata;
import io.github.mtrevisan.hunlinter.datastructures.fsa.stemming.SequenceEncoderInterface;
import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.parsers.affix.AffixData;
import io.github.mtrevisan.hunlinter.parsers.dictionary.DictionaryParser;
import io.github.mtrevisan.hunlinter.parsers.dictionary.generators.WordGenerator;
import io.github.mtrevisan.hunlinter.parsers.enums.InflectionTag;
import io.github.mtrevisan.hunlinter.parsers.enums.MorphologicalTag;
import io.github.mtrevisan.hunlinter.parsers.enums.PartOfSpeechTag;
import io.github.mtrevisan.hunlinter.parsers.vos.DictionaryEntry;
import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;
import io.github.mtrevisan.hunlinter.services.text.StringHelper;
import io.github.mtrevisan.hunlinter.workers.WorkerManager;
import io.github.mtrevisan.hunlinter.workers.core.IndexDataPair;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDataParser;
import io.github.mtrevisan.hunlinter.workers.core.WorkerDictionary;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;


public class PoSFSAWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(PoSFSAWorker.class);

	public static final String WORKER_NAME = "Part-of-Speech FSA Extractor";

	private static final String SINGLE_POS_NOT_PRESENT = "Part-of-Speech not unique";
	private static final byte POS_FSA_TAG_SEPARATOR = (byte)'+';


	public PoSFSAWorker(final ParserManager parserManager, final File outputFile){
		this(parserManager.getAffixData(), parserManager.getDicParser(), parserManager.getWordGenerator(), parserManager.getLanguage(),
			outputFile);
	}

	public PoSFSAWorker(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator,
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
		final DictionaryMetadata metadata;
		try{
			metadata = readMetadata(affixData, outputFile, charset);
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
		final byte separator = metadata.getSeparator();
		final SequenceEncoderInterface sequenceEncoder = metadata.getSequenceEncoderType().get();


		final ByteArrayList encodings = new ByteArrayList(1_000_000.f);
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final String line = indexData.getData();
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Inflection> inflections = wordGenerator.applyAffixRules(dicEntry);

			encode(encodings, inflections, separator, sequenceEncoder);

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
				final byte[] encoding = list.data[index];
				builder.add(encoding);

				if(++ progress % progressStep == 0)
					setProgress(++ progressIndex, 100);

				sleepOnPause();
			}

			//release memory
			list.clear();

			return builder.complete();
		};
		final Function<FSAAbstract, File> step4 = fsa -> {
			resetProcessing("Compressing FSA (step 4/4)");

			final FSASerializerInterface serializer = new CFSASerializer();
			try(final ByteArrayOutputStream os = new ByteArrayOutputStream()){
				serializer.serialize(fsa, os, percent -> {
					setProgress(percent, 100);

					sleepOnPause();
				});

				Files.write(outputFile.toPath(), os.toByteArray());

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

	private DictionaryMetadata readMetadata(final AffixData affixData, final File outputFile, final Charset charset)
			throws IOException{
		final Path metadataPath = MetadataBuilder.getMetadataPath(outputFile);
		if(!metadataPath.toFile().exists())
			MetadataBuilder.createPOSInfo(affixData, "prefix", metadataPath, charset);

		return MetadataBuilder.read(metadataPath);
	}

	private void encode(final ByteArrayList encodings, final Collection<Inflection> inflections, final byte separator,
			final SequenceEncoderInterface sequenceEncoder){
		ByteBuffer tag = ByteBuffer.allocate(0);

		for(final Inflection inflection : inflections){
			//subdivide morphologicalFields into PART_OF_SPEECH, INFLECTIONAL_SUFFIX, INFLECTIONAL_PREFIX, and STEM
			final Map<MorphologicalTag, List<String>> bucket = extractMorphologicalTags(inflection);

			//extract Part-of-Speech
			final List<String> pos = bucket.get(MorphologicalTag.PART_OF_SPEECH);
			if(pos == null || pos.size() != 1)
				throw new LinterException(SINGLE_POS_NOT_PRESENT);

			final String word = inflection.getWord().toLowerCase(Locale.ROOT);
			//target
			final byte[] inflectedWord = StringHelper.getRawBytes(word);

			//extract stem
			final List<String> stems = bucket.get(MorphologicalTag.STEM);

			//extract inflection
			final List<String> suffixInflection = bucket.get(MorphologicalTag.INFLECTIONAL_SUFFIX);
			final List<String> prefixInflection = bucket.get(MorphologicalTag.INFLECTIONAL_PREFIX);
			tag = clearAndEnsureCapacity(tag, 512);
			tag.put(StringHelper.getRawBytes(PartOfSpeechTag.createFromCodeAndValue(pos.get(0)).getTag()));
			extractInflection(suffixInflection, tag);
			extractInflection(prefixInflection, tag);
			tag.flip();

			for(final String stem : stems){
				//source
				byte[] inflectionStem = StringHelper.getRawBytes(stem);
				//remove the initial part `st:`
				inflectionStem = Arrays.copyOfRange(inflectionStem, 3, inflectionStem.length);

				final byte[] encoded = sequenceEncoder.encode(inflectedWord, inflectionStem);

				int offset = inflectedWord.length;
				final byte[] assembled = new byte[offset + 1 + encoded.length + 1 + tag.remaining()];
				System.arraycopy(inflectedWord, 0, assembled, 0, offset);
				assembled[offset ++] = separator;
				System.arraycopy(encoded, 0, assembled, offset, encoded.length);
				offset += encoded.length;
				assembled[offset ++] = separator;
				System.arraycopy(tag.array(), 0, assembled, offset, tag.remaining());
				encodings.add(assembled);
			}
		}
	}

	/**
	 * Ensure the buffer's capacity is large enough to hold a given number
	 * of elements. If the input buffer is not large enough, a new buffer is allocated
	 * and returned.
	 *
	 * @param elements The required number of elements to be appended to the buffer.
	 * @param buffer   The buffer to check or {@code null} if a new buffer should be
	 *                 allocated.
	 * @return Returns the same buffer or a new buffer with the given capacity.
	 */
	private ByteBuffer clearAndEnsureCapacity(ByteBuffer buffer, final int elements){
		if(buffer == null || buffer.capacity() < elements)
			buffer = ByteBuffer.allocate(elements);
		else
			buffer.clear();
		return buffer;
	}

	private void extractInflection(final Iterable<String> suffixInflection, final ByteBuffer output){
		if(suffixInflection != null)
			for(final String code : suffixInflection){
				final String[] tags = InflectionTag.createFromCodeAndValue(code).getTags();
				for(final String tag : tags)
					output.put(POS_FSA_TAG_SEPARATOR)
						.put(StringHelper.getRawBytes(tag));
			}
	}

	//NOTE: the only morphological tags really needed are: PART_OF_SPEECH, INFLECTIONAL_SUFFIX, INFLECTIONAL_PREFIX, and STEM
	private Map<MorphologicalTag, List<String>> extractMorphologicalTags(final Inflection inflection){
		return SetHelper.bucket(inflection.getMorphologicalFieldsAsArray(), MorphologicalTag::createFromCode,
			MorphologicalTag.class);
	}


//	public static void main(final String[] args){
//		final String input = "C:\\Users\\mauro\\AppData\\Local\\Temp\\hunlinter-pos.dat";
//		final String[] buildOptions = {
//			"--overwrite",
//			"--accept-cr",
//			"--exit", "false",
//			"--format", "CFSA2",
//			"--input", input
//		};
//TimeWatch watch = TimeWatch.start();
//		morfologik.tools.DictCompile.main(buildOptions);
//watch.stop();
//System.out.println(watch.toStringMillis());
//	}

}
