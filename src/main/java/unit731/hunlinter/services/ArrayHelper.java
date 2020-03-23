package unit731.hunlinter.services;


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
		if(start == 0)
			return source;

		final int subLen = source.length - start;
		if(subLen < 0)
			throw new StringIndexOutOfBoundsException(subLen);

		final byte[] destination = new byte[subLen];
		System.arraycopy(source, start, destination, 0, subLen);
		return destination;
	}

}
