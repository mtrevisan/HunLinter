package unit731.hunlinter.workers.dictionary;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import unit731.hunlinter.parsers.ParserManager;
import unit731.hunlinter.parsers.dictionary.DictionaryParser;
import unit731.hunlinter.parsers.dictionary.generators.WordGenerator;
import unit731.hunlinter.parsers.vos.DictionaryEntry;
import unit731.hunlinter.parsers.vos.Production;
import unit731.hunlinter.services.FileHelper;
import unit731.hunlinter.services.fsa.FSA;
import unit731.hunlinter.services.fsa.serializers.CFSA2Serializer;
import unit731.hunlinter.services.fsa.builders.FSABuilder;
import unit731.hunlinter.services.fsa.stemming.BufferUtils;
import unit731.hunlinter.services.fsa.stemming.DictionaryMetadata;
import unit731.hunlinter.services.fsa.stemming.ISequenceEncoder;
import unit731.hunlinter.services.sorters.externalsorter.ExternalSorter;
import unit731.hunlinter.services.sorters.externalsorter.ExternalSorterOptions;
import unit731.hunlinter.services.system.TimeWatch;
import unit731.hunlinter.services.text.StringHelper;
import unit731.hunlinter.workers.WorkerManager;
import unit731.hunlinter.workers.core.IndexDataPair;
import unit731.hunlinter.workers.core.WorkerDataParser;
import unit731.hunlinter.workers.core.WorkerDictionary;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import static unit731.hunlinter.services.system.LoopHelper.forEach;


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
		final ISequenceEncoder sequenceEncoder = metadata.getSequenceEncoderType().get();


		final File supportFile;
		final BufferedWriter writer;
		try{
			supportFile = FileHelper.createDeleteOnExitFile("hunlinter-pos", ".dat");
			final OutputStream out = new GZIPOutputStream(new FileOutputStream(supportFile), 2048){
				{
					def.setLevel(Deflater.BEST_SPEED);
				}
			};
			writer = new BufferedWriter(new OutputStreamWriter(out, charset));
		}
		catch(final IOException e){
			throw new RuntimeException(e);
		}

		final Consumer<IndexDataPair<String>> lineProcessor = indexData -> {
			final DictionaryEntry dicEntry = wordGenerator.createFromDictionaryLine(indexData.getData());
			final Production[] productions = wordGenerator.applyAffixRules(dicEntry);

			forEach(productions, production -> {
				final List<String> lines = production.toStringPoSFSA();

				//encode lines
				final List<String> encodedLines = encode(lines, separator, sequenceEncoder);

				forEach(encodedLines, line -> writeLine(writer, line));
			});
		};
		final FSABuilder builder = new FSABuilder();
		final Consumer<IndexDataPair<String>> fsaProcessor = indexData -> {
			final byte[] chs = StringHelper.getRawBytes(indexData.getData());
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
			.withDataCancelledCallback(e -> closeWriter(writer))
			.withRelaunchException(true);

		final Function<Void, File> step1 = ignored -> {
			prepareProcessing("Reading dictionary file (step 1/4)");

			final Path dicPath = dicParser.getDicFile().toPath();
			processLines(dicPath, charset, lineProcessor);
			closeWriter(writer);

			return supportFile;
		};
		final Function<File, File> step2 = file -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Create support file (step 2/4)");

			setProgress(0);

			final String filenameNoExtension = FilenameUtils.removeExtension(outputFile.getAbsolutePath());
			final File outputInfoFile = new File(filenameNoExtension + ".info");
			if(!outputInfoFile.exists()){
				final List<String> content = Arrays.asList(
					"fsa.dict.separator=" + Production.POS_FSA_SEPARATOR,
					"fsa.dict.encoding=" + charset.name().toLowerCase(),
					"fsa.dict.encoder=prefix");
				try{
					FileHelper.saveFile(outputInfoFile.toPath(), StringUtils.CR, charset, content);
				}
				catch(final Exception e){
					throw new RuntimeException(e);
				}
			}

			setProgress(50);

			LOGGER.info(ParserManager.MARKER_APPLICATION, "Sorting");

			//sort file & remove duplicates
			final ExternalSorter sorter = new ExternalSorter();
			final ExternalSorterOptions options = ExternalSorterOptions.builder()
				.charset(charset)
				.sortInParallel()
				//lexical order
				.comparator(Comparator.naturalOrder())
				.useInputAsZip()
				.useZip()
				.removeDuplicates()
				.build();
			try{
TimeWatch watch = TimeWatch.start();
				sorter.sort(file, options, file);
watch.stop();
System.out.println(watch.toStringMillis());
			}
			catch(final Exception e){
				throw new RuntimeException(e);
			}

			return file;
		};
		final Function<File, FSA> step3 = file -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Create FSA (step 3/4)");

			processLines(file.toPath(), charset, fsaProcessor);

			if(!file.delete())
				LOGGER.warn("Cannot delete support file {}", file.getAbsolutePath());

			return builder.complete();
		};
		final Function<FSA, File> step4 = fsa -> {
			LOGGER.info(ParserManager.MARKER_APPLICATION, "Compress FSA (step 4/4)");

			final CFSA2Serializer serializer = new CFSA2Serializer();
			try(final ByteArrayOutputStream os = new ByteArrayOutputStream()){
				serializer.serialize(fsa, os);

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

	private DictionaryMetadata readMetadata(final Charset charset, final File outputFile){
		final String filenameNoExtension = FilenameUtils.removeExtension(outputFile.getAbsolutePath());
		final File outputInfoFile = new File(filenameNoExtension + ".info");
		if(!outputInfoFile.exists()){
			final List<String> content = Arrays.asList(
				"fsa.dict.separator=" + Production.POS_FSA_SEPARATOR,
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

	private List<String> encode(final List<String> words, final byte separator, final ISequenceEncoder sequenceEncoder){
		ByteBuffer encoded = ByteBuffer.allocate(0);
		ByteBuffer source = ByteBuffer.allocate(0);
		ByteBuffer target = ByteBuffer.allocate(0);
		ByteBuffer tag = ByteBuffer.allocate(0);
		ByteBuffer assembled = ByteBuffer.allocate(0);
		for(int i = 0, max = words.size(); i < max; i ++){
			final byte[] row = StringHelper.getRawBytes(words.get(i));
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

			assembled = BufferUtils.clearAndEnsureCapacity(assembled,
				target.remaining() + 1 + encoded.remaining() + 1 + tag.remaining());

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
		return words;
	}

	private static int indexOf(final byte separator, final byte[] row, int fromIndex){
		while(fromIndex < row.length){
			if(row[fromIndex] == separator)
				return fromIndex;

			fromIndex ++;
		}
		return -1;
	}

}
