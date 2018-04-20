package unit731.hunspeller.collections.ahocorasicktrie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


/**
 * Implementation of the Aho-Corasick string matching algorithm, described in the paper "Efficient String Matching: An Aid to Bibliographic
 * Search", written by Alfred V. Aho and Margaret J. Corasick, Bell Laboratories, 1975
 * 
 * https://github.com/lagodiuk/aho-corasick-optimized
 */
public class AhoCorasick<V>{

	private static final int INITIAL_STATE = 0;

	private final Map<Integer, Map<Character, Integer>> goTo;
	private final Map<Integer, List<String>> output;
	private final Map<Integer, Integer> fail;


	public AhoCorasick(String... patterns){
		goTo = new HashMap<>();
		goTo.put(0, new HashMap<>());

		output = new HashMap<>();
		fail = new HashMap<>();

		initializeGoTo(patterns);
		initializeFail();
	}

	private void initializeGoTo(String... patterns){
		int newState = 0;
		for(String pattern : patterns){
			int state = INITIAL_STATE;
			int chrIdx = 0;
			while(goTo.get(state).containsKey(pattern.charAt(chrIdx)) && chrIdx < pattern.length()){
				state = goTo(state, pattern.charAt(chrIdx));
				chrIdx ++;
			}

			while(chrIdx < pattern.length()){
				newState = newState + 1;
				goTo.put(newState, new HashMap<>());

				Map<Character, Integer> charToState = goTo.get(state);
				if(charToState == null)
					charToState = new HashMap<>();
				charToState.put(pattern.charAt(chrIdx), newState);
				goTo.put(state, charToState);
				state = newState;
				chrIdx ++;
			}

			output.put(state, new ArrayList<>(Arrays.asList(pattern)));
		}
	}

	private void initializeFail(){
		Queue<Integer> queue = new LinkedList<>();

		for(int stateReachableFromInitial : goTo.get(INITIAL_STATE).values()){
			queue.add(stateReachableFromInitial);
			fail.put(stateReachableFromInitial, INITIAL_STATE);
		}

		while(!queue.isEmpty()){
			int nodeId = queue.remove();

			for(Map.Entry<Character, Integer> kv : goTo.get(nodeId).entrySet()){
				char transition = kv.getKey();
				int node = kv.getValue();

				int state = fail.get(nodeId);
				while(state != INITIAL_STATE && !goTo.get(state).containsKey(transition))
					state = fail.get(state);

				fail.put(node, goTo(state, transition));

				output(node).addAll(output(fail.get(node)));


				queue.add(node);
			}
		}
	}

	public static void main(String[] args){
		AhoCorasick<String> tree = new AhoCorasick<>("test", "tent", "tank", "rest");
	}

	public void match(String text, Visitor<V> visitor){
//		int state = INITIAL_STATE;
//		for(int i = 0; i < text.length(); i++){
//			char chr = text.charAt(i);
//
//			while(isFail(state, chr))
//				state = fail.get(state);
//			state = goTo(state, chr);
//
//			List<String> matched = output(state);
//			for(int j = 0; j < matched.size(); j++){
//				String found = matched.get(j);
//				//begin index in text (inclusive): (i - found.length()) + 1
//				//end index in text (exclusive): i
//				boolean stop = visitor.visit(found);
//				if(stop)
//					return;
//			}
//		}
	}

	private boolean isFail(int state, char character){
		return (state != INITIAL_STATE && !goTo.get(state).containsKey(character));
	}

	private int goTo(int state, char character){
		return (state != INITIAL_STATE || goTo.get(state).containsKey(character)? goTo.get(state).get(character): 0);
	}

	private List<String> output(int s){
		return (output.containsKey(s)? output.get(s): Collections.emptyList());
	}

}
