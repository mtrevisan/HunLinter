package unit731.hunlinter.services.datastructures;


import java.util.ArrayList;
import java.util.List;


/**
 * https://github.com/apple/foundationdb/blob/master/bindings/java/src/main/com/apple/foundationdb/tuple/ByteArrayUtil.java
 */
public class ArrayHelper{

	private ArrayHelper(){}

	public static boolean startsWith(final byte[] source, final byte[] match){
		if(match.length > source.length)
			return false;

		for(int i = 0; i < match.length; i ++)
			if(source[i] != match[i])
				return false;
		return true;
	}

	public static boolean endsWith(final byte[] source, final byte[] match){
		int indexMatch = match.length;
		int indexSource = source.length;
		if(indexMatch > indexSource)
			return false;

		while(indexMatch > 0)
			if(source[-- indexSource] != match[-- indexMatch])
				return false;
		return true;
	}

	public static byte[] concatenate(final byte[]... parts){
		//create destination array
		int length = 0;
		for(int i = 0; i < parts.length; i ++)
			length += parts[i].length;
		final byte[] destination = new byte[length];

		//concatenate each part
		int offset = 0;
		for(int i = 0; i < parts.length; i ++){
			final int len = parts[i].length;
			System.arraycopy(parts[i], 0, destination, offset, len);
			offset += len;
		}

		return destination;
	}

	public static void concatenate(final byte[] destination, int offset, final byte[]... parts){
		//concatenate each part
		for(int i = 0; i < parts.length; i ++){
			final int len = parts[i].length;
			System.arraycopy(parts[i], 0, destination, offset, len);
			offset += len;
		}
	}

	public static byte[] subarray(final byte[] source, final int start){
		return subarray(source, start, source.length);
	}

	public static byte[] subarray(final byte[] source, final int start, final int end){
		if(start == 0 && end == source.length)
			return source;

		final int subLen = end - start;
		final byte[] destination = new byte[subLen];
		System.arraycopy(source, start, destination, 0, subLen);
		return destination;
	}

	/**
	 * Splits the provided text into an array, using whitespace as the separator.
	 * Whitespace is defined by Character.isWhitespace(char).
	 */
	public static byte[][] split(final byte[] source){
		final int len = source.length;
		if(len == 0)
			return new byte[0][];

		final List<byte[]> list = new ArrayList<>();
		int i = 0;
		int start = 0;
		boolean match = false;
		while(i < len){
			if(Character.isWhitespace(source[i])){
				if(match){
					list.add(subarray(source, start, i));
					match = false;
				}

				i ++;
				start = i;
			}
			else{
				match = true;
				i ++;
			}
		}

		if(match)
			list.add(subarray(source, start, i));

		return list.toArray(byte[][]::new);
	}

}
