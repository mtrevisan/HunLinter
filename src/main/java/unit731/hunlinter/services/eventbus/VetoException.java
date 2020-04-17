package unit731.hunlinter.services.eventbus;

/**
 * Thrown by subscribers in their {@link EventHandler} annotated
 * methods to indicate that a "veto" of the event has occured.
 * In order for a veto to be allowed, the subscriber must set the
 * {@link EventHandler#canVeto()} property to true, indicating
 * that the subscriber wishes to veto events of the specified type.
 * <p>
 * The VetoException is simply a marker class that extends {@link RuntimeException}
 * to indicate the veto.
 *
 * @author Adam Taft
 */
public class VetoException extends RuntimeException{

}
