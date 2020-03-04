package unit731.hunlinter.languages;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.PatternHelper;
import unit731.hunlinter.services.text.StringHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Tokenizes a sentence into words.
 * Punctuation and whitespace gets their own tokens.
 * The tokenizer is a quite simple character-based one, though it knows about urls and will put them in one token,
 * if fully specified including a protocol (like {@code http://foobar.org}).
 *
 * @see <a href="https://rgxdb.com/try">Regex DB</a>
 */
public class WordTokenizer{

	private static final Pattern PATTERN_PLACEHOLDER = PatternHelper.pattern("(\0*)");

	//@see <a href="https://www.ietf.org/rfc/rfc4648.txt">RFC-4648</a>
	private static final String BASE64 = "(?:[a-zA-Z0-9+\\/]{4})*(?:[a-zA-Z0-9+\\/]{3}=|[a-zA-Z0-9+\\/]{2}==|[a-zA-Z0-9+\\/]{1}===)";
//	private static final String SEMANTIC_VERSIONING = "[Vv]?(?:(?<major>(?:0|[1-9](?:(?:0|[1-9])+)*))[.](?<minor>(?:0|[1-9](?:(?:0|[1-9])+)*))[.](?<patch>(?:0|[1-9](?:(?:0|[1-9])+)*))(?:-(?<prerelease>(?:(?:(?:[A-Za-z]|-)(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)?|(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)(?:[A-Za-z]|-)(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)?)|(?:0|[1-9](?:(?:0|[1-9])+)*))(?:[.](?:(?:(?:[A-Za-z]|-)(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)?|(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)(?:[A-Za-z]|-)(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)?)|(?:0|[1-9](?:(?:0|[1-9])+)*)))*))?(?:[+](?<build>(?:(?:(?:[A-Za-z]|-)(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)?|(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)(?:[A-Za-z]|-)(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)?)|(?:(?:0|[1-9])+))(?:[.](?:(?:(?:[A-Za-z]|-)(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)?|(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)(?:[A-Za-z]|-)(?:(?:(?:0|[1-9])|(?:[A-Za-z]|-))+)?)|(?:(?:0|[1-9])+)))*))?)";
//	private static final String PHONE_NUMBER = "[+]?(?=(?:[^\\dx]*\\d){7})(?:\\(\\d+(?:\\.\\d+)?\\)|\\d+(?:\\.\\d+)?)(?:[ -]?(?:\\(\\d+(?:\\.\\d+)?\\)|\\d+(?:\\.\\d+)?))*(?:[ ]?(?:x|ext)\\.?[ ]?\\d{1,5})?";
	private static final String DATE_ISO8601 = "(?<!\\d)(?:[+-]?\\d{4}(?!\\d{2}\\b))(?:(-?)(?:(?:00[1-9]|0[1-9]\\d|[12]\\d{2}|3[0-5]\\d|36[0-6])|(?:3[0-6]\\d)|(?:[0-2]\\d{2})|(?:0[1-9]|1[0-2])(?:\\2(?:0[1-9]|[12]\\d|3[01]))?|(?:\\d{1,2})|W(?:[0-4]\\d|5[0-2])(?:-?[1-7])?)(?!\\d)(?:[T\\s](?:(?:(?:[01]\\d|2[0-3])(?:(:?)[0-5]\\d)?|24\\:?00)(?:[.,]\\d+(?!:))?)?(?:\\3[0-5]\\d(?:[.,]\\d+)?)?(?:[zZ]|(?:[+-])(?:[01]\\d|2[0-3]):?(?:[0-5]\\d)?)?)?)?";
	private static final String TIME = "(?<!\\d)(?:(?:0?[1-9]|1[0-2])(?::|\\.)[0-5]\\d(?:(?::|\\.)[0-5]\\d)? ?[aApP][mM])|(?:(?:0?\\d|1\\d|2[0-3])(?::|\\.)[0-5]\\d(?:(?::|\\.)[0-5]\\d)?)";
	//@see <a href="https://www.ietf.org/rfc/rfc0822.txt">RFC-0822</a>
	private static final String EMAIL = "([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x22([^\\x0d\\x22\\x5c\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x22)(\\x2e([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x22([^\\x0d\\x22\\x5c\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x22))*\\x40([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x5b([^\\x0d\\x5b-\\x5d\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x5d)(\\x2e([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x5b([^\\x0d\\x5b-\\x5d\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x5d))*";
	//@see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC-3986</a>
	private static final String URL = "(?:(?:[a-zA-Z][a-zA-Z\\d+-.]*):)(?:\\/\\/(?:(?:[^:]*)(?::(?:[^@]*))?@)?(?:(?:[a-zA-Z0-9-.%]+)|(?:\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|(?:\\[(?:[a-fA-F\\d.:]+)\\]))?(?::(?:\\d*))?(?:(?:\\/[^\\/]*)*)|(?:\\/[^\\#\\?]+(?:\\/[^\\#\\?]*)*)?|(?:[^\\/]+(?:\\/[^\\#\\?]*)*))?(?:\\?(?:[^#]*))?(?:\\#(?:[^#]*))?";
	private static final Pattern PATTERN_UNBREAKABLE;
	static{
		final StringJoiner sj = new StringJoiner("|");
		sj
			.add(BASE64)
//			.add(SEMANTIC_VERSIONING)
//			.add(PHONE_NUMBER)
			.add(DATE_ISO8601)
			.add(TIME)
			.add(EMAIL)
			.add(URL)
		;
		PATTERN_UNBREAKABLE = PatternHelper.pattern("(" + sj.toString() + ")");
	}

	public static final String DEFAULT_TOKENIZING_CHARACTERS = "\u0020\u00A0\u115f" +
		"\u1160\u1680"
		+ "\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007"
		+ "\u2008\u2009\u200A\u200B\u200c\u200d\u200e\u200f"
		+ "\u2028\u2029\u202a\u202b\u202c\u202d\u202e\u202f"
		+ "\u205F\u2060\u2061\u2062\u2063\u206A\u206b\u206c\u206d"
		+ "\u206E\u206F\u3000\u3164\ufeff\uffa0\ufff9\ufffa\ufffb"
		+ ",.;()[]{}=*#∗×·+÷<>!?:/|\\\"'«»„”“`´‘’‛′›‹…¿¡→‼⁇⁈⁉_"
		//em dash
		+ "—"
		+ "\t\n\r";

	private static final String HORIZONTAL_EXPANDED_ELLIPSIS = "...";
	private static final String HORIZONTAL_ELLIPSIS = "…";


	private final String tokenizingCharacters;


	public WordTokenizer(){
		this(DEFAULT_TOKENIZING_CHARACTERS);
	}

	public WordTokenizer(final String tokenizingCharacters){
		this.tokenizingCharacters = tokenizingCharacters;
	}

	public List<String> tokenize(String text){
		text = StringUtils.replace(text, HORIZONTAL_EXPANDED_ELLIPSIS, HORIZONTAL_ELLIPSIS);

		final String placeholder = StringUtils.repeat("\0", StringUtils.EMPTY,
			StringHelper.maxRepeating(text, '\0') + 1);

		//find all urls and emails, substitute with placeholder
		final List<String> unbreakableText = new ArrayList<>();
		text = PATTERN_UNBREAKABLE.matcher(text)
			.replaceAll(m -> {
				unbreakableText.add(m.group(1));
				return placeholder;
			});

		return extractTokens(text, placeholder, unbreakableText);
	}

	private List<String> extractTokens(final String text, final String placeholder, final List<String> unbreakableText){
		final List<String> result = new ArrayList<>();
		int index = 0;
		final StringTokenizer st = new StringTokenizer(text, tokenizingCharacters, true);
		while(st.hasMoreElements()){
			final String token = st.nextToken();

			//restore placeholders with original
			result.add(token.equals(placeholder)? unbreakableText.get(index ++): token);
		}
		return result;
	}

	/*
	 * NOTE: explicit check for {@code containingChars} speeds up method by factor of ~10
	 */
	protected List<String> join(final List<String> list, final Pattern pattern, final String containingChars,
			final Function<String, String> groupSubstituter){
		final String text = String.join(StringUtils.EMPTY, list);

		final List<String> result = new ArrayList<>();
		if((containingChars == null || StringUtils.containsAny(text, containingChars)) && pattern.matcher(text).find()){
			final Matcher matcher = pattern.matcher(text);
			int currentPosition = 0;
			int idx = 0;
			while(matcher.find()){
				final int start = matcher.start();
				final int end = matcher.end();
				while(currentPosition < end){
					if(currentPosition < start)
						result.add(list.get(idx));
					else if(currentPosition == start){
						final String group = matcher.group();
						result.add(groupSubstituter != null? groupSubstituter.apply(group): group);
					}

					currentPosition += list.get(idx)
						.length();
					idx ++;
				}
			}
			if(currentPosition < text.length())
				result.addAll(list.subList(idx, list.size()));
		}
		else
			result.addAll(list);
		return result;
	}

}
