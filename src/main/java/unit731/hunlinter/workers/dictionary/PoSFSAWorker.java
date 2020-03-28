package unit731.hunlinter.workers.dictionary;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.enums.InflectionTag;
import unit731.hunlinter.parsers.enums.MorphologicalTag;
import unit731.hunlinter.parsers.enums.PartOfSpeechTag;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.SetHelper;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.serializers.CFSA2Serializer;
import unit731.hunlinter.services.fsa.builders.FSABuilder;
import unit731.hunlinter.services.fsa.stemming.BufferUtils;
import unit731.hunlinter.services.fsa.stemming.DictionaryMetadata;
import unit731.hunlinter.services.fsa.stemming.SequenceEncoderInterface;
import unit731.hunlinter.services.system.TimeWatch;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;


public class PoSFSAWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(PoSFSAWorker.class);

	public static final String WORKER_NAME = "Part–of–Speech FSA Extractor";


	public PoSFSAWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser));

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		final Charset charset = dicParser.getCharset();
		final DictionaryMetadata metadata = readMetadata(charset, outputFile);
		final byte separator = metadata.getSeparator();
		final SequenceEncoderInterface sequenceEncoder = metadata.getSequenceEncoderType().get();


//		final File supportFile;
//		final BufferedWriter writer;
//		try{
//			supportFile = FileHelper.createDeleteOnExitFile("hunlinter-pos", ".dat");
//			writer = Files.newBufferedWriter(supportFile.toPath(), charset);
//		}
//		catch(final IOException e){
//			throw new RuntimeException(e);
//		}

//		final Collator collator = Collator.getInstance();
//		final List<CollationKey> list = new ArrayList<>();
//		final ArrayList<String> list = new ArrayList<>();
		final List<Inflection> inflections = new ArrayList<>(50_000_000);
//		final StringList list = new StringList();
		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Inflection[] currentInflections = wordGenerator.applyAffixRules(dicEntry);

			inflections.addAll(Arrays.asList(currentInflections));

//			forEach(currentInflections, inflection -> {
//				final List<String> lines = inflection.toStringPoSFSA(sequenceEncoder);

				//encode lines
//				encode(lines, separator, sequenceEncoder);

//				forEach(lines, line -> writeLine(writer, line, StringUtils.LF));
//				forEach(lines, line -> list.add(collator.getCollationKey(line)));
//				forEach(lines, list::add);
//			});
		};
		final FSABuilder builder = new FSABuilder();
		final Consumer<IndexDataPair<byte[]>> fsaProcessor = indexData -> builder.add(indexData.getData());

		getWorkerData()
			.withParallelProcessing()
//			.withDataCancelledCallback(e -> closeWriter(writer))
			.withRelaunchException();

		final Function<Void, Void> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/5)");

TimeWatch watch = TimeWatch.start();
			final Path dicPath = dicParser.getDicFile().toPath();
			processLines(dicPath, charset, lineProcessor);
