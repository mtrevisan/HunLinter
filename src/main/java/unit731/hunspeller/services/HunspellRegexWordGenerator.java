package unit731.hunspeller.services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import lombok.AccessLevel;
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
 * 
 * TODO https://github.com/zhztheplayer/DFA-Regex
 */
public class HunspellRegexWordGenerator{

	private static final int EPSILON_TRANSITION = -1;


	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	private static class State{

		private final String word;
		private Modifier modifier;
		private final int index;

		private State(){
			word = StringUtils.EMPTY;
			modifier = Modifier.ONE;
			index = EPSILON_TRANSITION;
		}

		public boolean isAccept(){
			return (index == EPSILON_TRANSITION);
		}

		public int[] getTransitions(){
			int[] result;
			switch(modifier){
				case ONE:
					result = new int[]{index + 1};
					break;

				case ZERO_OR_ONE:
					result = new int[]{EPSILON_TRANSITION, index};
					break;

				case ANY:
				default:
					result = new int[]{EPSILON_TRANSITION, index, index + 1};
			}
			return result;
		}

	}

	private static final State EMPTY_STATE = new State(StringUtils.EMPTY, Modifier.ONE, EPSILON_TRANSITION);

	private static enum Modifier{
		ONE, ZERO_OR_ONE, ANY;
	};

	@AllArgsConstructor
	private static class GeneratedElement{
		private final String word;
		private final int stateIndex;
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
				automaton.add(new State(part, Modifier.ONE, EPSILON_TRANSITION));
				size ++;
			}
		}
	}

	/**
	 * Generate a subList with a maximum size of <code>limit</code> of words that matches the given regex.
	 * <p>
	 * The Strings are ordered in lexicographical order.
	 * 
	 * https://cs.stackexchange.com/questions/40819/how-to-create-dfa-from-regular-expression-without-using-nfa
	 * https://cstheory.stackexchange.com/questions/14939/what-algorithms-exist-for-construction-a-dfa-that-recognizes-the-language-descri/14946#14946
	 * https://medium.com/@DmitrySoshnikov/building-a-regexp-machine-part-2-finite-automata-nfa-fragments-5a7c5c005ef0
	 * https://swtch.com/~rsc/regexp/regexp1.html
	 * https://www.tutorialspoint.com/automata_theory/constructing_fa_from_re.htm
	 * https://core.ac.uk/download/pdf/82527579.pdf
	 * https://www.cse.cuhk.edu.hk/~siuon/csci3130-f16/slides/lec03.pdf
	 * https://lambda.uta.edu/cse5317/spring01/notes/node9.html
	 * https://github.com/alirezakay/RegexToDFA
	 * https://github.com/felipemoura/RegularExpression-to-NFA-to-DFA
	 * http://matt.might.net/articles/implementation-of-nfas-and-regular-expressions-in-java/
	 * http://www-igm.univ-mlv.fr/~berstel/Lothaire/AppliedCW/ProgrammesJava/BibliJava/NFA.java
	 * http://www.vesalainen.org/javalpg
	 *
	 * @param limit	The maximum size of the list
	 * @return	The list of words that matcher the given regex
	 */
	public List<String> generateAll(int limit){
		List<String> matchedWords = new ArrayList<>(limit);
		int matchedWordCounter = 0;

		Queue<GeneratedElement> queue = new LinkedList<>();
		queue.add(new GeneratedElement(automaton.get(0).word, 0));
		while(!queue.isEmpty()){
			GeneratedElement elem = queue.remove();
			String subword = elem.word;
			State state = automaton.get(elem.stateIndex);

			int[] transitions = state.getTransitions();
			if(!subword.isEmpty() && state.isAccept()){
				matchedWords.add(subword);

				matchedWordCounter ++;
				if(matchedWordCounter == limit)
					break;
			}

			for(int transition : transitions)
				if(0 <= transition && transition < automaton.size())
					queue.add(new GeneratedElement(subword + automaton.get(transition).word, transition));
		}

		return matchedWords;
	}

}
