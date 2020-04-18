package unit731.hunlinter.services.eventbus.events;

import java.util.EventObject;


/**
 * For any exceptions that occur on the bus during handler execution, this event will be published.
 *
 * @see <a href="https://github.com/taftster/simpleeventbus">Simple Event Bus</a>
 */
public class BusExceptionEvent extends EventObject{

	private final Throwable cause;


	public BusExceptionEvent(final Object subscriber, final Throwable cause){
		super(subscriber);

		this.cause = cause;
	}

	public Object getSubscriber(){
		return getSource();
	}

	public Throwable getCause(){
		return cause;
	}

}