package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import unit731.hunspeller.collections.radixtree.dtos.SearchResult;
import unit731.hunspeller.collections.radixtree.exceptions.DuplicateKeyException;
import unit731.hunspeller.collections.radixtree.sequencers.SequencerInterface;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeTraverser;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeNode;


/**
 * An Aho-Corasick radix tree.
 * If {@link #prepare() prepare} is called, an implementation of the Aho-Corasick string matching algorithm (described in the paper
 * "Efficient String Matching: An Aid to Bibliographic Search", written by Alfred Vaino Aho and Margaret J. Corasick, Bell Laboratories, 1975)
 * is created, allowing for fast dictionary matching.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Aho%E2%80%93Corasick_algorithm">Aho-Corasick algorithm</a>
 * @see <a href="http://www.cs.uku.fi/~kilpelai/BSA05/lectures/slides04.pdf">Biosequence Algorithms, Spring 2005 - Lecture 4: Set Matching and Aho-Corasick Algorithm</a>
 * @see <a href="http://informatika.stei.itb.ac.id/~rinaldi.munir/Stmik/2014-2015/Makalah2015/Makalah_IF221_Strategi_Algoritma_2015_032.pdf">Aho-Corasick Algorithm in Pattern Matching</a>
 * @see <a href="https://github.com/hankcs/AhoCorasickDoubleArrayTrie">Aho-Corasick double-array trie</a>
 * @see <a href="https://github.com/robert-bor/aho-corasick">Aho-Corasick</a>
 *
 * @param <S>	The sequence/key type
 * @param <V>	The type of values stored in the tree
 */
public class AhoCorasickTree<S, V extends Serializable> extends RadixTree<S, V>{

	private final RadixTreeTraverser<S, V> prepareTraverser = new RadixTreeTraverser<S, V>(){
		@Override
		public void traverse(S wholeKey, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
			if(parent == root)
				return;

			S currentKey = node.getKey();
			int keySize = sequencer.length(currentKey);
			for(int i = 0; i < keySize; i ++){
				S subkey = sequencer.subSequence(currentKey, i);

				RadixTreeNode<S, V> state = findDeepestNode(subkey, parent);

				S stateKey = state.getKey();
				int lcpLength = longestCommonPrefixLength(subkey, stateKey);
				if(lcpLength > 0){
					int nodeKeyLength = sequencer.length(stateKey);
					if(lcpLength < nodeKeyLength)
						//split fail
						state.split(lcpLength, sequencer);
					if(lcpLength + i < keySize)
						//split node
						node.split(lcpLength + i, sequencer);

					//link fail to node
					node.setFailNode(state);

					//out(u) += out(f(u))
					node.addAdditionalValues(state);

					break;
				}
			}

			if(node.getFailNode() == null)
				node.setFailNode(root);
		}

		/** Find the deepest node labeled by a proper suffix of the current child */
		private RadixTreeNode<S, V> findDeepestNode(S subkey, RadixTreeNode<S, V> parent){
			RadixTreeNode<S, V> state;
			RadixTreeNode<S, V> fail = parent.getFailNode();
			while((state = transit(fail, subkey)) == null)
				fail = fail.getFailNode();
			return state;
		}

		private RadixTreeNode<S, V> transit(RadixTreeNode<S, V> node, S prefix){
			RadixTreeNode<S, V> result = root;
			Collection<RadixTreeNode<S, V>> children = node.getChildren();
			if(children != null){
				Iterator<RadixTreeNode<S, V>> itr = children.iterator();
				while(itr.hasNext()){
					RadixTreeNode<S, V> child = itr.next();
					int lcpLength = longestCommonPrefixLength(child.getKey(), prefix);
					if(lcpLength > 0){
						result = child;
						break;
					}
				}
			}
			return result;
		}
	};


	private boolean prepared;


	public static <K, T extends Serializable> RadixTree<K, T> createTree(SequencerInterface<K> sequencer){
		return new AhoCorasickTree<>(sequencer, false);
	}

	public static <K, T extends Serializable> RadixTree<K, T> createTreeNoDuplicates(SequencerInterface<K> sequencer){
		return new AhoCorasickTree<>(sequencer, true);
	}

	private AhoCorasickTree(SequencerInterface<S> sequencer, boolean noDuplicatesAllowed){
		super(sequencer, noDuplicatesAllowed);
	}

	/** Initializes the fail transitions of all nodes (except for the root). */
	@Override
	public void prepare(){
		//process children of the root
		root.forEachChildren(child -> child.setFailNode(root));

		traverseBFS(prepareTraverser);

		prepared = true;
	}

	public void clearFailTransitions(){
		RadixTreeTraverser<S, V> traverser = (wholeKey, node, parent) -> {
			node.setFailNode(null);
			node.clearAdditionalValues();
		};
		traverseBFS(traverser);

		prepared = false;
	}

	/**
	 * Perform a search and return all the entries that are contained into the given text.
	 * 
	 * @param text	The text to search into
	 * @return	The iterator of all the entries found inside the given text
	 * @throws NullPointerException	If the given text is <code>null</code>
	 */
	@Override
	public Iterator<SearchResult<S, V>> searchPrefixedBy(S text){
		Objects.requireNonNull(text);

		if(!prepared)
			throw new IllegalStateException("Cannot perform search until prepare() is called");

		Iterator<SearchResult<S, V>> itr = new Iterator<SearchResult<S, V>>(){

			private RadixTreeNode<S, V> lastMatchedNode = root;
			private int currentIndex = 0;


			@Override
			public boolean hasNext(){
				try{
					RadixTreeNode<S, V> node = lastMatchedNode;
					for(int i = currentIndex; i < sequencer.length(text); i ++){
						node = node.getNextNode(text, i, sequencer);
						if(node == null)
							node = root;
						else if(node.hasValue())
							return true;
						else
							i += sequencer.length(node.getKey()) - 1;
					}
				}
				catch(NoSuchElementException e){}
				return false;
			}

			@Override
			public SearchResult<S, V> next(){
				for(int i = currentIndex; i < sequencer.length(text); i ++){
					lastMatchedNode = lastMatchedNode.getNextNode(text, i, sequencer);
					if(lastMatchedNode == null)
						lastMatchedNode = root;
					else if(lastMatchedNode.hasValue()){
						currentIndex = i + 1;
						return new SearchResult<>(lastMatchedNode, i);
					}
					else
						i += sequencer.length(lastMatchedNode.getKey()) - 1;
				}

				throw new NoSuchElementException();
			}

			@Override
			public void remove(){
				throw new UnsupportedOperationException();
			}
		};
		return itr;
	}

	@Override
	public V put(S key, V value){
		if(prepared)
			clearFailTransitions();

		return super.put(key, value);
	}

	@Override
	public V remove(S key){
		if(prepared)
			clearFailTransitions();

		return super.remove(key);
	}

}
