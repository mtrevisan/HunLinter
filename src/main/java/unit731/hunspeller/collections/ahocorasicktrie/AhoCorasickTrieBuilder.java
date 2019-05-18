package unit731.hunspeller.collections.ahocorasicktrie;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;


/**
 * A builder to build the AhoCorasickTrie
 * 
 * @param <V>	The type of values stored in the tree
 */
class AhoCorasickTrieBuilder<V extends Serializable>{

	private final RadixTrieNode rootNode = new RadixTrieNode();

	/** Whether the position has been used */
	private boolean used[];
	/** The allocation size of the dynamic array */
	private int allocSize;
	/** A parameter that controls the memory growth speed of the dynamic array */
	private int memoryGrowthSpeed;
	/** The next position to check for unused memory */
	private int nextCheckPos;
	/** The size of the key-pair sets */
	private int keySize;


	/**
	 * Build a AhoCorasickTrie from a map
	 *
	 * @param map	A map containing key-value pairs
	 */
	@SuppressWarnings("unchecked")
	public AhoCorasickTrie<V> build(Map<String, V> map){
		Objects.requireNonNull(map);

		AhoCorasickTrie<V> trie = new AhoCorasickTrie<>();

		//save the outer values
		int size = map.size();
		trie.outerValue = new ArrayList<>(size);
		trie.outerValue.addAll(map.values());
		trie.keyLength = new int[size];

		//construct a two-point trie tree
		Set<String> keySet = map.keySet();
		addAllKeywords(trie, keySet);

		//vuilding a double array trie tree based on a two-point trie tree
		buildTrie(trie, keySet.size());

		//build the failure table and merge the output table
		constructFailureStates(trie);

		return trie;
	}

	/**
	 * Fetch siblings of a parent node
	 *
	 * @param parent	Parent node
	 * @param siblings	Parent node's child nodes, i.e. the siblings
	 * @return	The amount of the siblings
	 */
	private int fetch(RadixTrieNode parent, List<Map.Entry<Integer, RadixTrieNode>> siblings){
		if(parent.isAcceptable()){
			RadixTrieNode fakeNode = new RadixTrieNode(-parent.getDepth() - 1);
			fakeNode.addChildrenId(parent.getLargestChildrenId());
			siblings.add(new AbstractMap.SimpleEntry<>(0, fakeNode));
		}
		for(Map.Entry<Character, RadixTrieNode> entry : parent.getSuccess().entrySet())
			siblings.add(new AbstractMap.SimpleEntry<>(entry.getKey() + 1, entry.getValue()));
		return siblings.size();
	}

	/**
	 * add a collection of keywords
	 *
	 * @param keywordSet the collection holding keywords
	 */
	private void addAllKeywords(AhoCorasickTrie<V> trie, Collection<String> keywordSet){
		int i = 0;
		for(String keyword : keywordSet)
			addKeyword(trie, keyword, i ++);
	}

	/**
	 * Add a keyword
	 *
	 * @param keyword	A keyword
	 * @param id	The index of the keyword
	 */
	private void addKeyword(AhoCorasickTrie<V> trie, String keyword, int id){
		RadixTrieNode currentState = rootNode;
		for(Character character : keyword.toCharArray())
			currentState = currentState.addState(character);
		currentState.addChildrenId(id);
		trie.keyLength[id] = keyword.length();
	}

	private void constructFailureStates(AhoCorasickTrie<V> trie){
		if(!trie.isEmpty()){
			int size = trie.check.length + 1;
			trie.next = new int[size];
			trie.next[1] = trie.base[0];
			trie.output = new int[size][];
			Queue<RadixTrieNode> queue = new ArrayDeque<>();

			//the first step is to set the failure of the node with depth 1 to the root node
			for(RadixTrieNode depthOneState : rootNode.getStates()){
				depthOneState.setFailure(rootNode, trie.next);
				queue.add(depthOneState);
				constructOutput(trie, depthOneState);
			}

			//the second step is to create a failure table for the node with depth > 1, which is a BFS
			while(!queue.isEmpty()){
				RadixTrieNode currentState = queue.remove();

				for(Character transition : currentState.getTransitions()){
					RadixTrieNode targetState = currentState.nextState(transition);
					queue.add(targetState);

					RadixTrieNode traceFailureState = currentState.failure();
					while(traceFailureState.nextState(transition) == null)
						traceFailureState = traceFailureState.failure();
					RadixTrieNode newFailureState = traceFailureState.nextState(transition);
					targetState.setFailure(newFailureState, trie.next);
					targetState.addChildrenIds(newFailureState.getChildrenIds());
					constructOutput(trie, targetState);
				}
			}
		}
	}