//			closeWriter(writer);
watch.stop();
System.out.println("1: " + watch.toStringMillis());

			return null;
		};
		final Function<Void, List<byte[]>> step2 = ignored -> {
			resetProcessing("Encoding lines (step 2/5)");

TimeWatch watch = TimeWatch.start();
			final List<byte[]> encodings = encode(inflections, separator, sequenceEncoder);
watch.stop();
System.out.println("2: " + watch.toStringMillis());

			return encodings;
		};
		final Function<List<byte[]>, List<byte[]>> step3 = encodings -> {
			resetProcessing("Sorting (step 3/5)");

TimeWatch watch = TimeWatch.start();
			//remove duplicates & sort file
			encodings = new ArrayList<>(new HashSet<>(encodings));

			setProgress(50);

			Collections.sort(encodings, FSABuilder.LEXICAL_ORDERING);
watch.stop();
System.out.println("3: " + watch.toStringMillis());
//			final ExternalSorter sorter = new ExternalSorter();
//			final ExternalSorterOptions options = ExternalSorterOptions.builder()
//				.charset(charset)
//				.sortInParallel()
//				//lexical order
//				.comparator(Comparator.naturalOrder())
//				.removeDuplicates()
//				.lineSeparator(StringUtils.LF)
//				.build();
//			try{
//TimeWatch watch = TimeWatch.start();
//				sorter.sort(file, options, file);
//watch.stop();
//System.out.println(watch.toStringMillis());
//			}
//			catch(final Exception e){
//				throw new RuntimeException(e);
//			}

			return encodings;
		};
		final Function<List<byte[]>, FSA> step4 = encodings -> {
			resetProcessing("Create FSA (step 4/5)");

			getWorkerData()
				.withNoHeader()
				.withSequentialProcessing();

TimeWatch watch = TimeWatch.start();
			int index = 0;
			for(final byte[] encoding : encodings){
				fsaProcessor.accept(IndexDataPair.of(index, encoding));

				setProgress(++ index, encodings.size());
			}
watch.stop();
System.out.println("4: " + watch.toStringMillis());

//			if(!file.delete())
//				LOGGER.warn("Cannot delete support file {}", file.getAbsolutePath());

			return builder.complete();
		};
		final Function<FSA, File> step5 = fsa -> {
			resetProcessing("Compress FSA (step 5/5)");

			final CFSA2Serializer serializer = new CFSA2Serializer();
			try(final ByteArrayOutputStream os = new ByteArrayOutputStream()){
TimeWatch watch = TimeWatch.start();
				serializer.serialize(fsa, os);
watch.stop();
System.out.println("5: " + watch.toStringMillis());

				Files.write(outputFile.toPath(), os.toByteArray());

				finalizeProcessing("Successfully processed " + workerData.getWorkerName() + ": " + outputFile.getAbsolutePath());

				return outputFile;
			}
			catch(final Exception e){
				throw new RuntimeException(e.getMessage());
			}
		};
		final Function<File, Void> step6 = WorkerManager.openFolderStep(LOGGER);
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4).andThen(step5).andThen(step6));
	}

	private DictionaryMetadata readMetadata(final Charset charset, final File outputFile){
		final String filenameNoExtension = FilenameUtils.removeExtension(outputFile.getAbsolutePath());
		final File outputInfoFile = new File(filenameNoExtension + ".info");
		if(!outputInfoFile.exists()){
			final List<String> content = Arrays.asList(
				"fsa.dict.separator=" + Inflection.POS_FSA_SEPARATOR,
				"fsa.dict.encoding=" + charset.name().toLowerCase(),
				"fsa.dict.encoder=prefix");
			try{
				FileHelper.saveFile(outputInfoFile.toPath(), StringUtils.CR, charset, content);
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}
		}

		final Path metadataPath = DictionaryMetadata.getExpectedMetadataLocation(outputFile.toPath());
		try(final InputStream is = new BufferedInputStream(Files.newInputStream(metadataPath))){
			return DictionaryMetadata.read(is);
		}
		catch(final Exception e){
			throw new RuntimeException(e);
		}
	}

	private static final String SINGLE_POS_NOT_PRESENT = "Part-of-Speech not unique";
	private static final String SINGLE_STEM_NOT_PRESENT = "Stem not unique";
	private static final byte POS_FSA_TAG_SEPARATOR = (byte)'+';

	private List<byte[]> encode(final List<Inflection> inflections, final byte separator,
			final SequenceEncoderInterface sequenceEncoder){
		ByteBuffer encoded = ByteBuffer.allocate(0);
		ByteBuffer source = ByteBuffer.allocate(0);
		ByteBuffer target = ByteBuffer.allocate(0);
		ByteBuffer tag = ByteBuffer.allocate(0);
		ByteBuffer assembled = ByteBuffer.allocate(0);
		final List<byte[]> out = new ArrayList<>();
		int index = 0;
		for(final Inflection inflection : inflections){
			//subdivide morphologicalFields into PART_OF_SPEECH, INFLECTIONAL_SUFFIX, INFLECTIONAL_PREFIX, and STEM
			final Map<MorphologicalTag, List<String>> bucket = extractMorphologicalTags(inflection);

			//extract Part-of-Speech
			final List<String> pos = bucket.get(MorphologicalTag.PART_OF_SPEECH);
			if(pos.size() != 1)
				throw new LinterException(SINGLE_POS_NOT_PRESENT);
			//extract stem
			final List<String> stem = bucket.get(MorphologicalTag.STEM);
			if(stem.size() != 1)
				throw new LinterException(SINGLE_STEM_NOT_PRESENT);

			final byte[] inflectionStem = StringHelper.getRawBytes(stem.get(0));
			source = BufferUtils.clearAndEnsureCapacity(source, inflectionStem.length - 3);
			source.put(inflectionStem, 3, inflectionStem.length - 3);
			source.flip();

			final byte[] inflectedWord = StringHelper.getRawBytes(inflection.getWord());
			target = BufferUtils.clearAndEnsureCapacity(target, inflectedWord.length);
			target.put(inflectedWord);
			target.flip();

			//extract Inflection
			final List<String> suffixInflection = bucket.get(MorphologicalTag.INFLECTIONAL_SUFFIX);
			final List<String> prefixInflection = bucket.get(MorphologicalTag.INFLECTIONAL_PREFIX);

			tag = BufferUtils.clearAndEnsureCapacity(tag, 512);
			tag.put(StringHelper.getRawBytes(PartOfSpeechTag.createFromCodeAndValue(pos.get(0)).getTag()));
			if(suffixInflection != null)
				for(final String code : suffixInflection)
					for(final String t : InflectionTag.createFromCodeAndValue(code).getTags())
						tag.put(POS_FSA_TAG_SEPARATOR)
							.put(StringHelper.getRawBytes(t));
			if(prefixInflection != null)
				for(final String code : prefixInflection)
					for(final String t : InflectionTag.createFromCodeAndValue(code).getTags())
						tag.put(POS_FSA_TAG_SEPARATOR)
							.put(StringHelper.getRawBytes(t));
			tag.flip();


			encoded = sequenceEncoder.encode(target, source, encoded);

			assembled = BufferUtils.clearAndEnsureCapacity(assembled,
				target.capacity() + 1 + encoded.capacity() + 1 + tag.remaining());
			assembled.put(target);
			assembled.put(separator);
			assembled.put(encoded);
			if(tag.hasRemaining()){
				assembled.put(separator);
				assembled.put(tag);
			}
			assembled.flip();

			out.add(BufferUtils.toArray(assembled));

			setProgress(++ index, inflections.size());
		}
		return out;
	}

	//NOTE: the only morphological tags really needed are: PART_OF_SPEECH, INFLECTIONAL_SUFFIX, INFLECTIONAL_PREFIX, and STEM
	private Map<MorphologicalTag, List<String>> extractMorphologicalTags(final Inflection inflection){
		return SetHelper.bucket(inflection.getMorphologicalFieldsAsArray(), MorphologicalTag::createFromCode,
			MorphologicalTag.class);
	}

	private static int indexOf(final byte separator, final byte[] row, int fromIndex){
		while(fromIndex < row.length){
			if(row[fromIndex] == separator)
				return fromIndex;

			fromIndex ++;
		}
		return -1;
	}


	public static void main(final String[] args){
		final String input = "C:\\Users\\mauro\\AppData\\Local\\Temp\\hunlinter-pos.dat";
		final String[] buildOptions = {
			"--overwrite",
			"--accept-cr",
			"--exit", "false",
			"--format", "CFSA2",
			"--input", input
		};
TimeWatch watch = TimeWatch.start();
		morfologik.tools.DictCompile.main(buildOptions);
watch.stop();
System.out.println(watch.toStringMillis());
	}

}
