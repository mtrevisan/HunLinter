package unit731.hunlinter.services.eventbus;

import java.util.ServiceLoader;


/**
 * An {@link EventBusInterface} factory that will return a singleton implementation loaded via the Java 6
 * {@link ServiceLoader}. By default, without changes to the jar, a {@link BasicEventBus} implementation
 * will be returned via the factory methods.
 * <p>
 * This static factory also includes the same methods as EventBus which will delegate to the ServiceLoader
 * loaded instance. Thus, the class creates a convenient single location for which client code can be hooked
 * to the configured EventBus.
 *
 * @see <a href="https://github.com/taftster/simpleeventbus">Simple Event Bus</a>
 */
public final class EventBusService{

	private static final EventBusInterface EVENT_BUS = new BasicEventBus();


	public static void subscribe(final Object subscriber){
		EVENT_BUS.subscribe(subscriber);
	}

	public static void unsubscribe(final Object subscriber){
		EVENT_BUS.unsubscribe(subscriber);
	}

	public static void publish(final Object event){
		EVENT_BUS.publish(event);
	}

	public static boolean hasPendingEvents(){
		return EVENT_BUS.hasPendingEvents();
	}

}
