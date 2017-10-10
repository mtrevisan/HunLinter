package unit731.hunspeller.collections.trie;

import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencer;


/**
 * An implementation of a Tree.
 * 
 * @param <S>	The sequence type.
 * @param <H>	The hash type (used to find a particular child).
 * @param <V>	The value type.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Tree<S, H, V> extends Trie<S, H, V>{

	private final TrieNode<S, H, V> root = TrieNode.makeRoot();


	public Tree(TrieSequencer<S, H> sequencer){
		super(sequencer);
	}

	/**
	 * Searches in the Trie based on the sequence query.
	 *
	 * @param sequence	The query sequence.
	 * @param matchType	The matching logic.
	 * @param callback	The callback to be executed on match found.
	 * @return	The node that best matched the query based on the logic.
	 */
	@Override
	protected TrieNode<S, H, V> searchAndApply(S sequence, TrieMatch matchType, BiConsumer<TrieNode<S, H, V>, H> callback){
		Objects.requireNonNull(sequence);

		int sequenceLength = sequencer.lengthOf(sequence);
		int sequenceOffset = root.getEndIndex();
		//if the sequence is empty, return null
		if(sequenceLength == 0 || sequenceLength < sequenceOffset)
			return null;

		H stem = sequencer.hashOf(sequence, sequenceOffset);
		TrieNode<S, H, V> parent = root;
		Set<TrieNode<S, H, V>> nodes = root.getChildrenForRetrieve(stem, sequencer);
		Stack<TrieNode<S, H, V>> stack = new Stack<>();
		stack.addAll(nodes);
		while(!stack.isEmpty()){
			TrieNode<S, H, V> node = stack.pop();

			int nodeLength = node.getEndIndex() - node.getStartIndex();
			int max = Math.min(nodeLength, sequenceLength - sequenceOffset);
			int matches = sequencer.matchesGet(node.getSequence(), node.getStartIndex(), sequence, sequenceOffset, max);

			sequenceOffset += matches;

			if(matches != max || matches == max && max != nodeLength)
				//not found
				node = null;
			else if(sequenceOffset == sequenceLength || !node.hasChildren()){
				if(callback != null && node.isLeaf() && sequencer.startsWith(sequence, node.getSequence()))
					callback.accept(parent, stem);

				break;
			}
			else{
				//call callback for each leaf node found so far
				if(callback != null && node.isLeaf() && sequencer.startsWith(sequence, node.getSequence()))
					callback.accept(parent, stem);

				stem = sequencer.hashOf(sequence, sequenceOffset);
				Set<TrieNode<S, H, V>> next = node.getChildrenForRetrieve(stem, sequencer);

				//if there is no next, node could be a STARTS_WITH match
				if(next == null)
					break;

				parent = node;
				stack.addAll(next);
			}
		}

		//EXACT matches
		if(!stack.isEmpty() && matchType == TrieMatch.EXACT){
			//check length of last node against query
			int endIndex = node.getEndIndex();
			if(!node.isLeaf() || endIndex != sequenceLength)
				return null;

			//check actual sequence values
			S seq = node.getSequence();
			if(sequencer.lengthOf(seq) != sequenceLength || sequencer.matchesGet(seq, 0, sequence, 0, endIndex) != sequenceLength)
				return null;
		}

		return node;
	}

}
