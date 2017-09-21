package unit731.hunspeller.collections.trie;

import org.junit.Assert;
import org.junit.Test;
import unit731.hunspeller.collections.trie.sequencers.StringTrieSequencer;
import unit731.hunspeller.collections.trie.sequencers.TrieSequencer;


public class TrieNodeTest{

	@Test
	public void nodeSplit(){
		//create: abc-c and abc-d
		TrieSequencer<String> sequencer = new StringTrieSequencer();
		TrieNode<String> node0 = new TrieNode<>("abcc", 0, 3, null);
		TrieNode<String> node1 = new TrieNode<>("abcc", 3, 3, null);
		TrieNode<String> node2 = new TrieNode<>("abcd", 3, 3, null);
		node0.addChild('c', node1);
		node0.addChild('d', node2);

		//split on ab-c
		TrieNode<String> newNode = node0.split(1, null, sequencer);

		//verify ab-c-c and ab-c-d
		Assert.assertEquals(1, node0.getEndIndex());
		TrieNode<String> child1 = node0.getChild('b');
		Assert.assertNotNull(child1);
		TrieNode<String> child2 = child1.getChild('c');
		Assert.assertNotNull(child2);
		TrieNode<String> child3 = child1.getChild('d');
		Assert.assertNotNull(child3);
	}

}
