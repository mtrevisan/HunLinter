package unit731.hunspeller.parsers.hyphenation;

import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class Hyphenation{

	private final List<String> syllabes;
	private final List<String> compounds;
	private final List<String> rules;
	private final String breakCharacter;


	public Hyphenation(final List<String> syllabes, final List<String> compounds, final List<String> rules, final String breakCharacter){
		Objects.requireNonNull(syllabes);
		Objects.requireNonNull(compounds);
		Objects.requireNonNull(rules);
		Objects.requireNonNull(breakCharacter);

		this.syllabes = syllabes;
		this.compounds = compounds;
		this.rules = rules;
		this.breakCharacter = breakCharacter;
	}

	public List<String> getSyllabes(){
		return syllabes;
	}

	public List<String> getCompounds(){
		return compounds;
	}

	public List<String> getRules(){
		return rules;
	}

	public String getBreakCharacter(){
		return breakCharacter;
	}

	/**
	 * @param idx	Index with respect to the word from which to extract the index of the corresponding syllabe
	 * @return the (relative) index of the syllabe at the given (global) index
	 */
	public int getSyllabeIndex(int idx){
		int k = -1;
		final int size = countSyllabes();
		for(int i = 0; i < size; i ++){
			final String syllabe = syllabes.get(i);
			idx -= syllabe.length();
			if(idx < 0){
				k = i;
				break;
			}
		}
		return k;
	}

	/**
	 * @param idx	Index with respect to the word from which to extract the index of the corresponding syllabe
	 * @return the syllabe at the given (global) index
	 */
	public String getSyllabe(final int idx){
		return syllabes.get(getSyllabeIndex(idx));
	}

	public int countSyllabes(){
		return syllabes.size();
	}

	/**
	 * @param idx	Index of syllabe to extract, if negative then it's relative to the last syllabe
	 * @return the syllabe at the given (relative) index
	 */
	public String getAt(final int idx){
		return syllabes.get(restoreRelativeIndex(idx));
	}

	private int restoreRelativeIndex(final int idx){
		return (idx + countSyllabes()) % countSyllabes();
	}

	public boolean isHyphenated(){
		return !rules.isEmpty();
	}

	public boolean isCompound(){
		return (compounds.size() > 1);
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final Hyphenation rhs = (Hyphenation)obj;
		return new EqualsBuilder()
			.append(syllabes, rhs.syllabes)
			.append(breakCharacter, rhs.breakCharacter)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(syllabes)
			.append(breakCharacter)
			.toHashCode();
	}

}
