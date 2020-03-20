package unit731.hunlinter.services.fsa;


/**
 * State visitor.
 *
 * @see FSA#visitPostOrder(StateVisitor)
 */
public interface StateVisitor{

	boolean accept(int state);

}
