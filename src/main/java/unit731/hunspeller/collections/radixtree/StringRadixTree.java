package unit731.hunspeller.collections.radixtree;

import unit731.hunspeller.collections.radixtree.tree.RadixTree;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringRadixTree<V extends Serializable> extends RadixTree<String, V>{

}
