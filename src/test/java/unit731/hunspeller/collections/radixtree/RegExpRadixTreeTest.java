package unit731.hunspeller.collections.radixtree;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.collections.radixtree.sequencers.RegExpSequencer;
import unit731.hunspeller.collections.radixtree.exceptions.DuplicateKeyException;


public class RegExpRadixTreeTest{

	private final SecureRandom rng = new SecureRandom();


	@Test
	public void testEmptyTree(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		Assertions.assertTrue(tree.isEmpty());
		Assertions.assertEquals(0, tree.size());
	}

	@Test
	public void testSingleInsertion(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("test", 1);

		Assertions.assertEquals(1, tree.size());
		Assertions.assertTrue(tree.containsKeyPrefixedBy("test"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("tes"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("testt"));
	}

	@Test
	public void testMultipleInsertions(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);
		tree.put("rest", 4);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(1, tree.getPrefixedBy("test").intValue());
		Assertions.assertEquals(2, tree.getPrefixedBy("tent").intValue());
		Assertions.assertEquals(3, tree.getPrefixedBy("tank").intValue());
		Assertions.assertEquals(4, tree.getPrefixedBy("rest").intValue());
	}

	@Test
	public void testMultipleInsertionOfTheSameKey(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);
		tree.put("rest", 4);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(1, tree.getPrefixedBy("test").intValue());
		Assertions.assertEquals(2, tree.getPrefixedBy("tent").intValue());
		Assertions.assertEquals(3, tree.getPrefixedBy("tank").intValue());
		Assertions.assertEquals(4, tree.getPrefixedBy("rest").intValue());

		tree.put("test", 9);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(9, tree.getPrefixedBy("test").intValue());
		Assertions.assertEquals(2, tree.getPrefixedBy("tent").intValue());
		Assertions.assertEquals(3, tree.getPrefixedBy("tank").intValue());
		Assertions.assertEquals(4, tree.getPrefixedBy("rest").intValue());
	}

	@Test
	public void testPrefixFetch(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("rest", 3);
		tree.put("tank", 4);

		Assertions.assertEquals(4, tree.size());
		assertEqualsWithSort(tree.getValuesPrefixedBy(StringUtils.EMPTY), new ArrayList<>(tree.valuesPrefixedBy()));
		assertEqualsWithSort(new Integer[]{1, 2, 4}, tree.getValuesPrefixedBy("t").toArray(new Integer[3]));
		assertEqualsWithSort(new Integer[]{1, 2}, tree.getValuesPrefixedBy("te").toArray(new Integer[2]));
		Assertions.assertArrayEquals(new Object[0], tree.getValuesPrefixedBy("asd").toArray());
	}

	@Test
	public void testSpook(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("pook", 1);
		tree.put("spook", 2);

		Assertions.assertEquals(2, tree.size());
		Assertions.assertEquals(tree.keySetPrefixedBy().stream().map(key -> String.join(StringUtils.EMPTY, key)).collect(Collectors.toSet()),
			new HashSet<>(Arrays.asList("pook", "spook")));
	}

	@Test
	public void testRemoval(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);

		Assertions.assertEquals(3, tree.size());
		Assertions.assertTrue(tree.containsKeyPrefixedBy("tent"));

		tree.removePrefixedBy("key");

		Assertions.assertEquals(3, tree.size());
		Assertions.assertTrue(tree.containsKeyPrefixedBy("tent"));

		tree.removePrefixedBy("tent");

