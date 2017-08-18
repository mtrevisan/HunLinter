package unit731.hunspeller.collections.intervalmap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * An IntervalMap represents a map of Interval objects to values, with the additional ability to query with a single parameter
 * and find the values of all intervals that contain it.
 *
 * @see <a href="https://github.com/stevenschlansker/IntervalMap">IntervalMap</a>
 * @see <a href="https://github.com/Breinify/brein-time-utilities">Brein Time Utilities</a>
 * @see <a href="https://en.wikipedia.org/wiki/Interval_tree">Interval Tree</a>
 *
 * @param <K> the type of the intervals' bounds
 * @param <V> the type of the values stored in the Map
 */
public class IntervalMap<K extends Comparable<K>, V> implements Map<Interval<K>, V>{

	private IntervalNode<K, V> root;


//	public IntervalMap<K, V> getContaining(Interval<K> interval){
//		return find(interval, root, new IntervalMap<>());
//	}

	public IntervalMap<K, V> getContaining(K point){
		return find(point, root, new IntervalMap<>());
	}

//	public Collection<IInterval> overlap(final IInterval query){
//		if(this.root == null)
//			return Collections.emptyList();
//		else{
//			final List<IInterval> result = new ArrayList<>();
//			_overlap(this.root, query, result);
//			return result;
//		}
//	}
//
//	protected void _overlap(final IntervalTreeNode node, final IInterval query, final Collection<IInterval> result){
//		if(node == null)
//			return;
//
//		//check if the current node overlaps
//		if(node.compare(node.getStart(), query.getNormEnd()) <= 0 && node.compare(node.getEnd(), query.getNormStart()) >= 0)
//			node.getIntervals().forEach(result::add);
//		if(node.hasLeft() && node.compare(node.getLeft().getMax(), query.getNormStart()) >= 0)
//			_overlap(node.getLeft(), query, result);
//
//		_overlap(node.getRight(), query, result);
//	}

	private IntervalMap<K, V> find(K point, IntervalNode<K, V> current, IntervalMap<K, V> result){
		//don't search nodes that don't exist
		if(current == null)
			return result;
		//if current is to the right of the rightmost point of any interval in this node and all children, there won't be any matches
		if(current.getInterval().compareTo(point) == 0)
			result.put(current.getInterval(), current.getValue());
		//skip if no children
		if(current.getMaxChildIntervalEnd() == null || current.getMaxChildIntervalEnd().compareTo(point) < 0)
			return result;
		//search left children
		if(current.getLeft() != null)
			find(point, current.getLeft(), result);
		//if point is to the left of the start of this interval, then it can't be in any child to the right
		if(point.compareTo(current.getInterval().getLowerBound()) < 0)
			return result;
		//otherwise, search right children
		if(current.getRight() != null)
			find(point, current.getRight(), result);

		return result;
	}

	@Override
	public void clear(){
		root = null;
	}

	@Override
	public boolean containsKey(Object key){
		return (findUnchecked(key, root, new LinkedList<>()) != null);
	}

	@Override
	public boolean containsValue(Object value){
		return (traverse(new Traversal<K, V, Boolean>(){
			@Override
			public Boolean visit(IntervalNode<K, V> node){
				return (node.getValue().equals(value)? Boolean.TRUE: null);
			}
		}) != null);
	}

	@Override
	public Set<Entry<Interval<K>, V>> entrySet(){
		Set<Entry<Interval<K>, V>> result = new HashSet<>();
		traverse(new Traversal<K, V, Void>(){
			@Override
			public Void visit(IntervalNode<K, V> node){
				result.add(new Entry<Interval<K>, V>(){
					@Override
					public Interval<K> getKey(){
						return node.getInterval();
					}

					@Override
					public V getValue(){
						return node.getValue();
					}

					@Override
					public V setValue(V value){
						node.setValue(value);
						return value;
					}
				});
				return null;
			}
		});
		return result;
	}

	@Override
	public V get(Object key){
		IntervalNode<K, V> node = findUnchecked(key, root, new LinkedList<>());
		return (node != null? node.getValue(): null);
	}

