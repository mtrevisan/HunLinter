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
 *		<li><b>success</b>: successfully transferred to another node</li>
 *		<li><b>failure</b>: if you cannot jump along the string, jump to a shallow node</li>
 *		<li><b>emits</b>: hit a pattern string</li>
 * </ul>
 * <p>
 * The root node is slightly different.
 * The root node has no failure function. Its "failure" refers to moving to the next node according to the string path.
 * Other nodes have a failure status.
 */
public class RadixTrieNode{

	/** The length of the pattern string is also the depth of this node */
	protected final int depth;

	/** The fail function, if there is no match, jumps to this node. */
	private RadixTrieNode failure;

	/** Record mode string as long as this node is reachable */
	private Set<Integer> childrenIds;

	/** The goto table, also known as the transfer function. Move to the next node according to the next character of the string */
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

	/** Add a matching pattern string (this node corresponds to this pattern string) */
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
		return failure;
	}

	public void setFailure(RadixTrieNode failure, int[] fail){
		this.failure = failure;
		fail[id] = failure.id;
	}

	/**
	 * Move to the next node
	 *
	 * @param character 希望按此字符转移
	 * @param ignoreRootNode	Whether to ignore the root node, it should be true if the root node calls itself, otherwise it is false
	 * @return	Transfer result
	 */
	private RadixTrieNode nextNode(Character character, boolean ignoreRootNode){
		RadixTrieNode nextNode = success.get(character);
		if(!ignoreRootNode && nextNode == null && depth == 0)
			nextNode = this;
		return nextNode;
	}

	/** According to the character transfer, the root node transfer failure will return itself (never return null) */
	public RadixTrieNode nextNode(Character character){
		return nextNode(character, false);
	}

	/** According to character transfer, any node transfer failure will return null */
	public RadixTrieNode nextNodeIgnoreRoot(Character character){
		return nextNode(character, true);
	}

	public RadixTrieNode addNode(Character character){
		RadixTrieNode nextNode = nextNodeIgnoreRoot(character);
		if(nextNode == null){
			nextNode = new RadixTrieNode(depth + 1);
			success.put(character, nextNode);
		}
		return nextNode;
	}

	public Collection<RadixTrieNode> getNodes(){
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
			.append(failure, rhs.failure)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(depth)
			.append(id)
			.append(childrenIds)
			.append(success)
			.append(failure)
			.toHashCode();
	}

	@Override
	public String toString(){
		return new ToStringBuilder(this)
			.append("depth", depth)
			.append("id", id)
			.append("childrenIds", childrenIds)
			.append("success", success.keySet())
			.append("failureId", (failure == null? "-1": failure.id))
			.append("failure", failure)
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
