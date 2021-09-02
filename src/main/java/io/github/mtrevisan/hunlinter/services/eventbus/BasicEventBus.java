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
package io.github.mtrevisan.hunlinter.services.eventbus;

import io.github.mtrevisan.hunlinter.services.eventbus.events.BusExceptionEvent;
import io.github.mtrevisan.hunlinter.services.eventbus.events.VetoEvent;
import io.github.mtrevisan.hunlinter.services.eventbus.exceptions.VetoException;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;


/**
 * A simple Event Bus implementation which receives events or messages from various sources and distributes
 * them to all subscribers of the event type.
 * This is highly useful for programs which are event driven. Swing applications in particular can benefit
 * from an event bus architecture, as opposed to the traditional event listener architecture it employs.
 * <p>
 * The BasicEventBus class is thread safe and uses a background thread to notify the subscribers of the event.
 * The subscribers are notified in a serial fashion, and only one event will be published at a time. Though,
 * the {@link #publish(Object)} method is done in a non-blocking way.
 * <p>
 * Subscribers subscribe to the EventBus using the {@link #subscribe(Object)} method. A specific subscriber
 * type is not required, but the subscriber will be reflected to find all methods annotated with the
 * {@link EventHandler} annotations. These methods will be invoked as needed by the event bus based on the
 * type of the first parameter to the annotated method.
 * <p>
 * An event handler can indicate that it can veto events by setting the {@link EventHandler#canVeto()}
 * value to {@code true}. This will inform the EventBus of the subscriber's desire to veto the event. A
 * vetoed event will not be sent to the regular subscribers.
 * <p>
 * During publication of an event, all veto EventHandler methods will be notified first and allowed to
 * throw a {@link VetoException} indicating that the event has been vetoed and should not be published to
 * the remaining event handlers. If no vetoes have been made, the regular subscriber handlers will be
 * notified of the event.
 * <p>
 * Subscribers are stored using a {@link WeakReference} such that a memory leak can be avoided if the
 * client fails to unsubscribe at the end of the use. However, calling the {@link #unsubscribe(Object)}
 * method is highly recommended none-the-less.
 *
 * @see <a href="https://github.com/taftster/simpleeventbus">Simple Event Bus</a>
 */
public class BasicEventBus implements EventBusInterface{

	private final Collection<HandlerInfo> handlers = new CopyOnWriteArrayList<>();
	private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
	private final BlockingQueue<HandlerInfo> killQueue = new LinkedBlockingQueue<>();

	/** The ExecutorService used to handle event delivery to the event handlers. */
	private final ExecutorService executorService;

	/**
	 * Should the event bus wait for the regular handlers to finish processing the event messages before
	 * continuing to the next event. Defaults to {@code false} which is sensible for most use cases.
	 */
	private final boolean waitForHandlers;


	/**
	 * Default constructor sets up the executorService property to use the
	 * {@link Executors#newCachedThreadPool()} implementation. The configured ExecutorService will have
	 * a custom ThreadFactory such that the threads returned will be daemon threads (and thus not block
	 * the application from shutting down).
	 */
	public BasicEventBus(){
		this(false);
	}

	/**
	 * Default constructor sets up the executorService property to use the
	 * {@link Executors#newCachedThreadPool()} implementation. The configured ExecutorService will have
	 * a custom ThreadFactory such that the threads returned will be daemon threads (and thus not block
	 * the application from shutting down).
	 *
	 * @param waitForHandlers	Should the event bus wait for the regular handlers to finish processing the event messages before
	 * 	continuing to the next event.
	 */
	public BasicEventBus(final boolean waitForHandlers){
		this(Executors.newCachedThreadPool(new ThreadFactory(){

			private final ThreadFactory delegate = Executors.defaultThreadFactory();

			@Override
			public Thread newThread(final Runnable r){
				final Thread t = delegate.newThread(r);
				t.setDaemon(true);
				return t;
			}
		}), waitForHandlers);
	}

	public BasicEventBus(final ExecutorService executorService, final boolean waitForHandlers){
		//start the background daemon consumer thread
		final Thread eventQueueThread = new Thread(new EventQueueRunner(), "EventQueue Consumer Thread");
		eventQueueThread.setDaemon(true);
		eventQueueThread.start();

		final Thread killQueueThread = new Thread(new KillQueueRunner(), "KillQueue Consumer Thread");
		killQueueThread.setDaemon(true);
		killQueueThread.start();

		this.executorService = executorService;
		this.waitForHandlers = waitForHandlers;
	}


