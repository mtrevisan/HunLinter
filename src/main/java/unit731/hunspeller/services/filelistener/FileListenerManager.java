package unit731.hunspeller.services.filelistener;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;


//https://gist.github.com/hindol-viz/394ebc553673e2cd0699
//https://github.com/Hindol/commons
//https://stackoverflow.com/questions/16251273/can-i-watch-for-single-file-change-with-watchservice-not-the-whole-directory
@Slf4j
public class FileListenerManager implements FileListener, Runnable{

	private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();


	private Future<?> watcherTask;
	private final AtomicBoolean running;
	private final Set<Path> watchedPath;
	private final FileChangeListener listener;


	public FileListenerManager(FileChangeListener listener){
		Objects.requireNonNull(listener);

		running = new AtomicBoolean(false);
		watchedPath = Collections.newSetFromMap(newConcurrentMap());
		this.listener = listener;
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
	public void register(String dirPath) throws IOException{
		Objects.requireNonNull(dirPath);

		Path dir = Paths.get(dirPath);
		if(!Files.isDirectory(dir))
			throw new IllegalArgumentException(dirPath + " is not a directory");

		log.info("Watching files under " + dirPath + " for changes");
	}

	@Override
	public void run(){
		WatchService watcher = createWatcher();

		ConcurrentMap<WatchKey, Path> watchKeyToDirPath = addWatchedPaths(watcher);

		while(!Thread.interrupted()){
			try{
				WatchKey key = watcher.take();

				Path dir = watchKeyToDirPath.get(key);
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
			return FileSystems.getDefault().newWatchService();
		}
		catch(IOException e){
			throw new RuntimeException("Exception while creating watch service", e);
		}
	}

	private ConcurrentMap<WatchKey, Path> addWatchedPaths(WatchService watcher){
		ConcurrentMap<WatchKey, Path> watchKeyToDirPath = newConcurrentMap();
		for(Path dir : watchedPath){
			try{
				WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
					StandardWatchEventKinds.ENTRY_DELETE);
				watchKeyToDirPath.put(key, dir);
			}
			catch(IOException e){
				log.error("Exception while watching {}", dir, e);
			}
		}
		return watchKeyToDirPath;
	}

	private <K, V> ConcurrentMap<K, V> newConcurrentMap(){
		return new ConcurrentHashMap<>();
	}

	private void notifyListeners(WatchKey key){
		for(WatchEvent<?> event : key.pollEvents()){
			WatchEvent.Kind<?> eventKind = event.kind();
			if(eventKind.equals(StandardWatchEventKinds.OVERFLOW)){
				//overflow occurs when the watch event queue is overflown with events
				//TODO notify all listeners

				break;
			}

			@SuppressWarnings("unchecked")
			Path file = ((WatchEvent<Path>)event).context();

			if(eventKind.equals(StandardWatchEventKinds.ENTRY_CREATE))
				listener.fileCreated(file);
			else if(eventKind.equals(StandardWatchEventKinds.ENTRY_MODIFY))
				listener.fileModified(file);
			else if(eventKind.equals(StandardWatchEventKinds.ENTRY_DELETE))
				listener.fileDeleted(file);
		}
	}

}
