package unit731.hunspeller.collections.intervalmap;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class IntervalMapTest{

	private static final String ZEROFIVE = "zerofive";
	private static final String MINUSFIVETEN = "minusfiveten";
	private static final String THREEFIVE = "threefive";
	private static final String FIVETEN = "fiveten";
	private static final Interval<Integer> ZERO_FIVE_INTERVAL = new Interval<Integer>(0, 5);
	private static final Interval<Integer> MINUSFIVE_TEN_INTERVAL = new Interval<Integer>(10, -5);
	private static final Interval<Integer> THREE_FIVE_INTERVAL = new Interval<Integer>(3, 5);
	private static final Interval<Integer> FIVE_TEN_INTERVAL = new Interval<Integer>(5, 10);

	private final IntervalMap<Integer, String> test = new IntervalMap<>();


	@Before
	public void setUp() throws Exception{
		test.put(FIVE_TEN_INTERVAL, FIVETEN);
		test.put(THREE_FIVE_INTERVAL, THREEFIVE);
		test.put(MINUSFIVE_TEN_INTERVAL, MINUSFIVETEN);
		test.put(ZERO_FIVE_INTERVAL, ZEROFIVE);
	}

	@Test
	public void getContaining(){
		IntervalMap<Integer, String> result = test.getContaining(0);

		Assert.assertTrue(result.containsValue(MINUSFIVETEN));
		Assert.assertTrue(result.containsValue(ZEROFIVE));
		Assert.assertFalse(result.containsValue(FIVETEN));
		Assert.assertFalse(result.containsValue(THREEFIVE));
	}

	@Test
	public void clear(){
		test.clear();

		Assert.assertEquals(0, test.size());
		Assert.assertFalse(test.containsKey(THREE_FIVE_INTERVAL));
		Assert.assertFalse(test.containsValue(ZEROFIVE));
	}

	@Test
	public void containsKey(){
		Assert.assertTrue(test.containsKey(THREE_FIVE_INTERVAL));
		Assert.assertTrue(test.containsKey(FIVE_TEN_INTERVAL));
		Assert.assertTrue(test.containsKey(new Interval<>(5, 0)));
		Assert.assertTrue(test.containsKey(new Interval<>(-5, 10)));
		Assert.assertFalse(test.containsKey(new Interval<>(2, 5)));
		Assert.assertFalse(test.containsKey(new Interval<>(3, 6)));
		Assert.assertFalse(test.containsKey(new Interval<>(-5, 11)));
	}

	@Test
	public void containsValue(){
		Assert.assertTrue(test.containsValue(FIVETEN));
		Assert.assertTrue(test.containsValue(THREEFIVE));
		Assert.assertTrue(test.containsValue(MINUSFIVETEN));
		Assert.assertTrue(test.containsValue(ZEROFIVE));
		Assert.assertFalse(test.containsValue("blahblah"));
	}

	@Test
	public void entrySet(){
		Set<Entry<Interval<Integer>, String>> eSet = test.entrySet();

		Assert.assertEquals(test.size(), test.entrySet().size());
		for(Entry<Interval<Integer>, String> e : eSet){
			Assert.assertTrue(test.containsKey(e.getKey()));
			Assert.assertTrue(test.containsValue(e.getValue()));
		}
	}

	@Test
	public void get(){
		Assert.assertEquals(THREEFIVE, test.get(THREE_FIVE_INTERVAL));
		Assert.assertEquals(FIVETEN, test.get(FIVE_TEN_INTERVAL));
		Assert.assertEquals(ZEROFIVE, test.get(new Interval<>(5, 0)));
		Assert.assertEquals(MINUSFIVETEN, test.get(new Interval<>(-5, 10)));
	}

	@Test
	public void isEmpty(){
		Assert.assertFalse(test.isEmpty());

		test.clear();

		Assert.assertTrue(test.isEmpty());
	}

	@Test
	public void keySet(){
		Set<Interval<Integer>> keySet = test.keySet();

		Assert.assertEquals(test.size(), keySet.size());
		for(Interval<Integer> key : keySet)
			Assert.assertTrue(test.containsKey(key));
	}

	@Test
	public void put(){
		int oldHashCode = test.hashCode();

		Assert.assertEquals(4, test.size());

		test.put(new Interval<>(4, 5), "fourfive");

		Assert.assertEquals(5, test.size());
		Assert.assertNotSame(test.hashCode(), oldHashCode);
	}

//	@Test
//	public void remove(){
//		Assert.assertEquals(4, test.size());
//
//		test.remove(FIVE_TEN_INTERVAL);
//
//		Assert.assertEquals(3, test.size());
//	}

	@Test
	public void values(){
		Collection<String> values = test.values();

		Assert.assertTrue(values.contains(FIVETEN));
		Assert.assertTrue(values.contains(THREEFIVE));
		Assert.assertTrue(values.contains(MINUSFIVETEN));
		Assert.assertTrue(values.contains(ZEROFIVE));
		Assert.assertEquals(4, values.size());
	}

}
