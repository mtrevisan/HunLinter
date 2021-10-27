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
package io.github.mtrevisan.hunlinter.workers.core;

import io.github.mtrevisan.hunlinter.parsers.ParserManager;
import io.github.mtrevisan.hunlinter.services.Packager;
import io.github.mtrevisan.hunlinter.services.log.ExceptionHelper;
import io.github.mtrevisan.hunlinter.services.system.JavaHelper;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterException;
import io.github.mtrevisan.hunlinter.workers.exceptions.LinterIllegalArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


public class WorkerProject extends WorkerAbstract<WorkerDataProject>{

	private static final Logger LOGGER = LoggerFactory.getLogger(WorkerProject.class);

	public static final String WORKER_NAME = "Project";

	@FunctionalInterface
	interface StageFunction{
		void execute() throws IOException, SAXException;
	}


	protected WorkerProject(final WorkerDataProject workerData){
		super(workerData);
	}

	@Override
	protected final Void doInBackground(){
		prepareProcessing(WORKER_NAME, null);

		final Packager packager = workerData.getPackager();
		try{
			final ParserManager parserManager = workerData.getParserManager();
			final List<StageFunction> stages = Arrays.asList(
				() -> parserManager.openAffixFile(packager.getAffixFile()),
				() -> parserManager.openHyphenationFile(packager.getHyphenationFile()),
				parserManager::getCorrectnessChecker,
				() -> parserManager.prepareDictionaryFile(packager.getDictionaryFile()),
				() -> parserManager.openAidFile(parserManager.getAidFile()),
				() -> parserManager.openThesaurusFile(packager.getThesaurusDataFile()),
				() -> parserManager.openAutoCorrectFile(packager.getAutoCorrectFile()),
				() -> parserManager.openSentenceExceptionsFile(packager.getSentenceExceptionsFile()),
				() -> parserManager.openWordExceptionsFile(packager.getWordExceptionsFile()));
			for(int index = 0; index < stages.size(); index ++){
				stages.get(index).execute();
				setWorkerProgress(index + 1, stages.size());

				sleepOnPause();
			}

			finalizeProcessing(WORKER_NAME, "Project loaded successfully");
		}
		catch(final FileNotFoundException e){
			logExceptionError(e);
		}
		catch(final IOException | SAXException | LinterException | LinterIllegalArgumentException e){
			if(JavaHelper.isInterruptedException(e))
				cancel(e);
			else
				logExceptionError(e);
		}

		return null;
	}

	private static void logExceptionError(final Exception e){
		final String errorMessage = ExceptionHelper.getMessageNoLineNumber(e);
		LOGGER.error(ParserManager.MARKER_APPLICATION, errorMessage);
	}

}
