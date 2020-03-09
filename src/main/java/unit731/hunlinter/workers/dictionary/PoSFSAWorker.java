package unit731.hunlinter.workers.dictionary;

import morfologik.stemming.ISequenceEncoder;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.builders.FSABuilder;
import unit731.hunlinter.services.fsa.builders.FSASerializer;
import unit731.hunlinter.services.fsa.stemming.BufferUtils;
import unit731.hunlinter.services.fsa.stemming.Dictionary;
import unit731.hunlinter.services.fsa.stemming.DictionaryLookup;
import unit731.hunlinter.services.fsa.stemming.DictionaryMetadata;
import unit731.hunlinter.services.fsa.tools.SerializationFormat;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;
import unit731.hunlinter.workers.exceptions.LinterException;
import unit731.hunlinter.services.FileHelper;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;


public class PoSFSAWorker extends WorkerDictionary{

	private static final Logger LOGGER = LoggerFactory.getLogger(PoSFSAWorker.class);

	public static final String WORKER_NAME = "Part–of–Speech FSA Extractor";


	public PoSFSAWorker(final DictionaryParser dicParser, final WordGenerator wordGenerator, final File outputFile){
		super(new WorkerDataParser<>(WORKER_NAME, dicParser)
			.withParallelProcessing(true));

		Objects.requireNonNull(wordGenerator);
		Objects.requireNonNull(outputFile);


		final BiConsumer<BufferedWriter, Pair<Integer, String>> lineProcessor = (writer, line) -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(line.getValue());
			final List<Production> productions = wordGenerator.applyAffixRules(dicEntry);

			try{
				for(final Production production : productions)
					for(final String text : production.toStringPoSFSA()){
						writer.write(text);
						writer.write(StringUtils.LF);
					}
			}
			catch(final Exception e){
				throw new LinterException(e.getMessage());
			}
		};
		final Runnable completed = () -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Post-processing");

			try{
				final String filenameNoExtension = FilenameUtils.removeExtension(outputFile.getAbsolutePath());
				final File outputInfoFile = new File(filenameNoExtension + ".info");
				if(!outputInfoFile.exists()){
					final Charset charset = dicParser.getCharset();
					final List<String> content = Arrays.asList(
						"fsa.dict.separator=" + Production.POS_FSA_SEPARATOR,
						"fsa.dict.encoding=" + charset.name().toLowerCase(),
						"fsa.dict.encoder=prefix");
					FileHelper.saveFile(outputInfoFile.toPath(), StringUtils.CR, charset, content);
				}

				buildFSA(outputFile.toString(), filenameNoExtension + ".dict");

				finalizeProcessing("File written: " + filenameNoExtension + ".dict");

				FileHelper.browse(outputFile);
			}
			catch(final Exception e){
				LOGGER.warn("Exception while creating the FSA file for Part–of–Speech", e);
			}
		};

		setWriteDataProcessor(lineProcessor, outputFile);
		getWorkerData()
			.withDataCompletedCallback(completed);
	}

	private void buildFSA(final List<String> words, final String input, final String output) throws Exception{
		final Path inputPath = Path.of(input);
		final Path outputPath = Path.of(output);
		final SerializationFormat format = SerializationFormat.CFSA2;

		final Path metadataPath = DictionaryMetadata.getExpectedMetadataLocation(inputPath);

		if(!Files.isRegularFile(metadataPath))
			throw new IllegalArgumentException("Dictionary metadata file for the input does not exist: " + metadataPath
				+ "\r\nThe metadata file (with at least the column separator and byte encoding) is required. Check out the examples.");

//		final Path outputPath = metadataPath.resolveSibling(metadataPath.getFileName().toString().replaceAll("\\." + DictionaryMetadata.METADATA_FILE_EXTENSION + "$", ".dict"));

		final DictionaryMetadata metadata;
		try(final InputStream is = new BufferedInputStream(Files.newInputStream(metadataPath))){
			metadata = DictionaryMetadata.read(is);
		}

		final CharsetDecoder charsetDecoder = metadata.getDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);

		final byte separator = metadata.getSeparator();
		final ISequenceEncoder sequenceEncoder = metadata.getSequenceEncoderType().get();

		if(!words.isEmpty()){
			Iterator<byte[]> i = words.iterator();
			byte[] row = i.next();
			final int separatorCount = countOf(separator, row);

			if(separatorCount < 1 || separatorCount > 2)
				throw new IllegalArgumentException("Invalid input. Each row must consist of [base,inflected,tag?] columns, where ',' is a separator character (declared as: %s). This row contains %d separator characters: %s", Character.isJavaIdentifierPart(metadata.getSeparatorAsChar())? "'" + Character.toString(metadata.getSeparatorAsChar()) + "'": "0x" + Integer.toHexString((int) separator & 0xff), separatorCount, new String(row, charsetDecoder.charset()));

			while(i.hasNext()){
				row = i.next();
				final int count = countOf(separator, row);
				if(count != separatorCount)
					throw new IllegalArgumentException("The number of separators (%d) is inconsistent with previous lines: %s", count, new String(row, charsetDecoder.charset()));
			}
		}

		ByteBuffer encoded = ByteBuffer.allocate(0);
		ByteBuffer source = ByteBuffer.allocate(0);
		ByteBuffer target = ByteBuffer.allocate(0);
		ByteBuffer tag = ByteBuffer.allocate(0);
		ByteBuffer assembled = ByteBuffer.allocate(0);
		for(int i = 0, max = words.size(); i < max; i++){
			final byte[] row = words.get(i);
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

			words.set(i, BufferUtils.toArray(assembled));
		}

		//lexical order
		Collections.sort(words);
		final FSA fsa = FSABuilder.build(words);

		final FSASerializer serializer = format.getSerializer();
		try(final OutputStream os = new BufferedOutputStream(Files.newOutputStream(outputPath))){
			serializer.serialize(fsa, os);
		}

		//if validating, try to scan the input
		final DictionaryLookup dictionaryLookup = new DictionaryLookup(new Dictionary(fsa, metadata));
		for(final Iterator<?> i = dictionaryLookup.iterator(); i.hasNext(); i.next()){
			// Do nothing, just scan and make sure no exceptions are thrown.
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
