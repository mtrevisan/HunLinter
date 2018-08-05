package unit731.hunspeller.services;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


//https://dzone.com/articles/java-concurrency-read-write-lo
//https://blog.takipi.com/java-8-stampedlocks-vs-readwritelocks-and-synchronized/
//https://www.javaspecialists.eu/talks/jfokus13/PhaserAndStampedLock.pdf
//https://www.javaspecialists.eu/archive/Issue165.html
public class ReadWriteLockable{

	private static final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();


	public void acquireReadLock(){
		READ_WRITE_LOCK.readLock().lock();
	}

	public void releaseReadLock(){
		READ_WRITE_LOCK.readLock().unlock();
	}

	protected void acquireWriteLock(){
		READ_WRITE_LOCK.writeLock().lock();
	}

	protected void releaseWriteLock(){
		READ_WRITE_LOCK.writeLock().unlock();
	}
	
}
