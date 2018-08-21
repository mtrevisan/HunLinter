package unit731.hunspeller.services.regexgenerator;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.services.PatternService;


/**
 * A class that help generating words that match a given regular expression.
 * <p>
 * It generates all values that are matched by the Regex, or a random value.
 * </p>
 *
 * @see <a href="https://github.com/mifmif/Generex">Generex</a>
 * @see <a href="https://github.com/bluezio/xeger">Xeger</a>
 * @see <a href="http://www.brics.dk/automaton/index.html">dk.brics.automaton</a>
 */
public class HunspellRegexWordGenerator{

	@AllArgsConstructor
	private static class GeneratedElement{
		private final String word;
		private final State state;
	}

	@AllArgsConstructor
	private static class StateNodeElement{
		private final State state;
		private final HunspellAutomataNode node;
	}


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

	private static final Matcher MATCHER_REQUOTE_SPECIAL_CHARS = PatternService.matcher("[.^$*+?(){|\\[\\\\@]");
	private static final Matcher MATCHER_REQUOTE = PatternService.matcher("\\\\Q(.*?)\\\\E");


	private final Automaton automaton;
	private final boolean ignoreEmptyWord;

	private HunspellAutomataNode rootNode;


	/**
	 * @param regex	Regex used to generate the set
	 * @param ignoreEmptyWord	Does not consider ε as a valid response if set
	 */
	public HunspellRegexWordGenerator(String regex, boolean ignoreEmptyWord){
		Objects.requireNonNull(regex);

		regex = StringUtils.replaceEach(requote(regex),
			PREDEFINED_CHARACTER_CLASSES.keySet().toArray(new String[PREDEFINED_CHARACTER_CLASSES.size()]),
			PREDEFINED_CHARACTER_CLASSES.values().toArray(new String[PREDEFINED_CHARACTER_CLASSES.size()]));
		automaton = PatternService.automaton(regex);
		this.ignoreEmptyWord = ignoreEmptyWord;
	}

	/**
	 * Requote a regular expression by escaping some parts of it from generation without need to escape each special
	 * character one by one. <br> this is done by setting the part to be interpreted as normal characters (thus, quote
	 * all meta-characters) between \Q and \E , ex : <br> <code> minion_\d{3}\Q@gru.evil\E </code> <br> will be
	 * transformed to : <br> <code> minion_\d{3}\@gru\.evil </code>
	 */
	private String requote(String regex){
		//http://stackoverflow.com/questions/399078/what-special-characters-must-be-escaped-in-regular-expressions
		//adding "@" prevents StackOverflowError inside generex: https://github.com/mifmif/Generex/issues/21
		StringBuilder sb = new StringBuilder(regex);
		Matcher matcher = MATCHER_REQUOTE.reset(regex);
		while(matcher.find())
			sb.replace(matcher.start(), matcher.end(), MATCHER_REQUOTE_SPECIAL_CHARS.reset(matcher.group(1)).replaceAll("\\\\$0"));
		return sb.toString();
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
	 * @see <a href="https://cs.stackexchange.com/questions/71371/why-isnt-it-simple-to-count-the-number-of-words-in-a-regular-language">Why isn't it simple to count the number of words in a regular language?</a>
	 * @see <a href="https://cs.stackexchange.com/questions/1045/number-of-words-of-a-given-length-in-a-regular-language">Number of words of a given length in a regular language</a>
	 * @see <a href="https://cstheory.stackexchange.com/questions/8200/counting-words-accepted-by-a-regular-grammar">Counting words accepted by a regular grammar</a>
	 * 
	 * @return	The number of words that are matched by the given pattern, or {@value #INFINITY} if infinite.
	 */
	public long wordCount(){
		long count = 0l;
		try{
			if(!isInfinite()){
				buildRootNode();

				count = rootNode.getMatchedWordCount();

				if(ignoreEmptyWord && automaton.getInitialState().isAccept())
					count --;
			}
		}
		catch(StackOverflowError e){}
		return count;
	}

	/**
	 * Build list of nodes that represent all the possible transactions from the given <code>state</code>.
	 */
	private void buildRootNode(){
		if(rootNode == null){
			Queue<StateNodeElement> queue = new LinkedList<>();
			rootNode = new HunspellAutomataNode();
			rootNode.setTransitionCount(1);
			queue.add(new StateNodeElement(automaton.getInitialState(), rootNode));
			while(!queue.isEmpty()){
				StateNodeElement elem = queue.remove();
				State state = elem.state;
				HunspellAutomataNode node = elem.node;

				List<HunspellAutomataNode> transactionNodes = new ArrayList<>();
				node.setNextNodes(transactionNodes);

				if(state.isAccept()){
					HunspellAutomataNode acceptedNode = new HunspellAutomataNode();
					acceptedNode.setTransitionCount(1);
					transactionNodes.add(acceptedNode);
				}
				List<Transition> transitions = state.getSortedTransitions(true);
				for(Transition transition : transitions){
					HunspellAutomataNode tn = new HunspellAutomataNode();
					int transitionsCount = transition.getMax() - transition.getMin() + 1;
					tn.setTransitionCount(transitionsCount);
					transactionNodes.add(tn);

					queue.add(new StateNodeElement(transition.getDest(), tn));
				}
			}

			rootNode.updateMatchedWordCount();
		}
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

	private String generate(State initialState, Function<Integer, Integer> fnTransition,
			BiFunction<Character, Character, Character> fnCharIntoTransition){
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
	 * Generate a subList with a maximum size of <code>limit</code> of words that matches the given regex.
	 * <p>
	 * The Strings are ordered in lexicographical order.
	 *
	 * @param limit	The maximum size of the list
	 * @return	The list of words that matcher the given regex
	 */
	public List<String> generateAll(int limit){
		List<String> matchedWords = new ArrayList<>(0);
		int matchedWordCounter = 0;

		Queue<GeneratedElement> queue = new LinkedList<>();
		queue.add(new GeneratedElement(StringUtils.EMPTY, automaton.getInitialState()));
		while(!queue.isEmpty()){
			if(matchedWordCounter == limit)
				break;

			GeneratedElement elem = queue.remove();
			String subword = elem.word;
			State state = elem.state;
			List<Transition> transitions = state.getSortedTransitions(false);
			boolean emptyTransitions = transitions.isEmpty();
			if((!ignoreEmptyWord || !subword.isEmpty()) && (emptyTransitions || state.isAccept())){
				matchedWords.add(subword);
				matchedWordCounter ++;

				if(emptyTransitions)
					continue;
			}

			for(Transition transition : transitions)
				for(char chr = transition.getMin(); chr <= transition.getMax(); chr ++)
					queue.add(new GeneratedElement(subword + chr, transition.getDest()));
		}

		return matchedWords;
	}

}