	private void constructOutput(AhoCorasickTrie<V> trie, RadixTrieNode targetState){
		Collection<Integer> childrenIds = targetState.getChildrenIds();
		if(childrenIds == null || childrenIds.isEmpty())
			return;

		int output[] = new int[childrenIds.size()];
		Iterator<Integer> it = childrenIds.iterator();
		for(int i = 0; i < output.length; i ++)
			output[i] = it.next();
		trie.output[targetState.getId()] = output;
	}

	private void buildTrie(AhoCorasickTrie<V> trie, int keySize){
		memoryGrowthSpeed = 0;
		this.keySize = keySize;

		int totalKeysLen = 0;
		for(int i = 0; i < trie.keyLength.length; i ++)
			totalKeysLen += trie.keyLength[i];
		int newSize = 65_536 + totalKeysLen * 2 + 1;
		trie.check = new int[newSize];
		trie.base = new int[newSize];
		used = new boolean[newSize];
		allocSize = newSize;

		trie.base[0] = 1;
		nextCheckPos = 0;

		List<Map.Entry<Integer, RadixTrieNode>> siblings = new ArrayList<>(rootNode.getSuccess().entrySet().size());
		fetch(rootNode, siblings);
		insert(trie, siblings);
	}

	/** Allocate the memory of the dynamic array */
	private void resize(AhoCorasickTrie<V> trie, int newSize){
		trie.base = Arrays.copyOf(trie.base, newSize);
		trie.check = Arrays.copyOf(trie.check, newSize);
		used = Arrays.copyOf(used, newSize);
		allocSize = newSize;
	}

	/**
	 * Insert the siblings into the double-array trie
	 *
	 * @param siblings	The siblings being inserted
	 * @return	The position to insert them
	 */
	private int insert(AhoCorasickTrie<V> trie, List<Map.Entry<Integer, RadixTrieNode>> siblings){
		int begin = 0;
		if(!siblings.isEmpty()){
			int pos = Math.max(siblings.get(0).getKey() + 1, nextCheckPos) - 1;
			int nonZeroNum = 0;
			int first = 0;

			if(allocSize <= pos)
				resize(trie, pos + 1);

			outer:
			//the goal of this loop body is to find n free spaces that satisfy base[begin + a1...an] == 0, a1...an are n nodes in siblings
			while(true){
				pos ++;

				if(allocSize <= pos)
					resize(trie, pos + 1);

				if(trie.check[pos] != 0){
					nonZeroNum ++;
					continue;
				}
				else if(first == 0){
					nextCheckPos = pos;
					first = 1;
				}

				//the distance of the current position from the first sibling node
				begin = pos - siblings.get(0).getKey();
				if(allocSize <= (begin + siblings.get(siblings.size() - 1).getKey())){
					//prevent progress from generating zero divide errors
					double l = (1.05 > (double)keySize / (memoryGrowthSpeed + 1)? 1.05: (double)keySize / (memoryGrowthSpeed + 1));
					resize(trie, (int)(allocSize * l));
				}

				if(used[begin])
					continue;

				for(int i = 1; i < siblings.size(); i ++)
					if(trie.check[begin + siblings.get(i).getKey()] != 0)
						continue outer;

				break;
			}

			// -- Simple heuristics --
			//if the percentage of non-empty contents in check between the index `nextCheckPos` and `check` is greater than
			//some constant value (e.g. 0.9), new `next_check_pos` index is written by `check`
			if((double)nonZeroNum / (pos - nextCheckPos + 1) >= 0.95)
				//from the position `next_check_pos` to `pos`, if the occupied space is above 95%, the next time you insert the node,
				//you can start directly from the `pos` position
				nextCheckPos = pos;
			used[begin] = true;

			for(Map.Entry<Integer, RadixTrieNode> sibling : siblings)
				trie.check[begin + sibling.getKey()] = begin;

			for(Map.Entry<Integer, RadixTrieNode> sibling : siblings){
				List<Map.Entry<Integer, RadixTrieNode>> newSiblings = new ArrayList<>(sibling.getValue().getSuccess().entrySet().size() + 1);

				//the termination of a word and not the prefix of other words, in fact, is the leaf node
				if(fetch(sibling.getValue(), newSiblings) == 0){
					trie.base[begin + sibling.getKey()] = (-sibling.getValue().getLargestChildrenId() - 1);
					memoryGrowthSpeed ++;
				}
				else{
					//DFS
					int h = insert(trie, newSiblings);
					trie.base[begin + sibling.getKey()] = h;
				}
				sibling.getValue().setId(begin + sibling.getKey());
			}
		}
		return begin;
	}

}
