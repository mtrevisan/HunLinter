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
package unit731.hunlinter.workers.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.datastructures.SetHelper;
import unit731.hunlinter.datastructures.SimpleDynamicArray;
import unit731.hunlinter.datastructures.fsa.FSA;
import unit731.hunlinter.datastructures.fsa.builders.FSABuilder;
import unit731.hunlinter.datastructures.fsa.builders.LexicographicalComparator;
import unit731.hunlinter.datastructures.fsa.builders.MetadataBuilder;
import unit731.hunlinter.datastructures.fsa.lookup.DictionaryLookup;
import unit731.hunlinter.datastructures.fsa.lookup.WordData;
import unit731.hunlinter.datastructures.fsa.serializers.CFSA2Serializer;
import unit731.hunlinter.datastructures.fsa.serializers.FSASerializer;
import unit731.hunlinter.datastructures.fsa.stemming.BufferUtils;
import unit731.hunlinter.datastructures.fsa.stemming.Dictionary;
import unit731.hunlinter.datastructures.fsa.stemming.DictionaryMetadata;
import unit731.hunlinter.datastructures.fsa.stemming.SequenceEncoderInterface;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.affix.AffixData;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.enums.InflectionTag;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.enums.PartOfSpeechTag;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.sorters.SmoothSort;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
		this(parserManager.getAffixData(), parserManager.getDicParser(), parserManager.getWordGenerator(), outputFile);
	}

	public PoSFSAWorker(final AffixData affixData, final DictionaryParser dicParser, final WordGenerator wordGenerator,
			final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		getWorkerData()
			.withParallelProcessing()
			.withCancelOnException();

		Objects.requireNonNull(affixData, "Affix data cannot be null");
		Objects.requireNonNull(dicParser, "Dictionary parser cannot be null");
		Objects.requireNonNull(wordGenerator, "Word generator cannot be null");
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


		final SimpleDynamicArray<byte[]> encodings = new SimpleDynamicArray<>(byte[].class, 50_000_000, 1.2f);
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final String line = indexData.getData();
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final Inflection[] inflections = wordGenerator.applyAffixRules(dicEntry);

			final SimpleDynamicArray<byte[]> currentEncodings = encode(inflections, separator, sequenceEncoder);

			encodings.addAll(currentEncodings);

			sleepOnPause();
		};
		final FSABuilder builder = new FSABuilder();
		final Consumer<byte[]> fsaProcessor = builder::add;

		final Function<Void, SimpleDynamicArray<byte[]>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/5)");

			final Path dicPath = dicParser.getDicFile().toPath();
			processLines(dicPath, charset, lineProcessor);

			return encodings;
		};
		final Function<SimpleDynamicArray<byte[]>, SimpleDynamicArray<byte[]>> step2 = list -> {
			resetProcessing("Sorting (step 2/5)");

			//sort list
			SmoothSort.sort(list.data, 0, list.limit, LexicographicalComparator.lexicographicalComparator(),
				percent -> {
					setProgress(percent, 100);

					sleepOnPause();
				});

			return list;
		};
		final Function<SimpleDynamicArray<byte[]>, FSA> step3 = list -> {
			resetProcessing("Creating FSA (step 3/5)");

			getWorkerData()
				.withNoHeader()
				.withSequentialProcessing();

			int progress = 0;
			int progressIndex = 0;
			final int progressStep = (int)Math.ceil(list.limit / 100.f);
			for(int index = 0; index < list.limit; index ++){
				final byte[] encoding = list.data[index];
				fsaProcessor.accept(encoding);

				//release memory
				list.data[index] = null;

				if(++ progress % progressStep == 0)
					setProgress(++ progressIndex, 100);

				sleepOnPause();
			}

			//release memory
			list.clear();

			return builder.complete();
		};
		final Function<FSA, File> step4 = fsa -> {
			resetProcessing("Compressing FSA (step 4/5)");

			final FSASerializer serializer = new CFSA2Serializer();
			try(final ByteArrayOutputStream os = new ByteArrayOutputStream()){
				serializer.serialize(fsa, os, percent -> {
					setProgress(percent, 100);

					sleepOnPause();
				});

				Files.write(outputFile.toPath(), os.toByteArray());

				return outputFile;
			}
			catch(final Exception e){
				throw new RuntimeException(e.getMessage());
			}
		};
		final Function<File, File> step5 = fsa -> {
			resetProcessing("Verifying correctness (step 5/5)");

			try{
				//verify by reading
				final Iterable<WordData> s = new DictionaryLookup(Dictionary.read(outputFile.toPath()));
				for(final Iterator<?> i = s.iterator(); i.hasNext(); i.next()){}

				finalizeProcessing("Successfully processed " + workerData.getWorkerName() + ": " + outputFile.getAbsolutePath());
			}
			catch(final Exception e){
				throw new RuntimeException(e.getMessage());
			}

			return outputFile;
		};
		final Function<File, Void> step6 = WorkerManager.openFolderStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4).andThen(step5).andThen(step6));
	}

	private DictionaryMetadata readMetadata(final AffixData affixData, final File outputFile, final Charset charset)
			throws IOException{
		final Path metadataPath = MetadataBuilder.getMetadataPath(outputFile);
		if(!metadataPath.toFile().exists())
			MetadataBuilder.create(affixData, "prefix", metadataPath, charset);

		return MetadataBuilder.read(metadataPath);
	}

	private SimpleDynamicArray<byte[]> encode(final Inflection[] inflections, final byte separator,
			final SequenceEncoderInterface sequenceEncoder){
		ByteBuffer tag = ByteBuffer.allocate(0);

		final SimpleDynamicArray<byte[]> out = new SimpleDynamicArray<>(byte[].class, inflections.length, 1.2f);
		for(final Inflection inflection : inflections){
			//subdivide morphologicalFields into PART_OF_SPEECH, INFLECTIONAL_SUFFIX, INFLECTIONAL_PREFIX, and STEM
			final Map<MorphologicalTag, List<String>> bucket = extractMorphologicalTags(inflection);

			//extract Part-of-Speech
			final List<String> pos = bucket.get(MorphologicalTag.PART_OF_SPEECH);
			if(pos == null || pos.size() != 1)
				throw new LinterException(SINGLE_POS_NOT_PRESENT);

			final String word = inflection.getWord();
			//target
			final byte[] inflectedWord = StringHelper.getRawBytes(word);

			//extract stem
			final List<String> stems = bucket.get(MorphologicalTag.STEM);
			final byte[][] encodedStems = new byte[stems.size()][];

			//extract inflection
			final List<String> suffixInflection = bucket.get(MorphologicalTag.INFLECTIONAL_SUFFIX);
			final List<String> prefixInflection = bucket.get(MorphologicalTag.INFLECTIONAL_PREFIX);
			tag = BufferUtils.clearAndEnsureCapacity(tag, 512);
			tag.put(StringHelper.getRawBytes(PartOfSpeechTag.createFromCodeAndValue(pos.get(0)).getTag()));
			extractInflection(suffixInflection, tag);
			extractInflection(prefixInflection, tag);
			tag.flip();

			int position = 0;
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
				encodedStems[position ++] = assembled;
			}
			out.addAll(encodedStems);
		}
		return out;
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
