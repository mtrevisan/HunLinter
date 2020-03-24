package unit731.hunlinter.languages;

import java.text.MessageFormat;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import unit731.hunlinter.parsers.vos.Inflection;
import unit731.hunlinter.workers.exceptions.LinterException;


public class LetterMatcherEntry{

	private final MessageFormat messagePattern;
	private final String masterLetter;
	private final String[] wrongFlags;
	private final String correctRule;


	public LetterMatcherEntry(final MessageFormat messagePattern, final String masterLetter, final String[] wrongFlags,
			final String correctRule){
		this.messagePattern = messagePattern;
		this.masterLetter = masterLetter;
		this.wrongFlags = wrongFlags;
		this.correctRule = correctRule;
	}

	public void match(final Inflection inflection){
		for(final String flag : wrongFlags)
			if(inflection.hasContinuationFlag(flag))
				throw new LinterException(messagePattern.format(new Object[]{masterLetter, flag, correctRule}));
	}


	@Override
	public boolean equals(final Object obj){
		if(obj == this)
			return true;
		if(obj == null || obj.getClass() != getClass())
			return false;

		final LetterMatcherEntry rhs = (LetterMatcherEntry)obj;
		return new EqualsBuilder()
			.append(messagePattern, rhs.messagePattern)
			.append(masterLetter, rhs.masterLetter)
			.append(wrongFlags, rhs.wrongFlags)
			.append(correctRule, rhs.correctRule)
			.isEquals();
	}

	@Override
	public int hashCode(){
		return new HashCodeBuilder()
			.append(messagePattern)
			.append(masterLetter)
			.append(wrongFlags)
			.append(correctRule)
			.toHashCode();
	}

}
