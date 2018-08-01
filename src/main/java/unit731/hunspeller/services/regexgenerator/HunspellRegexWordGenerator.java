package unit731.hunspeller.services.regexgenerator;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
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

	@Getter
	private final List<String> matchedWords = new ArrayList<>(0);
	private int matchedWordCounter = 0;

	private HunspellAutomataNode rootNode;
	private boolean isTransactionNodeBuilt;


	public HunspellRegexWordGenerator(String regex){
		Objects.requireNonNull(regex);

		regex = StringUtils.replaceEachRepeatedly(requote(regex),
			PREDEFINED_CHARACTER_CLASSES.keySet().toArray(new String[PREDEFINED_CHARACTER_CLASSES.size()]),
			PREDEFINED_CHARACTER_CLASSES.values().toArray(new String[PREDEFINED_CHARACTER_CLASSES.size()]));
		RegExp re = new RegExp(regex);
		automaton = re.toAutomaton();
	}

	/**
	 * Requote a regular expression by escaping some parts of it from generation without need to escape each special
	 * character one by one. <br> this is done by setting the part to be interpreted as normal characters (thus, quote
	 * all meta-characters) between \Q and \E , ex : <br> <code> minion_\d{3}\Q@gru.evil\E </code> <br> will be
	 * transformed to : <br> <code> minion_\d{3}\@gru\.evil </code>
	 *
	 * @param regex
	 * @return
	 */
	private String requote(String regex){
		//http://stackoverflow.com/questions/399078/what-special-characters-must-be-escaped-in-regular-expressions
		//adding "@" prevents StackOverflowError inside generex: https://github.com/mifmif/Generex/issues/21
		Pattern patternSpecial = Pattern.compile("[.^$*+?(){|\\[\\\\@]");
		StringBuilder sb = new StringBuilder(regex);
		Matcher matcher = Pattern.compile("\\\\Q(.*?)\\\\E")
			.matcher(sb);
		while(matcher.find()){
			sb.replace(matcher.start(), matcher.end(), patternSpecial.matcher(matcher.group(1)).replaceAll("\\\\$0"));
			//matcher.reset();
		}
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
	 * @return	The number of words that are matched by the given pattern.
	 * @throws StackOverflowError	If the given pattern generates a large, possibly infinite, number of words.
	 */
	public long wordCount(){
		if(isInfinite())
			throw new StackOverflowError();

		buildRootNode();

		return rootNode.getMatchedWordCount();
	}

	/** Prepare the rootNode and it's child nodes so that we can get matchedString by index */
	private void buildRootNode(){
		if(isTransactionNodeBuilt)
			return;

		isTransactionNodeBuilt = true;

		rootNode = new HunspellAutomataNode();
		rootNode.setCharCount(1);
		List<HunspellAutomataNode> nextNodes = prepareTransactionNodes(automaton.getInitialState());
		rootNode.setNextNodes(nextNodes);
		rootNode.updateMatchedWordCount();
	}

	private int preparedTransactionNode;

	/**
	 * Build list of nodes that present possible transactions from the <code>state</code>.
	 *
	 * @param state
	 * @return
	 */
	private List<HunspellAutomataNode> prepareTransactionNodes(State state){
		List<HunspellAutomataNode> transactionNodes = new ArrayList<>();
		if(preparedTransactionNode == Integer.MAX_VALUE / 2)
			return transactionNodes;

		preparedTransactionNode ++;

		if(state.isAccept()){
			HunspellAutomataNode acceptedNode = new HunspellAutomataNode();
			acceptedNode.setCharCount(1);
			transactionNodes.add(acceptedNode);
		}
		List<Transition> transitions = state.getSortedTransitions(true);
		for(Transition transition : transitions){
			HunspellAutomataNode trsNode = new HunspellAutomataNode();
			int nbrChar = transition.getMax() - transition.getMin() + 1;
			trsNode.setCharCount(nbrChar);
			List<HunspellAutomataNode> nextNodes = prepareTransactionNodes(transition.getDest());
			trsNode.setNextNodes(nextNodes);
			transactionNodes.add(trsNode);
		}
		return transactionNodes;
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
	 * Generate all strings that matches the given regex.
	 *
	 * @return	All the words that will be matcher by the given regex
	 */
	public List<String> generateAll(){
		return generateAll(Integer.MAX_VALUE);
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
		matchedWords.clear();
		matchedWordCounter = 0;
		generate(StringUtils.EMPTY, automaton.getInitialState(), limit);
		return matchedWords;
	}

	private void generate(String subword, State state, int limit){
		if(matchedWordCounter == limit)
			return;

		List<Transition> transitions = state.getSortedTransitions(true);
		if(transitions.isEmpty() || state.isAccept()){
			matchedWords.add(subword);
			matchedWordCounter ++;

			if(transitions.isEmpty())
				return;
		}

		for(Transition transition : transitions)
			for(char chr = transition.getMin(); chr <= transition.getMax(); chr ++)
				generate(subword + chr, transition.getDest(), limit);
	}


	@Override
	public Iterator<String> iterator(){
		Iterator<String> itr = new Iterator<String>(){
			private boolean found;
			private final Deque<IterationStep> steps = new ArrayDeque<>();
			private final StringBuilder sb = new StringBuilder();

			@Override
			public boolean hasNext(){
				if(found)
					return true;
				if(steps.isEmpty())
					return false;

				nextImpl();
				return found;
			}

			private void nextImpl(){
				IterationStep currentStep;
				while(!steps.isEmpty() && !found){
					currentStep = steps.pop();
					found = currentStep.build(sb, steps);
				}
			}

			@Override
			public String next(){
				if(!found)
					nextImpl();

				if(!found)
					throw new IllegalStateException();

				found = false;
				return sb.toString();
			}
		};

		return itr;
	}

	/**
	 * A step, in the iteration process, to build a string using {@code State}s.
	 * <p>
	 * It's responsible to keep the information of a {@code State}, like current char and transitions that need to be followed.
	 * Also it adds (and removes) the characters while iterating over the characters (when the state has a range) and
	 * transitions.
	 * <p>
	 * Implementation based on {@code SpecialOperations.getFiniteStrings(Automaton,int)}, but in a non-recursive way to avoid
	 * {@code StackOverflowError}s.
	 *
	 * @see State
	 * @see dk.brics.automaton.SpecialOperations#getFiniteStrings(dk.brics.automaton.Automaton,int)
	 */
	private static class IterationStep{

		private final Iterator<Transition> iteratorTransitions;
		private Transition currentTransition;
		private char currentChar;

		public IterationStep(State state){
			iteratorTransitions = state.getSortedTransitions(true).iterator();
		}

		public boolean build(StringBuilder stringBuilder, Deque<IterationStep> steps){
			if(hasCurrentTransition())
				currentChar ++;
			else if(!moveToNextTransition()){
				removeLastChar(stringBuilder);

				return false;
			}

			if(currentChar <= currentTransition.getMax()){
				stringBuilder.append(currentChar);
				if(currentTransition.getDest().isAccept()){
					pushForDestinationOfCurrentTransition(steps);
					if(currentChar >= currentTransition.getMax())
						currentTransition = null;

					return true;
				}
				pushForDestinationOfCurrentTransition(steps);

				return false;
			}
			steps.push(this);
			currentTransition = null;

			return false;
		}

		private boolean hasCurrentTransition(){
			return (currentTransition != null);
		}

		private boolean moveToNextTransition(){
			if(!iteratorTransitions.hasNext())
				return false;

			currentTransition = iteratorTransitions.next();
			currentChar = currentTransition.getMin();
			return true;
		}

		private static void removeLastChar(StringBuilder sb){
			int len = sb.length();
			if(len > 0)
				sb.deleteCharAt(len - 1);
		}

		private void pushForDestinationOfCurrentTransition(Deque<IterationStep> steps){
			steps.push(this);
			steps.push(new IterationStep(currentTransition.getDest()));
		}

	}

}
