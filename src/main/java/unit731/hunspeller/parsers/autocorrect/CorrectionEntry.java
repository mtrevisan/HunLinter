package unit731.hunspeller.parsers.autocorrect;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.text.StringEscapeUtils;

import java.util.Objects;


public class CorrectionEntry implements Comparable<CorrectionEntry>{

	private final String incorrectForm;
	private final String correctForm;


	public CorrectionEntry(final String incorrectForm, final String correctForm){
		Objects.requireNonNull(incorrectForm);
		Objects.requireNonNull(correctForm);

		this.incorrectForm = incorrectForm;
		this.correctForm = correctForm;
	}

	public String getIncorrectForm(){
		return incorrectForm;
	}

//	public String getEscapedIncorrectForm(){
//		return escape(incorrectForm);
//	}

	public String getCorrectForm(){
		return correctForm;
	}

//	public String getEscapedCorrectForm(){
//		return escape(correctForm);
//	}

	/** Escape HTML entities as Unicode Hex */
//	private String escape(final String text){
//		return StringEscapeUtils.escapeHtml4(text);
//	}

	@Override
	public String toString(){
		return ("\"" + incorrectForm + " -> " + correctForm + "\"");
	}

	@Override
	public int compareTo(final CorrectionEntry other){
		return new CompareToBuilder()
			.append(incorrectForm, other.incorrectForm)
			.append(correctForm, other.correctForm)
			.toComparison();
	}

	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final CorrectionEntry rhs = (CorrectionEntry)obj;
		return new EqualsBuilder()
			.append(incorrectForm, rhs.incorrectForm)
			.append(correctForm, rhs.correctForm)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(incorrectForm)
			.append(correctForm)
			.toHashCode();
	}

}
