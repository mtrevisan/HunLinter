package unit731.hunlinter.services.fsa.builders;


/**
 * State visitor.
 *
 * @see FSA#visitInPostOrder(StateVisitor)
 * @see FSA#visitInPreOrder(StateVisitor)
 */
public interface StateVisitor{

	boolean accept(int state);

}
