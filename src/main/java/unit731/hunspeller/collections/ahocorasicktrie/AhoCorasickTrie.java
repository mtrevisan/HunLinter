package unit731.hunspeller.collections.ahocorasicktrie;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * An implementation of Aho Corasick algorithm based on Double Array Trie
 * 
 * @see <a href="https://github.com/hankcs/AhoCorasickDoubleArrayTrie">Aho-Corasick double-array trie</a>
 * 
 * @param <V>	The type of values stored in the tree
 */
public class AhoCorasickTrie<V> implements Serializable{

	/** check array of the Double-Array Trie structure */
	int check[];
	/** base array of the Double-Array Trie structure */
	int base[];
	/** fail table of the Aho-Corasick automata */
	int fail[];
	/** output table of the Aho-Corasick automata */
	int[][] output;
	/** outer value array */
	V[] v;

	/** the length of every key */
	int[] l;

	/** the size of base and check array */
	int size;


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

}
