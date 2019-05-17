package unit731.hunspeller.collections.ahocorasicktrie;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


/**
 * A builder to build the AhoCorasickTrie
 * 
 * @param <V>	The type of values stored in the tree
 */
class AhoCorasickTrieBuilder<V>{

	/** check array of the Double-Array Trie structure */
//	private int check[];
//	/** base array of the Double-Array Trie structure */
//	private int base[];
//	/** fail table of the Aho-Corasick automata */
//	private int fail[];
//	/** output table of the Aho-Corasick automata */
//	private int[][] output;
//	/** outer value array */
//	private V[] v;
//
//	/** the length of every key */
//	private int[] l;
//
//	/** the size of base and check array */
//	private int size;


	/** The root state of trie */
	private RadixTrieNode rootState = new RadixTrieNode();
	/** Whether the position has been used */
	private boolean used[];
	/** The allocSize of the dynamic array */
	private int allocSize;
	/** A parameter controls the memory growth speed of the dynamic array */
	private int progress;
	/** The next position to check unused memory */
	private int nextCheckPos;
	/** The size of the key-pair sets */
	private int keySize;


	/**
	 * Build a AhoCorasickTrie from a map
	 *
	 * @param map	A map containing key-value pairs
	 */
	public AhoCorasickTrie<V> build(Map<String, V> map){
		AhoCorasickTrie<V> trie = new AhoCorasickTrie<>();
		// 把值保存下来
		trie.v = (V[])map.values().toArray();
		trie.l = new int[trie.v.length];
		Set<String> keySet = map.keySet();
		// 构建二分trie树
		addAllKeyword(trie, keySet);
		// 在二分trie树的基础上构建双数组trie树
		buildTrie(trie, keySet.size());
		used = null;
		// 构建failure表并且合并output表
		constructFailureStates(trie);
		rootState = null;
		loseWeight(trie);
		return trie;
	}

	/**
	 * fetch siblings of a parent node
	 *
	 * @param parent parent node
	 * @param siblings parent node's child nodes, i . e . the siblings
	 * @return the amount of the siblings
	 */
	private int fetch(RadixTrieNode parent, List<Map.Entry<Integer, RadixTrieNode>> siblings){
		if(parent.isAcceptable()){
			RadixTrieNode fakeNode = new RadixTrieNode( - (parent.getDepth() + 1));  // 此节点是parent的子节点，同时具备parent的输出
			fakeNode.addEmit(parent.getLargestValueId());
			siblings.add(new AbstractMap.SimpleEntry<>(0, fakeNode));
		}
		for(Map.Entry<Character, RadixTrieNode> entry : parent.getSuccess().entrySet())
			siblings.add(new AbstractMap.SimpleEntry<>(entry.getKey() + 1, entry.getValue()));
		return siblings.size();
	}

	/**
	 * add a keyword
	 *
	 * @param keyword a keyword
	 * @param index the index of the keyword
	 */
	private void addKeyword(AhoCorasickTrie<V> trie, String keyword, int index){
		RadixTrieNode currentState = this.rootState;
		for(Character character : keyword.toCharArray()){
			currentState = currentState.addState(character);
		}
		currentState.addEmit(index);
		trie.l[index] = keyword.length();
	}

	/**
	 * add a collection of keywords
	 *
	 * @param keywordSet the collection holding keywords
	 */
	private void addAllKeyword(AhoCorasickTrie<V> trie, Collection<String> keywordSet){
		int i = 0;
		for(String keyword : keywordSet){
			addKeyword(trie, keyword, i ++);
		}
	}

	/**
	 * construct failure table
	 */
	private void constructFailureStates(AhoCorasickTrie<V> trie){
		trie.fail = new int[trie.size + 1];
		trie.fail[1] = trie.base[0];
		trie.output = new int[trie.size + 1][];
		Queue<RadixTrieNode> queue = new ArrayDeque<>();

		// 第一步，将深度为1的节点的failure设为根节点
		for(RadixTrieNode depthOneState : this.rootState.getStates()){
			depthOneState.setFailure(this.rootState, trie.fail);
			queue.add(depthOneState);
			constructOutput(trie, depthOneState);
		}

		// 第二步，为深度 > 1 的节点建立failure表，这是一个bfs
		while(!queue.isEmpty()){
			RadixTrieNode currentState = queue.remove();

			for(Character transition : currentState.getTransitions()){
				RadixTrieNode targetState = currentState.nextState(transition);
				queue.add(targetState);

				RadixTrieNode traceFailureState = currentState.failure();
				while(traceFailureState.nextState(transition) == null){
					traceFailureState = traceFailureState.failure();
				}
				RadixTrieNode newFailureState = traceFailureState.nextState(transition);
				targetState.setFailure(newFailureState, trie.fail);
				targetState.addEmit(newFailureState.emit());
				constructOutput(trie, targetState);
			}
		}
	}

	/**
	 * construct output table
	 */
	private void constructOutput(AhoCorasickTrie<V> trie, RadixTrieNode targetState){
		Collection<Integer> emit = targetState.emit();
		if(emit == null || emit.isEmpty()){
			return;
		}
		int output[] = new int[emit.size()];
		Iterator<Integer> it = emit.iterator();
		for(int i = 0; i < output.length;  ++ i){
			output[i] = it.next();
		}
		trie.output[targetState.getIndex()] = output;
	}

