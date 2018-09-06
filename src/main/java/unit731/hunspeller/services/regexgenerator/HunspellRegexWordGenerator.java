package unit731.hunspeller.services.regexgenerator;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import lombok.AllArgsConstructor;
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
 */
public class HunspellRegexWordGenerator{

	@AllArgsConstructor
	private static class GeneratedElement{
		private final String word;
		private final State state;
	}


	private final Automaton automaton;


	/**
	 * @param regex	Regex used to generate the set
	 */
	public HunspellRegexWordGenerator(String regex){
		Objects.requireNonNull(regex);

		RegExp re = new RegExp(regex);
		automaton = re.toAutomaton();
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
		queue.add(new GeneratedElement(StringUtils.EMPTY, automaton.getInitialState()));
		while(!queue.isEmpty()){
			GeneratedElement elem = queue.remove();
			String subword = elem.word;
			State state = elem.state;
			List<Transition> transitions = state.getSortedTransitions(false);
			boolean emptyTransitions = transitions.isEmpty();
			if(!subword.isEmpty() && (emptyTransitions || state.isAccept())){
				matchedWords.add(subword);

				matchedWordCounter ++;
				if(matchedWordCounter == limit)
					break;

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
