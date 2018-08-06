package unit731.hunspeller.services.concurrency;

import java.util.HashMap;
import java.util.Map;


/** @see <a href="https://dzone.com/articles/java-concurrency-read-write-lo">Java Concurrency: Read / Write Locks</a> */
public class ReentrantReadWriteLock{

	private final Map<Thread, Integer> readingThreads = new HashMap<>();

	private int writeAccesses = 0;
	private int writeRequests = 0;
	private Thread writingThread = null;


	public synchronized void lockRead() throws InterruptedException{
		Thread callingThread = Thread.currentThread();
		while(!canGrantReadAccess(callingThread))
			wait();

		readingThreads.put(callingThread, (getReadAccessCount(callingThread) + 1));
	}

	private boolean canGrantReadAccess(Thread callingThread){
		return (isWriter(callingThread) || !hasWriter() && (isReader(callingThread) || !hasWriteRequests()));
	}

	public synchronized void unlockRead(){
		Thread callingThread = Thread.currentThread();
		if(!isReader(callingThread))
			throw new IllegalMonitorStateException("Calling Thread does not hold a read lock on this ReadWriteLock");

		int accessCount = getReadAccessCount(callingThread);
		if(accessCount == 1)
			readingThreads.remove(callingThread);
		else
			readingThreads.put(callingThread, (accessCount - 1));
		notifyAll();
	}

	public synchronized void lockWrite() throws InterruptedException{
		writeRequests ++;
		Thread callingThread = Thread.currentThread();
		while(!canGrantWriteAccess(callingThread))
			wait();
		writeRequests --;
		writeAccesses ++;
		writingThread = callingThread;
	}

	public synchronized void unlockWrite() throws InterruptedException{
		if(!isWriter(Thread.currentThread()))
			throw new IllegalMonitorStateException("Calling Thread does not hold the write lock on this ReadWriteLock");

		writeAccesses --;
		if(writeAccesses == 0)
			writingThread = null;

		notifyAll();
	}

	private boolean canGrantWriteAccess(Thread callingThread){
		return (isOnlyReader(callingThread) || !hasReaders() && (writingThread == null || isWriter(callingThread)));
	}

	private int getReadAccessCount(Thread callingThread){
		Integer accessCount = readingThreads.get(callingThread);
		return (accessCount != null? accessCount: 0);
	}

	private boolean hasReaders(){
		return !readingThreads.isEmpty();
	}

	private boolean isReader(Thread callingThread){
		return (readingThreads.get(callingThread) != null);
	}

	private boolean isOnlyReader(Thread callingThread){
		return (readingThreads.size() == 1 && readingThreads.get(callingThread) != null);
	}

	private boolean hasWriter(){
		return (writingThread != null);
	}

	private boolean isWriter(Thread callingThread){
		return (writingThread == callingThread);
	}

	private boolean hasWriteRequests(){
		return (writeRequests > 0);
	}

}
