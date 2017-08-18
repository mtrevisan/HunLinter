package unit731.hunspeller.collections.intervalmap;

import org.junit.Assert;
import org.junit.Test;


public class IntervalTest{

	private final Interval<Integer> test1 = new Interval<>(1, 4);
	private final Interval<Integer> test2 = new Interval<>(4, 5);
	private final Interval<Integer> test3 = new Interval<>(5, 10);


	@Test
	public void hashCodeAreDifferent(){
		Assert.assertNotSame(test1.hashCode(), test2.hashCode());
		Assert.assertNotSame(test2.hashCode(), test3.hashCode());
		Assert.assertNotSame(test1.hashCode(), test3.hashCode());
	}

	@Test
	public void getLowerBound(){
		Assert.assertEquals(1, test1.getLowerBound().intValue());
		Assert.assertEquals(5, test3.getLowerBound().intValue());
	}

	@Test
	public void getUpperBound(){
		Assert.assertEquals(test1.getUpperBound(), test2.getLowerBound());
		Assert.assertEquals(test2.getUpperBound(), test3.getLowerBound());
	}

	@Test
	public void equalsObject(){
		Assert.assertEquals(new Interval<>(1, 4), test1);
		Assert.assertEquals(new Interval<>(4, 5), test2);
		if(test1.equals(test2))
			Assert.fail();
	}

	@Test
	public void swappedBounds(){
		Assert.assertEquals(new Interval<>(1, 10), new Interval<>(10, 1));
		Assert.assertEquals(new Interval<>(0.6, 0.888), new Interval<>(0.888, 0.6));
	}

	@Test
	public void overlapsWith(){
		Assert.assertTrue(test1.overlaps(test2));
		Assert.assertTrue(test2.overlaps(test3));
		Assert.assertFalse(test1.overlaps(test3));
	}

}