	/**
	 * Subscribe the specified instance as a potential event subscriber.
	 * The subscriber must annotate a method with the {@link EventHandler} annotation if it expects to
	 * receive notifications.
	 * <p>
	 * Note that the EventBus maintains a {@link WeakReference} to the subscriber, but it is still advised
	 * to call the {@link #unsubscribe(Object)} method if the subscriber does not wish to receive events
	 * any longer.
	 *
	 * @param subscriber	The subscriber object which will receive notifications on {@link EventHandler}
	 * 	annotated methods.
	 */
	public void subscribe(final Object subscriber){
		//lookup to see if we have any subscriber instances already
		final boolean subscribedAlready = loadHandlerIntoQueue(subscriber);
		if(!subscribedAlready)
			subscribeAnnotatedMethods(subscriber);
	}

	private boolean loadHandlerIntoQueue(final Object subscriber){
		boolean subscribedAlready = false;
		for(final HandlerInfo info : handlers){
			final Object otherSubscriber = info.getSubscriber();
			if(otherSubscriber == null){
				try{
					killQueue.put(info);
				}
				catch(final InterruptedException e){
					e.printStackTrace();
				}

				continue;
			}
			if(subscriber == otherSubscriber)
				subscribedAlready = true;
		}
		return subscribedAlready;
	}

	private void subscribeAnnotatedMethods(final Object subscriber){
		final Method[] methods = subscriber.getClass().getDeclaredMethods();
		for(final Method method : methods){
			//look for the EventHandler annotation on the method, if it exists
			//if it doesn't exist, this returns null, and go to the next method
			final EventHandler eh = method.getAnnotation(EventHandler.class);
			if(eh == null)
				continue;

			//evaluate the parameters of the method (only a single parameter of the Object type is
			//allowed for the handler method)
			final Class<?>[] parameters = method.getParameterTypes();
			if(parameters.length != 1)
				throw new IllegalArgumentException("EventHandler methods must specify a single Object parameter.");

			//add the subscriber to the list
			final HandlerInfo info = new HandlerInfo(parameters[0], method, subscriber, eh.canVeto());
			handlers.add(info);
		}
	}


	/***
	 * Unsubscribe the specified subscriber from receiving future published events.
	 *
	 * @param subscriber	The object to unsubscribe from future events.
	 */
	public void unsubscribe(final Object subscriber){
		//remove handler from queue
		final Collection<HandlerInfo> killList = new ArrayList<>(handlers.size());
		for(final HandlerInfo info : handlers){
			final Object obj = info.getSubscriber();
			if(obj == null || obj == subscriber)
				killList.add(info);
		}
		handlers.removeAll(killList);
	}


	/**
	 * Publish the specified event to the event bus.
	 * Based on the type of the event, the EventBus will publish the event to the subscribing objects.
	 *
	 * @param event	The event to publish on the event bus.
	 */
	public void publish(final Object event){
		try{
			queue.put(event);
		}
		catch(final InterruptedException e){
			e.printStackTrace();

			throw new RuntimeException(e);
		}
	}


	/**
	 * Returns if the event bus has pending events.
	 *
	 * @param event	The event to query for.
	 * @return	If the event bus has pending events of given type to publish.
	 */
	public boolean hasPendingEvents(final Object event){
		return queue.contains(event);
	}

	/**
	 * Returns if the event bus has pending events.
	 *
	 * @return	Returns true if the event bus has pending events to publish.
	 */
	public boolean hasPendingEvents(){
		return !queue.isEmpty();
	}


	//the background thread consumer, simply extracts any events from the queue and publishes them
	private class EventQueueRunner implements Runnable{
		@SuppressWarnings("InfiniteLoopStatement")
		@Override
		public void run(){
			try{
				while(true)
					notifySubscribers(queue.take());
			}
			catch(final InterruptedException e){
				e.printStackTrace();

				throw new RuntimeException(e);
			}
		}

		//called on the background thread
		private void notifySubscribers(final Object evt){
			//roll through the subscribers
			final Collection<HandlerInfoCallable> vetoHandlers = new ArrayList<>(handlers.size());
			final Collection<HandlerInfoCallable> regularHandlers = new ArrayList<>(handlers.size());
			subdivideHandlers(evt, vetoHandlers, regularHandlers);

			final boolean vetoCalled = dispatchToVetoableHandlers(evt, vetoHandlers);

			if(!vetoCalled)
				dispatchToRegularHandlers(regularHandlers);
		}

