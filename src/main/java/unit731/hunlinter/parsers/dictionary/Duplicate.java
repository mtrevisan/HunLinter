package unit731.hunlinter.parsers.dictionary;

import java.util.Objects;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunlinter.parsers.vos.Inflection;


public class Duplicate{

	private final Inflection inflection;
	private final String word;
	private final int lineIndex;


	public Duplicate(final Inflection inflection, final String word, final int lineIndex){
		Objects.requireNonNull(inflection);
		Objects.requireNonNull(word);

		this.inflection = inflection;
		this.word = word;
		this.lineIndex = lineIndex;
	}

	public Inflection getInflection(){
		return inflection;
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
