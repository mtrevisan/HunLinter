package unit731.hunspeller.collections.radixtree;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.collections.radixtree.utils.RadixTreeNode;
import unit731.hunspeller.collections.radixtree.exceptions.DuplicateKeyException;


class StringRadixTreeTest{

	private final SecureRandom rng = new SecureRandom();


	@Test
	void emptyTree(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		Assertions.assertTrue(tree.isEmpty());
		Assertions.assertEquals(0, tree.size());
	}

	@Test
	void singleInsertion(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("test", 1);

		Assertions.assertEquals(1, tree.size());
		Assertions.assertTrue(tree.containsKey("test", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("tes", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("testt", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void multipleInsertions(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);
		tree.put("rest", 4);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(1, tree.get("test", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(2, tree.get("tent", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(3, tree.get("tank", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(4, tree.get("rest", RadixTree.PrefixType.PREFIXED_BY).intValue());
	}

	@Test
	void multipleInsertionOfTheSameKey(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);
		tree.put("rest", 4);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(1, tree.get("test", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(2, tree.get("tent", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(3, tree.get("tank", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(4, tree.get("rest", RadixTree.PrefixType.PREFIXED_BY).intValue());

		tree.put("test", 9);

		Assertions.assertEquals(4, tree.size());
		Assertions.assertEquals(9, tree.get("test", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(2, tree.get("tent", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(3, tree.get("tank", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(4, tree.get("rest", RadixTree.PrefixType.PREFIXED_BY).intValue());
	}

	@Test
	void prefixFetch(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("rest", 3);
		tree.put("tank", 4);

		Assertions.assertEquals(4, tree.size());
		assertEqualsWithSort(tree.getValues(StringUtils.EMPTY, RadixTree.PrefixType.PREFIXED_BY), new ArrayList<>(tree.values(RadixTree.PrefixType.PREFIXED_BY)));
		assertEqualsWithSort(new Integer[]{1, 2, 4}, tree.getValues("t", RadixTree.PrefixType.PREFIXED_BY).toArray(new Integer[3]));
		assertEqualsWithSort(new Integer[]{1, 2}, tree.getValues("te", RadixTree.PrefixType.PREFIXED_BY).toArray(new Integer[2]));
		Assertions.assertArrayEquals(new Object[0], tree.getValues("asd", RadixTree.PrefixType.PREFIXED_BY).toArray());
	}

	@Test
	void spook(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("pook", 1);
		tree.put("spook", 2);

		Assertions.assertEquals(2, tree.size());
		assertEqualsWithSort(tree.keySet(RadixTree.PrefixType.PREFIXED_BY).toArray(new String[2]), new String[]{
			"pook",
			"spook"
		});
	}

	@Test
	void removal(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("test", 1);
		tree.put("tent", 2);
		tree.put("tank", 3);

		Assertions.assertEquals(3, tree.size());
		Assertions.assertTrue(tree.containsKey("tent", RadixTree.PrefixType.PREFIXED_BY));

		tree.remove("key");

		Assertions.assertEquals(3, tree.size());
		Assertions.assertTrue(tree.containsKey("tent", RadixTree.PrefixType.PREFIXED_BY));

		tree.remove("tent");

		Assertions.assertEquals(2, tree.size());
		Assertions.assertEquals(1, tree.get("test", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertFalse(tree.containsKey("tent", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(3, tree.get("tank", RadixTree.PrefixType.PREFIXED_BY).intValue());
	}

	@Test
	void manyInsertions(){
		StringRadixTree<BigInteger> tree = StringRadixTree.createTree();

		//n in [100, 500]
		int n = rng.nextInt(401) + 100;

		List<BigInteger> strings = generateRandomStrings(n);
		strings.forEach(x -> tree.put(x.toString(32), x));

		Assertions.assertEquals(strings.size(), tree.size());
		strings.forEach(x -> Assertions.assertTrue(tree.containsKey(x.toString(32), RadixTree.PrefixType.PREFIXED_BY)));
		assertEqualsWithSort(strings, new ArrayList<>(tree.values(RadixTree.PrefixType.PREFIXED_BY)));
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
	void contains(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("abc", 1);
		tree.put("abb", 2);
		tree.put("ac", 3);
		tree.put("a", 4);

		Assertions.assertTrue(tree.containsKey("a", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("ab", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("c", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void duplicatedEntry(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("abc", 1);
		tree.put("abc", 2);

		Assertions.assertFalse(tree.containsKey("a", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("ab", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("abc", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(2, tree.get("abc", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertFalse(tree.containsKey("c", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void collectPrefixes(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("a", 1);
		tree.put("ab", 2);
		tree.put("bc", 3);
		tree.put("cd", 4);
		tree.put("abc", 5);

		List<Integer> prefixes = tree.getValues("abcd", RadixTree.PrefixType.PREFIXED_TO);
		Integer[] datas = prefixes.stream()
			.sorted()
			.toArray(Integer[]::new);
		Assertions.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	void emptyConstructor(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		Assertions.assertTrue(tree.isEmpty());
		Assertions.assertFalse(tree.containsKey("word", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey(StringUtils.EMPTY, RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void defaultValueConstructor(){
		StringRadixTree<Boolean> tree = StringRadixTree.createTree();

		Assertions.assertNull(tree.get("meow", RadixTree.PrefixType.PREFIXED_BY));

		tree.put("meow", Boolean.TRUE);

		Assertions.assertEquals(Boolean.TRUE, tree.get("meow", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertNull(tree.get("world", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void simplePut(){
		StringRadixTree<Boolean> tree = StringRadixTree.createTree();

		Assertions.assertTrue(tree.isEmpty());

		tree.put("java.lang.", Boolean.TRUE);
		tree.put("java.i", Boolean.TRUE);
		tree.put("java.io.", Boolean.TRUE);
		tree.put("java.util.concurrent.", Boolean.TRUE);
		tree.put("java.util.", Boolean.FALSE);
		tree.put("java.lang.Boolean", Boolean.FALSE);

		Assertions.assertFalse(tree.isEmpty());
		Assertions.assertEquals(1, tree.getValues("java.lang.Integer", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(1, tree.getValues("java.lang.Long", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(2, tree.getValues("java.lang.Boolean", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(2, tree.getValues("java.io.InputStream", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(1, tree.getValues("java.util.ArrayList", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(2, tree.getValues("java.util.concurrent.ConcurrentHashMap", RadixTree.PrefixType.PREFIXED_TO).size());
	}

	@Test
	void hasStartsWithMatch(){
		StringRadixTree<Boolean> tree = StringRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(1, tree.getValues("wowzacowza", RadixTree.PrefixType.PREFIXED_TO).size());
	}

	@Test
	void hasExactMatch(){
		StringRadixTree<Boolean> tree = StringRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertTrue(tree.containsKey("wowza", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void getStartsWithMatch(){
		StringRadixTree<Boolean> tree = StringRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(1, tree.getValues("wowzacowza", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(1, tree.getValues("bookshelfmania", RadixTree.PrefixType.PREFIXED_TO).size());
	}

	@Test
	void getExactMatch(){
		StringRadixTree<Boolean> tree = StringRadixTree.createTree();

		tree.put("bookshelf", Boolean.TRUE);
		tree.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(Boolean.FALSE, tree.get("wowza", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(Boolean.TRUE, tree.get("bookshelf", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertNull(tree.get("bookshelf2", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void removeBack(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("hello", 0);
		tree.put("hello world", 1);

		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(1, tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY).intValue());

		Integer r1 = tree.remove("hello world");

		Assertions.assertNotNull(r1);
		Assertions.assertEquals(1, r1.intValue());

		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertNull(tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void removeFront(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("hello", 0);
		tree.put("hello world", 1);

		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(1, tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY).intValue());

		Integer r0 = tree.remove("hello world");

		Assertions.assertNotNull(r0);
		Assertions.assertEquals(1, r0.intValue());

		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertNull(tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void removeFrontManyChildren(){
		StringRadixTree<Integer> tree = StringRadixTree.createTree();

		tree.put("hello", 0);
		tree.put("hello world", 1);
		tree.put("hello, clarice", 2);

		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(1, tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(2, tree.get("hello, clarice", RadixTree.PrefixType.PREFIXED_BY).intValue());

		Integer r0 = tree.remove("hello world");

		Assertions.assertNotNull(r0);
		Assertions.assertEquals(1, r0.intValue());

		Assertions.assertNull(tree.get("hello world", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(0, tree.get("hello", RadixTree.PrefixType.PREFIXED_BY).intValue());
		Assertions.assertEquals(2, tree.get("hello, clarice", RadixTree.PrefixType.PREFIXED_BY).intValue());
	}


	static <T extends Comparable<? super T>> void assertEqualsWithSort(List<T> a, List<T> b){
		Collections.sort(a);
		Collections.sort(b);
		Assertions.assertEquals(a, b);
	}

	static <T extends Comparable<? super T>> void assertEqualsWithSort(T[] a, T[] b){
		Arrays.sort(a);
		Arrays.sort(b);
		Assertions.assertArrayEquals(a, b);
	}


	@Test
	void searchForPartialParentAndLeafKeyWhenOverlapExists(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(0, tree.getValues("abe", RadixTree.PrefixType.PREFIXED_BY).size());
		Assertions.assertEquals(0, tree.getValues("abd", RadixTree.PrefixType.PREFIXED_BY).size());
	}

	@Test
	void searchForLeafNodesWhenOverlapExists(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(1, tree.getValues("abcd", RadixTree.PrefixType.PREFIXED_BY).size());
		Assertions.assertEquals(1, tree.getValues("abce", RadixTree.PrefixType.PREFIXED_BY).size());
	}

	@Test
	void searchForStringSmallerThanSharedParentWhenOverlapExists(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(2, tree.getValues("ab", RadixTree.PrefixType.PREFIXED_BY).size());
		Assertions.assertEquals(2, tree.getValues("a", RadixTree.PrefixType.PREFIXED_BY).size());
	}

	@Test
	void searchForStringEqualToSharedParentWhenOverlapExists(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("abcd", "abcd");
		tree.put("abce", "abce");

		Assertions.assertEquals(2, tree.getValues("abc", RadixTree.PrefixType.PREFIXED_BY).size());
	}

	@Test
	void insert(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("bat", "bat");
		tree.put("ape", "ape");
		tree.put("bath", "bath");
		tree.put("banana", "banana");

		Assertions.assertEquals(new RadixTreeNode<>("ple", "apple"), tree.find("apple", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(new RadixTreeNode<>("t", "bat"), tree.find("bat", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(new RadixTreeNode<>("e", "ape"), tree.find("ape", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(new RadixTreeNode<>("h", "bath"), tree.find("bath", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertEquals(new RadixTreeNode<>("nana", "banana"), tree.find("banana", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void insertExistingUnrealNodeConvertsItToReal(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assertions.assertFalse(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));

		tree.put("apple", "apple");

		Assertions.assertTrue(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void duplicatesAllowed(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

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
	void duplicatesNotAllowed(){
		StringRadixTree<String> tree = StringRadixTree.createTreeNoDuplicates();

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
	void insertWithRepeatingPatternsInKey(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

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
	void deleteNodeWithNoChildren(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");

		Assertions.assertNotNull(tree.remove("apple"));
	}

	@Test
	void deleteNodeWithOneChild(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");

		Assertions.assertNotNull(tree.remove("apple"));
		Assertions.assertTrue(tree.containsKey("applepie", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void deleteNodeWithMultipleChildren(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("applepie", "applepie");
		tree.put("applecrisp", "applecrisp");

		Assertions.assertNotNull(tree.remove("apple"));
		Assertions.assertTrue(tree.containsKey("applepie", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("applecrisp", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void cantDeleteSomethingThatDoesntExist(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		Assertions.assertNull(tree.remove("apple"));
	}

	@Test
	void cantDeleteSomethingThatWasAlreadyDeleted(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.remove("apple");

		Assertions.assertNull(tree.remove("apple"));
	}

	@Test
	void childrenNotAffectedWhenOneIsDeleted(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		tree.remove("apple");

		Assertions.assertTrue(tree.containsKey("appleshack", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("applepie", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("ape", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertFalse(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void siblingsNotAffectedWhenOneIsDeleted(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ball", "ball");

		tree.remove("apple");

		Assertions.assertTrue(tree.containsKey("ball", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void cantDeleteUnrealNode(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertNull(tree.remove("ap"));
	}

	@Test
	void cantFindRootNode(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		Assertions.assertNull(tree.find("", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void findSimpleInsert(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");

		Assertions.assertNotNull(tree.find("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void containsSimpleInsert(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");

		Assertions.assertTrue(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void findChildInsert(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");
		tree.put("appletree", "appletree");
		tree.put("appleshackcream", "appleshackcream");

		Assertions.assertNotNull(tree.find("appletree", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertNotNull(tree.find("appleshackcream", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertNotNull(tree.containsKey("ape", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void getPrefixes(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("h", "h");
		tree.put("hey", "hey");
		tree.put("hell", "hell");
		tree.put("hello", "hello");
		tree.put("hat", "hat");
		tree.put("cat", "cat");

		Assertions.assertFalse(tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).isEmpty());
		Assertions.assertTrue(tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("h"));
		Assertions.assertTrue(tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hell"));
		Assertions.assertTrue(tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hello"));
		Assertions.assertTrue(!tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("he"));
		Assertions.assertTrue(!tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hat"));
		Assertions.assertTrue(!tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("cat"));
		Assertions.assertTrue(!tree.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hey"));
		Assertions.assertTrue(tree.getValues("animal", RadixTree.PrefixType.PREFIXED_TO).isEmpty());
	}

	@Test
	void containsChildInsert(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");
		tree.put("appletree", "appletree");
		tree.put("appleshackcream", "appleshackcream");

		Assertions.assertTrue(tree.containsKey("appletree", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("appleshackcream", RadixTree.PrefixType.PREFIXED_BY));
		Assertions.assertTrue(tree.containsKey("ape", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void cantFindNonexistantNode(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		Assertions.assertNull(tree.find("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void doesntContainNonexistantNode(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		Assertions.assertFalse(tree.containsKey("apple", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void cantFindUnrealNode(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertNull(tree.find("ap", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void doesntContainUnrealNode(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("ape", "ape");

		Assertions.assertFalse(tree.containsKey("ap", RadixTree.PrefixType.PREFIXED_BY));
	}

	@Test
	void searchPrefix_LimitGreaterThanPossibleResults(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValues("app", RadixTree.PrefixType.PREFIXED_BY);
		Assertions.assertEquals(4, result.size());

		Assertions.assertTrue(result.contains("appleshack"));
		Assertions.assertTrue(result.contains("appleshackcream"));
		Assertions.assertTrue(result.contains("applepie"));
		Assertions.assertTrue(result.contains("apple"));
	}

	@Test
	void searchPrefix_LimitLessThanPossibleResults(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		List<String> result = tree.getValues("appl", RadixTree.PrefixType.PREFIXED_BY);
		Assertions.assertEquals(4, result.size());

		Assertions.assertTrue(result.contains("appleshack"));
		Assertions.assertTrue(result.contains("applepie"));
		Assertions.assertTrue(result.contains("apple"));
	}

	@Test
	void getSize(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");
		tree.put("appleshackcream", "appleshackcream");
		tree.put("applepie", "applepie");
		tree.put("ape", "ape");

		Assertions.assertTrue(tree.size() == 5);
	}

	@Test
	void deleteReducesSize(){
		StringRadixTree<String> tree = StringRadixTree.createTree();

		tree.put("apple", "apple");
		tree.put("appleshack", "appleshack");

		tree.remove("appleshack");

		Assertions.assertTrue(tree.size() == 1);
	}

}
