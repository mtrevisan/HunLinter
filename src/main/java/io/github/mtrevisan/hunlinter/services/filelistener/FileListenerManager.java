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
package io.github.mtrevisan.hunlinter.services.filelistener;

import io.github.mtrevisan.hunlinter.datastructures.SetHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;


/**
 * @see <a href="https://gist.github.com/hindol-viz/394ebc553673e2cd0699">Hindol viz</a>
 * @see <a href="https://github.com/Hindol/commons">Hindol commons</a>
 */
public class FileListenerManager implements FileListener, Runnable{

	private static final Logger LOGGER = LoggerFactory.getLogger(FileListenerManager.class);

	private static final String ASTERISK = "*";

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

	private static final FileSystem FILE_SYSTEM_DEFAULT = FileSystems.getDefault();

	private static final Map<WatchEvent.Kind<?>, BiConsumer<FileChangeListener, Path>> FILE_CHANGE_LISTENER_BY_EVENT
		= new HashMap<>(2);
	static{
		FILE_CHANGE_LISTENER_BY_EVENT.put(StandardWatchEventKinds.ENTRY_MODIFY, FileChangeListener::fileModified);
		FILE_CHANGE_LISTENER_BY_EVENT.put(StandardWatchEventKinds.ENTRY_DELETE, FileChangeListener::fileDeleted);
	}


	private WatchService watcher;
	private Future<?> watcherTask;
	private final AtomicBoolean running;
	private final ConcurrentMap<WatchKey, Path> watchKeyToDirPath;
	private final ConcurrentMap<Path, Set<FileChangeListener>> dirPathToListeners;
	private final ConcurrentMap<FileChangeListener, Set<PathMatcher>> listenerToFilePatterns;


	public FileListenerManager(){
		watcher = createWatcher();
		running = new AtomicBoolean(false);
		watchKeyToDirPath = new ConcurrentHashMap<>(0);
		dirPathToListeners = new ConcurrentHashMap<>(0);
		listenerToFilePatterns = new ConcurrentHashMap<>(0);
	}

	private static WatchService createWatcher(){
		try{
			return FILE_SYSTEM_DEFAULT.newWatchService();
		}
		catch(final IOException ioe){
			throw new RuntimeException("Exception while creating watch service", ioe);
		}
	}

	@Override
	public final void start(){
		if(running.compareAndSet(false, true))
			watcherTask = EXECUTOR.submit(this);
	}

	@Override
	public final void stop(){
		if(running.get()){
			running.set(false);

			watcherTask.cancel(true);
			watcherTask = null;

			//this piece of code prevents presenting a file as modified while the listener has been stopped and subsequently restarted
			try{
				watcher.close();
			}
			catch(final IOException ignored){}
			watcher = createWatcher();
		}
	}

	@Override
	public final void register(final FileChangeListener listener, final String... patterns){
		Objects.requireNonNull(listener, "Listener cannot be null");

		for(final String pattern : patterns){
			final Path dir = (new File(pattern)).getParentFile()
				.toPath();
			if(!dir.toFile().exists())
				LOGGER.warn("File or folder '{}' doesn't exists", dir);
			else{
				if(!dirPathToListeners.containsKey(dir))
					addWatchKeyToDir(dir);

				dirPathToListeners.computeIfAbsent(dir, key -> SetHelper.newConcurrentSet())
					.add(listener);
			}
		}

		addFilePatterns(listener, patterns);
	}

	@Override
	public final void unregisterAll(){
		stop();

		watchKeyToDirPath.clear();
		dirPathToListeners.clear();
		listenerToFilePatterns.clear();
	}

	private void addWatchKeyToDir(final Path dir){
		try{
			final WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

			watchKeyToDirPath.put(key, dir);
		}
		catch(final IOException ioe){
			LOGGER.error("Exception while watching {}", dir, ioe);
		}
	}

