/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.hunlinter.datastructures.ahocorasicktrie;

import io.github.mtrevisan.hunlinter.services.log.ShortPrefixNotNullToStringStyle;
import io.github.mtrevisan.hunlinter.services.system.LoopHelper;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static io.github.mtrevisan.hunlinter.services.system.LoopHelper.forEach;


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
	private final int depth;

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
	public RadixTrieNode(final int depth){
		this.depth = depth;
	}

	public int getDepth(){
		return depth;
	}

	/**
	 * Add a matching pattern string (this node corresponds to this pattern string)
	 *
	 * @param key	Key of this node
	 */
	public void addChildrenId(final int key){
		if(childrenIds == null)
			childrenIds = new HashSet<>();

		childrenIds.add(key);
	}

	public Integer getLargestChildrenId(){
		return (childrenIds != null && !childrenIds.isEmpty()? childrenIds.iterator().next(): null);
	}

	/**
	 * Add some matching pattern strings
	 *
	 * @param childrenIds	Id of the children to add
	 */
	public void addChildrenIds(final Iterable<Integer> childrenIds){
		LoopHelper.forEach(childrenIds, this::addChildrenId);
	}

	/**
	 * Get the pattern string represented by this node
	 *
	 * @return	The children ids
	 */
	public Collection<Integer> getChildrenIds(){
		return (childrenIds == null? Collections.emptyList(): childrenIds);
	}

	/**
	 * Whether it is a terminal node
	 *
	 * @return	Whether this is a leaf node
	 */
	public boolean isAcceptable(){
		return (depth > 0 && childrenIds != null);
	}

	/**
	 * Get the failure node
	 *
	 * @return	The fail node
	 */
	public RadixTrieNode failure(){
		return failure;
	}

	public void setFailure(final RadixTrieNode failure, final int[] fail){
		this.failure = failure;
		fail[id] = failure.id;
	}

	/**
	 * Move to the next node
	 *
	 * @param character	Character to get the next node from
	 * @param ignoreRootNode	Whether to ignore the root node, it should be true if the root node calls itself, otherwise it is false
	 * @return	Transfer result
	 */
	private RadixTrieNode nextNode(final Character character, final boolean ignoreRootNode){
		RadixTrieNode nextNode = success.get(character);
		if(!ignoreRootNode && nextNode == null && depth == 0)
			nextNode = this;
		return nextNode;
	}

	/**
	 * According to the character transfer, the root node transfer failure will return itself (never return null)
	 *
	 * @param character	Character to get the next node from
	 * @return	The next node
	 */
	public RadixTrieNode nextNode(final Character character){
		return nextNode(character, false);
	}

	public RadixTrieNode getFailureNode(final Character transition){
		RadixTrieNode traceFailureNode = failure;
		while(traceFailureNode.nextNode(transition) == null)
			traceFailureNode = traceFailureNode.failure;
		return traceFailureNode.nextNode(transition);
	}

	/**
	 * According to character transfer, any node transfer failure will return null
	 *
	 * @param character	Character to get the next node from
	 * @return	The next node (ignoring root node)
	 */
	public RadixTrieNode nextNodeIgnoreRoot(final Character character){
		return nextNode(character, true);
	}

	public RadixTrieNode addNode(final Character character){
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

	public Map<Character, RadixTrieNode> getSuccess(){
		return success;
	}

	public int getId(){
		return id;
	}

	public void setId(final int id){
		this.id = id;
	}


	@Override
	public String toString(){
		return new ToStringBuilder(this, ShortPrefixNotNullToStringStyle.SHORT_PREFIX_NOT_NULL_STYLE)
			.append("depth", depth)
			.append("id", id)
			.append("childrenIds", childrenIds)
			.append("success", success.keySet())
			.append("failureId", (failure == null? "-1": failure.id))
			.append("failure", failure)
			.toString();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final RadixTrieNode rhs = (RadixTrieNode)obj;
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

}
