package unit731.hunspeller.parsers.dictionary.dtos;

import java.util.Objects;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunspeller.parsers.vos.Production;


public class Duplicate{

	private final Production production;
	private final String word;
	private final int lineIndex;


	public Duplicate(final Production production, final String word, final int lineIndex){
		Objects.requireNonNull(production);
		Objects.requireNonNull(word);

		this.production = production;
		this.word = word;
		this.lineIndex = lineIndex;
	}

	public Production getProduction(){
		return production;
	}

	public String getWord(){
		return word;
	}

	public int getLineIndex(){
		return lineIndex;
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final Duplicate rhs = (Duplicate)obj;
		return new EqualsBuilder()
			.append(lineIndex, rhs.lineIndex)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(lineIndex)
			.toHashCode();
	}

}
