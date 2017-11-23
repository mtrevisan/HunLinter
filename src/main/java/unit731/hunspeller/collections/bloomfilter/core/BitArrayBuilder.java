package unit731.hunspeller.collections.bloomfilter.core;

import java.util.Objects;


public class BitArrayBuilder{

	public static enum Type{FAST, JAVA};


	public static BitArray getBitArray(Type type, int bits){
		Objects.nonNull(type);

		BitArray ba = null;
		switch(type){
			case FAST:
				ba = new FastBitArray(bits);
				break;

			case JAVA:
				ba = new JavaBitArray(bits);
		}
		return ba;
	}

	public static BitArray getBitArray(Type type, long[] data){
		Objects.nonNull(type);

		BitArray ba = null;
		switch(type){
			case FAST:
				ba = new FastBitArray(data);
				break;

			case JAVA:
				ba = new JavaBitArray(data);
		}
		return ba;
	}

}