	private void buildTrie(AhoCorasickTrie<V> trie, int keySize){
		progress = 0;
		this.keySize = keySize;
		resize(trie, 65536 * 32); // 32个双字节

		trie.base[0] = 1;
		nextCheckPos = 0;

		RadixTrieNode root_node = this.rootState;

		List<Map.Entry<Integer, RadixTrieNode>> siblings = new ArrayList<Map.Entry<Integer, RadixTrieNode>>(root_node.getSuccess().entrySet().size());
		fetch(root_node, siblings);
		insert(trie, siblings);
	}

	/**
	 * allocate the memory of the dynamic array
	 *
	 * @param newSize
	 * @return
	 */
	private int resize(AhoCorasickTrie<V> trie, int newSize){
		int[] base2 = new int[newSize];
		int[] check2 = new int[newSize];
		boolean used2[] = new boolean[newSize];
		if(allocSize > 0){
			System.arraycopy(trie.base, 0, base2, 0, allocSize);
			System.arraycopy(trie.check, 0, check2, 0, allocSize);
			System.arraycopy(used, 0, used2, 0, allocSize);
		}

		trie.base = base2;
		trie.check = check2;
		used = used2;

		return allocSize = newSize;
	}

	/**
	 * insert the siblings to double array trie
	 *
	 * @param siblings the siblings being inserted
	 * @return the position to insert them
	 */
	private int insert(AhoCorasickTrie<V> trie, List<Map.Entry<Integer, RadixTrieNode>> siblings){
		int begin = 0;
		int pos = Math.max(siblings.get(0).getKey() + 1, nextCheckPos) - 1;
		int nonzero_num = 0;
		int first = 0;

		if(allocSize <= pos){
			resize(trie, pos + 1);
		}

		outer:
		// 此循环体的目标是找出满足base[begin + a1...an]  == 0的n个空闲空间,a1...an是siblings中的n个节点
		while(true){
			pos ++;

			if(allocSize <= pos){
				resize(trie, pos + 1);
			}

			if(trie.check[pos] != 0){
				nonzero_num ++;
				continue;
			}
			else if(first == 0){
				nextCheckPos = pos;
				first = 1;
			}

			begin = pos - siblings.get(0).getKey(); // 当前位置离第一个兄弟节点的距离
			if(allocSize <= (begin + siblings.get(siblings.size() - 1).getKey())){
				// progress can be zero // 防止progress产生除零错误
				double l = (1.05 > 1.0 * keySize / (progress + 1)) ? 1.05 : 1.0 * keySize / (progress + 1);
				resize(trie, (int)(allocSize * l));
			}

			if(used[begin]){
				continue;
			}

			for(int i = 1; i < siblings.size(); i ++){
				if(trie.check[begin + siblings.get(i).getKey()] != 0){
					continue outer;
				}
			}

			break;
		}

		// -- Simple heuristics --
		// if the percentage of non-empty contents in check between the
		// index
		// 'next_check_pos' and 'check' is greater than some constant value
		// (e.g. 0.9),
		// new 'next_check_pos' index is written by 'check'.
		if(1.0 * nonzero_num / (pos - nextCheckPos + 1) >= 0.95){
			nextCheckPos = pos; // 从位置 next_check_pos 开始到 pos 间，如果已占用的空间在95%以上，下次插入节点时，直接从 pos 位置处开始查找
		}
		used[begin] = true;

		trie.size = (trie.size > begin + siblings.get(siblings.size() - 1).getKey() + 1) ? size : begin + siblings.get(siblings.size() - 1).getKey() + 1;

		for(Map.Entry<Integer, RadixTrieNode> sibling : siblings){
			trie.check[begin + sibling.getKey()] = begin;
		}

		for(Map.Entry<Integer, RadixTrieNode> sibling : siblings){
			List<Map.Entry<Integer, RadixTrieNode>> new_siblings = new ArrayList<Map.Entry<Integer, RadixTrieNode>>(sibling.getValue().getSuccess().entrySet().size() + 1);

			if(fetch(sibling.getValue(), new_siblings) == 0) // 一个词的终止且不为其他词的前缀，其实就是叶子节点
			{
				trie.base[begin + sibling.getKey()] = ( - sibling.getValue().getLargestValueId() - 1);
				progress ++;
			}
			else{
				int h = insert(trie, new_siblings);   // dfs
				trie.base[begin + sibling.getKey()] = h;
			}
			sibling.getValue().setIndex(begin + sibling.getKey());
		}
		return begin;
	}

	/**
	 * free the unnecessary memory
	 */
	private void loseWeight(AhoCorasickTrie<V> trie){
		int nbase[] = new int[trie.size + 65535];
		System.arraycopy(trie.base, 0, nbase, 0, trie.size);
		trie.base = nbase;

		int ncheck[] = new int[trie.size + 65535];
		System.arraycopy(trie.check, 0, ncheck, 0, trie.size);
		trie.check = ncheck;
	}

}
