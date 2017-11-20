package unit731.hunspeller.services.filelistener;

import java.io.File;
import java.io.IOException;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;


//https://gist.github.com/hindol-viz/394ebc553673e2cd0699
//https://stackoverflow.com/questions/16251273/can-i-watch-for-single-file-change-with-watchservice-not-the-whole-directory
@Slf4j
public class FileListenerManager{

	private WatchService watcher;
	private final ConcurrentMap<Path, Set<FileChangeListener>> dirPathToListenersMap = newConcurrentMap();
	private final ConcurrentMap<WatchKey, Path> watchKeyToDirPathMap = newConcurrentMap();

	@Getter private boolean started;


	public FileListenerManager(FileListener fl){
		Objects.requireNonNull(fl);

		try{
			watcher = FileSystems.getDefault().newWatchService();

			start();
		}
		catch(IOException e){
			log.error(null, e);
		}
	}

	public boolean addFile(File file){
		if(watcher != null){
			try{
				Path path = file.toPath();
				path.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

				WatchKey watckKey = watcher.take();
				List<WatchEvent<?>> events = watckKey.pollEvents();
				for(WatchEvent event : events){
					if(event.kind() == StandardWatchEventKinds.ENTRY_CREATE)
						System.out.println("Created: " + event.context().toString());
					if(event.kind() == StandardWatchEventKinds.ENTRY_DELETE)
						System.out.println("Delete: " + event.context().toString());
					if(event.kind() == StandardWatchEventKinds.ENTRY_MODIFY)
						System.out.println("Modify: " + event.context().toString());
				}

				return true;
			}
			catch(IOException | InterruptedException e){
				log.error(null, e);
			}
		}
		return false;
	}

	/** Starts the service. This method blocks until the service has completely started. */
	public void start(){
		if(isRunning.compareAndSet(false, true)){
			Thread runnerThread = new Thread(this, DirectoryWatchService.class.getSimpleName());
			runnerThread.start();
		}
	}

	/** Stops the service. This method blocks until the service has completely shut down. */
	public void stop(){
		//lill thread lazily
		isRunning.set(false);
	}

	/**
	 * Notifies the implementation of <em>this</em> interface that <code>dirPath</code>
	 * should be monitored for file system events. If the changed file matches any
	 * of the <code>patterns</code>, <code>listener</code> should be notified.
	 *
	 * @param listener	The listener.
	 * @param dirPath		The directory path.
	 * @param patterns	Zero or more file patterns to be matched against file names.
	 *							If none provided, matches <em>any</em> file.
	 * @throws IOException	If <code>dirPath</code> is not a directory.
	 */
	public void register(FileChangeListener listener, String dirPath, String... patterns) throws IOException{
		Path dir = Paths.get(dirPath);
		if(!Files.isDirectory(dir))
			throw new IllegalArgumentException(dirPath + " is not a directory");

		if(!dirPathToListenersMap.containsKey(dir)){
			WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY,
				StandardWatchEventKinds.ENTRY_DELETE);

			watchKeyToDirPathMap.put(key, dir);
		}
		dirPathToListenersMap.computeIfAbsent(dir, key -> newConcurrentSet())
			.add(listener);

		Set<PathMatcher> pp = newConcurrentSet();
		Arrays.stream(patterns)
			.map(FileListenerManager::matcherForExpression)
			.forEach(pp::add);
		if(pp.isEmpty())
			//match everything if no filter is found
			pp.add(matcherForExpression("*"));

		listenerToFilePatternsMap.put(listener, pp);

		log.info("Watching files matching " + Arrays.toString(patterns) + " under " + dirPath + " for changes.");
	}

	private static <T> Set<T> newConcurrentSet(){
		return Collections.newSetFromMap(newConcurrentMap());
	}

	private static <K, V> ConcurrentMap<K, V> newConcurrentMap(){
		return new ConcurrentHashMap<>();
	}

	private static PathMatcher matcherForExpression(String pattern){
		return FileSystems.getDefault()
			.getPathMatcher("glob:" + pattern);
	}

}
