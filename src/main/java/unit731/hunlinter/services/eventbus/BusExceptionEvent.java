package unit731.hunlinter.services.eventbus;

import java.util.EventObject;


/**
 * For any exceptions that occur on the bus during handler
 * execution, this event will be published.
 *
 * @author Adam Taft
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