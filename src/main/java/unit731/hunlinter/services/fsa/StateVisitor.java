package unit731.hunlinter.services.fsa;


/**
 * State visitor.
 *
 * @see FSA#visitPostOrder(StateVisitor)
 * @see "org.carrot2.morfologik-parent, 2.1.7-SNAPSHOT, 2020-01-02"
 */
public interface StateVisitor{

	boolean accept(final int state);

}
