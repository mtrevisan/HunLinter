package unit731.hunlinter.workers.dictionary;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.serializers.CFSA2Serializer;
import unit731.hunlinter.services.fsa.builders.FSABuilder;
import unit731.hunlinter.services.fsa.stemming.BufferUtils;
import unit731.hunlinter.services.fsa.stemming.Dictionary;
import unit731.hunlinter.services.fsa.stemming.DictionaryLookup;
import unit731.hunlinter.services.fsa.stemming.DictionaryMetadata;
import unit731.hunlinter.services.fsa.stemming.ISequenceEncoder;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


public class PoSFSAWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(PoSFSAWorker.class);

	public static final String WORKER_NAME = "Part–of–Speech FSA Extractor";


	public PoSFSAWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);

		final Charset charset = dicParser.getCharset();


		final Set<String> words = new HashSet<>();
		final BiConsumer<Integer, String> lineProcessor = (row, line) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line);
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			productions.stream()
				.map(Production::toStringPoSFSA)
				.flatMap(List::stream)
				.forEach(words::add);
		};
		final FSABuilder builder = new FSABuilder();
		final Consumer<String> fsaProcessor = word -> {
			final byte[] chs = word.getBytes(StandardCharsets.UTF_8);
			builder.add(chs);
		};
//		final Runnable completed = () -> {
//			LOGGER.info(ParserManager.MARKER_APPLICATION, "Post-processing");
//
//			try{
//				final String filenameNoExtension = FilenameUtils.removeExtension(outputFile.getAbsolutePath());
//				final File outputInfoFile = new File(filenameNoExtension + ".info");
//				if(!outputInfoFile.exists()){
//					final List<String> content = Arrays.asList(
//						"fsa.dict.separator=" + Production.POS_FSA_SEPARATOR,
//						"fsa.dict.encoding=" + charset.name().toLowerCase(),
//						"fsa.dict.encoder=prefix");
//					FileHelper.saveFile(outputInfoFile.toPath(), StringUtils.CR, charset, content);
//				}
//
//				buildFSA(new ArrayList<>(words), outputFile.toString(), filenameNoExtension + ".dict");
//
//				finalizeProcessing("File written: " + filenameNoExtension + ".dict");
//
//				FileHelper.browse(outputFile);
//			}
//			catch(final Exception e){
//				LOGGER.warn("Exception while creating the FSA file for Part–of–Speech", e);
//			}
//		};

		getWorkerData()
