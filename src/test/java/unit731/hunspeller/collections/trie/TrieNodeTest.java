package unit731.hunspeller.collections.trie;

import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.collections.trie.sequencers.StringTrieSequencer;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencer;


public class TrieNodeTest{

	@Test
	public void removeChildWithoutChildren(){
		TrieSequencer<String> sequencer = new StringTrieSequencer();
		String sequence = "abcd";
		TrieNode<String, String> node = new TrieNode<>(sequence, 0, 3, null);

		TrieNode<String, String> removedNode = node.removeChild(sequencer.hashOf(sequence, 0), sequencer);

		Assert.assertNull(removedNode);
		Assert.assertFalse(node.hasChildren());
	}

	@Test
	public void removeChildWithOneChild(){
		TrieSequencer<String> sequencer = new StringTrieSequencer();
		String sequence = "abc";
		TrieNode<String, String> node = new TrieNode<>(sequence, 0, 3, null);
		TrieNode<String, String> node1 = new TrieNode<>("abcd", 3, 3, null);
		node.addChild('d', node1);

		TrieNode<String, String> removedNode = node.removeChild('a', sequencer);

		Assert.assertNull(removedNode);
		Assert.assertNotNull(node.getChild('d', sequencer));
	}

	@Test
	public void nodeSplit(){
		//create: abc, abc-d, and abc-e
		TrieSequencer<String> sequencer = new StringTrieSequencer();
		TrieNode<String, String> node0 = new TrieNode<>("abc", 0, 3, null);
		TrieNode<String, String> node1 = new TrieNode<>("abcd", 3, 3, null);
		TrieNode<String, String> node2 = new TrieNode<>("abce", 3, 3, null);
		node0.addChild((int)'d', node1);
		node0.addChild((int)'e', node2);

		//split on ab-c
		node0.split(2, null, sequencer);

		//verify ab-c-d, and ab-c-e
		Assert.assertEquals(2, node0.getEndIndex());
		TrieNode<String, String> child1 = node0.getChild((int)'c', sequencer);
		Assert.assertNotNull(child1);
		TrieNode<String, String> child2 = child1.getChild((int)'d', sequencer);
		Assert.assertNotNull(child2);
		TrieNode<String, String> child3 = child1.getChild((int)'e', sequencer);
		Assert.assertNotNull(child3);
	}

}
