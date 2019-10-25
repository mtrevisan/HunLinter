package unit731.hunspeller.parsers.autocorrect;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.text.StringEscapeUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class CorrectionEntry implements Comparable<CorrectionEntry>{

	private String incorrectForm;
	private String correctForm;


	public CorrectionEntry(final String incorrectForm, final String correctForm){
		Objects.requireNonNull(incorrectForm);
		Objects.requireNonNull(correctForm);

		this.incorrectForm = incorrectForm;
		this.correctForm = correctForm;
	}

	public String getIncorrectForm(){
		return incorrectForm;
	}

	public String getEscapedIncorrectForm(){
		return escape(incorrectForm);
	}

	public String getCorrectForm(){
		return correctForm;
	}

	public String getEscapedCorrectForm(){
		return escape(correctForm);
	}

	private String escape(final String text){
		String result = StringUtils.EMPTY;
		for(final char chr : text.toCharArray()){
			String str = (chr > 127?
				"&#x" + StringUtils.leftPad(Integer.toHexString(chr), 4, '0') + ";":
				String.valueOf(chr));
			result += str;
		}
		return result;

//		final byte[] as = text.getBytes(StandardCharsets.UTF_8);
//		String a = new String(as, StandardCharsets.UTF_8);
//		String e = StringEscapeUtils.escapeJava(text);
//		return e;

//		return text.chars()
//			.mapToObj(chr -> (char)chr)
//			.map(chr -> (chr > 127?
//				"\\u" + StringUtils.leftPad(Integer.toHexString(chr), 4, '0'):
//				String.valueOf(chr)))
//			.collect(Collectors.joining(StringUtils.EMPTY));
	}

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
