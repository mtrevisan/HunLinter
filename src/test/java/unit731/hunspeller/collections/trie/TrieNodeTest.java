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
		TrieNode<String> node = new TrieNode<>(sequence, 0, 3, null);

		TrieNode<String> removedNode = node.removeChild(sequencer.hashOf(sequence, 0));

		Assert.assertNull(removedNode);
		Assert.assertFalse(node.hasChildren());
	}

	@Test
	public void removeChildWithOneChild(){
		String sequence = "abc";
		TrieNode<String> node = new TrieNode<>(sequence, 0, 3, null);
		TrieNode<String> node1 = new TrieNode<>("abcd", 3, 3, null);
		node.addChild('d', node1);

		TrieNode<String> removedNode = node.removeChild('a');

		Assert.assertNull(removedNode);
		Assert.assertNotNull(node.getChild('d'));
	}

	@Test
	public void nodeSplit(){
		//create: abc, abc-d, and abc-e
		TrieSequencer<String> sequencer = new StringTrieSequencer();
		TrieNode<String> node0 = new TrieNode<>("abc", 0, 3, null);
		TrieNode<String> node1 = new TrieNode<>("abcd", 3, 3, null);
		TrieNode<String> node2 = new TrieNode<>("abce", 3, 3, null);
		node0.addChild('d', node1);
		node0.addChild('e', node2);

		//split on ab-c
		node0.split(2, null, sequencer);

		//verify ab-c-d, and ab-c-e
		Assert.assertEquals(2, node0.getEndIndex());
		TrieNode<String> child1 = node0.getChild('c');
		Assert.assertNotNull(child1);
		TrieNode<String> child2 = child1.getChild('d');
		Assert.assertNotNull(child2);
		TrieNode<String> child3 = child1.getChild('e');
		Assert.assertNotNull(child3);
	}

}
