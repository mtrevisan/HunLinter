package unit731.hunspeller.collections.intervalmap;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;


public class IntervalNode<K extends Comparable<K>, V>{
	
	@Getter private Interval<K> interval;
	@Getter private K maxChildIntervalEnd;
//	private int maxChildDepth;
	@Setter private int leftCount;
	@Setter private int rightCount;
	@Getter @Setter private V value;
	@Getter @Setter private IntervalNode<K, V> left;
	@Getter @Setter private IntervalNode<K, V> right;


	IntervalNode(@NonNull Interval<K> interval, @NonNull V value){
		this.interval = interval;
		this.value = value;
	}

	public V add(IntervalNode<K, V> newborn){
		V result = null;
		int comparison = interval.compareTo(newborn.interval);
		if(comparison < 0){
			if(right == null){
				right = newborn;
				rightCount ++;
				adjustMaxChildInterval(newborn);
//				maxChildDepth = Math.max(maxChildDepth, 1);
			}
			else{
				result = right.add(newborn);
				adjustMaxChildInterval(newborn);
				if(result == null)
					//a new node was added
					rightCount ++;
			}
		}
		else if(comparison > 0){
			if(left == null){
				left = newborn;
				leftCount ++;
				adjustMaxChildInterval(newborn);
//				maxChildDepth = Math.max(maxChildDepth, 1);
			}
			else{
				result = left.add(newborn);
				adjustMaxChildInterval(newborn);
				if(result == null)
					//a new node was added
					leftCount ++;
			}
		}
		else{
			V oldvalue = value;
			value = newborn.value;
			result = oldvalue;
		}
		return result;
	}

	private void adjustMaxChildInterval(IntervalNode<K, V> newborn){
		if(maxChildIntervalEnd == null || maxChildIntervalEnd.compareTo(newborn.interval.getUpperBound()) < 0)
			maxChildIntervalEnd = newborn.interval.getUpperBound();
	}

	public int getCount(){
		return leftCount + rightCount;
	}

}
