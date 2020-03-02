package unit731.hunlinter.languages;

import org.apache.commons.lang3.StringUtils;
import unit731.hunlinter.services.PatternHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Tokenizes a sentence into words.
 * Punctuation and whitespace gets their own tokens.
 * The tokenizer is a quite simple character-based one, though it knows about urls and will put them in one token,
 * if fully specified including a protocol (like {@code http://foobar.org}).
 */
public class WordTokenizer{

	private static final String PLACEHOLDER = "\0\0\0";
	private static final List<String> PROTOCOLS = List.of("http", "https", "ftp", "sftp");
	private static final Pattern URL_CHARS = Pattern.compile("[a-zA-Z0-9/%$-_.+!*'(),?#]+");
	private static final Pattern DOMAIN_CHARS = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9-]+");
	private static final String PATTERN_DATE_ISO8601 = "(?:[\\+-]?\\d{4}(?!\\d{2}\\b))(?:(-?)(?:(?:0[1-9]|1[0-2])(?:\\1(?:[12]\\d|0[1-9]|3[01]))?|W(?:[0-4]\\d|5[0-2])(?:-?[1-7])?|(?:00[1-9]|0[1-9]\\d|[12]\\d{2}|3(?:[0-5]\\d|6[1-6])))(?:[T\\s](?:(?:(?:[01]\\d|2[0-3])(?:(:?)[0-5]\\d)?|24\\:?00)(?:[\\.,]\\d+(?!:))?)?(?:\\2[0-5]\\d(?:[\\.,]\\d+)?)?(?:[zZ]|(?:[\\+-])(?:[01]\\d|2[0-3]):?(?:[0-5]\\d)?)?)?)?";
	private static final String PATTERN_TIME = "(0?[1-9]|1[0-2])[:.][0-5]\\d([:.][0-5]\\d)? ?[aApP][mM]|(0?\\d|1\\d|2[0-3])[:.][0-5]\\d(?:[:.][0-5]\\d)?";
	//@see <a href="https://www.ietf.org/rfc/rfc0822.txt">RFC-0822</a>
	private static final String PATTERN_EMAIL = "([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x22([^\\x0d\\x22\\x5c\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x22)(\\x2e([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x22([^\\x0d\\x22\\x5c\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x22))*\\x40([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x5b([^\\x0d\\x5b-\\x5d\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x5d)(\\x2e([^\\x00-\\x20\\x22\\x28\\x29\\x2c\\x2e\\x3a-\\x3c\\x3e\\x40\\x5b-\\x5d\\x7f-\\xff]+|\\x5b([^\\x0d\\x5b-\\x5d\\x80-\\xff]|\\x5c[\\x00-\\x7f])*\\x5d))*";
	//https://rgxdb.com/r/29JZFQEP
	//@see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC-3986</a>
	//$2 = scheme, $4 = authority, $5 = path, $7 = query, $9 = fragment
	private static final String PATTERN_URI_IPv4 = "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)){3}";

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

	private static final String DOT = ".";
	private static final String COLUMN = ":";
	private static final String SLASH = "/";
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
		List<String> result = new ArrayList<>();
		text = StringUtils.replace(text, HORIZONTAL_EXPANDED_ELLIPSIS, HORIZONTAL_ELLIPSIS);

		//find all urls and emails, substitute with placeholder
		final List<String> unbreakableText = new ArrayList<>();
		text = PatternHelper.pattern(PATTERN_DATE_ISO8601 /*+ "|" + PATTERN_TIME + "|" + PATTERN_EMAIL*/).matcher(text)
			.replaceAll(m -> {
				unbreakableText.add(m.group());
				return PLACEHOLDER;
			});

		int index = 0;
		final StringTokenizer st = new StringTokenizer(text, tokenizingCharacters, true);
		while(st.hasMoreElements()){
			final String restoredSubtext = st.nextToken();
			result.add(StringUtils.contains(restoredSubtext, PLACEHOLDER)? unbreakableText.get(index ++): restoredSubtext);
		}

		//restore placeholders with original
//		result = joinUrls(result);
		return result;
	}

	/*
	 * NOTE: explicit check for {@code containingChars} speeds up method by factor of ~10
	 */
	protected List<String> join(final List<String> list, final Pattern pattern, final String containingChars,
			final Function<String, String> groupSubstituter){
		final StringBuilder sb = new StringBuilder();
		for(final String item : list)
			sb.append(item);
		final String text = sb.toString();

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

	/**
	 * The generic URI syntax consists of a hierarchical sequence of components referred to as the scheme, authority, path, query, and fragment.
	 * @see <a href="https://www.ietf.org/rfc/rfc3986.txt">RFC-3986</a>
	 * @see <a href="https://www.ietf.org/rfc/rfc0822.txt">RFC-0822</a>
	 *
	 * See RFC1738 and <a href="http://stackoverflow.com/questions/1856785/characters-allowed-in-a-url">Characters allowed in a URL</a>
	*/
	private List<String> joinUrls(final List<String> list){
		final List<String> newList = new ArrayList<>();
		boolean inUrl = false;
		final StringBuilder url = new StringBuilder();
		String urlQuote = null;
		for(int i = 0; i < list.size(); i ++){
			if(urlStartsAt(i, list)){
				inUrl = true;
				if(i - 1 >= 0)
					urlQuote = list.get(i - 1);
				url.append(list.get(i));
			}
			else if(inUrl && urlEndsAt(i, list, urlQuote)){
				inUrl = false;
				urlQuote = null;
				newList.add(url.toString());
				url.setLength(0);
				newList.add(list.get(i));
			}
			else if(inUrl)
				url.append(list.get(i));
			else
				newList.add(list.get(i));
		}
		if(url.length() > 0)
			newList.add(url.toString());
		return newList;
	}

	private boolean urlStartsAt(final int index, final List<String> list){
		final String token = list.get(index);
		if(isProtocol(token) && list.size() > index + 3){
			final String nToken = list.get(index + 1);
			final String nnToken = list.get(index + 2);
			final String nnnToken = list.get(index + 3);
			if(nToken.equals(COLUMN) && nnToken.equals(SLASH) && nnnToken.equals(SLASH))
				return true;
		}
		if(list.size() > index + 1){
			//e.g. www.mydomain.org
			final String nToken = list.get(index);
			final String nnToken = list.get(index + 1);
			if(nToken.equals("www") && nnToken.equals(DOT))
				return true;
		}
		if(
			//e.g. mydomain.org/ (require slash to avoid missing errors that can be interpreted as domains)
			list.size() > index + 3
				//use this order so the regex only gets matched if needed
				&& list.get(index + 1).equals(DOT)
				&& list.get(index + 3).equals(SLASH)
				&& DOMAIN_CHARS.matcher(token).matches()
				&& DOMAIN_CHARS.matcher(list.get(index + 2)).matches())
			return true;

		return (
			//e.g. sub.mydomain.org/ (require slash to avoid missing errors that can be interpreted as domains)
			list.size() > index + 5
			//use this order so the regex only gets matched if needed
			&& list.get(index + 1).equals(DOT)
			&& list.get(index + 3).equals(DOT)
			&& list.get(index + 5).equals(SLASH)
			&& DOMAIN_CHARS.matcher(token).matches()
			&& DOMAIN_CHARS.matcher(list.get(index + 2)).matches()
			&& DOMAIN_CHARS.matcher(list.get(index + 4)).matches());
	}

	private boolean isProtocol(final String token){
		return PROTOCOLS.contains(token);
	}

	private boolean urlEndsAt(final int index, final List<String> list, final String urlQuote){
		boolean result = false;
		final String token = list.get(index);
		//this is guesswork
		if(StringUtils.isWhitespace(token) || token.equals(")") || token.equals("]"))
			result = true;
		else if(list.size() > index + 1){
			final String nextToken = list.get(index + 1);
			if((StringUtils.isWhitespace(nextToken)
					|| StringUtils.equalsAny(nextToken, "\"", "»", "«", "‘", "’", "“", "”", "'", DOT))
					&& (StringUtils.equalsAny(token, DOT, ",", ";", COLUMN, "!", "?") || token.equals(urlQuote)))
				result = true;
			else if(!URL_CHARS.matcher(token).matches())
				result = true;
		}
		else if(!URL_CHARS.matcher(token).matches() || token.equals(DOT))
			result = true;
		return result;
	}

}
