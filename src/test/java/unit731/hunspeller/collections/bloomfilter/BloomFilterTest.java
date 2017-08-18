package unit731.hunspeller.collections.bloomfilter;

import unit731.hunspeller.collections.bloomfilter.interfaces.BloomFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;

import org.junit.Test;


public class BloomFilterTest{

	private static final int MAX = 1000 * 1000;

	private static final double FPP = 0.01;


	@Test
	public void defaultFilter(){
		BloomFilter<String> filter = new InMemoryBloomFilter<>(10 * MAX, FPP);

		//generate two one-million uuid arrays
		List<String> contained = new ArrayList<>();
		List<String> unused = new ArrayList<>();
		for(int index = 0; index < MAX; index ++){
			contained.add(UUID.randomUUID().toString());
			unused.add(UUID.randomUUID().toString());
		}

		//now add to filter
		for(String uuid : contained)
			filter.add(uuid);

		//now start checking
		for(String uuid : contained)
			Assert.assertTrue(filter.contains(uuid));
		int fpp = 0;
		for(String uuid : unused){
			boolean present = filter.contains(uuid);
			if(present){
				//false positive
				Assert.assertEquals(false, contained.contains(uuid));
				fpp ++;
			}
		}

		//add another one million more uuids
		List<String> more = new ArrayList<>();
		for(int index = 0; index < MAX; index ++)
			more.add(UUID.randomUUID().toString());
		for(String uuid : more)
			filter.add(uuid);

		//check again
		for(String uuid : more)
			Assert.assertTrue(filter.contains(uuid));
		for(int index = 0; index < MAX; index ++){
			String uuid = UUID.randomUUID().toString();
			boolean present = filter.contains(uuid);
			if(present){
				// false positive
				Assert.assertEquals(false, contained.contains(uuid));
				fpp ++;
			}
		}
		System.out.println("False positives found in two millions: " + fpp);
	}

}
