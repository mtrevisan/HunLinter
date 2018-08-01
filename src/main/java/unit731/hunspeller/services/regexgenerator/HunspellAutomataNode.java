package unit731.hunspeller.services.regexgenerator;

import java.util.ArrayList;
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

	private int charCount = 1;
	@Getter
	private long matchedWordCount = 0l;
	@Setter
	private List<HunspellAutomataNode> nextNodes = new ArrayList<>();
	private boolean matchedWordCountUpdated;


	/**
	 * Calculate the number of string that will be generated until the
	 * transaction presented by this node, and set the result in
	 * <code>nbrMatchedString</code>.
	 */
	public void updateMatchedWordCount(){
		if(matchedWordCountUpdated)
			return;

		if(nextNodes.isEmpty())
			matchedWordCount = charCount;
		else
			for(HunspellAutomataNode childNode : nextNodes){
				childNode.updateMatchedWordCount();
				matchedWordCount += charCount * childNode.matchedWordCount;
			}
		matchedWordCountUpdated = true;
	}

	public void setCharCount(int charCount){
		this.charCount = charCount;
	}

}
