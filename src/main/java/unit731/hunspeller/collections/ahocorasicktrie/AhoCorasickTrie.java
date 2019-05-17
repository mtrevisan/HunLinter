package unit731.hunspeller.collections.ahocorasicktrie;

import java.io.Serializable;
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
 * An implementation of Aho Corasick algorithm based on Double Array Trie
 * 
 * @see <a href="https://github.com/hankcs/AhoCorasickDoubleArrayTrie">Aho-Corasick double-array trie</a>
 * 
 * @param <V>	The type of values stored in the tree
 */
public class AhoCorasickTrie<V> implements Serializable{

	/** check array of the Double-Array Trie structure */
	private int check[];
	/** base array of the Double-Array Trie structure */
	private int base[];
	/** fail table of the Aho-Corasick automata */
	private int fail[];
	/** output table of the Aho-Corasick automata */
	private int[][] output;
	/** outer value array */
	private V[] v;

	/** the length of every key */
	private int[] l;

	/** the size of base and check array */
	private int size;


	/**
	 * Parse text
	 *
	 * @param text	The text
	 * @return	A list of outputs
	 */
	public List<Hit<V>> parseText(CharSequence text){
		int currentState = 0;
		List<Hit<V>> collectedEmits = new ArrayList<>();
		for(int i = 0; i < text.length(); i ++){
			currentState = getState(currentState, text.charAt(i));
			storeEmits(i + 1, currentState, collectedEmits);
		}
		return collectedEmits;
	}

	/** Store output */
	private void storeEmits(int position, int currentState, List<Hit<V>> collectedEmits){
		int[] hitArray = output[currentState];
		if(hitArray != null)
			for(int hit : hitArray)
				collectedEmits.add(new Hit<>(position - l[hit], position, v[hit]));
	}

	/**
	 * Parse text
	 *
	 * @param text	The text
	 * @param processor	A processor which handles the output
	 */
	public void parseText(CharSequence text, HitProcessor<V> processor){
		int currentState = 0;
		exit:
		for(int i = 0; i < text.length(); i ++){
			final int position = i + 1;
			currentState = getState(currentState, text.charAt(i));
			int[] hitArray = output[currentState];
			if(hitArray != null)
				for(int hit : hitArray){
					boolean proceed = processor.hit(position - l[hit], position, v[hit]);
					if(!proceed)
						break exit;
				}
		}
	}

	/**
	 * Checks if the text contains at least one substring
	 *
	 * @param text	Source text to check
	 * @return	<code>true</code> if string contains at least one substring
	 */
	public boolean matches(String text){
		int currentState = 0;
		for(int i = 0; i < text.length(); i ++){
			currentState = getState(currentState, text.charAt(i));
			if(output[currentState] != null)
				return true;
		}
		return false;
	}

	public V get(String key){
		int index = exactMatchSearch(key);
		return (index >= 0? v[index]: null);
	}

	/**
	 * Update a value corresponding to a key
	 *
	 * @param key	The key
	 * @param value	The value
	 * @return	successful or not（failure if there is no key）
	 */
	public boolean set(String key, V value){
		int index = exactMatchSearch(key);
		if(index >= 0){
			v[index] = value;
			return true;
		}
		return false;
	}

	/**
	 * NOTE: Supports failure function
	 */
	private int getState(int currentState, char character){
		int newCurrentState = transitionWithRoot(currentState, character);
		//backup to failure nodes
		while(newCurrentState == -1){
			currentState = fail[currentState];
			newCurrentState = transitionWithRoot(currentState, character);
		}
		return newCurrentState;
	}

	/** Transition of a state, if the state is root and it failed, then returns the root */
	private int transitionWithRoot(int nodePos, char c){
		int b = base[nodePos];
		int idx = b + c + 1;
		if(b != check[idx])
			return (nodePos == 0? 0: -1);
		return idx;
	}

	/**
	 * Match exactly by a key
	 *
	 * @param key	The key
	 * @return	The index of the key (you can use it as a perfect hash function)
	 */
	private int exactMatchSearch(String key){
		return exactMatchSearch(key, 0, 0, 0);
	}

