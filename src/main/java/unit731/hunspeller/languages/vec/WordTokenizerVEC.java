package unit731.hunspeller.languages.vec;

import org.apache.commons.lang3.StringUtils;
import unit731.hunspeller.languages.WordTokenizer;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WordTokenizerVEC extends WordTokenizer{

	private static final String UNICODE_APOSTROPHE = "'";
	private static final String UNICODE_MODIFIER_LETTER_APOSTROPHE = "\u02BC";
	private static final String UNICODE_APOSTROPHES_PATTERN = "[" + UNICODE_APOSTROPHE + UNICODE_MODIFIER_LETTER_APOSTROPHE + "]";

	private static final Pattern APOSTROPHE = Pattern.compile("(?i)"
		+ "([dglƚnsv]|(a|[ai\u2019]n)dó|[kps]o|pu?ò|st|tan|kuan|tut|([n\u2019]|in)t|tèr[sŧ]|k[uo]art|kuint|sèst|[kp]a|sen[sŧ]|komò|fra|nu|re|intor)" + UNICODE_APOSTROPHES_PATTERN + "(?=[" + Pattern.quote(TOKENIZING_CHARACTERS) + "])"
		+ "|"
		+ "(?<=\\s)" + UNICODE_APOSTROPHES_PATTERN + "[^" + Pattern.quote(TOKENIZING_CHARACTERS) + "]+"
	);


	@Override
	public List<String> tokenize(final String text) {
		List<String> list = new ArrayList<>();
		final StringTokenizer st = new StringTokenizer(text, TOKENIZING_CHARACTERS, true);
		while(st.hasMoreElements()){
			final String token = st.nextToken();
			list.add(token);
		}
		list = joinApostrophes(list);
		list = joinEmails(list);
		list = joinUrls(list);
		return list;
	}

	protected List<String> joinApostrophes(final List<String> list){
		final StringBuilder sb = new StringBuilder();
		for(final String item : list)
			sb.append(item);
		final String text = sb.toString();

		final List<String> result = new ArrayList<>();
		if(StringUtils.contains(text, UNICODE_APOSTROPHE) || StringUtils.contains(text, UNICODE_MODIFIER_LETTER_APOSTROPHE)){
			final Matcher matcher = APOSTROPHE.matcher(text);
			int currentPosition = 0;
			int idx = 0;
			while(matcher.find()){
				final int start = matcher.start();
				final int end = matcher.end();
				while(currentPosition < end){
					if(currentPosition < start)
						result.add(list.get(idx));
					else if(currentPosition == start){
						//substitute Unicode Apostrophe with Unicode Modifier Letter Apostrophe
						final String group = matcher.group()
							.replaceAll(UNICODE_APOSTROPHE, UNICODE_MODIFIER_LETTER_APOSTROPHE);
						result.add(group);
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
