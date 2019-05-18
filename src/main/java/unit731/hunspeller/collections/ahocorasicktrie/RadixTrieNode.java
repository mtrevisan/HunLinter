package unit731.hunspeller.collections.ahocorasicktrie;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * A node has the following functions
 * <ul>
 *		<li><b>success</b>: successfully transferred to another state</li>
 *		<li><b>failure</b>: if you cannot jump along the string, jump to a shallow node</li>
 *		<li><b>emits</b>: hit a pattern string</li>
 * </ul>
 * <p>
 * The root node is slightly different.
 * The root node has no failure function. Its "failure" refers to moving to the next state according to the string path.
 * Other nodes have a failure status.
 */
public class RadixTrieNode{

	/** The length of the pattern string is also the depth of this state */
	protected final int depth;

	/** The fail function, if there is no match, jumps to this state. */
	private RadixTrieNode failureId;

	/** Record mode string as long as this state is reachable */
	private Set<Integer> childrenIds;

	/** The goto table, also known as the transfer function. Move to the next state according to the next character of the string */
	private final Map<Character, RadixTrieNode> success = new TreeMap<>();

	/** Corresponding subscript in double array */
	private int id;


	/** Construct a node with a depth of 0 */
	public RadixTrieNode(){
		this(0);
	}

	/**
	 * Construct a node with a depth of depth
	 *
	 * @param depth	The depth of this node
	 */
	public RadixTrieNode(int depth){
		this.depth = depth;
	}

	public int getDepth(){
		return depth;
	}

	/** Add a matching pattern string (this state corresponds to this pattern string) */
	public void addChildrenId(int keyword){
		if(childrenIds == null)
			childrenIds = new HashSet<>();

		childrenIds.add(keyword);
	}

	public Integer getLargestChildrenId(){
		return (childrenIds != null && !childrenIds.isEmpty()? childrenIds.iterator().next(): null);
	}

	/** Add some matching pattern strings */
	public void addChildrenIds(Collection<Integer> childrenIds){
		childrenIds.forEach(this::addChildrenId);
	}

	/** Get the pattern string represented by this node */
	public Collection<Integer> getChildrenIds(){
		return (childrenIds == null? Collections.<Integer>emptyList(): childrenIds);
	}

	/** Whether it is a terminal node */
	public boolean isAcceptable(){
		return (childrenIds != null);
	}

	/** Get the failure node */
	public RadixTrieNode failure(){
		return failureId;
	}

	public void setFailure(RadixTrieNode failure, int[] fail){
		this.failureId = failure;
		fail[id] = failure.id;
	}

	/**
	 * Move to the next state
	 *
	 * @param character 希望按此字符转移
	 * @param ignoreRootState	Whether to ignore the root node, it should be true if the root node calls itself, otherwise it is false
	 * @return	Transfer result
	 */
	private RadixTrieNode nextState(Character character, boolean ignoreRootState){
		RadixTrieNode nextState = success.get(character);
		if(!ignoreRootState && nextState == null && depth == 0)
			nextState = this;
		return nextState;
	}

	/** According to the character transfer, the root node transfer failure will return itself (never return null) */
	public RadixTrieNode nextState(Character character){
		return nextState(character, false);
	}

	/** According to character transfer, any node transfer failure will return null */
	public RadixTrieNode nextStateIgnoreRootState(Character character){
		return nextState(character, true);
	}

	public RadixTrieNode addState(Character character){
		RadixTrieNode nextState = nextStateIgnoreRootState(character);
		if(nextState == null){
			nextState = new RadixTrieNode(depth + 1);
			success.put(character, nextState);
		}
		return nextState;
	}

	public Collection<RadixTrieNode> getStates(){
		return success.values();
	}

	public Collection<Character> getTransitions(){
		return success.keySet();
	}

	@Override
	public boolean equals(Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		RadixTrieNode rhs = (RadixTrieNode)obj;
		return new EqualsBuilder()
			.append(depth, rhs.depth)
			.append(id, rhs.id)
			.append(childrenIds, rhs.childrenIds)
			.append(success, rhs.success)
			.append(failureId, rhs.failureId)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(depth)
			.append(id)
			.append(childrenIds)
			.append(success)
			.append(failureId)
			.toHashCode();
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this)
			.append("depth", depth)
			.append("id", id)
			.append("childrenIds", childrenIds)
			.append("success", success.keySet())
			.append("failureId", (failureId == null? "-1": failureId.id))
			.append("failure", failureId)
			.toString();
	}

	public Map<Character, RadixTrieNode> getSuccess(){
		return success;
	}

	public int getId(){
		return id;
	}

	public void setId(int id){
		this.id = id;
	}

}