	/**
	 * Match exactly by a key
	 *
	 * @param key	The key
	 * @param pos	The begin index of char array
	 * @param len	The length of the key
	 * @param nodePos	The starting position of the node for searching
	 * @return	The index of the key (you can use it as a perfect hash function)
	 */
	private int exactMatchSearch(String key, int pos, int len, int nodePos){
		if(len <= 0)
			len = key.length();
		if(nodePos <= 0)
			nodePos = 0;

		int result = -1;

		char[] keyChars = key.toCharArray();

		int b = base[nodePos];
		int p;
		for(int i = pos; i < len; i ++){
			p = b + (int)(keyChars[i]) + 1;
			if(b == check[p])
				b = base[p];
			else
				return result;
		}

		p = b;
		int n = base[p];
		if(b == check[p] && n < 0)
			result =  - n - 1;
		return result;
	}

	/** Get the size of the keywords */
	public int size(){
		return v.length;
	}


	/**
	 * Build a AhoCorasickDoubleArrayTrie from a map
	 *
	 * @param map	A map containing key-value pairs
	 */
	public void build(Map<String, V> map){
		new Builder()
			.build(map);
	}

	/**
	 * A builder to build the AhoCorasickDoubleArrayTrie
	 */
	private class Builder{

		/**
		 * the root state of trie
		 */
		private RadixTrieNode rootState = new RadixTrieNode();
		/**
		 * whether the position has been used
		 */
		private boolean used[];
		/**
		 * the allocSize of the dynamic array
		 */
		private int allocSize;
		/**
		 * a parameter controls the memory growth speed of the dynamic array
		 */
		private int progress;
		/**
		 * the next position to check unused memory
		 */
		private int nextCheckPos;
		/**
		 * the size of the key-pair sets
		 */
		private int keySize;

		/**
		 * Build from a map
		 *
		 * @param map a map containing key-value pairs
		 */
		@SuppressWarnings("unchecked")
		public void build(Map<String, V> map){
			// 把值保存下来
			v = (V[])map.values().toArray();
			l = new int[v.length];
			Set<String> keySet = map.keySet();
			// 构建二分trie树
			addAllKeyword(keySet);
			// 在二分trie树的基础上构建双数组trie树
			buildDoubleArrayTrie(keySet.size());
			used = null;
			// 构建failure表并且合并output表
			constructFailureStates();
			rootState = null;
			loseWeight();
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
				siblings.add(new AbstractMap.SimpleEntry<Integer, RadixTrieNode>(0, fakeNode));
			}
			for(Map.Entry<Character, RadixTrieNode> entry : parent.getSuccess().entrySet()){
				siblings.add(new AbstractMap.SimpleEntry<Integer, RadixTrieNode>(entry.getKey() + 1, entry.getValue()));
			}
			return siblings.size();
		}

		/**
		 * add a keyword
		 *
		 * @param keyword a keyword
		 * @param index the index of the keyword
		 */
		private void addKeyword(String keyword, int index){
			RadixTrieNode currentState = this.rootState;
			for(Character character : keyword.toCharArray()){
				currentState = currentState.addState(character);
			}
			currentState.addEmit(index);
			l[index] = keyword.length();
		}

		/**
		 * add a collection of keywords
		 *
		 * @param keywordSet the collection holding keywords
		 */
		private void addAllKeyword(Collection<String> keywordSet){
			int i = 0;
			for(String keyword : keywordSet){
				addKeyword(keyword, i ++);
			}
		}

