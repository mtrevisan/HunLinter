package unit731.hunspeller.services.filelistener;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;


/**
 * @see <a href="https://gist.github.com/hindol-viz/394ebc553673e2cd0699">Hindol viz</a>
 * @see <a href="https://github.com/Hindol/commons">Hindol commons</a>
 */
@Slf4j
public class FileListenerManager implements FileListener, Runnable{

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

	private static final FileSystem FILE_SYSTEM_DEFAULT = FileSystems.getDefault();


	private final WatchService watcher;
	private Future<?> watcherTask;
	private final AtomicBoolean running;
	private final ConcurrentMap<WatchKey, Path> watchKeyToDirPath;
	private final ConcurrentMap<Path, Set<FileChangeListener>> dirPathToListeners;
	private final ConcurrentMap<FileChangeListener, Set<PathMatcher>> listenerToFilePatterns;


	public FileListenerManager(){
		watcher = createWatcher();
		running = new AtomicBoolean(false);
		watchKeyToDirPath = newConcurrentMap();
		dirPathToListeners = newConcurrentMap();
		listenerToFilePatterns = newConcurrentMap();
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
	public void register(FileChangeListener listener, String dirPath, String... patterns) throws IOException{
		Objects.requireNonNull(listener);
		Objects.requireNonNull(dirPath);

		Path dir = Paths.get(dirPath);
		if(!Files.isDirectory(dir))
			throw new IllegalArgumentException(dirPath + " is not a directory");

		if(!dirPathToListeners.containsKey(dir)){
			try{
				WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
					StandardWatchEventKinds.ENTRY_DELETE);

				watchKeyToDirPath.put(key, dir);
			}
			catch(IOException e){
				log.error("Exception while watching {}", dir, e);
			}
		}
		dirPathToListeners.computeIfAbsent(dir, key -> newConcurrentSet())
			.add(listener);

		addFilePatterns(listener, patterns);

		log.info("Watching files under " + dirPath + " for changes");
	}

	private void addFilePatterns(FileChangeListener listener, String[] patterns){
		Set<PathMatcher> filePatterns = newConcurrentSet();
		Arrays.stream(patterns)
			.map(this::matcherForExpression)
			.forEach(filePatterns::add);
		if(filePatterns.isEmpty())
			//match everything if no filter is found
			filePatterns.add(matcherForExpression("*"));

		listenerToFilePatterns.put(listener, filePatterns);
	}

	private PathMatcher matcherForExpression(String pattern){
		return FILE_SYSTEM_DEFAULT.getPathMatcher("glob:" + pattern);
	}

	@Override
	public void run(){
		while(!Thread.interrupted()){
			try{
				WatchKey key = watcher.take();

				Path dir = getDirPath(key);
				if(dir == null){
					log.warn("Watch key not recognized");

					continue;
				}

				notifyListeners(key);

				//reset key to allow further events for this key to be processed
				boolean valid = key.reset();
				if(!valid){
					watchKeyToDirPath.remove(key);

					log.warn("'{}' is inaccessible, stopping watch", dir);

					if(watchKeyToDirPath.isEmpty())
						break;
				}
			}
			catch(InterruptedException e){
				Thread.currentThread().interrupt();
			}
		}

		log.info("Stopping file watcher service");
	}

	private WatchService createWatcher() throws RuntimeException{
		try{
			return FILE_SYSTEM_DEFAULT.newWatchService();
		}
		catch(IOException e){
			throw new RuntimeException("Exception while creating watch service", e);
		}
	}

	private Path getDirPath(WatchKey key){
		return watchKeyToDirPath.get(key);
	}

	private <K, V> ConcurrentMap<K, V> newConcurrentMap(){
		return new ConcurrentHashMap<>();
	}

	private <T> Set<T> newConcurrentSet(){
		return Collections.newSetFromMap(newConcurrentMap());
	}

	private void notifyListeners(WatchKey key){
		List<WatchEvent<?>> pollEvents = key.pollEvents();
		for(WatchEvent<?> event : pollEvents){
			WatchEvent.Kind<?> eventKind = event.kind();
			if(eventKind.equals(StandardWatchEventKinds.OVERFLOW)){
				//overflow occurs when the watch event queue is overflown with events
				//TODO notify all listeners

				break;
			}

			@SuppressWarnings("unchecked")
			Path file = ((WatchEvent<Path>)event).context();

			Path dir = getDirPath(key);
			Set<FileChangeListener> listeners = matchedListeners(dir, file);

			if(eventKind.equals(StandardWatchEventKinds.ENTRY_CREATE))
				listeners.forEach(listener -> listener.fileCreated(file));
			else if(eventKind.equals(StandardWatchEventKinds.ENTRY_MODIFY))
				listeners.forEach(listener -> listener.fileModified(file));
			else if(eventKind.equals(StandardWatchEventKinds.ENTRY_DELETE))
				listeners.forEach(listener -> listener.fileDeleted(file));
		}
	}

	private Set<FileChangeListener> matchedListeners(Path dir, Path file){
		return getListeners(dir).stream()
			.filter(list -> matchesAny(file, getPatterns(list)))
			.collect(Collectors.toSet());
	}

	private Set<FileChangeListener> getListeners(Path dir){
		return dirPathToListeners.get(dir);
	}

	public boolean matchesAny(Path input, Set<PathMatcher> patterns){
		return patterns.stream()
			.anyMatch(pattern -> matches(input, pattern));
	}

	public boolean matches(Path input, PathMatcher pattern){
		return pattern.matches(input);
	}

	private Set<PathMatcher> getPatterns(FileChangeListener listener){
		return listenerToFilePatterns.get(listener);
	}

}
