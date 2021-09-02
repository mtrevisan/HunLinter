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

import io.github.mtrevisan.hunlinter.services.eventbus.events.VetoEvent;
import io.github.mtrevisan.hunlinter.services.eventbus.exceptions.VetoException;


/**
 * An EventBus is a simple pattern to promote loose coupling between various components.
 * An event bus can be used in place of a traditional listener or observer pattern, and is useful when ties between
 * multiple components become too complicated to track.
 * <p>
 * A traditional use case for an event bus is a Swing based application, where multiple actions and listeners are
 * configured to capture the various events of the application, such as mouse clicks, window events, data loading
 * events, etc. Swing promotes a one-to-one mapping between listener/listenee components, and as such, it can become
 * difficult to configure the various listeners without tightly coupling all the various components together.
 * <p>
 * With an event bus, events can be published to the bus and any class can be configured to listen for such events.
 * Thus, each individual component only needs to be tightly coupled with the event bus, and it can then receive
 * notifications about events that it cares to know about.
 * <p>
 * The event bus pattern has a simple interface, with a subscribe/publish type model. Any object can be subscribed
 * to the event bus, but to received messages from the bus, the object must have its methods annotated with the
 * {@link EventHandler} annotation. This annotation marks the methods of the subscriber class which should be used
 * to receive event bus events.
 * <p>
 * A published event has the potential to be vetoed and thus not propagated to other non-vetoing subscribers. This
 * is accomplished by setting the {@link EventHandler#canVeto()} property to true and throwing a {@link VetoException}
 * when the method is called from the EventBus. The event bus will note the veto and not relay the message to the
 * subscribers, but will instead send a {@link VetoEvent} out on the bus indicating that the published event has
 * been vetoed.
 *
 * @see <a href="https://github.com/taftster/simpleeventbus">Simple Event Bus</a>
 */
public interface EventBusInterface{

	void start();

	/**
	 * Subscribes the specified subscriber to the event bus.
	 * A subscribed object will be notified of any published events on the methods annotated with the
	 * {@link EventHandler} annotation.
	 * <p>
	 * Each event handler method should take a single parameter indicating the type of event it wishes to receive.
	 * When events are published on the bus, only subscribers who have an EventHandler method with a matching
	 * parameter of the same type as the published event will receive the event notification from the bus.
	 *
	 * @param subscriber	The object to subscribe to the event bus.
	 */
	void subscribe(final Object subscriber);

	/**
	 * Removes the specified object from the event bus subscription list.
	 * Once removed, the specified object will no longer receive events posted to the event bus.
	 *
	 * @param subscriber	The object previous subscribed to the event bus.
	 */
	void unsubscribe(final Object subscriber);

	/**
	 * Sends a message on the bus which will be propagated to the appropriate subscribers of the event type.
	 * Only subscribers which have elected to subscribe to the same event type as the supplied event will be
	 * notified of the event.
	 * <p>
	 * Events can be vetoed, indicating that the event should not propagate to the subscribers that don't
	 * have a veto. The subscriber can veto by setting the {@link EventHandler#canVeto()} return to {@code true}
	 * and by throwing a {@link VetoException}.
	 * <p>
	 * There is no specification given as to how the messages will be delivered, in terms of synchronous or
	 * asynchronous. The only requirement is that all the event handlers that can issue vetos be called before
	 * non-vetoing handlers. Most implementations will likely deliver messages asynchronously.
	 *
	 * @param event	The event to send out to the subscribers of the same type.
	 */
	void publish(final Object event);

	/**
	 * Indicates whether the bus has pending events to publish.
	 * Since message/event delivery can be asynchronous (on other threads), the method can be used to start
	 * or stop certain actions based on all the events having been published.
	 * I.e. perhaps before an application closes, etc.
	 *
	 * @param event	The event to query for.
	 * @return	If events of given type are still being delivered.
	 */
	boolean hasPendingEvents(final Object event);

	/**
	 * Indicates whether the bus has pending events to publish.
	 * Since message/event delivery can be asynchronous (on other threads), the method can be used to start
	 * or stop certain actions based on all the events having been published.
	 * I.e. perhaps before an application closes, etc.
	 *
	 * @return	If events are still being delivered.
	 */
	boolean hasPendingEvents();

}

