package unit731.hunspeller.services;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import java.util.Collections;
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
import org.apache.commons.lang3.StringUtils;


/**
 * A class that help generating words that match a given regular expression.
 * <p>
 * It generates all values that are matched by the Regex, or a random value.
 * </p>
 *
 * @see <a href="https://github.com/mifmif/Generex">Generex</a>
 */
public class HunspellRegexWordGenerator implements Iterable<String>{

	private static final Map<String, String> PREDEFINED_CHARACTER_CLASSES;
	static{
		Map<String, String> characterClasses = new HashMap<>();
		characterClasses.put("\\\\d", "[0-9]");
		characterClasses.put("\\\\D", "[^0-9]");
		characterClasses.put("\\\\s", "[ \t\n\f\r]");
		characterClasses.put("\\\\S", "[^ \t\n\f\r]");
		characterClasses.put("\\\\w", "[a-zA-Z_0-9]");
		characterClasses.put("\\\\W", "[^a-zA-Z_0-9]");
		PREDEFINED_CHARACTER_CLASSES = Collections.unmodifiableMap(characterClasses);
	}

	private final Automaton automaton;


	public HunspellRegexWordGenerator(String regex){
		Objects.requireNonNull(regex);

		regex = StringUtils.replaceEachRepeatedly(regex,
			PREDEFINED_CHARACTER_CLASSES.keySet().toArray(new String[PREDEFINED_CHARACTER_CLASSES.size()]),
			PREDEFINED_CHARACTER_CLASSES.values().toArray(new String[PREDEFINED_CHARACTER_CLASSES.size()]));
		RegExp re = new RegExp(regex);
		automaton = re.toAutomaton();
	}


	/**
	 * Tells whether or not the given pattern (or {@code Automaton}) is infinite, that is, generates an infinite number of words.
	 *
	 * @return	Whether the pattern (or {@code Automaton}) generates an infinite number of words
	 */
	public boolean isInfinite(){
		return !automaton.isFinite();
	}


	/**
	 * Generate a random word that matches the given pattern, and the string has a <code>minLength <= length <= maxLength</code>
	 *
	 * @param random	A random generator
	 * @return	A random word
	 */
	public String generate(Random random){
		Function<Integer, Integer> fnTransition = (max) -> getRandomInt(random, 0, max);
		BiFunction<Character, Character, Character> fnCharIntoTransition = (min, max) -> (char)getRandomInt(random, min, max);
		return generate(automaton.getInitialState(), fnTransition, fnCharIntoTransition);
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

	private String generate(State initialState, Function<Integer, Integer> fnTransition, BiFunction<Character, Character, Character> fnCharIntoTransition){
		State state = initialState;
		StringBuilder sb = new StringBuilder();
		Map<State, Set<Integer>> automatonSelectedTransitions = new HashMap<>();
		while(true){
			List<Transition> transitions = state.getSortedTransitions(false);
			if(transitions.isEmpty()){
				assert state.isAccept();

				break;
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
//
//	private String buildStringFromNode(Node node, int indexOrder){
//		String result = StringUtils.EMPTY;
//		long passedStringNbr = 0;
//		long step = node.getNbrMatchedString() / node.getNbrChar();
//		for(char usedChar = node.getMinChar(); usedChar <= node.getMaxChar();  ++ usedChar){
//			passedStringNbr += step;
//			if(passedStringNbr >= indexOrder){
//				passedStringNbr -= step;
//				indexOrder -= passedStringNbr;
//				result += usedChar;
//				break;
//			}
//		}
//		long passedStringNbrInChildNode = 0;
//		if(result.length() == 0)
//			passedStringNbrInChildNode = passedStringNbr;
//		for(Node childN : node.getNextNodes()){
//			passedStringNbrInChildNode += childN.getNbrMatchedString();
//			if(passedStringNbrInChildNode >= indexOrder){
//				passedStringNbrInChildNode -= childN.getNbrMatchedString();
//				indexOrder -= passedStringNbrInChildNode;
//				result = result.concat(buildStringFromNode(childN, indexOrder));
//				break;
//			}
//		}
//		return result;
//	}


	@Override
	public Iterator<String> iterator(){
		//TODO
		return null;
	}
	
}
