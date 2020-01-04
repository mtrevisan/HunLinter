package unit731.hunlinter.languages;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Tokenizes a sentence into words. Punctuation and whitespace gets their own tokens.
 * The tokenizer is a quite simple character-based one, though it knows about urls and will put them in one token,
 * if fully specified including a protocol (like {@code http://foobar.org}).
 *
 * @author Daniel Naber
 */
public class WordTokenizer{

	private static final List<String> PROTOCOLS = Collections.unmodifiableList(Arrays.asList("http", "https", "ftp"));
	private static final Pattern URL_CHARS = Pattern.compile("[a-zA-Z0-9/%$-_.+!*'(),\\?#]+");
	private static final Pattern DOMAIN_CHARS = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9-]+");
	private static final Pattern PATTERN_EMAIL = Pattern.compile("(?<!:)\\b[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))\\b");

	public static final String DEFAULT_TOKENIZING_CHARACTERS = "\u0020\u00A0\u115f" +
		"\u1160\u1680"
		+ "\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2007"
		+ "\u2008\u2009\u200A\u200B\u200c\u200d\u200e\u200f"
		+ "\u2028\u2029\u202a\u202b\u202c\u202d\u202e\u202f"
		+ "\u205F\u2060\u2061\u2062\u2063\u206A\u206b\u206c\u206d"
		+ "\u206E\u206F\u3000\u3164\ufeff\uffa0\ufff9\ufffa\ufffb"
		+ ",.;()[]{}=*#∗×·+÷<>!?:/|\\\"'«»„”“`´‘’‛′›‹…¿¡→‼⁇⁈⁉_"
		+ "—"  // em dash
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
		final StringTokenizer st = new StringTokenizer(text, tokenizingCharacters, true);
		while(st.hasMoreElements())
			result.add(st.nextToken());

		result = joinEmails(result);
		result = joinUrls(result);
		return result;
	}

	private List<String> joinEmails(final List<String> list){
		return join(list, PATTERN_EMAIL, "@", null);
	}

	/**
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

	/** see rfc1738 and <a href="http://stackoverflow.com/questions/1856785/characters-allowed-in-a-url">Characters allowed in a URL</a> */
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
		final String token = list.get(index);
		//this is guesswork
		if(StringUtils.isWhitespace(token) || token.equals(")") || token.equals("]"))
			return true;
		else if(list.size() > index + 1){
			final String nextToken = list.get(index + 1);
			if((StringUtils.isWhitespace(nextToken)
					|| StringUtils.equalsAny(nextToken, "\"", "»", "«", "‘", "’", "“", "”", "'", DOT))
					&& (StringUtils.equalsAny(token, DOT, ",", ";", COLUMN, "!", "?") || token.equals(urlQuote)))
				return true;
			else if(!URL_CHARS.matcher(token).matches())
				return true;
		}
		else if(!URL_CHARS.matcher(token).matches() || token.equals(DOT))
			return true;
		return false;
	}

}
