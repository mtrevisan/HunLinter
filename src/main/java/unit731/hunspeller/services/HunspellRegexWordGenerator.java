package unit731.hunspeller.services;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * A class that help generating words that match a given regular expression.
 * <p>
 * It generate all values that are matched by the Regex, a random value, or you can generate only a specific string based on it's
 * lexicographical order.
 * </p>
 *
 * @see <a href="https://github.com/mifmif/Generex">Generex</a>
 */
public class HunspellRegexWordGenerator implements Iterable<String>{

	private final Automaton automaton;


	public HunspellRegexWordGenerator(String regex){
		Objects.requireNonNull(regex);

		RegExp re = new RegExp(regex);
		automaton = re.toAutomaton();
	}


	/**
	 * Generate a random word that matches the given pattern.
	 *
	 * @param random	A random generator
	 * @return	A random word
	 */
	public String generate(Random random){
		return generate(random, 1, Integer.MAX_VALUE);
	}

	/**
	 * Generate a random word that matches the given pattern, and the word has a <code>length >= minLength</code>
	 *
	 * @param random	A random generator
	 * @param minLength	Minimum word length
	 * @return	A random word
	 */
	public String generate(Random random, int minLength){
		return generate(random, minLength, Integer.MAX_VALUE);
	}

	/**
	 * Generate a random word that matches the given pattern, and the string has a <code>minLength <= length <= maxLength</code>
	 *
	 * @param random	A random generator
	 * @param minLength	Minimum word length
	 * @param maxLength	Maximum word length
	 * @return	A random word
	 */
	public String generate(Random random, int minLength, int maxLength){
		Function<Integer, Integer> fnTransition = (max) -> getRandomInt(random, 0, max);
		BiFunction<Character, Character, Character> fnCharIntoTransition = (min, max) -> (char)getRandomInt(random, min, max);
		return generate(automaton.getInitialState(), fnTransition, fnCharIntoTransition, minLength, maxLength);
	}

//	private String prepareRandom(Random random, String wordMatch, State state, int minLength, int maxLength){
//		List<Transition> transitions = state.getSortedTransitions(false);
//		Set<Integer> selectedTransitions = new HashSet<>();
//		String result = wordMatch;
//
//		for(int resultLength = -1; selectedTransitions.size() < transitions.size() && (resultLength < minLength || resultLength > maxLength); resultLength = result.length()){
//			if(randomPrepared(wordMatch, state, minLength, maxLength, transitions)){
//				result = wordMatch;
//				break;
//			}
//
//			int nextInt = random.nextInt(transitions.size());
//			if(!selectedTransitions.contains(nextInt)){
//				selectedTransitions.add(nextInt);
//
//				Transition randomTransition = transitions.get(nextInt);
//				int diff = randomTransition.getMax() - randomTransition.getMin() + 1;
//				int randomOffset = (diff > 0? random.nextInt(diff): diff);
//				char randomChar = (char)(randomOffset + randomTransition.getMin());
//				result = prepareRandom(random, wordMatch + randomChar, randomTransition.getDest(), minLength, maxLength);
//			}
//		}
//
//		return result;
//	}

	private String generate(State initialState, Function<Integer, Integer> fnTransition, BiFunction<Character, Character, Character> fnCharIntoTransition, int minLength, int maxLength){
		State state = initialState;
		StringBuilder sb = new StringBuilder();
		Map<State, Set<Integer>> automatonSelectedTransitions = new HashMap<>();
		while(true){
			List<Transition> transitions = state.getSortedTransitions(false);
			if(transitions.isEmpty()){
				if(minLength <= sb.length() && sb.length() <= maxLength)
					break;

				state = initialState;
				transitions = state.getSortedTransitions(false);
				sb.setLength(0);
				automatonSelectedTransitions.clear();
			}

			//choose a transition
			int option;
			automatonSelectedTransitions.putIfAbsent(state, new HashSet<>());
			Set<Integer> selectedTransitions = automatonSelectedTransitions.get(state);
			do{
				option = fnTransition.apply(transitions.size());
			}while(selectedTransitions.size() < transitions.size() && selectedTransitions.contains(option));
			selectedTransitions.add(option);
			Transition transition = transitions.get(option);

			//moving on to next transition
			char chr = fnCharIntoTransition.apply(transition.getMin(), transition.getMax());
			sb.append(chr);

			state = transition.getDest();
		}
		return sb.toString();
	}

	/**
	 * Generates a random number within the given bounds.
	 *
	 * @param min	The minimum number (inclusive).
	 * @param max	The maximum number (inclusive).
	 * @param random	The object used as the randomizer.
	 * @return	A random number in the given range.
	 */
	private int getRandomInt(Random random, int min, int max){
		//use random.nextInt as it guarantees a uniform distribution
		return (max > min? random.nextInt(max - min): 0) + min;
	}


	/**
	 * @param index	The index at which to extract the word
	 * @return	The matched word by the given pattern in the given it's order in the sorted list of matched words.<br>
	 *		<code>indexOrder</code> between <code>1</code> and <code>n</code> where <code>n</code> is the number of matched words.<br>
	 *		If <code>indexOrder >= n</code>, return an empty string. If there is an infinite number of words that matches the given Regex,
	 *		the method throws {@code StackOverflowError}.
	 */
//	public String generate(int index){
//		buildRootNode();
//		if(index == 0)
//			index = 1;
//		String result = buildStringFromNode(rootNode, index);
//		result = result.substring(1, result.length() - 1);
//		return result;
//	}

	@Override
	public Iterator<String> iterator(){
		//TODO
		return null;
	}
	
}
