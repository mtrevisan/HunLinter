package unit731.hunlinter.services.filelistener;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
		= new HashMap<>(3);
	static{
		FILE_CHANGE_LISTENER_BY_EVENT.put(StandardWatchEventKinds.ENTRY_MODIFY, FileChangeListener::fileModified);
		FILE_CHANGE_LISTENER_BY_EVENT.put(StandardWatchEventKinds.ENTRY_DELETE, FileChangeListener::fileDeleted);
	}


	private final WatchService watcher;
	private Future<?> watcherTask;
	private final AtomicBoolean running;
	private final ConcurrentMap<WatchKey, Path> watchKeyToDirPath;
	private final ConcurrentMap<Path, Set<FileChangeListener>> dirPathToListeners;
	private final ConcurrentMap<FileChangeListener, Set<PathMatcher>> listenerToFilePatterns;


	public FileListenerManager(){
		watcher = createWatcher();
		running = new AtomicBoolean(false);
		watchKeyToDirPath = new ConcurrentHashMap<>();
		dirPathToListeners = new ConcurrentHashMap<>();
		listenerToFilePatterns = new ConcurrentHashMap<>();
	}

	private WatchService createWatcher() throws RuntimeException{
		try{
			return FILE_SYSTEM_DEFAULT.newWatchService();
		}
		catch(final IOException e){
			throw new RuntimeException("Exception while creating watch service", e);
		}
	}

	@Override
	public void start(){
		if(running.compareAndSet(false, true))
			watcherTask = EXECUTOR.submit(this);
	}

	@Override
	public void stop(){
		if(running.get()){
			watcherTask.cancel(true);
			watcherTask = null;

			running.set(false);
		}
	}

	@Override
	public void register(final FileChangeListener listener, final String... patterns){
		Objects.requireNonNull(listener);

		for(final String pattern : patterns){
			final Path dir = (new File(pattern)).getParentFile().toPath();
			if(!dir.toFile().exists())
				LOGGER.warn("File or folder '{}' does not exists", dir);
			else{
				if(!dirPathToListeners.containsKey(dir))
					addWatchKeyToDir(dir);

				dirPathToListeners.computeIfAbsent(dir, key -> newConcurrentSet())
					.add(listener);
			}
		}

		addFilePatterns(listener, patterns);
	}

	@Override
	public void unregisterAll(){
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
		catch(final IOException e){
			LOGGER.error("Exception while watching {}", dir, e);
		}
	}

	private void addFilePatterns(FileChangeListener listener, String[] patterns){
		final Set<PathMatcher> filePatterns = newConcurrentSet();
		Arrays.stream(patterns)
			.map(this::matcherForExpression)
			.forEach(filePatterns::add);
		if(filePatterns.isEmpty())
			//match everything if no filter is found
			filePatterns.add(matcherForExpression(ASTERISK));

		listenerToFilePatterns.put(listener, filePatterns);
	}

	private <T> Set<T> newConcurrentSet(){
		return Collections.newSetFromMap(new ConcurrentHashMap<>());
	}

	private PathMatcher matcherForExpression(String pattern){
		return FILE_SYSTEM_DEFAULT.getPathMatcher("glob:" + pattern.substring(pattern.lastIndexOf(File.separator) + 1));
	}

	@Override
	public void run(){
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
	private void preventMultipleEvents(){
		try{ Thread.sleep(50); }
		catch(final InterruptedException e){
			Thread.currentThread().interrupt();
		}
	}

	private boolean manageKey(WatchKey key){
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

	/** Reset key to allow further events for this key to be processed */
	private boolean resetKey(WatchKey key, Path dir){
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
				break;

			final BiConsumer<FileChangeListener, Path> listenerMethod = FILE_CHANGE_LISTENER_BY_EVENT.get(eventKind);
			if(listenerMethod != null){
				@SuppressWarnings("unchecked")
				final Path file = ((WatchEvent<Path>)event).context();

				final Path dir = getDirPath(key);
				final Set<FileChangeListener> listeners = matchedListeners(dir, file);

				listeners.forEach(listener -> listenerMethod.accept(listener, file));
			}
		}
	}

	private Set<FileChangeListener> matchedListeners(final Path dir, final Path file){
		return getListeners(dir).stream()
			.filter(list -> matchesAny(file, getPatterns(list)))
			.collect(Collectors.toSet());
	}

	private Set<FileChangeListener> getListeners(final Path dir){
		return dirPathToListeners.get(dir);
	}

	public boolean matchesAny(final Path input, final Set<PathMatcher> patterns){
		return patterns.stream()
			.anyMatch(pattern -> matches(input, pattern));
	}

	public boolean matches(final Path input, final PathMatcher pattern){
		return pattern.matches(input);
	}

	private Set<PathMatcher> getPatterns(final FileChangeListener listener){
		return listenerToFilePatterns.get(listener);
	}

}