		/**
		 * construct failure table
		 */
		private void constructFailureStates(){
			fail = new int[size + 1];
			fail[1] = base[0];
			output = new int[size + 1][];
			Queue<RadixTrieNode> queue = new ArrayDeque<RadixTrieNode>();

			// 第一步，将深度为1的节点的failure设为根节点
			for(RadixTrieNode depthOneState : this.rootState.getStates()){
				depthOneState.setFailure(this.rootState, fail);
				queue.add(depthOneState);
				constructOutput(depthOneState);
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
					targetState.setFailure(newFailureState, fail);
					targetState.addEmit(newFailureState.emit());
					constructOutput(targetState);
				}
			}
		}

		/**
		 * construct output table
		 */
		private void constructOutput(RadixTrieNode targetState){
			Collection<Integer> emit = targetState.emit();
			if(emit == null || emit.isEmpty()){
				return;
			}
			int output[] = new int[emit.size()];
			Iterator<Integer> it = emit.iterator();
			for(int i = 0; i < output.length;  ++ i){
				output[i] = it.next();
			}
			AhoCorasickTrie.this.output[targetState.getIndex()] = output;
		}

		private void buildDoubleArrayTrie(int keySize){
			progress = 0;
			this.keySize = keySize;
			resize(65536 * 32); // 32个双字节

			base[0] = 1;
			nextCheckPos = 0;

			RadixTrieNode root_node = this.rootState;

			List<Map.Entry<Integer, RadixTrieNode>> siblings = new ArrayList<Map.Entry<Integer, RadixTrieNode>>(root_node.getSuccess().entrySet().size());
			fetch(root_node, siblings);
			insert(siblings);
		}

		/**
		 * allocate the memory of the dynamic array
		 *
		 * @param newSize
		 * @return
		 */
		private int resize(int newSize){
			int[] base2 = new int[newSize];
			int[] check2 = new int[newSize];
			boolean used2[] = new boolean[newSize];
			if(allocSize > 0){
				System.arraycopy(base, 0, base2, 0, allocSize);
				System.arraycopy(check, 0, check2, 0, allocSize);
				System.arraycopy(used, 0, used2, 0, allocSize);
			}

			base = base2;
			check = check2;
			used = used2;

			return allocSize = newSize;
		}

		/**
		 * insert the siblings to double array trie
		 *
		 * @param siblings the siblings being inserted
		 * @return the position to insert them
		 */
		private int insert(List<Map.Entry<Integer, RadixTrieNode>> siblings){
			int begin = 0;
			int pos = Math.max(siblings.get(0).getKey() + 1, nextCheckPos) - 1;
			int nonzero_num = 0;
			int first = 0;

			if(allocSize <= pos){
				resize(pos + 1);
			}

			outer:
			// 此循环体的目标是找出满足base[begin + a1...an]  == 0的n个空闲空间,a1...an是siblings中的n个节点
			while(true){
				pos ++;

				if(allocSize <= pos){
					resize(pos + 1);
				}

				if(check[pos] != 0){
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
					resize((int)(allocSize * l));
				}

				if(used[begin]){
					continue;
				}

				for(int i = 1; i < siblings.size(); i ++){
					if(check[begin + siblings.get(i).getKey()] != 0){
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

			size = (size > begin + siblings.get(siblings.size() - 1).getKey() + 1) ? size : begin + siblings.get(siblings.size() - 1).getKey() + 1;

			for(Map.Entry<Integer, RadixTrieNode> sibling : siblings){
				check[begin + sibling.getKey()] = begin;
			}

			for(Map.Entry<Integer, RadixTrieNode> sibling : siblings){
				List<Map.Entry<Integer, RadixTrieNode>> new_siblings = new ArrayList<Map.Entry<Integer, RadixTrieNode>>(sibling.getValue().getSuccess().entrySet().size() + 1);

				if(fetch(sibling.getValue(), new_siblings) == 0) // 一个词的终止且不为其他词的前缀，其实就是叶子节点
				{
					base[begin + sibling.getKey()] = ( - sibling.getValue().getLargestValueId() - 1);
					progress ++;
				}
				else{
					int h = insert(new_siblings);   // dfs
					base[begin + sibling.getKey()] = h;
				}
				sibling.getValue().setIndex(begin + sibling.getKey());
			}
			return begin;
		}

		/**
		 * free the unnecessary memory
		 */
		private void loseWeight(){
			int nbase[] = new int[size + 65535];
			System.arraycopy(base, 0, nbase, 0, size);
			base = nbase;

			int ncheck[] = new int[size + 65535];
			System.arraycopy(check, 0, ncheck, 0, size);
			check = ncheck;
		}

	}

}
