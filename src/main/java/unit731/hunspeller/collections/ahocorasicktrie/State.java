package unit731.hunspeller.collections.ahocorasicktrie;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;


/**
 * <p>
 * A state has the following functions
 * </p>
 * <ul>
 * <li>success; Successfully transferred to another state</li>
 * <li>failure; If you cannot jump along the string, jump to a shallower node</li>
 * <li>emits; Hit a pattern string</li>
 * </ul>
 * <p>
 * The root node is slightly different. The root node has no failure function. Its "failure" refers to the transition to the next state according to the string path. Other nodes have a failure state.
 * </p>
 *
 * @author Robert Bor
 * @param <S>
 */
public class State<S>{

	/** The length of the pattern string, also the depth of this state */
	@Getter
	protected final int depth;

	/** The fail function jumps to this state if there is no match */
	@Getter
	private State<S> failure;

	/** As long as this state is reachable, record the pattern string */
	private Set<Integer> emits;
	/** The goto table, also known as the transfer function. Moves to the next state based on the next character of the string */
	@Getter
	private final Map<S, State<S>> success = new TreeMap<>();

	/** Corresponding subscripts in a double array */
	@Getter
	@Setter
	private int index;


	/** Construct a node with a depth of 0 */
	public State(){
		this(0);
	}

	/**
	 * Construct a node with a depth of <code>depth</code>
	 *
	 * @param depth	The depth
	 */
	public State(int depth){
		this.depth = depth;
	}

	/**
	 * Add a matching pattern string (this status corresponds to this pattern string)
	 *
	 * @param keyword	The keyword
	 */
	public void addEmit(int keyword){
		emits = ObjectUtils.defaultIfNull(emits, new TreeSet<>(Collections.reverseOrder()));

		emits.add(keyword);
	}

	/**
	 * Get the maximum value
	 *
	 * @return	The value
	 */
	public Integer getLargestValueId(){
		return (emits != null && !emits.isEmpty()? emits.iterator().next(): null);
	}

	/**
	 * Add some matching pattern strings
	 *
	 * @param emits
	 */
	public void addEmit(Collection<Integer> emits){
		emits.forEach(this::addEmit);
	}

	/**
	 * Get the pattern string(s) represented by this node
	 *
	 * @return
	 */
	public Collection<Integer> emit(){
		return (emits == null? Collections.<Integer>emptyList(): emits);
	}

	/**
	 * Is it an end state?
	 *
	 * @return
	 */
	public boolean isAcceptable(){
		return (depth > 0 && emits != null);
	}

	/**
	 * Set the failure state
	 *
	 * @param failState
	 * @param fail
	 */
	public void setFailure(State<S> failState, int fail[]){
		failure = failState;

		fail[index] = failure.index;
	}

	/**
	 * Transfer to the next state
	 *
	 * @param character	Hope to press this character to transfer
	 * @param ignoreRootState	Whether to ignore the root node, if it is the root node to call it should be true, otherwise false
	 * @return Transfer results
	 */
	private State<S> nextState(S character, boolean ignoreRootState){
		State<S> nextState = success.get(character);
		if(!ignoreRootState && nextState == null && depth == 0)
			nextState = this;

		return nextState;
	}

	/**
	 * According to character transfer, the root node fails to return itself (it never returns null)
	 *
	 * @param character
	 * @return
	 */
	public State<S> nextState(S character){
		return nextState(character, false);
	}

	/**
	 * According to character transfer, any node transfer failure will return null
	 *
	 * @param character
	 * @return
	 */
	public State<S> nextStateIgnoreRootState(S character){
		return nextState(character, true);
	}

	public State<S> addState(S character){
		State<S> nextState = nextStateIgnoreRootState(character);
		if(nextState == null){
			nextState = new State<>(depth + 1);
			success.put(character, nextState);
		}
		return nextState;
	}

	public Collection<State<S>> getStates(){
		return success.values();
	}

	public Collection<S> getTransitions(){
		return success.keySet();
	}

}