	@Override
	public boolean isEmpty(){
		return (root == null);
	}

	@Override
	public Set<Interval<K>> keySet(){
		Set<Interval<K>> result = new HashSet<>();
		traverse(new Traversal<K, V, Void>(){
			@Override
			public Void visit(IntervalNode<K, V> node){
				result.add(node.getInterval());
				return null;
			}
		});
		return result;
	}

	@Override
	public V put(Interval<K> key, V value){
		IntervalNode<K, V> newborn = new IntervalNode<>(key, value);
		V result = null;
		if(root == null)
			root = newborn;
		else
			result = root.add(newborn);
		return result;
	}

	@Override
	public void putAll(Map<? extends Interval<K>, ? extends V> m){
		m.entrySet()
			.forEach(e -> put(e.getKey(), e.getValue()));
	}

	@Override
	public V remove(Object key){
		List<IntervalNode<K, V>> trace = new LinkedList<>();
		IntervalNode<K, V> node = findUnchecked(key, root, trace);
		if(node.getLeft() == null && node.getRight() == null){
			IntervalNode<K, V> parent = trace.get(0);
			if(parent.getLeft() == node){
				parent.setLeft(null);
				parent.setLeftCount(0);
			}
			else{
				assert parent.getRight() == node;

				parent.setRight(null);
				parent.setRightCount(0);
			}
			return node.getValue();
		}
		else if(node.getLeft() == null){
			IntervalNode<K, V> successor = node.getRight();
			IntervalNode<K, V> successorsParent = node;
			while(successor.getLeft() != null){
				successorsParent = successor;
				successor = successor.getLeft();
			}
			//TODO
			// more
		}
		else{
			IntervalNode<K, V> successor = node.getLeft();
			IntervalNode<K, V> successorsParent = node;
			while(successor.getRight() != null){
				successorsParent = successor;
				successor = successor.getRight();
			}
			//TODO
			// more
		}
		throw new AssertionError();
	}

	private IntervalNode<K, V> findUnchecked(Object key, IntervalNode<K, V> current, List<IntervalNode<K, V>> trace){
		if(root == null || !(key instanceof Interval<?>))
			return null;

		@SuppressWarnings("unchecked")
		Interval<K> ikey = (Interval<K>)key;
		@SuppressWarnings("unchecked")
		Class<K> paramClass = (Class<K>)ikey.getLowerBound().getClass();
		@SuppressWarnings("unchecked")
		Class<K> intervalClass = (Class<K>)root.getInterval().getLowerBound().getClass();
		if(!(intervalClass.isAssignableFrom(paramClass)))
			return null;

		try{
			return find(ikey, current, trace);
		}
		catch(ClassCastException e){
			return null;
		}
	}

	private IntervalNode<K, V> find(Interval<K> key, IntervalNode<K, V> current, List<IntervalNode<K, V>> trace){
		if(current == null)
			return null;
		if(current.getInterval().equals(key))
			return current;

		trace.add(0, current);
		return find(key, (current.getInterval().getLowerBound().compareTo(key.getLowerBound()) < 0? current.getRight(): current.getLeft()), trace);
	}

	@Override
	public int size(){
		return (root != null? root.getCount() + 1: 0);
	}

	@Override
	public Collection<V> values(){
		Collection<V> result = new ArrayList<>();
		traverse(new Traversal<K, V, Void>(){
			@Override
			public Void visit(IntervalNode<K, V> node){
				result.add(node.getValue());
				return null;
			}
		});
		return result;
	}

	private <R> R traverse(Traversal<K, V, R> t){
		Deque<IntervalNode<K, V>> queue = new LinkedList<>();
		queue.offerFirst(root);
		while(!queue.isEmpty()){
			IntervalNode<K, V> node = queue.pop();
			if(node == null)
				continue;

			R result = t.visit(node);
			if(result != null)
				return result;

			queue.offerFirst(node.getLeft());
			queue.offerFirst(node.getRight());
		}
		return null;
	}

}
