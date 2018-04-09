package unit731.hunspeller.collections.trie;

import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.collections.trie.sequencers.StringTrieSequencer;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencerInterface;


public class TrieNodeTest{

	@Test
	public void removeChildWithoutChildren(){
		TrieSequencerInterface<String, Integer> sequencer = new StringTrieSequencer();
		String sequence = "abcd";
		TrieNode<String, Integer, String> node = new TrieNode<>(sequence, 0, 3, null);

		TrieNode<String, Integer, String> removedNode = node.removeChild(sequencer.hashOf(sequence, 0), sequencer);

		Assert.assertNull(removedNode);
		Assert.assertFalse(node.hasChildren());
	}

	@Test
	public void removeChildWithOneChild(){
		TrieSequencerInterface<String, Integer> sequencer = new StringTrieSequencer();
		String sequence = "abc";
		TrieNode<String, Integer, String> node = new TrieNode<>(sequence, 0, 3, null);
		TrieNode<String, Integer, String> node1 = new TrieNode<>("abcd", 3, 3, null);
		node.addChild((int)'d', node1);

		TrieNode<String, Integer, String> removedNode = node.removeChild((int)'a', sequencer);

		Assert.assertNull(removedNode);
		Assert.assertNotNull(node.getChildForRetrieve((int)'d', sequencer));
	}

	@Test
	public void nodeSplit(){
		//create: abc, abc-d, and abc-e
		TrieSequencerInterface<String, Integer> sequencer = new StringTrieSequencer();
		TrieNode<String, Integer, String> node0 = new TrieNode<>("abc", 0, 3, null);
		TrieNode<String, Integer, String> node1 = new TrieNode<>("abcd", 3, 3, null);
		TrieNode<String, Integer, String> node2 = new TrieNode<>("abce", 3, 3, null);
		node0.addChild((int)'d', node1);
		node0.addChild((int)'e', node2);

		//split on ab-c
		node0.split(2, null, sequencer);

		//verify ab-c-d, and ab-c-e
		Assert.assertEquals(2, node0.getEndIndex());
		TrieNode<String, Integer, String> child1 = node0.getChildForRetrieve((int)'c', sequencer);
		Assert.assertNotNull(child1);
		TrieNode<String, Integer, String> child2 = child1.getChildForRetrieve((int)'d', sequencer);
		Assert.assertNotNull(child2);
		TrieNode<String, Integer, String> child3 = child1.getChildForRetrieve((int)'e', sequencer);
		Assert.assertNotNull(child3);
	}

}