//			.withDataCompletedCallback(completed)
			.withRelaunchException(true);

		final Function<Void, List<Pair<Integer, String>>> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/4)");

			return readLines();
		};
		final Function<List<Pair<Integer, String>>, Set<String>> step2 = lines -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Extract words (step 2/4)");

			executeReadProcess(lineProcessor, lines);

			return words;
		};
		final Function<Set<String>, FSA> step3 = uniqueWordsSet -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Create FSA (step 3/4)");

			//lexical order
			final List<String> uniqueWords = new ArrayList<>(uniqueWordsSet);
			Collections.sort(uniqueWords);

			executeReadProcessNoIndex(fsaProcessor, uniqueWords);

			return builder.complete();
		};
		final Function<FSA, File> step4 = fsa -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Compress FSA (step 4/4)");

			//FIXME
			final CFSA2Serializer serializer = new CFSA2Serializer();
			try(final OutputStream os = new BufferedOutputStream(Files.newOutputStream(outputFile.toPath()))){
				serializer.serialize(fsa, os);

				finalizeProcessing("Successfully processed " + workerData.getWorkerName());

				return null;
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}
		};
		final Function<File, Void> step5 = file -> {
			finalizeProcessing("File written: " + file.getAbsolutePath());

			WorkerManager.openFolderStep(LOGGER).apply(file);

			return null;
		};
		setProcessor(step1.andThen(step2).andThen(step3).andThen(step4).andThen(step5));
	}

	private void buildFSA(final List<String> words, final String input, final String output) throws Exception{
		final Path inputPath = Path.of(input);
		final Path outputPath = Path.of(output);

		final Path metadataPath = DictionaryMetadata.getExpectedMetadataLocation(inputPath);

		if(!Files.isRegularFile(metadataPath))
			throw new IllegalArgumentException("Dictionary metadata file for the input does not exist: " + metadataPath
				+ "\r\nThe metadata file (with at least the column separator and byte encoding) is required. Check out the examples.");

//		final Path outputPath = metadataPath.resolveSibling(metadataPath.getFileName().toString().replaceAll("\\." + DictionaryMetadata.METADATA_FILE_EXTENSION + "$", ".dict"));

		final DictionaryMetadata metadata;
		try(final InputStream is = new BufferedInputStream(Files.newInputStream(metadataPath))){
			metadata = DictionaryMetadata.read(is);
		}

		final CharsetDecoder charsetDecoder = metadata.getDecoder()
			.onMalformedInput(CodingErrorAction.REPORT)
			.onUnmappableCharacter(CodingErrorAction.REPORT);

		final byte separator = metadata.getSeparator();
		final ISequenceEncoder sequenceEncoder = metadata.getSequenceEncoderType().get();

		if(!words.isEmpty()){
			//lexical order
			Collections.sort(words);

			Iterator<String> i = words.iterator();
			byte[] row = i.next().getBytes(StandardCharsets.UTF_8);
			final int separatorCount = countOf(separator, row);

			if(separatorCount < 1 || separatorCount > 2){
				String separatorCharacter = (Character.isJavaIdentifierPart(metadata.getSeparatorAsChar())? "'" + metadata.getSeparatorAsChar() + "'": "0x" + Integer.toHexString((int) separator & 0xff));
				throw new IllegalArgumentException("Invalid input. Each row must consist of [base,inflected,tag?] columns, where ',' is a separator character"
					+ " (declared as: " + separatorCharacter + "). This row contains " + separatorCount + " separator characters: " + new String(row, charsetDecoder.charset()));
			}

			while(i.hasNext()){
				row = i.next().getBytes(StandardCharsets.UTF_8);
				final int count = countOf(separator, row);
				if(count != separatorCount)
					throw new IllegalArgumentException("The number of separators (" + count + ") is inconsistent with previous lines: " + new String(row, charsetDecoder.charset()));
			}
		}

		ByteBuffer encoded = ByteBuffer.allocate(0);
		ByteBuffer source = ByteBuffer.allocate(0);
		ByteBuffer target = ByteBuffer.allocate(0);
		ByteBuffer tag = ByteBuffer.allocate(0);
		ByteBuffer assembled = ByteBuffer.allocate(0);
		for(int i = 0, max = words.size(); i < max; i++){
			final byte[] row = words.get(i).getBytes(StandardCharsets.UTF_8);
			final int sep1 = indexOf(separator, row, 0);
			int sep2 = indexOf(separator, row, sep1 + 1);
			if(sep2 < 0)
				sep2 = row.length;

			source = BufferUtils.clearAndEnsureCapacity(source, sep1);
			source.put(row, 0, sep1);
			source.flip();

			final int len = sep2 - (sep1 + 1);
			target = BufferUtils.clearAndEnsureCapacity(target, len);
			target.put(row, sep1 + 1, len);
			target.flip();

			final int len2 = row.length - (sep2 + 1);
			tag = BufferUtils.clearAndEnsureCapacity(tag, len2);
			if(len2 > 0)
				tag.put(row, sep2 + 1, len2);
			tag.flip();

			encoded = sequenceEncoder.encode(encoded, target, source);

			assembled = BufferUtils.clearAndEnsureCapacity(assembled, target.remaining() + 1 + encoded.remaining() + 1 + tag.remaining());

			assembled.put(target);
			assembled.put(separator);
			assembled.put(encoded);
			if(tag.hasRemaining()){
				assembled.put(separator);
				assembled.put(tag);
			}
			assembled.flip();

			words.set(i, new String(BufferUtils.toArray(assembled), StandardCharsets.UTF_8));
		}

		//lexical order
		Collections.sort(words);
		List<byte[]> in = words.stream()
			.map(word -> word.getBytes(StandardCharsets.UTF_8))
			.collect(Collectors.toList());
		final FSABuilder builder = new FSABuilder();
		final FSA fsa = builder.build(in);

		final CFSA2Serializer serializer = new CFSA2Serializer();
		try(final OutputStream os = new BufferedOutputStream(Files.newOutputStream(outputPath))){
			serializer.serialize(fsa, os);
		}

		//if validating, try to scan the input
		final DictionaryLookup dictionaryLookup = new DictionaryLookup(new Dictionary(fsa, metadata));
		//noinspection StatementWithEmptyBody
		for(final Iterator<?> i = dictionaryLookup.iterator(); i.hasNext(); i.next()){
			//do nothing, just scan and make sure no exceptions are thrown.
		}
	}

	private static int countOf(final byte separator, final byte[] row){
		int cnt = 0;
		for(int i = row.length; --i >= 0; )
			if(row[i] == separator)
				cnt ++;
		return cnt;
	}

	private static int indexOf(final byte separator, final byte[] row, int fromIndex){
		while(fromIndex < row.length){
			if(row[fromIndex] == separator)
				return fromIndex;

			fromIndex++;
		}
		return -1;
	}

}
