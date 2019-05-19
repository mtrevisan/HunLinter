package unit731.hunspeller.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;


public class StringHelper{

	public static enum Casing{
		/** All lower case or neutral case, e.g. "lowercase" or "123" */
		LOWER_CASE,
		/** Start upper case, rest lower case, e.g. "Initcap" */
		TITLE_CASE,
		/** All upper case, e.g. "UPPERCASE" or "ALL4ONE" */
		ALL_CAPS,
		/** Camel case, start lower case, e.g. "camelCase" */
		CAMEL_CASE,
		/** Pascal case, start upper case, e.g. "PascalCase" */
		PASCAL_CASE
	}


	private StringHelper(){}

	public static Casing classifyCasing(String text){
		if(StringUtils.isBlank(text))
			return Casing.LOWER_CASE;

		int lower = 0;
		int upper = 0;
		for(char chr : text.toCharArray())
			if(Character.isAlphabetic(chr)){
				if(Character.isLowerCase(chr))
					lower ++;
				else if(Character.isUpperCase(chr))
					upper ++;
			}
		if(upper == 0)
			return Casing.LOWER_CASE;

		boolean fistCapital = (Character.isUpperCase(text.charAt(0)));
		if(fistCapital && upper == 1)
			return Casing.TITLE_CASE;

		if(lower == 0)
			return Casing.ALL_CAPS;

		return (fistCapital? Casing.PASCAL_CASE: Casing.CAMEL_CASE);
	}

	/**
	 * Finds the length and the index at which starts the Longest Common Substring
	 *
	 * @param keyA	Character sequence A
	 * @param keyB	Character sequence B
	 * @return	The indexes of keyA and keyB of the start of the longest common substring between <code>A</code> and <code>B</code>
	 * @throws IllegalArgumentException	If either <code>A</code> or <code>B</code> is <code>null</code>
	 */
	public static Pair<Integer, Integer> longestCommonSubstring(String keyA, String keyB){
		int m = keyA.length();
		int n = keyB.length();

		if(m < n){
			Pair<Integer, Integer> indexes = longestCommonSubstring(keyB, keyA);
			return Pair.of(indexes.getRight(), indexes.getLeft());
		}

		//matrix to store result of two consecutive rows at a time
		int[][] len = new int[2][n];
		int currentRow = 0;

		//for a particular value of i and j, len[currRow][j] stores length of LCS in string X[0..i] and Y[0..j]
		int lcsMaxLength = 0;
		int lcsIndexA = -1;
		int lcsIndexB = -1;
		for(int i = 0; i < m; i ++){
			for(int j = 0; j < n; j ++){
				int cost;
				if(keyA.charAt(i) != keyB.charAt(j))
					cost = 0;
				else if(i == 0 || j == 0)
					cost = 1;
				else
					cost = len[currentRow][j - 1] + 1;
				len[1 - currentRow][j] = cost;

				if(cost > lcsMaxLength){
					lcsMaxLength = cost;

					lcsIndexA = i;
					lcsIndexB = j;
				}
			}

			//make current row as previous row and previous row as new current row
			currentRow = 1 - currentRow;
		}
		return Pair.of(lcsIndexA, lcsIndexB);
	}

}