	private void addFilePatterns(final FileChangeListener listener, final String[] patterns){
		final Set<PathMatcher> filePatterns = SetHelper.newConcurrentSet();
		if(patterns != null)
			for(final String pattern : patterns)
				filePatterns.add(matcherForExpression(pattern));
		if(filePatterns.isEmpty())
			//match everything if no filter is found
			filePatterns.add(matcherForExpression(ASTERISK));

		listenerToFilePatterns.computeIfAbsent(listener, k -> new HashSet<>(1))
			.addAll(filePatterns);
	}

	private static PathMatcher matcherForExpression(final String pattern){
		final String syntaxAndPattern = "glob:" + pattern.substring(pattern.lastIndexOf(File.separator) + 1);
		return FILE_SYSTEM_DEFAULT.getPathMatcher(syntaxAndPattern);
	}

	@Override
	public final void run(){
		while(!Thread.interrupted()){
			try{
				final WatchKey key = watcher.take();

				preventMultipleEvents();

				if(manageKey(key))
					break;
			}
			catch(final InterruptedException e){
				Thread.currentThread().interrupt();
			}
		}

		LOGGER.info("Stopping file watcher service");
	}

	/**
	 * Prevent receiving two separate ENTRY_MODIFY events: file modified and timestamp updated.
	 * Instead, receive one ENTRY_MODIFY event with two counts
	 */
	private static void preventMultipleEvents(){
		try{ Thread.sleep(100l); }
		catch(final InterruptedException e){
			Thread.currentThread().interrupt();
		}
	}

	private boolean manageKey(final WatchKey key){
		boolean stopWatching = false;
		final Path dir = getDirPath(key);
		if(dir == null)
			LOGGER.warn("Watch key not recognized");
		else{
			notifyListeners(key);

			stopWatching = resetKey(key, dir);
		}
		return stopWatching;
	}

	/** Reset key to allow further events for this key to be processed. */
	private boolean resetKey(final WatchKey key, final Path dir){
		boolean stopWatching = false;
		final boolean valid = key.reset();
		if(!valid){
			watchKeyToDirPath.remove(key);

			LOGGER.warn("'{}' is inaccessible, stop watching", dir);

			if(watchKeyToDirPath.isEmpty())
				stopWatching = true;
		}
		return stopWatching;
	}

	private Path getDirPath(final WatchKey key){
		return watchKeyToDirPath.get(key);
	}

	private void notifyListeners(final WatchKey key){
		final List<WatchEvent<?>> pollEvents = key.pollEvents();
		for(final WatchEvent<?> event : pollEvents){
			final WatchEvent.Kind<?> eventKind = event.kind();

			//overflow occurs when the watch event queue is overflown with events
			if(eventKind.equals(StandardWatchEventKinds.OVERFLOW))
				continue;

			final BiConsumer<FileChangeListener, Path> listenerMethod = FILE_CHANGE_LISTENER_BY_EVENT.get(eventKind);
			if(listenerMethod != null){
				@SuppressWarnings("unchecked")
				final Path file = ((WatchEvent<Path>)event).context();

				final Path dir = getDirPath(key);
				final Set<FileChangeListener> listeners = matchedListeners(dir, file);

				final Path path = Path.of(dir.toAbsolutePath().toString(), file.toString());
				for(final FileChangeListener listener : listeners)
					listenerMethod.accept(listener, path);
			}
		}
	}

	private Set<FileChangeListener> matchedListeners(final Path dir, final Path file){
		final Set<FileChangeListener> listeners = getListeners(dir);
		final Set<FileChangeListener> set = new HashSet<>(listeners.size());
		for(final FileChangeListener listener : listeners){
			if(matchesAny(file, getPatterns(listener)))
				set.add(listener);
		}
		return set;
	}

	private Set<FileChangeListener> getListeners(final Path dir){
		return dirPathToListeners.get(dir);
	}

	public static boolean matchesAny(final Path input, final Iterable<PathMatcher> patterns){
		if(patterns != null)
			for(final PathMatcher pattern : patterns)
				if(matches(input, pattern))
					return true;
		return false;
	}

	public static boolean matches(final Path input, final PathMatcher pattern){
		return pattern.matches(input);
	}

	private Set<PathMatcher> getPatterns(final FileChangeListener listener){
		return listenerToFilePatterns.get(listener);
	}

}
