package unit731.hunlinter.services.eventbus.events;

import unit731.hunlinter.services.eventbus.EventHandler;
import unit731.hunlinter.services.eventbus.exceptions.VetoException;

import java.util.EventObject;


/**
 * A VetoEvent is sent out of the event bus when a veto has been made by the subscriber.
 * The subscriber will have indicated a veto by throwing a {@link VetoException} in the {@link EventHandler} annotated method.
 *
 * @see <a href="https://github.com/taftster/simpleeventbus">Simple Event Bus</a>
 */
public class VetoEvent extends EventObject{

	public VetoEvent(final Object event){
		super(event);
	}

}
