package unit731.hunspeller.parsers.dictionary.valueobjects;

import java.util.Objects;
import java.util.regex.Matcher;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.parsers.affix.strategies.FlagParsingStrategy;
import unit731.hunspeller.services.PatternService;


@EqualsAndHashCode(of = "entry")
public class AffixEntry{

	public static final String SLASH = "/";
	private static final Matcher MATCHER_ENTRY = PatternService.matcher("\t.*$");

	private static final String DOT = ".";
	private static final String ZERO = "0";


	@AllArgsConstructor
	public static enum Type{
		SUFFIX("SFX"),
		PREFIX("PFX");


		@Getter
		private final String flag;

		public static Type toEnum(String flag){
			Type[] types = Type.values();
			for(Type type : types)
				if(type.getFlag().equals(flag))
					return type;
			return null;
		}
	}


	@Getter
	private final Type type;
	/** ID used to represent the affix */
	@Getter
	private final String flag;
	@Getter
	private final String[] continuationFlags;
	/** condition that must be met before the affix can be applied */
	@Getter
	private final AffixCondition condition;
	/** string to strip */
//	private final String removing;
	private final int removeLength;
	/** string to append */
	private final String appending;
	@Getter
	private final String[] morphologicalFields;

	private final String entry;


	public AffixEntry(String line, FlagParsingStrategy strategy){
		Objects.requireNonNull(line);
		Objects.requireNonNull(strategy);

		String[] lineParts = StringUtils.split(line, null, 6);
		String ruleType = lineParts[0];
		this.flag = lineParts[1];
		String removal = lineParts[2];
		String[] additionParts = StringUtils.split(lineParts[3], SLASH);
		String addition = additionParts[0];
		String cond = (lineParts.length > 4? lineParts[4]: DOT);
		morphologicalFields = (lineParts.length > 5? StringUtils.split(lineParts[5]): new String[0]);

		type = Type.toEnum(ruleType);
		String[] classes = strategy.parseFlags((additionParts.length > 1? additionParts[1]: null));
		continuationFlags = (classes.length > 0? classes: null);
		condition = new AffixCondition(cond, type);
		removeLength = (!ZERO.equals(removal)? removal.length(): 0);
//		removing = (!ZERO.equals(removal)? removal: StringUtils.EMPTY);
		appending = (!ZERO.equals(addition)? addition: StringUtils.EMPTY);

		if(removeLength > 0){
			if(isSuffix()){
				if(!cond.endsWith(removal))
					throw new IllegalArgumentException("This line has the condition part that not ends with the removal part: " + line);
				if(appending.length() > 1 && removal.charAt(0) == appending.charAt(0))
					throw new IllegalArgumentException("This line has characters in common between removed and added part: " + line);
			}
			else{
				if(!cond.startsWith(removal))
					throw new IllegalArgumentException("This line has the condition part that not starts with the removal part: " + line);
				if(appending.length() > 1 && removal.charAt(removal.length() - 1) == appending.charAt(appending.length() - 1))
					throw new IllegalArgumentException("This line has characters in common between removed and added part: " + line);
			}
		}

		entry = PatternService.clear(line, MATCHER_ENTRY);
	}

	public final boolean isSuffix(){
		return (type == Type.SUFFIX);
	}

	public boolean match(String word){
		return condition.match(word, type);
	}

	public String applyRule(String word, boolean isFullstrip) throws IllegalArgumentException{
		if(!isFullstrip && word.length() == removeLength)
			throw new IllegalArgumentException("Cannot strip full words without the flag FULLSTRIP");

		return (isSuffix()? word.substring(0, word.length() - removeLength) + appending: appending + word.substring(removeLength));
	}

//	public String undoRule(String word) throws IllegalArgumentException{
//		int stripLength = appending.length();
//		return (isSuffix()? word.substring(0, word.length() - stripLength) + removing: removing + word.substring(stripLength));
//	}

	@Override
	public String toString(){
		return entry;
	}

}
