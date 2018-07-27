package unit731.hunspeller.parsers.dictionary.workers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import lombok.extern.slf4j.Slf4j;
import unit731.hunspeller.Backbone;
import unit731.hunspeller.parsers.dictionary.valueobjects.RuleProductionEntry;


@Slf4j
public class WordlistWorker extends WorkerDictionaryReadWriteBase{

	public WordlistWorker(Backbone backbone, File outputFile){
		Objects.requireNonNull(backbone);
		Objects.requireNonNull(outputFile);


		BiConsumer<BufferedWriter, String> body = (writer, line) -> {
			List<RuleProductionEntry> productions = backbone.applyRules(line);

			try{
				for(RuleProductionEntry production : productions){
					writer.write(production.getWord());
					writer.newLine();
				}
			}
			catch(IOException e){
				throw new IllegalArgumentException(e);
			}
		};
		Runnable done = () -> {
			log.info(Backbone.MARKER_APPLICATION, "File written: {}", outputFile.getAbsolutePath());
		};
		createWorker(backbone, outputFile, body, done);
	}

}
