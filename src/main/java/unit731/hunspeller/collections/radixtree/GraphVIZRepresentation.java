package unit731.hunspeller.collections.radixtree;

import java.io.Serializable;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeNode;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeTraverser;


/**
 * @see <a href="http://www.webgraphviz.com/">WebGraphviz online</a>
 */
public class GraphVIZRepresentation{

	private static final String GRAPHVIZ_STYLE_BEGIN = " [";
	private static final String GRAPHVIZ_STYLE_END = "];";
	private static final String GRAPHVIZ_STYLE_STRING_BOUNDARY = "\"";
	private static final String GRAPHVIZ_ATTRIBUTE_SEPARATOR = ", ";
	private static final char GRAPHVIZ_TAB = '\t';
	private static final char GRAPHVIZ_NEW_LINE = '\n';
	private static final String GRAPHVIZ_STYLE_ARROW = " -> ";
	private static final String GRAPHVIZ_STYLE_LABEL = "label=";
	private static final String GRAPHVIZ_STYLE_SHAPE = "shape=";
	private static final String GRAPHVIZ_STYLE_FAILURE_TRANSITION = GRAPHVIZ_STYLE_BEGIN + "style=dashed, color=gray, constraint=false"
		+ GRAPHVIZ_STYLE_END;
	private static final String GRAPHVIZ_STYLE_STATE_WITHOUT_OUTPUT = GRAPHVIZ_STYLE_BEGIN + GRAPHVIZ_STYLE_SHAPE + "circle"
		+ GRAPHVIZ_ATTRIBUTE_SEPARATOR + GRAPHVIZ_STYLE_LABEL + GRAPHVIZ_STYLE_STRING_BOUNDARY + GRAPHVIZ_STYLE_STRING_BOUNDARY
		+ GRAPHVIZ_STYLE_END;
	private static final String GRAPHVIZ_STYLE_STATE_WITH_OUTPUT_PRE_LABEL = GRAPHVIZ_STYLE_BEGIN + GRAPHVIZ_STYLE_SHAPE + "doublecircle"
		+ GRAPHVIZ_ATTRIBUTE_SEPARATOR + GRAPHVIZ_STYLE_LABEL + GRAPHVIZ_STYLE_STRING_BOUNDARY;
	private static final String GRAPHVIZ_STYLE_STATE_WITH_OUTPUT_POST_LABEL = GRAPHVIZ_STYLE_STRING_BOUNDARY + GRAPHVIZ_STYLE_END;


	/**
	 * @see <a href="http://www.webgraphviz.com/">GraphVIZ</a>
	 * 
	 * @param tree	The tree for which to extract the Graphviz representation
	 * @param displayEdgesToInitialState	Whether to include the failure edges directing to the root node
	 * @return	The GraphVIZ representation of this tree
	 *
	 * @param <S>	The sequence/key type
	 * @param <V>	The type of values stored in the tree
	 */
	public <S, V extends Serializable> String generateGraphVIZRepresentation(RadixTree<S, V> tree, boolean displayEdgesToInitialState){
		StringBuffer sb = new StringBuffer(40);
		sb.append("digraph automaton{")
			.append(GRAPHVIZ_NEW_LINE)
			.append(GRAPHVIZ_TAB)
			.append("graph [rankdir=LR];")
			.append(GRAPHVIZ_NEW_LINE);

		RadixTreeTraverser<S, V> traverserNode = (wholeKey, node, parent) -> graphVIZAppendNode(sb, node);
		graphVIZAppendNode(sb, tree.root);
		tree.traverseBFS(traverserNode);

		RadixTreeTraverser<S, V> traverserForward = (wholeKey, node, parent) -> graphVIZAppendForwardTransition(sb, node, parent);
		tree.traverseBFS(traverserForward);

//		if(tree instanceof AhoCorasickTree){
//			RadixTreeTraverser<S, V> traverserFailure = (wholeKey, node, parent) -> graphVIZAppendFailureTransitions(tree, sb, node,
//				displayEdgesToInitialState);
//			tree.traverseBFS(traverserFailure);
//		}

		sb.append("}");
		return sb.toString();
	}

	private <S, V extends Serializable> void graphVIZAppendForwardTransition(StringBuffer sb, RadixTreeNode<S, V> node, RadixTreeNode<S, V> parent){
		sb.append(GRAPHVIZ_TAB)
			.append(System.identityHashCode(parent))
			.append(GRAPHVIZ_STYLE_ARROW)
			.append(System.identityHashCode(node))
			.append(GRAPHVIZ_STYLE_BEGIN)
			.append(GRAPHVIZ_STYLE_LABEL)
			.append(GRAPHVIZ_STYLE_STRING_BOUNDARY)
			.append(node.getKey())
			.append(GRAPHVIZ_STYLE_STRING_BOUNDARY)
			.append(GRAPHVIZ_STYLE_END)
			.append(GRAPHVIZ_NEW_LINE);
	}

	private <S, V extends Serializable> void graphVIZAppendFailureTransitions(RadixTree<S, V> tree, StringBuffer sb, RadixTreeNode<S, V> node,
			boolean displayEdgesToInitialState){
		if(displayEdgesToInitialState || node.getFailNode() != tree.root || node == tree.root)
			sb.append(GRAPHVIZ_TAB)
				.append(System.identityHashCode(node))
				.append(GRAPHVIZ_STYLE_ARROW)
				.append(System.identityHashCode(node.getFailNode()))
				.append(GRAPHVIZ_STYLE_FAILURE_TRANSITION)
				.append(GRAPHVIZ_NEW_LINE);
	}

	private <S, V extends Serializable> void graphVIZAppendNode(StringBuffer sb, RadixTreeNode<S, V> node){
		sb.append(GRAPHVIZ_TAB)
			.append(System.identityHashCode(node))
			.append(node.hasValue()? GRAPHVIZ_STYLE_STATE_WITH_OUTPUT_PRE_LABEL + node.getValue() + GRAPHVIZ_STYLE_STATE_WITH_OUTPUT_POST_LABEL:
				GRAPHVIZ_STYLE_STATE_WITHOUT_OUTPUT)
			.append(GRAPHVIZ_NEW_LINE);
	}
	
}
