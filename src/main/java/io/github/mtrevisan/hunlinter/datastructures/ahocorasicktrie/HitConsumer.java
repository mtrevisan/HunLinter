package io.github.mtrevisan.hunlinter.datastructures.ahocorasicktrie;


public interface HitConsumer{

	boolean apply(final int[] hits, final int index);

}
