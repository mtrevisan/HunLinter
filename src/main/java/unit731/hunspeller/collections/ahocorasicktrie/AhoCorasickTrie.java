package unit731.hunspeller.collections.ahocorasicktrie;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.collections.ahocorasicktrie.sequencers.SequencerInterface;
import unit731.hunspeller.collections.ahocorasicktrie.sequencers.StringSequencer;


/**
 * An implementation of Aho Corasick algorithm based on Double Array Trie
 * 
 * @see <a href="https://github.com/hankcs/AhoCorasickDoubleArrayTrie">AhoCorasickDoubleArrayTrie</a>
 *
 * @param <S>	The sequence/key type
 * @param <V>	The type of values stored in the tree
 */
public class AhoCorasickTrie<S, V extends Serializable> implements Map<S, V>{

	/** check array of the Double Array Trie structure */
	protected int check[];
	/** base array of the Double Array Trie structure */
	protected int base[];
	/** the size of base and check array */
	protected int size;

	/** table of the fail transitions of the automaton mapping: "state" -> "new state" */
	protected int fail[];
	/** table of the outputs of every state mapping: "state" -> "matched patterns" */
	protected int[][] output;
	/** outer value array */
	private V[] values;
	/** the length of every key */
	private int[] keyLengths;


	/**
	 * Build a AhoCorasickDoubleArrayTrie from a map
	 *
	 * @param map	A map containing key-value pairs
	 */
	public void create(Map<S, V> map){
		new Builder().build(map);
	}

	/**
	 * Gets a list of hits whose associated value is contained into the <code>text</code>.
	 *
	 * @param text	Source text to check
	 * @return	A list of values
	 * @throws NullPointerException	If the text is <code>null</code>
	 */
	public List<V> extractSubstrings(String text){
		List<V> collectedEmits = new ArrayList<>();
		Visitor<V> visitor = value -> {
			collectedEmits.add(value);
			return false;
		};
		visit(text, visitor);

		return collectedEmits;
	}

	/**
	 * Call a given callback for every hit whose associated value is contained into the <code>text</code>.
	 *
	 * @param text	Source text to check
	 * @param visitor	A visitor which handles the output
	 * @throws NullPointerException	If the text is <code>null</code>
	 */
	public void visit(String text, Visitor<V> visitor){
		Objects.requireNonNull(text);
		Objects.requireNonNull(visitor);

		int currentState = 0;
		for(int position = 1; position <= text.length(); position ++){
			currentState = getState(currentState, text.charAt(position - 1));

			int[] hitArray = output[currentState];
			if(hitArray != null)
				for(int hit : hitArray){
					//begin index in text (inclusive): position - keyLengths[hit]
					//end index in text (exclusive): position
					boolean stop = visitor.visit(values[hit]);
					if(stop)
						return;
				}
		}
	}

	/** Transmit state, supports failure function */
	private int getState(int currentState, char character){
		int newCurrentState = transition(currentState, character);
		while(newCurrentState == -1){
			currentState = fail[currentState];
			newCurrentState = transition(currentState, character);
		}
		return newCurrentState;
	}

	/** Transition of a state */
	private int transition(int nodePosition, char chr){
		int b = base[nodePosition];
		int p = b + chr + 1;
		if(b != check[p])
			//if the state is root and it failed, then returns the root
			return (nodePosition == 0? 0: -1);

		return p;
	}

	@Override
	public boolean isEmpty(){
		return (size() == 0);
	}

	/**
	 * Checks that <code>text</code> contains at least one substring
	 *
	 * @param key	Source text to check
	 * @return	<code>true</code> if string contains at least one substring
	 */
	@Override
	public boolean containsKey(Object key){
		Objects.requireNonNull(key);

		int foundNode = get((String)key, 0, 0, 0);
		return (foundNode >= 0);
	}

	@Override
	public boolean containsValue(Object value){
		Objects.requireNonNull(value);

		boolean[] result = new boolean[]{false};
		Visitor<V> visitor = v -> {
			result[0] = (v == value || v.equals(value));
			return result[0];
		};
		//TODO
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//		visit(visitor);

//		return result[0];
	}

