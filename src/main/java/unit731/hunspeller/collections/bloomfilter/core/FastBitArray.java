package unit731.hunspeller.collections.bloomfilter.core;

import static java.lang.Math.abs;

import java.math.RoundingMode;
import java.util.Objects;
import lombok.Getter;


/**
 * A fast bit-set implementation that allows direct access to data property so that it can be easily serialized.
 */
public class FastBitArray implements BitArray{

	/** The data-set */
	private final long[] data;

	/** The current bit count */
	@Getter private int bitCount;


	/**
	 * Construct an instance of the {@link FastBitArray} that can hold the given number of bits
	 *
	 * @param bits the number of bits this instance can hold
	 */
	public FastBitArray(long bits){
		this(new long[checkedCast(divide(bits, 64, RoundingMode.CEILING))]);
	}

	public FastBitArray(long[] data){
		if(data == null || data.length == 0)
			throw new IllegalArgumentException("Data must be valued");

		this.data = data;
		bitCount = 0;
		for(long value : data)
			bitCount += Long.bitCount(value);
	}

	@Override
	public boolean get(int index){
		return ((data[index >> 6] & (1l << index)) != 0l);
	}

	/** Returns true if the bit changed value. */
	@Override
	public boolean set(int index){
		if(!get(index)){
			data[index >> 6] |= (1l << index);
			bitCount ++;
			return true;
		}
		return false;
	}

	@Override
	public void clear(int index){
		if(get(index)){
			data[index >> 6] &= ~(1l << index);
			bitCount --;
		}
	}

	@Override
	public void clearAll(){
		int size = data.length;
		while(size > 0)
			data[-- size] = 0l;
	}

	/**
	 * Number of bits
	 *
	 * @return total number of bits allocated
	 */
	@Override
	public int bitSize(){
		return data.length * Long.SIZE;
	}

	/**
	 * Returns the {@code int} value that is equal to {@code value}, if possible.
	 *
	 * @param value any value in the range of the {@code int} type
	 *
	 * @return the {@code int} value that equals {@code value}
	 *
	 * @throws IllegalArgumentException if {@code value} is greater than {@link Integer#MAX_VALUE} or less than {@link Integer#MIN_VALUE}
	 */
	public static int checkedCast(long value){
		int result = (int)value;
		if(result != value)
			//don't use checkArgument here, to avoid boxing
			throw new IllegalArgumentException("Out of range: " + value);

		return result;
	}

	/**
	 * Returns the result of dividing {@code p} by {@code q}, rounding using the specified {@code RoundingMode}.
	 *
	 * @param p	Dividend
	 * @param q	Divisor
	 * @param mode	Rounding behavior
	 * @return The (rounded) result of the division
	 * @throws ArithmeticException if {@code q == 0}, or if {@code mode == UNNECESSARY} and {@code a} is not an integer multiple of {@code b}
	 */
	@SuppressWarnings("fallthrough")
	public static long divide(long p, long q, RoundingMode mode){
		Objects.requireNonNull(mode, "Rounding mode cannot be null");

		//throws if q == 0
		long div = p / q;
		//equals p % q
		long rem = p - q * div;

		if(rem == 0)
			return div;

		/*
		 * Normal Java division rounds towards 0, consistently with
		 * RoundingMode.DOWN. We just have to deal with the cases where rounding
		 * towards 0 is wrong, which typically depends on the sign of p / q.
		 */
		//signum is 1 if p and q are both nonnegative or both negative, and -1 otherwise
		int signum = (1 | (int)((p ^ q) >> (Long.SIZE - 1)));
		boolean increment;
		switch(mode){
			case UNNECESSARY:
				checkRoundingUnnecessary(rem == 0);
			case DOWN:
				increment = false;
				break;

			case UP:
				increment = true;
				break;

			case CEILING:
				increment = (signum > 0);
				break;

			case FLOOR:
				increment = (signum < 0);
				break;

			case HALF_EVEN:
			case HALF_DOWN:
			case HALF_UP:
				long absRem = abs(rem);
				long cmpRemToHalfDivisor = absRem - (abs(q) - absRem);
				//subtracting two nonnegative longs can't overflow cmpRemToHalfDivisor has the same sign as compare(abs(rem), abs(q) / 2).
				if(cmpRemToHalfDivisor == 0)
					//exactly on the half mark
					increment = (mode == RoundingMode.HALF_UP | (mode == RoundingMode.HALF_EVEN & (div & 1) != 0));
				else
					//closer to the UP value
					increment = (cmpRemToHalfDivisor > 0);
				break;

			default:
				throw new AssertionError();
		}
		return (increment? div + signum: div);
	}

	private static void checkRoundingUnnecessary(boolean condition){
		if(!condition)
			throw new ArithmeticException("mode was UNNECESSARY, but rounding was necessary");
	}

}
