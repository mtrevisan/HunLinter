package unit731.hunlinter.services.eventbus.exceptions;

import unit731.hunlinter.services.eventbus.EventHandler;


/**
 * Thrown by subscribers in their {@link EventHandler} annotated methods to indicate that a "veto" of the event has occurred.
 * In order for a veto to be allowed, the subscriber must set the {@link EventHandler#canVeto()} property to {@code true},
 * indicating that the subscriber wishes to veto events of the specified type.
 * <p>
 * The VetoException is simply a marker class that extends {@link RuntimeException} to indicate the veto.
 *
 * @see <a href="https://github.com/taftster/simpleeventbus">Simple Event Bus</a>
 */
public class VetoException extends RuntimeException{

}
