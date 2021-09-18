/**
 * Copyright (c) 2021 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.services.system;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


//https://www.generacodice.com/en/articolo/48172/How-do-I-implement-task-prioritization-using-an-ExecutorService-in-Java-5
//https://stackoverflow.com/questions/3198660/java-executors-how-can-i-set-task-priority
//https://stackoverflow.com/questions/807223/how-do-i-implement-task-prioritization-using-an-executorservice-in-java-5/42831172#42831172
//https://funofprograming.wordpress.com/2016/10/08/priorityexecutorservice-for-java/
//https://dzone.com/articles/performing-tasks-in-parallel-and-with-different-pr
class PriorityExecutors{

	static ExecutorService newFixedThreadPool(final int threadCount){
		return new PriorityExecutor(threadCount, threadCount, 0L, TimeUnit.MILLISECONDS);
	}

	private static class PriorityExecutor extends ThreadPoolExecutor{
		private static final int DEFAULT_PRIORITY = 0;
		private static final AtomicLong instanceCounter = new AtomicLong();

		@SuppressWarnings({"unchecked"})
		PriorityExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit){
			super(corePoolSize, maximumPoolSize, keepAliveTime, unit,
				(BlockingQueue)new PriorityBlockingQueue<>(10, ComparableTask.comparatorByPriorityAndSequentialOrder()));
		}

		@Override
		public final void execute(final Runnable command){
			//if this is ugly then delegator pattern needed
			if(command instanceof ComparableTask)
				super.execute(command);
			else{
				super.execute(newComparableRunnableFor(command));
			}
		}

		private Runnable newComparableRunnableFor(final Runnable runnable){
			return new ComparableRunnable(ensurePriorityRunnable(runnable));
		}

		@Override
		protected final <T> RunnableFuture<T> newTaskFor(final Callable<T> callable){
			return new ComparableFutureTask<>(ensurePriorityCallable(callable));
		}

		@Override
		protected final <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value){
			return new ComparableFutureTask<>(ensurePriorityRunnable(runnable), value);
		}

		private <T> PriorityCallable<T> ensurePriorityCallable(final Callable<T> callable){
			return (callable instanceof PriorityCallable)? (PriorityCallable<T>)callable: PriorityCallable.of(callable, DEFAULT_PRIORITY);
		}

		private PriorityRunnable ensurePriorityRunnable(final Runnable runnable){
			return (runnable instanceof PriorityRunnable)? (PriorityRunnable)runnable: PriorityRunnable.of(runnable, DEFAULT_PRIORITY);
		}

		private class ComparableFutureTask<T> extends FutureTask<T> implements ComparableTask{
			private final Long sequentialOrder = instanceCounter.getAndIncrement();
			private final HasPriority hasPriority;

			ComparableFutureTask(final PriorityCallable<T> priorityCallable){
				super(priorityCallable);

				hasPriority = priorityCallable;
			}

			ComparableFutureTask(final PriorityRunnable priorityRunnable, final T result){
				super(priorityRunnable, result);

				hasPriority = priorityRunnable;
			}

			@Override
			public final long getInstanceCount(){
				return sequentialOrder;
			}

			@Override
			public final int getPriority(){
				return hasPriority.getPriority();
			}
		}

		private static class ComparableRunnable implements ComparableTask{
			private final Long instanceCount = instanceCounter.getAndIncrement();
			private final HasPriority hasPriority;
			private final Runnable runnable;

			ComparableRunnable(final PriorityRunnable priorityRunnable){
				this.runnable = priorityRunnable;
				this.hasPriority = priorityRunnable;
			}

			@Override
			public final void run(){
				runnable.run();
			}

			@Override
			public final int getPriority(){
				return hasPriority.getPriority();
			}

			@Override
			public final long getInstanceCount(){
				return instanceCount;
			}
		}

		private interface ComparableTask extends Runnable{
			int getPriority();

			long getInstanceCount();

			static Comparator<ComparableTask> comparatorByPriorityAndSequentialOrder(){
				return (o1, o2) -> {
					final int priorityResult = Integer.compare(o2.getPriority(), o1.getPriority());
					return (priorityResult != 0? priorityResult: Long.compare(o1.getInstanceCount(), o2.getInstanceCount()));
				};
			}

		}

	}

	private interface HasPriority{
		int getPriority();
	}

	public interface PriorityCallable<V> extends Callable<V>, HasPriority{

		static <V> PriorityCallable<V> of(final Callable<V> callable, final int priority){
			return new PriorityCallable<V>(){
				@Override
				public V call() throws Exception{
					return callable.call();
				}

				@Override
				public int getPriority(){
					return priority;
				}
			};
		}
	}

	public interface PriorityRunnable extends Runnable, HasPriority{

		static PriorityRunnable of(final Runnable runnable, final int priority){
			return new PriorityRunnable(){
				@Override
				public void run(){
					runnable.run();
				}

				@Override
				public int getPriority(){
					return priority;
				}
			};
		}
	}

}
