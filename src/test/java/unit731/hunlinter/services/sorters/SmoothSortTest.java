package unit731.hunlinter.services.sorters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunlinter.services.fsa.builders.FSABuilder;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


public class SmoothSortTest{

	private static final Comparator<Object> NATURAL_ORDER = (first, second) -> ((Comparable<Object>)first).compareTo(second);

	private static BigInteger HUGE = BigInteger.ONE.shiftLeft(100);
	private static Random RND = new Random(666);


	@Test
	void allEqualInt(){
		int len = 10_000;
		Integer[] data = new Integer[len];
		for(int i = 0; i < len; i ++)
			data[i] = 666;

		SmoothSort.sort(data, NATURAL_ORDER);

		assertSort(data);

		data = SorterHelper.removeDuplicates(data);
		Assertions.assertEquals(1, data.length);
	}

	@Test
	void ascending10RndAtEndInt(){
		int len = 10_000;
		Integer[] data = new Integer[len];
		int endStart = len - 10;
		for(int i = 0; i < endStart; i ++)
			data[i] = i;
		for(int i = endStart; i < len; i ++)
			data[i] = RND.nextInt(endStart + 10);

		SmoothSort.sort(data, NATURAL_ORDER);

		assertSort(data);
	}

	@Test
	void ascending3RndExchInt(){
		int len = 10_000;
		Integer[] data = new Integer[len];
		for(int i = 0; i < len; i ++)
			data[i] = i;
		for(int i = 0; i < 3; i ++)
			swap(data, RND.nextInt(data.length), RND.nextInt(data.length));

		SmoothSort.sort(data, NATURAL_ORDER);

		assertSort(data);
	}

	@Test
	void ascendingInt(){
		int len = 10_000;
		Integer[] data = new Integer[len];
		for(int i = 0; i < len; i ++)
			data[i] = i;

		SmoothSort.sort(data, NATURAL_ORDER);

		assertSort(data);
	}

	@Test
	void duplicatesGaloreInt(){
		int len = 10_000;
		Integer[] data = new Integer[len];
		for(int i = 0; i < len; i ++)
			data[i] = RND.nextInt(4);

		SmoothSort.sort(data, NATURAL_ORDER);

		assertSort(data);
	}

	@Test
	void pseudoAscendingString(){
		int len = 10_000;
		String[] data = new String[len];
		for(int i = 0; i < len; i ++)
			data[i] = Integer.toString(i);

		SmoothSort.sort(data, NATURAL_ORDER);

		assertSort(data);
	}

	@Test
	void descendingInt(){
		int len = 10_000;
		Integer[] data = new Integer[len];
		for(int i = 0; i < len; i ++)
			data[i] = len - i;

		SmoothSort.sort(data, NATURAL_ORDER);

		assertSort(data);
	}

	@Test
	void randomBigInt(){
		int len = 10_000;
		BigInteger[] data = new BigInteger[len];
		for(int i = 0; i < len; i ++)
			data[i] = HUGE.add(BigInteger.valueOf(RND.nextInt(len)));

		SmoothSort.sort(data, NATURAL_ORDER);

		assertSort(data);
	}

	@Test
	void randomInt(){
		int len = 10_000;
		Integer[] data = new Integer[len];
		for(int i = 0; i < len; i ++)
			data[i] = RND.nextInt();

		SmoothSort.sort(data, NATURAL_ORDER);

		assertSort(data);
	}

	@Test
	void randomInt2(){
		int len = 10;
		List<byte[]> data = new ArrayList<>(len);
		for(int i = 0; i < len; i ++)
			data.add((""+RND.nextInt()).getBytes());

		SmoothSort.sort(data, FSABuilder.LEXICAL_ORDERING);

		assertSort(data);
	}

	private void assertSort(List<byte[]> data){
		for(int i = 1; i < data.size(); i ++)
			Assertions.assertTrue(FSABuilder.LEXICAL_ORDERING.compare(data.get(i - 1), data.get(i)) <= 0);
	}


	private void assertSort(Integer[] data){
		for(int i = 1; i < data.length; i ++)
			Assertions.assertTrue(data[i - 1] <= data[i]);
	}

	private void assertSort(BigInteger[] data){
		for(int i = 1; i < data.length; i ++)
			Assertions.assertTrue(data[i - 1].compareTo(data[i]) <= 0);
	}

	private void assertSort(String[] data){
		for(int i = 1; i < data.length; i ++)
			Assertions.assertTrue(data[i - 1].compareTo(data[i]) <= 0);
	}

	private static void swap(Object[] a, int i, int j){
		Object t = a[i];
		a[i] = a[j];
		a[j] = t;
	}

}
