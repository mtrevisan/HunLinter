package unit731.hunlinter.services.fsa.stemming;

import java.util.List;


/**
 * A generic &quot;stemmer&quot; interface
 *
 * @see "org.carrot2.morfologik-parent, 2.1.8-SNAPSHOT, 2020-01-02"
 */
public interface IStemmer{

	/**
	 * Returns a list of {@link WordData} entries for a given word. The returned
	 * list is never <code>null</code>. Depending on the stemmer's
	 * implementation the {@link WordData} may carry the stem and additional
	 * information (tag) or just the stem.
	 * <p>
	 * The returned list and any object it contains are not usable after a
	 * subsequent call to this method. Any data that should be stored in between
	 * must be copied by the caller.
	 *
	 * @param word The word (typically inflected) to look up base forms for.
	 * @return A list of {@link WordData} entries (possibly empty).
	 */
	List<WordData> lookup(CharSequence word);

}