		Assertions.assertEquals(2, tree.size());
		Assertions.assertEquals(1, tree.getPrefixedBy("test").intValue());
		Assertions.assertFalse(tree.containsKeyPrefixedBy("tent"));
		Assertions.assertEquals(3, tree.getPrefixedBy("tank").intValue());
	}

	@Test
	public void testManyInsertions(){
		RegExpRadixTree<BigInteger> tree = RegExpRadixTree.createTree();

		//n in [100, 500]
		int n = rng.nextInt(401) + 100;

		List<BigInteger> strings = generateRandomStrings(n);
		strings.forEach(x -> tree.put(x.toString(32), x));

		Assertions.assertEquals(strings.size(), tree.size());
		strings.forEach(x -> Assertions.assertTrue(tree.containsKeyPrefixedBy(x.toString(32))));
		assertEqualsWithSort(strings, new ArrayList<>(tree.valuesPrefixedBy()));
	}

	private List<BigInteger> generateRandomStrings(int n){
		List<BigInteger> strings = new ArrayList<>();
		while(n -- > 0){
			BigInteger bigint = new BigInteger(20, rng);
			if(!strings.contains(bigint))
				strings.add(bigint);
		}
		return strings;
	}


	@Test
	public void contains(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("abc", 1);
		tree.put("abb", 2);
		tree.put("ac", 3);
		tree.put("a", 4);

		Assertions.assertTrue(tree.containsKeyPrefixedBy("a"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("ab"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("c"));
	}

	@Test
	public void duplicatedEntry(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("abc", 1);
		tree.put("abc", 2);

		Assertions.assertFalse(tree.containsKeyPrefixedBy("a"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("ab"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("abc"));
		Assertions.assertEquals(2, tree.getPrefixedBy("abc").intValue());
		Assertions.assertFalse(tree.containsKeyPrefixedBy("c"));
	}

	@Test
	public void collectPrefixes(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("a", 1);
		tree.put("ab", 2);
		tree.put("bc", 3);
		tree.put("cd", 4);
		tree.put("abc", 5);

		List<Integer> prefixes = tree.getValuesPrefixedTo("abcd");
		Integer[] datas = prefixes.stream()
			.sorted()
			.toArray(Integer[]::new);
		Assertions.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	public void emptyConstructor(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		Assertions.assertTrue(tree.isEmpty());
		Assertions.assertFalse(tree.containsKeyPrefixedBy("word"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy(StringUtils.EMPTY));
	}

	@Test
	public void defaultValueConstructor(){
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

		Assertions.assertNull(tree.getPrefixedBy("meow"));

		tree.put("meow", Boolean.TRUE);

		Assertions.assertEquals(Boolean.TRUE, tree.getPrefixedBy("meow"));
		Assertions.assertNull(tree.getPrefixedBy("world"));
	}

	@Test
	public void simplePut(){
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

		Assertions.assertTrue(tree.isEmpty());

		tree.put("java.lang.", Boolean.TRUE);
		tree.put("java.i", Boolean.TRUE);
		tree.put("java.io.", Boolean.TRUE);
		tree.put("java.util.concurrent.", Boolean.TRUE);
		tree.put("java.util.", Boolean.FALSE);
		tree.put("java.lang.Boolean", Boolean.FALSE);

		Assertions.assertFalse(tree.isEmpty());
		Assertions.assertEquals(1, tree.getValuesPrefixedTo("java.lang.Integer").size());
		Assertions.assertEquals(1, tree.getValuesPrefixedTo("java.lang.Long").size());
		Assertions.assertEquals(2, tree.getValuesPrefixedTo("java.lang.Boolean").size());
		Assertions.assertEquals(2, tree.getValuesPrefixedTo("java.io.InputStream").size());
		Assertions.assertEquals(1, tree.getValuesPrefixedTo("java.util.ArrayList").size());
		Assertions.assertEquals(2, tree.getValuesPrefixedTo("java.util.concurrent.ConcurrentHashMap").size());
	}

	@Test
	public void hasStartsWithMatch(){
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(1, tree.getValuesPrefixedTo("wowzacowza").size());
	}

	@Test
	public void hasExactMatch(){
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertTrue(tree.containsKeyPrefixedBy("wowza"));
	}

	@Test
	public void getStartsWithMatch(){
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(1, tree.getValuesPrefixedTo("wowzacowza").size());
		Assertions.assertEquals(1, tree.getValuesPrefixedTo("bookshelfmania").size());
	}

	@Test
	public void getExactMatch(){
		RegExpRadixTree<Boolean> tree = RegExpRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(Boolean.FALSE, tree.getPrefixedBy("wowza"));
		Assertions.assertEquals(Boolean.TRUE, tree.getPrefixedBy("bookshelf"));
		Assertions.assertNull(tree.getPrefixedBy("bookshelf2"));
	}

	@Test
	public void removeBack(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("hello", 0);
		tree.put("hello world", 1);

		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertEquals(1, tree.getPrefixedBy("hello world").intValue());

		Integer r1 = tree.removePrefixedBy("hello world");

		Assertions.assertNotNull(r1);
		Assertions.assertEquals(1, r1.intValue());

		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertNull(tree.getPrefixedBy("hello world"));
	}

	@Test
	public void removeFront(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("hello", 0);
		tree.put("hello world", 1);

		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertEquals(1, tree.getPrefixedBy("hello world").intValue());

		Integer r0 = tree.removePrefixedBy("hello world");

		Assertions.assertNotNull(r0);
		Assertions.assertEquals(1, r0.intValue());

		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertNull(tree.getPrefixedBy("hello world"));
	}

	@Test
	public void removeFrontManyChildren(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("hello", 0);
		tree.put("hello world", 1);
		tree.put("hello, clarice", 2);

		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertEquals(1, tree.getPrefixedBy("hello world").intValue());
		Assertions.assertEquals(2, tree.getPrefixedBy("hello, clarice").intValue());

		Integer r0 = tree.removePrefixedBy("hello world");

		Assertions.assertNotNull(r0);
		Assertions.assertEquals(1, r0.intValue());

		Assertions.assertNull(tree.getPrefixedBy("hello world"));
		Assertions.assertEquals(0, tree.getPrefixedBy("hello").intValue());
		Assertions.assertEquals(2, tree.getPrefixedBy("hello, clarice").intValue());
	}


	public static <T extends Comparable<? super T>> void assertEqualsWithSort(List<T> a, List<T> b){
		Collections.sort(a);
		Collections.sort(b);
		Assertions.assertEquals(a, b);
	}

	public static <T extends Comparable<? super T>> void assertEqualsWithSort(T[] a, T[] b){
		Arrays.sort(a);
		Arrays.sort(b);
		Assertions.assertArrayEquals(a, b);
	}


	@Test
	public void testSearchForPartialParentAndLeafKeyWhenOverlapExists(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(0, tree.getValuesPrefixedBy("abe").size());
		Assertions.assertEquals(0, tree.getValuesPrefixedBy("abd").size());
	}

	@Test
	public void testSearchForLeafNodesWhenOverlapExists(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(1, tree.getValuesPrefixedBy("abcd").size());
		Assertions.assertEquals(1, tree.getValuesPrefixedBy("abce").size());
	}

	@Test
	public void testSearchForStringSmallerThanSharedParentWhenOverlapExists(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(2, tree.getValuesPrefixedBy("ab").size());
		Assertions.assertEquals(2, tree.getValuesPrefixedBy("a").size());
	}

	@Test
	public void testSearchForStringEqualToSharedParentWhenOverlapExists(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(2, tree.getValuesPrefixedBy("abc").size());
	}

//	@Test
//	public void testInsert(){
//		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();
//
//		tree.put("apple", "apple");
//		tree.put("bat", "bat");
//		tree.put("ape", "ape");
//		tree.put("bath", "bath");
//		tree.put("banana", "banana");
//
//		Assertions.assertEquals(new RadixTreeNode(RegExpSequencer.splitSequence("ple"), "apple"), tree.find("apple"));
//		Assertions.assertEquals(new RadixTreeNode(RegExpSequencer.splitSequence("t"), "bat"), tree.find("bat"));
//		Assertions.assertEquals(new RadixTreeNode(RegExpSequencer.splitSequence("e"), "ape"), tree.find("ape"));
//		Assertions.assertEquals(new RadixTreeNode(RegExpSequencer.splitSequence("h"), "bath"), tree.find("bath"));
//		Assertions.assertEquals(new RadixTreeNode(RegExpSequencer.splitSequence("nana"), "banana"), tree.find("banana"));
//	}

	@Test
	public void testInsertExistingUnrealNodeConvertsItToReal(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assertions.assertFalse(tree.containsKeyPrefixedBy("apple"));

		tree.put("apple", "apple");

		Assertions.assertTrue(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testDuplicatesAllowed(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");

		try{
			tree.put("apple", "apple2");

			Assertions.assertTrue(true);
		}
		catch(DuplicateKeyException e){
			Assertions.fail("Duplicate should have been allowed");
		}
	}

	@Test
	public void testDuplicatesNotAllowed(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTreeNoDuplicates();

		tree.put("apple", "apple");

		try{
			tree.put("apple", "apple2");

			Assertions.fail("Duplicate should not have been allowed");
		}
		catch(DuplicateKeyException e){
			Assertions.assertEquals("Duplicate key: 'apple'", e.getMessage());
		}
	}

	@Test
	public void testInsertWithRepeatingPatternsInKey(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("xbox 360", "xbox 360");
		tree.put("xbox", "xbox");
		tree.put("xbox 360 games", "xbox 360 games");
		tree.put("xbox games", "xbox games");
		tree.put("xbox xbox 360", "xbox xbox 360");
		tree.put("xbox xbox", "xbox xbox");
		tree.put("xbox 360 xbox games", "xbox 360 xbox games");
		tree.put("xbox games 360", "xbox games 360");
		tree.put("xbox 360 360", "xbox 360 360");
		tree.put("xbox 360 xbox 360", "xbox 360 xbox 360");
		tree.put("360 xbox games 360", "360 xbox games 360");
		tree.put("xbox xbox 361", "xbox xbox 361");

		Assertions.assertEquals(12, tree.size());
	}

	@Test
	public void testDeleteNodeWithNoChildren(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");

		Assertions.assertNotNull(tree.removePrefixedBy("apple"));
	}

	@Test
	public void testDeleteNodeWithOneChild(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");

		Assertions.assertNotNull(tree.removePrefixedBy("apple"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("applepie"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testDeleteNodeWithMultipleChildren(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assertions.assertNotNull(tree.removePrefixedBy("apple"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("applepie"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("applecrisp"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testCantDeleteSomethingThatDoesntExist(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		Assertions.assertNull(tree.removePrefixedBy("apple"));
	}

	@Test
	public void testCantDeleteSomethingThatWasAlreadyDeleted(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.removePrefixedBy("apple");

		Assertions.assertNull(tree.removePrefixedBy("apple"));
	}

	@Test
	public void testChildrenNotAffectedWhenOneIsDeleted(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		tree.removePrefixedBy("apple");

		Assertions.assertTrue(tree.containsKeyPrefixedBy("appleshack"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("applepie"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("ape"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testSiblingsNotAffectedWhenOneIsDeleted(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ball", "ball");

		tree.removePrefixedBy("apple");

		Assertions.assertTrue(tree.containsKeyPrefixedBy("ball"));
	}

	@Test
	public void testCantDeleteUnrealNode(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertNull(tree.removePrefixedBy("ap"));
	}

	@Test
	public void testCantFindRootNode(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		Assertions.assertNull(tree.findPrefixedBy(""));
	}

	@Test
	public void testFindSimpleInsert(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");

		Assertions.assertNotNull(tree.findPrefixedBy("apple"));
	}

	@Test
	public void testContainsSimpleInsert(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");

		Assertions.assertTrue(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testFindChildInsert(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");
		tree.put("appletree", "appletree");
		tree.put("appleshackcream", "appleshackcream");

		Assertions.assertNotNull(tree.findPrefixedBy("appletree"));
		Assertions.assertNotNull(tree.findPrefixedBy("appleshackcream"));
		Assertions.assertNotNull(tree.containsKeyPrefixedBy("ape"));
	}

	@Test
	public void testGetPrefixes(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("h", "h");
		tree.put("hey", "hey");
		tree.put("hell", "hell");
		tree.put("hello", "hello");
		tree.put("hat", "hat");
		tree.put("cat", "cat");

		Assertions.assertFalse(tree.getValuesPrefixedTo("helloworld").isEmpty());
		Assertions.assertTrue(tree.getValuesPrefixedTo("helloworld").contains("h"));
		Assertions.assertTrue(tree.getValuesPrefixedTo("helloworld").contains("hell"));
		Assertions.assertTrue(tree.getValuesPrefixedTo("helloworld").contains("hello"));
		Assertions.assertTrue(!tree.getValuesPrefixedTo("helloworld").contains("he"));
		Assertions.assertTrue(!tree.getValuesPrefixedTo("helloworld").contains("hat"));
		Assertions.assertTrue(!tree.getValuesPrefixedTo("helloworld").contains("cat"));
		Assertions.assertTrue(!tree.getValuesPrefixedTo("helloworld").contains("hey"));
		Assertions.assertTrue(tree.getValuesPrefixedTo("animal").isEmpty());
	}

	@Test
	public void testContainsChildInsert(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");
		tree.put("appletree", "appletree");
		tree.put("appleshackcream", "appleshackcream");

		Assertions.assertTrue(tree.containsKeyPrefixedBy("appletree"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("appleshackcream"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("ape"));
	}

	@Test
	public void testCantFindNonexistantNode(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		Assertions.assertNull(tree.findPrefixedBy("apple"));
	}

	@Test
	public void testDoesntContainNonexistantNode(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		Assertions.assertFalse(tree.containsKeyPrefixedBy("apple"));
	}

	@Test
	public void testCantFindUnrealNode(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertNull(tree.findPrefixedBy("ap"));
	}

	@Test
	public void testDoesntContainUnrealNode(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertFalse(tree.containsKeyPrefixedBy("ap"));
	}

	@Test
	public void testSearchPrefix_LimitGreaterThanPossibleResults(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValuesPrefixedBy("app");
		Assertions.assertEquals(4, result.size());

		Assertions.assertTrue(result.contains("appleshack"));
		Assertions.assertTrue(result.contains("appleshackcream"));
		Assertions.assertTrue(result.contains("applepie"));
		Assertions.assertTrue(result.contains("apple"));
	}

	@Test
	public void testSearchPrefix_LimitLessThanPossibleResults(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValuesPrefixedBy("appl");
		Assertions.assertEquals(4, result.size());

		Assertions.assertTrue(result.contains("appleshack"));
		Assertions.assertTrue(result.contains("applepie"));
		Assertions.assertTrue(result.contains("apple"));
	}

	@Test
	public void testGetSize(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		Assertions.assertTrue(tree.size() == 5);
	}

	@Test
	public void testDeleteReducesSize(){
		RegExpRadixTree<String> tree = RegExpRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");

		tree.removePrefixedBy("appleshack");

		Assertions.assertTrue(tree.size() == 1);
	}


	@Test
	public void splitting(){
		String[] parts = RegExpSequencer.splitSequence("abd");
		Assertions.assertArrayEquals(new String[]{"a", "b", "d"}, parts);

		parts = RegExpSequencer.splitSequence("a[b]d");
		Assertions.assertArrayEquals(new String[]{"a", "[b]", "d"}, parts);

		parts = RegExpSequencer.splitSequence("a[bc]d");
		Assertions.assertArrayEquals(new String[]{"a", "[bc]", "d"}, parts);

		parts = RegExpSequencer.splitSequence("a[^b]d");
		Assertions.assertArrayEquals(new String[]{"a", "[^b]", "d"}, parts);

		parts = RegExpSequencer.splitSequence("a[^bc]d");
		Assertions.assertArrayEquals(new String[]{"a", "[^bc]", "d"}, parts);
	}

	@Test
	public void containsRegExp(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("a[bd]c", 1);
		tree.put("a[bd]b", 2);
		tree.put("a[^bcd]", 3);
		tree.put("a", 4);

		Assertions.assertTrue(tree.containsKeyPrefixedBy("a"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("abc"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("adc"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("aec"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("ae"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("ac"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("c"));
	}

	@Test
	public void duplicatedEntryRegExp(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("a[bc]c", 1);
		tree.put("a[bc]c", 2);

		Assertions.assertFalse(tree.containsKeyPrefixedBy("a"));
		Assertions.assertFalse(tree.containsKeyPrefixedBy("ab"));
		Assertions.assertTrue(tree.containsKeyPrefixedBy("abc"));
		Assertions.assertEquals(2, tree.getPrefixedBy("acc").intValue());
		Assertions.assertFalse(tree.containsKeyPrefixedBy("c"));
	}

	@Test
	public void collectPrefixesRegExp(){
		RegExpRadixTree<Integer> tree = RegExpRadixTree.createTree();

		tree.put("a", 1);
		tree.put("a[bcd]", 2);
		tree.put("[^ac]c", 3);
		tree.put("cd", 4);
		tree.put("aec", 5);

		Assertions.assertArrayEquals(new Integer[]{1, 2}, tree.getValuesPrefixedTo("abcd").toArray(new Integer[2]));
		Assertions.assertArrayEquals(new Integer[]{3}, tree.getValuesPrefixedTo("ec").toArray(new Integer[1]));
	}

}
