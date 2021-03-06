/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package unit731.hunlinter.services.system;

import org.apache.commons.lang3.ArrayUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import unit731.hunlinter.datastructures.FixedArray;
import unit731.hunlinter.datastructures.SimpleDynamicArray;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;


public final class LoopHelper{

	private LoopHelper(){}


	public static <T> void forEach(final FixedArray<T> array, final Consumer<T> fun){
		final int size = (array != null? array.limit: 0);
		for(int i = 0; i < size; i ++)
			fun.accept(array.data[i]);
	}

	public static <T> void forEach(final T[] array, final Consumer<T> fun){
		final int size = (array != null? array.length: 0);
		for(int i = 0; i < size; i ++)
			fun.accept(array[i]);
	}

	public static <T> void forEach(final Iterable<T> collection, final Consumer<T> fun){
		if(collection != null)
			for(final T elem : collection)
				fun.accept(elem);
	}


	public static <T> T match(final T[] array, final Predicate<T> condition){
		final int size = (array != null? array.length: 0);
		for(int i = 0; i < size; i ++){
			final T elem = array[i];
			if(condition.test(elem))
				return elem;
		}
		return null;
	}

	public static <T> T match(final Iterable<T> collection, final Predicate<T> condition){
		if(collection != null)
			for(final T elem : collection)
				if(condition.test(elem))
					return elem;
		return null;
	}


	public static <T> boolean allMatch(final Iterable<T> collection, final Predicate<T> condition){
		boolean result = true;
		if(collection != null)
			for(final T elem : collection)
				if(!condition.test(elem)){
					result = false;
					break;
				}
		return result;
	}


	public static <T> void applyIf(final T[] array, final Predicate<T> condition, final Consumer<T> fun){
		final int size = (array != null? array.length: 0);
		for(int i = 0; i < size; i ++){
			final T elem = array[i];
			if(condition.test(elem))
				fun.accept(elem);
		}
	}

	public static <T> void applyIf(final Iterable<T> collection, final Predicate<T> condition, final Consumer<T> fun){
		if(collection != null)
			for(final T elem : collection)
				if(condition.test(elem))
					fun.accept(elem);
	}

	public static void applyIf(final NodeList nodes, final Predicate<Node> condition, final Predicate<Node> fun){
		if(nodes != null)
			for(int i = 0; i < nodes.getLength(); i ++){
				final Node node = nodes.item(i);
				if(condition.test(node))
					fun.test(node);
			}
	}


	public static <T> T[] collectIf(final T[] array, final Predicate<T> condition){
		final int size = (array != null? array.length: 0);
		final SimpleDynamicArray<T> collect = new SimpleDynamicArray<T>((Class<T>)array.getClass().getComponentType(), size);
		for(int i = 0; i < size; i ++){
			final T elem = array[i];
			if(condition.test(elem))
				collect.add(elem);
		}
		return collect.extractCopy();
	}


	public static <T> T max(final Iterable<T> collection, final Comparator<T> comparator){
		T best = null;
		for(final T elem : collection)
			if(best == null || comparator.compare(elem, best) > 0)
				best = elem;
		return best;
	}


	public static <T> T[] removeIf(final T[] array, final Predicate<T> filter){
		int index = indexOf(array, filter, 0);
		if(index == -1)
			return ArrayUtils.clone(array);

		final int[] indices = new int[array.length - index];
		indices[0] = index;

		int count;
		for(count = 1; (index = indexOf(array, filter, indices[count - 1] + 1)) != -1; indices[count ++] = index){}

		return ArrayUtils.removeAll(array, Arrays.copyOf(indices, count));
	}

	private static <T> int indexOf(final T[] array, final Predicate<T> filter, final int startIndex){
		for(int i = startIndex; i < array.length; i ++)
			if(filter.test(array[i]))
				return i;
		return -1;
	}

}