		private void subdivideHandlers(final Object evt, final Collection<HandlerInfoCallable> vetoHandlers,
				final Collection<HandlerInfoCallable> regularHandlers){
			for(final HandlerInfo info : handlers){
				if(!info.matchesEvent(evt))
					continue;

				final HandlerInfoCallable hc = new HandlerInfoCallable(info, evt);
				if(info.isVetoHandler())
					vetoHandlers.add(hc);
				else
					regularHandlers.add(hc);
			}
		}

		private boolean dispatchToVetoableHandlers(final Object evt, final Collection<HandlerInfoCallable> vetoHandlers){
			//used to keep track if a veto was called (if so, the regular list won't be processed)
			boolean vetoCalled = false;

			//submit the veto calls to the executor service
			try{
				for(final Future<Boolean> f : executorService.invokeAll(vetoHandlers))
					if(f.get())
						vetoCalled = true;
			}
			catch(final Exception e){
				//this only happens if the executorService is interrupted, and by default, that shouldn't
				//really ever happen; or, if the callable sneaks out an exception, which again shouldn't happen
				vetoCalled = true;

				e.printStackTrace();
			}

			//VetoEvents cannot be vetoed
			if(vetoCalled && evt instanceof VetoEvent)
				vetoCalled = false;

			//simply return if a veto has occurred
			return vetoCalled;
		}

		private void dispatchToRegularHandlers(final Collection<HandlerInfoCallable> regularHandlers){
			//ExecutorService.invokeAll() in dispatchToVetoableHandlers blocks until all the results are computed.
			//For the regular handlers, we need to check if the waitForHandlers property is `true`. Otherwise,
			//(by default) we don't want invokeAll() to block. We don't care about the results, because no vetoes
			//are accounted for here and exceptions really shouldn't be thrown.
			if(waitForHandlers){
				try{
					executorService.invokeAll(regularHandlers);
				}
				catch(final Exception e){
					e.printStackTrace();
				}
			}
			else
				executorService.submit(() -> {
					try{
						executorService.invokeAll(regularHandlers);
					}
					catch(final Exception e){
						e.printStackTrace();
					}
				});
		}
	}

	//consumer runnable to remove handler infos from the subscription list if they are null (this is
	//if the GC has collected them)
	private class KillQueueRunner implements Runnable{
		@SuppressWarnings("InfiniteLoopStatement")
		@Override
		public void run(){
			try{
				while(true){
					final HandlerInfo info = killQueue.take();
					if(info.getSubscriber() == null)
						handlers.remove(info);
				}
			}
			catch(final InterruptedException e){
				e.printStackTrace();

				throw new RuntimeException(e);
			}
		}
	}


	//callable used to actually invoke the task
	//it eats any exception thrown and publishes an event back onto the bus
	private final class HandlerInfoCallable implements Callable<Boolean>{

		private final HandlerInfo handlerInfo;
		private final Object event;


		private HandlerInfoCallable(final HandlerInfo handlerInfo, final Object event){
			this.handlerInfo = handlerInfo;
			this.event = event;
		}

		/**
		 * Invokes the HandlerInfo's callback handler method.
		 * If any exceptions are thrown, besides a VetoException, a {@link BusExceptionEvent} will be published
		 * to the bus with the root cause of the problem. If a {@link VetoException} is thrown from the invoked
		 * method, a {@link VetoEvent} will be published to the bus and the call will return {@code true}.
		 * <p>
		 * The call has been modified to not throw any Exceptions. It will not, unlike the interface definition,
		 * throw an exception. All exceptions are handled locally.
		 *
		 * @return	If the event was vetoed.
		 */
		@Override
		public Boolean call(){
			try{
				final Object subscriber = handlerInfo.getSubscriber();
				if(subscriber == null){
					killQueue.put(handlerInfo);
					return Boolean.FALSE;
				}

				final Method m = handlerInfo.getMethod();
				m.setAccessible(true);
				m.invoke(subscriber, event);
				return Boolean.FALSE;
			}
			catch(final Exception e){
				Throwable cause = e;

				//find the root cause
				while(cause.getCause() != null)
					cause = cause.getCause();
				if(cause instanceof VetoException){
					publish(new VetoEvent(event));
					return Boolean.TRUE;
				}

				publish(new BusExceptionEvent(handlerInfo, cause));
				return Boolean.FALSE;
			}
		}

	}

}
