package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;
import unit731.hunspeller.collections.radixtree.dtos.SearchResult;
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
				Pair<Integer, Integer> lcs = longestCommonSubstring(subkey, stateKey);
				int lcsIndexA = lcs.getLeft();
				if(lcsIndexA >= 0){
					if(lcsIndexA > 0)
						//split current node
						node.split(lcsIndexA, sequencer);
					int lcsIndexB = lcs.getRight();
					if(lcsIndexB > 0)
						//split targeted fail node
						state = state.split(lcsIndexB + i, sequencer);

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

					Pair<Integer, Integer> lcs = longestCommonSubstring(child.getKey(), prefix);
					int lcsIndexA = lcs.getLeft();
					if(lcsIndexA >= 0){
						result = child;
						break;
					}
				}
			}
			return result;
		}

		/**
		 * Finds the length and the index at which starts the Longest Common Substring
		 *
		 * @param keyA	Character sequence A
		 * @param keyB	Character sequence B
		 * @return	The indexes of keyA and keyB of the start of the longest common substring between <code>A</code> and <code>B</code>
		 * @throws IllegalArgumentException	If either <code>A</code> or <code>B</code> is <code>null</code>
		 */
		private Pair<Integer, Integer> longestCommonSubstring(S keyA, S keyB){
			int m = sequencer.length(keyA);
			int n = sequencer.length(keyB);

			if(m < n){
				Pair<Integer, Integer> indexes = longestCommonSubstring(keyB, keyA);
				return Pair.of(indexes.getRight(), indexes.getLeft());
			}

			//matrix to store result of two consecutive rows at a time
			int[][] len = new int[2][n];
			int currRow = 0;

			//for a particular value of i and j, len[currRow][j] stores length of LCS in string X[0..i] and Y[0..j]
			int lcsLength = 0;
			int lcsIndexA = 0;
			int lcsIndexB = 0;
			for(int i = 0; i < m; i ++){
				for(int j = 0; j < n; j ++){
					if(i == 0 || j == 0)
						len[currRow][j] = 0;
					else if(sequencer.equalsAtIndex(keyA, keyB, i - 1, j - 1)){
						len[currRow][j] = len[1 - currRow][j - 1] + 1;
						if(len[currRow][j] > lcsLength){
							lcsLength = len[currRow][j];

							lcsIndexA = i;
							lcsIndexB = j;
						}
					}
					else
						len[currRow][j] = 0;
				}

				//make current row as previous row and previous row as new current row
				currRow = 1 - currRow;
			}
			return Pair.of(lcsIndexA - 1, lcsIndexB - 1);
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
				int idx = -1;
				for(int i = currentIndex; i < sequencer.length(text); i ++){
					lastMatchedNode = lastMatchedNode.getNextNode(text, i, sequencer);
					if(lastMatchedNode == null){
						lastMatchedNode = root;

						idx = -1;
					}
					else{
						if(idx < 0)
							idx = i;
						if(lastMatchedNode.hasValue()){
							currentIndex = i + 1;
							return new SearchResult<>(lastMatchedNode, idx);
						}
						else
							i += sequencer.length(lastMatchedNode.getKey()) - 1;
					}
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
