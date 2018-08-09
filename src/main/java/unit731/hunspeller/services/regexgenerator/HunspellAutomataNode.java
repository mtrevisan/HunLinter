package unit731.hunspeller.services.regexgenerator;

import java.util.List;
import lombok.Getter;
import lombok.Setter;


/**
 * Node class is used here to present a position in the Automata state.
 * <p>
 * Each node has a {@size} that present the number of possible characters that could
 * be used to go to the next possible position, and a list of node that present
 * the next positions.
 */
public class HunspellAutomataNode{

	@Setter
	private int transitionCount;
	@Getter
	private long matchedWordCount;
	@Setter
	private List<HunspellAutomataNode> nextNodes;
	private boolean matchedWordCountUpdated;


	/**
	 * Calculate the number of string that will be generated until the
	 * transaction presented by this node, and set the result in
	 * <code>nbrMatchedString</code>.
	 */
	public void updateMatchedWordCount(){
		if(matchedWordCountUpdated)
			return;

		if(nextNodes == null || nextNodes.isEmpty())
			matchedWordCount = transitionCount;
		else
			for(HunspellAutomataNode childNode : nextNodes){
				childNode.updateMatchedWordCount();
				matchedWordCount += transitionCount * childNode.matchedWordCount;
			}
		matchedWordCountUpdated = true;
	}

}