	/**
	 * Match exactly by a key
	 *
	 * @param key	The key
	 * @return	The value index of the key, <code>-1</code> indicates <code>null</code> (it can be used as a perfect hash function)
	 */
	@Override
	public V get(Object key){
		int index = get((String)key, 0, 0, 0);
		return (index >= 0? values[index]: null);
	}

	/**
	 * Match exactly by a key
	 * 
	 * @param key	The key
	 * @param position	The begin index of the key
	 * @param length	The length of the key
	 * @param nodePosition	The starting position of the node for searching
	 * @return	The value index of the key, <code>-1</code> indicates <code>null</code> (it can be used as a perfect hash function)
	 */
	private int get(String key, int position, int length, int nodePosition){
		Objects.requireNonNull(key);

		if(length <= 0)
			length = key.length();
		if(nodePosition < 0)
			nodePosition = 0;

		int result = -1;
		int b = base[nodePosition];
		int p;
		for(int i = position; i < length; i ++){
			p = b + key.charAt(i) + 1;
			if(b == check[p])
				b = base[p];
			else
				return result;
		}

		p = b;
		int n = base[p];
		if(b == check[p] && n < 0)
			result = -n - 1;
		return result;
	}

	@Override
	public V put(S key, V value){
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public void putAll(Map<? extends S, ? extends V> m){
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public V remove(Object key){
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Set<Entry<S, V>> entrySet(){
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Set<S> keySet(){
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Collection<V> values(){
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public int size(){
		return values.length;
	}

	@Override
	public void clear(){
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}



	/** A builder to build the AhoCorasickTrie */
	private class Builder{

		/** the root state of trie */
		private State<S> rootState = new State<>();
		/** whether the position has been used */
		private boolean used[];
		/** the allocSize of the dynamic array */
		private int allocSize;
		/** a parameter controls the memory growth speed of the dynamic array */
		private int progress;
		/** the next position to check unused memory */
		private int nextCheckPos;
		/** the size of the key-pair sets */
		private int keySize;

		private SequencerInterface<S> sequencer;

		/**
		 * Build from a map
		 *
		 * @param map	A map containing key-value pairs
		 */
		@SuppressWarnings("unchecked")
		public void build(Map<S, V> map){
			//save the value
			values = (V[])map.values().toArray();
			keyLengths = new int[values.length];

			//constructing a binary trie
			int i = 0;
			Set<S> keySet = map.keySet();
			for(S keyword : keySet)
				addKeyword(keyword, i ++);

			//building a double-array trie based on a binary trie
			buildDoubleArrayTrie(keySet.size());
			used = null;

			//build the failure table and merge the output table
			constructFailureStates();
			rootState = null;
			loseWeight();
		}

		/**
		 * add a keyword
		 *
		 * @param keyword a keyword
		 * @param index the index of the keyword
		 */
		private void addKeyword(S keyword, int index){
			State<S> currentState = rootState;
			int size = sequencer.length(keyword);
			for(int i = 0; i < size; i ++)
				currentState = currentState.addState(sequencer.charAtIndex(keyword, i));
			currentState.addEmit(index);
			keyLengths[index] = sequencer.length(keyword);
		}

		/**
		 * fetch siblings of a parent node
		 *
		 * @param parent parent node
		 * @param siblings parent node's child nodes, i . e . the siblings
		 * @return the amount of the siblings
		 */
		private int fetch(State<S> parent, List<Map.Entry<Integer, State<S>>> siblings){
			if(parent.isAcceptable()){
				State<S> fakeNode = new State<>(-(parent.getDepth() + 1));  // 此节点是parent的子节点，同时具备parent的输出
				fakeNode.addEmit(parent.getLargestValueId());
				siblings.add(new AbstractMap.SimpleEntry<>(0, fakeNode));
			}
			for(Map.Entry<S, State<S>> entry : parent.getSuccess().entrySet())
				siblings.add(new AbstractMap.SimpleEntry<>(entry.getKey() + 1, entry.getValue()));
			return siblings.size();
		}

		/**
		 * construct failure table
		 */
		private void constructFailureStates(){
			fail = new int[size + 1];
			fail[1] = base[0];
			output = new int[size + 1][];
			Queue<State<S>> queue = new ArrayDeque<>();

			// 第一步，将深度为1的节点的failure设为根节点
			for(State<S> depthOneState : this.rootState.getStates()){
				depthOneState.setFailure(this.rootState, fail);
				queue.add(depthOneState);

				constructOutput(depthOneState);
			}

			// 第二步，为深度 > 1 的节点建立failure表，这是一个bfs
			while(!queue.isEmpty()){
				State<S> currentState = queue.remove();

				for(S transition : currentState.getTransitions()){
					State<S> targetState = currentState.nextState(transition);
					queue.add(targetState);

					State<S> traceFailureState = currentState.getFailure();
					while(traceFailureState.nextState(transition) == null)
						traceFailureState = traceFailureState.getFailure();

					State<S> newFailureState = traceFailureState.nextState(transition);
					targetState.setFailure(newFailureState, fail);

					targetState.addEmit(newFailureState.emit());
					constructOutput(targetState);
				}
			}
		}

		/**
		 * construct output table
		 */
		private void constructOutput(State<S> targetState){
			Collection<Integer> emit = targetState.emit();
			if(emit == null || emit.isEmpty())
				return;

			int output[] = new int[emit.size()];
			Iterator<Integer> it = emit.iterator();
			for(int i = 0; i < output.length;  ++ i)
				output[i] = it.next();
			AhoCorasickTrie.this.output[targetState.getIndex()] = output;
		}

		private void buildDoubleArrayTrie(int keySize){
			progress = 0;
			this.keySize = keySize;
			resize(65536 * 32); // 32个双字节

			base[0] = 1;
			nextCheckPos = 0;

			State<S> root_node = this.rootState;

			List<Map.Entry<Integer, State<S>>> siblings = new ArrayList<>(root_node.getSuccess().entrySet().size());
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
		private int insert(List<Map.Entry<Integer, State<S>>> siblings){
			int begin = 0;
			int pos = Math.max(siblings.get(0).getKey() + 1, nextCheckPos) - 1;
			int nonzero_num = 0;
			int first = 0;

			if(allocSize <= pos)
				resize(pos + 1);

			outer:
			// 此循环体的目标是找出满足base[begin + a1...an]  == 0的n个空闲空间,a1...an是siblings中的n个节点
			while(true){
				pos ++;

				if(allocSize <= pos)
					resize(pos + 1);

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
					//progress can be zero // 防止progress产生除零错误
					double l = (1.05 > keySize / (progress + 1)? 1.05: keySize / (progress + 1));
					resize((int)(allocSize * l));
				}

				if(used[begin])
					continue;

				for(int i = 1; i < siblings.size(); i ++)
					if(check[begin + siblings.get(i).getKey()] != 0)
						continue outer;

				break;
			}

			// -- Simple heuristics --
			// if the percentage of non-empty contents in check between the
			// index
			// 'next_check_pos' and 'check' is greater than some constant value
			// (e.g. 0.9),
			// new 'next_check_pos' index is written by 'check'.
			if(1.0 * nonzero_num / (pos - nextCheckPos + 1) >= 0.95)
				nextCheckPos = pos; // 从位置 next_check_pos 开始到 pos 间，如果已占用的空间在95%以上，下次插入节点时，直接从 pos 位置处开始查找
			used[begin] = true;

			size = (size > begin + siblings.get(siblings.size() - 1).getKey() + 1) ? size : begin + siblings.get(siblings.size() - 1).getKey() + 1;

			for(Map.Entry<Integer, State<S>> sibling : siblings)
				check[begin + sibling.getKey()] = begin;

			for(Map.Entry<Integer, State<S>> sibling : siblings){
				List<Map.Entry<Integer, State<S>>> new_siblings = new ArrayList<>(sibling.getValue().getSuccess().entrySet().size() + 1);

				if(fetch(sibling.getValue(), new_siblings) == 0){ // 一个词的终止且不为其他词的前缀，其实就是叶子节点
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
