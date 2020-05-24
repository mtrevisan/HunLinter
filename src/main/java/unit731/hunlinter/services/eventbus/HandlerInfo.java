package unit731.hunlinter.services.eventbus;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;


/** Used to hold the subscriber details */
class HandlerInfo{

	private final Class<?> eventClass;
	private final Method method;
	private final WeakReference<?> subscriber;
	private final boolean vetoHandler;


	public HandlerInfo(final Class<?> eventClass, final Method method, final Object subscriber, final boolean vetoHandler){
		this.eventClass = eventClass;
		this.method = method;
		this.subscriber = new WeakReference<>(subscriber);
		this.vetoHandler = vetoHandler;
	}

	public boolean matchesEvent(final Object event){
		return event.getClass().equals(eventClass);
	}

	public Method getMethod(){
		return method;
	}

	public Object getSubscriber(){
		return subscriber.get();
	}

	public boolean isVetoHandler(){
		return vetoHandler;
	}

}
