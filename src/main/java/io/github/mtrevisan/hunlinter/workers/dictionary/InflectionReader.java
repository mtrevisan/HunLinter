package io.github.mtrevisan.hunlinter.workers.dictionary;

import io.github.mtrevisan.hunlinter.parsers.vos.Inflection;


@FunctionalInterface
public interface InflectionReader{

	void accept(final Inflection inflection, final int index);

}
