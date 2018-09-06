package unit731.hunspeller.services.regexgenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;


/**
 * A class that help generating words that match a given regular expression.
 * <p>
 * It does not support all valid Java regular expressions, but only '(', ')', '*' and '?'.
 * </p>
 *
 * @see <a href="https://github.com/mifmif/Generex">Generex</a>
 * @see <a href="https://github.com/bluezio/xeger">Xeger</a>
 * @see <a href="http://www.brics.dk/automaton/index.html">dk.brics.automaton</a>
 * 
 * TODO https://github.com/zhztheplayer/DFA-Regex
 */
public class HunspellRegexWordGenerator{

	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	private static class State{

		private final String word;
		private Modifier modifier;
		private State nextState;

		private State(State nextState){
			word = StringUtils.EMPTY;
			modifier = Modifier.ONE;
			this.nextState = nextState;
		}

		public boolean isAccept(){
			return (nextState == null);
		}

		public State[] getTransitions(){
			State[] result;
			switch(modifier){
				case ONE:
					result = new State[]{};
					break;

				case ZERO_OR_ONE:
					result = new State[]{new State(nextState)};
					break;

				case ANY:
				default:
					result = new State[]{new State(nextState), this};
			}
			if(nextState != null)
				result = ArrayUtils.addAll(result, nextState);
			return result;
		}

	}

	private static enum Modifier{
		ONE, ZERO_OR_ONE, ANY;
	};

	@AllArgsConstructor
	private static class GeneratedElement{
		private final String word;
		private final State state;
	}


	private final List<State> automaton;


	/**
	 * @param regex	Regex used to generate the set (each word should be enclosed by parentheses, eg. (abc)*(ed)?(fff))
	 */
	public HunspellRegexWordGenerator(String regex){
		Objects.requireNonNull(regex);

		String[] parts = StringUtils.split(regex, "()");
		int size = 0;
		automaton = new ArrayList<>(parts.length);
		for(String part : parts){
			char last = part.charAt(0);
			if(part.length() == 1 && (last == '?' || last == '*'))
				automaton.get(size - 1).modifier = (last == '?'? Modifier.ZERO_OR_ONE: Modifier.ANY);
			else{
				State newState = new State(part, Modifier.ONE, null);
				if(size > 0)
					automaton.get(size - 1).nextState = newState;
				automaton.add(newState);
				size ++;
			}
		}
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
		List<String> matchedWords = new ArrayList<>(limit);
		int matchedWordCounter = 0;

		Queue<GeneratedElement> queue = new LinkedList<>();
		queue.add(new GeneratedElement(automaton.get(0).word, automaton.get(0)));
		while(!queue.isEmpty()){
			GeneratedElement elem = queue.remove();
			String subword = elem.word;
			State state = elem.state;
			State[] transitions = state.getTransitions();
			if(!subword.isEmpty() && state.isAccept()){
				matchedWords.add(subword);

				matchedWordCounter ++;
				if(matchedWordCounter == limit)
					break;
			}

			for(State transition : transitions)
				if(transition.nextState != null)
					queue.add(new GeneratedElement(subword + transition.word, transition.nextState));
		}

		return matchedWords;
	}

}
