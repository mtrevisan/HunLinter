package unit731.hunspeller.collections.bloomfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Assert;

import org.junit.Test;
import unit731.hunspeller.collections.bloomfilter.core.BitArrayBuilder;


public class BloomFilterTest{

	private static final int MAX = 100 * 100;

	private static final double FPP = 0.01;


	@Test
	public void defaultFilter(){
		BloomFilterInterface<String> filter = new BloomFilter<>(BitArrayBuilder.Type.JAVA, 10 * MAX, FPP);

		//generate two one-million uuid arrays
		List<String> contained = new ArrayList<>();
		List<String> unused = new ArrayList<>();
		for(int index = 0; index < MAX; index ++){
			contained.add(UUID.randomUUID().toString());
			unused.add(UUID.randomUUID().toString());
		}

		//now add to filter
		contained.forEach(filter::add);

		//now start checking
		contained.stream()
			.map(filter::contains)
			.forEach(Assert::assertTrue);
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
		more.forEach(filter::add);

		//check again
		contained.stream()
			.map(filter::contains)
			.forEach(Assert::assertTrue);
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
