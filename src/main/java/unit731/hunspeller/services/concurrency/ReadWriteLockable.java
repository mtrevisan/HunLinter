package unit731.hunspeller.services.concurrency;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Supplier;


/** @see <a href="https://www.javaspecialists.eu/talks/jfokus13/PhaserAndStampedLock.pdf">Phaser and StampedLock Concurrency synchronizers</a> */
public class ReadWriteLockable{

	private static final ReadWriteLock READ_WRITE_LOCK = new ReentrantReadWriteLock();

	private static final StampedLock RW_LOCK = new StampedLock();


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

	public static <T> void writeValue(Supplier<T> getter, Consumer<T> setter, T newState){
		T oldState = getter.get();

		long stamp = RW_LOCK.readLock();
		try{
			while(getter.get() == oldState){
				long writeStamp = RW_LOCK.tryConvertToWriteLock(stamp);
				if(writeStamp != 0l){
					stamp = writeStamp;

					setter.accept(oldState);
					break;
				}

				RW_LOCK.unlockRead(stamp);
				stamp = RW_LOCK.writeLock();
			}
		}
		finally{
			RW_LOCK.unlock(stamp);
		}
	}

	public static <T> T readValue(Supplier<T> getter){
		long stamp = RW_LOCK.tryOptimisticRead();
		T currState = getter.get();
		if(!RW_LOCK.validate(stamp)){
			stamp = RW_LOCK.readLock();
			try{
				currState = getter.get();
			}
			finally{
				RW_LOCK.unlockRead(stamp);
			}
		}
		return currState;
	}

}
