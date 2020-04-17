package unit731.hunlinter.services.text;


public class ArrayHelper{

	private ArrayHelper(){}

	/** Compute the length of the shared prefix between two byte sequences */
	public static int longestCommonPrefix(final byte[] a, final byte[] b){
		int i = 0;
		final int max = Math.min(a.length, b.length);
		while(i < max && a[i] == b[i])
			i ++;
		return i;
	}

	/** Compute the length of the shared prefix between two byte sequences */
	public static int longestCommonPrefix(final byte[] a, int aStart, final byte[] b, int bStart){
		int i = 0;
		final int max = Math.min(a.length - aStart, b.length - bStart);
		while(i < max && a[aStart ++] == b[bStart ++])
			i ++;
		return i;
	}

}
