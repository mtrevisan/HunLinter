package unit731.hunspeller.collections.ahocorasicktrie;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unit731.hunspeller.collections.ahocorasicktrie.exceptions.DuplicateKeyException;


public class AhoCorasickTrieTest{

	@Test
	public void emptyTree(){
		Map<String, String> map = new HashMap<>();
		AhoCorasickTrie<String> trie = new AhoCorasickTrieBuilder<String>()
			.build(map);

		Assertions.assertTrue(trie.isEmpty());
		Assertions.assertEquals(0, trie.size());
	}

	@Test
	public void singleInsertion(){
		Map<String, Integer> map = Collections.singletonMap("test", 1);
		AhoCorasickTrie<Integer> trie = new AhoCorasickTrieBuilder<Integer>()
			.build(map);

		Assertions.assertEquals(1, trie.size());
		Assertions.assertTrue(trie.hasKey("test"));
		Assertions.assertFalse(trie.hasKey("tes"));
		Assertions.assertFalse(trie.hasKey("testt"));
		Assertions.assertTrue(trie.containsKey("test"));
		Assertions.assertFalse(trie.containsKey("tes"));
		Assertions.assertTrue(trie.containsKey("testt"));
	}

	@Test
	public void multipleInsertions(){
		Map<String, Integer> map = new HashMap<>();
		map.put("test", 1);
		map.put("tent", 2);
		map.put("tentest", 21);
		map.put("tank", 3);
		map.put("rest", 4);
		AhoCorasickTrie<Integer> trie = new AhoCorasickTrieBuilder<Integer>()
			.build(map);

		Assertions.assertEquals(5, trie.size());
		Assertions.assertEquals(1, trie.get("test").intValue());
		Assertions.assertEquals(2, trie.get("tent").intValue());
		Assertions.assertEquals(3, trie.get("tank").intValue());
		Assertions.assertEquals(4, trie.get("rest").intValue());
	}

	@Test
	public void multiplePutInsertions(){
		AhoCorasickTrieBuilder<Integer> builder = new AhoCorasickTrieBuilder<>();
		builder.put("test", 1);
		builder.put("tent", 2);
		builder.put("tentest", 21);
		builder.put("tank", 3);
		builder.put("rest", 4);
		AhoCorasickTrie<Integer> trie = builder.getTrie();

		Assertions.assertEquals(5, trie.size());
		Assertions.assertEquals(1, trie.get("test").intValue());
		Assertions.assertEquals(2, trie.get("tent").intValue());
		Assertions.assertEquals(3, trie.get("tank").intValue());
		Assertions.assertEquals(4, trie.get("rest").intValue());
	}

	@Test
	public void prepare(){
		Map<String, Integer> map = new HashMap<>();
		map.put("test", 1);
		map.put("tent", 2);
		map.put("tank", 3);
		map.put("rest", 4);
		AhoCorasickTrie<Integer> trie = new AhoCorasickTrieBuilder<Integer>()
			.build(map);

		List<SearchResult<Integer>> results = trie.searchInText("resting in the test");
		Assertions.assertEquals(2, results.size());
		SearchResult<Integer> search = results.get(0);
		Assertions.assertEquals(0, search.getIndexBegin());
		Assertions.assertEquals(4, search.getValue().intValue());
		search = results.get(1);
		Assertions.assertEquals(15, search.getIndexBegin());
		Assertions.assertEquals(1, search.getValue().intValue());

		results = trie.searchInText("blah");
		Assertions.assertTrue(results.isEmpty());
	}

	@Test
	public void multipleInsertionOfTheSameKey(){
		Map<String, Integer> map = new HashMap<>();
		map.put("test", 1);
		map.put("tent", 2);
		map.put("tank", 3);
		map.put("rest", 4);
		map.put("test", 9);
		AhoCorasickTrie<Integer> trie = new AhoCorasickTrieBuilder<Integer>()
			.build(map);

		Assertions.assertEquals(4, trie.size());
		Assertions.assertEquals(9, trie.get("test").intValue());
		Assertions.assertEquals(2, trie.get("tent").intValue());
		Assertions.assertEquals(3, trie.get("tank").intValue());
		Assertions.assertEquals(4, trie.get("rest").intValue());
	}

	@Test
	public void multipleInsertionOfTheSameKeyNoDuplicate(){
		Map<String, Integer> map = new HashMap<>();
		map.put("test", 1);
		map.put("tent", 2);
		map.put("tank", 3);
		map.put("rest", 4);
		AhoCorasickTrie<Integer> trie = new AhoCorasickTrieBuilder<Integer>()
			.build(map);

		Throwable exception = Assertions.assertThrows(DuplicateKeyException.class, () -> {
			AhoCorasickTrieBuilder<Integer> builder = new AhoCorasickTrieBuilder<>(trie)
				.noDuplicatesAllowed();
			builder.put("test", 9);
		});
		Assertions.assertEquals("Duplicate key inserted: 'test'", exception.getMessage());
	}

	/*@Test
	public void removal(){
		Map<String, Integer> map = new HashMap<>();
		map.put("test", 1);
		map.put("tent", 2);
		map.put("tank", 3);
		AhoCorasickTrie<Integer> trie = new AhoCorasickTrieBuilder<Integer>()
			.build(map);

		Assertions.assertEquals(3, trie.size());
		Assertions.assertTrue(trie.containsKey("tent"));

		trie.remove("key");

		Assertions.assertEquals(3, trie.size());
		Assertions.assertTrue(trie.containsKey("tent"));

		trie.remove("tent");

		Assertions.assertEquals(2, trie.size());
		Assertions.assertEquals(1, trie.get("test").intValue());
		Assertions.assertFalse(trie.containsKey("tent"));
		Assertions.assertEquals(3, trie.get("tank").intValue());
	}

	/*@Test
	public void manyInsertions(){
		RadixTree<String, BigInteger> trie = AhoCorasickTree.createTree(new StringSequencer());

		//n in [100, 500]
		int n = rng.nextInt(401) + 100;

		List<BigInteger> strings = generateRandomStrings(n);
		strings.forEach(x -> map.put(x.toString(32), x));

		Assertions.assertEquals(strings.size(), trie.size());
		strings.forEach(x -> Assertions.assertTrue(trie.containsKey(x.toString(32))));
		assertEqualsWithSort(strings, new ArrayList<>(trie.values()));
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
		RadixTree<String, Integer> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("abc", 1);
		map.put("abb", 2);
		map.put("ac", 3);
		map.put("a", 4);

		Assertions.assertTrue(trie.containsKey("a"));
		Assertions.assertFalse(trie.containsKey("ab"));
		Assertions.assertFalse(trie.containsKey("c"));
	}

	@Test
	public void duplicatedEntry(){
		RadixTree<String, Integer> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("abc", 1);
		map.put("abc", 2);

		Assertions.assertFalse(trie.containsKey("a"));
		Assertions.assertFalse(trie.containsKey("ab"));
		Assertions.assertTrue(trie.containsKey("abc"));
		Assertions.assertEquals(2, trie.get("abc").intValue());
		Assertions.assertFalse(trie.containsKey("c"));
	}

	@Test
	public void collectPrefixes(){
		RadixTree<String, Integer> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("a", 1);
		map.put("ab", 2);
		map.put("bc", 3);
		map.put("cd", 4);
		map.put("abc", 5);

		List<Integer> prefixes = trie.getValues("abcd", RadixTree.PrefixType.PREFIXED_TO);
		Integer[] datas = prefixes.stream()
			.sorted()
			.toArray(Integer[]::new);
		Assertions.assertArrayEquals(new Integer[]{1, 2, 5}, datas);
	}

	@Test
	public void emptyConstructor(){
		RadixTree<String, Integer> trie = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertTrue(trie.isEmpty());
		Assertions.assertFalse(trie.containsKey("word"));
		Assertions.assertFalse(trie.containsKey(StringUtils.EMPTY));
	}

	@Test
	public void defaultValueConstructor(){
		RadixTree<String, Boolean> trie = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(trie.get("meow"));

		map.put("meow", Boolean.TRUE);

		Assertions.assertEquals(Boolean.TRUE, trie.get("meow"));
		Assertions.assertNull(trie.get("world"));
	}

	@Test
	public void simplePut(){
		RadixTree<String, Boolean> trie = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertTrue(trie.isEmpty());

		map.put("java.lang.", Boolean.TRUE);
		map.put("java.i", Boolean.TRUE);
		map.put("java.io.", Boolean.TRUE);
		map.put("java.util.concurrent.", Boolean.TRUE);
		map.put("java.util.", Boolean.FALSE);
		map.put("java.lang.Boolean", Boolean.FALSE);

		Assertions.assertFalse(trie.isEmpty());
		Assertions.assertEquals(1, trie.getValues("java.lang.Integer", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(1, trie.getValues("java.lang.Long", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(2, trie.getValues("java.lang.Boolean", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(2, trie.getValues("java.io.InputStream", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(1, trie.getValues("java.util.ArrayList", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(2, trie.getValues("java.util.concurrent.ConcurrentHashMap", RadixTree.PrefixType.PREFIXED_TO).size());
	}

	@Test
	public void hasStartsWithMatch(){
		RadixTree<String, Boolean> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("bookshelf", Boolean.TRUE);
		map.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(1, trie.getValues("wowzacowza", RadixTree.PrefixType.PREFIXED_TO).size());
	}

	@Test
	public void hasExactMatch(){
		RadixTree<String, Boolean> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("bookshelf", Boolean.TRUE);
		map.put("wowza", Boolean.FALSE);

		Assertions.assertTrue(trie.containsKey("wowza"));
	}

	@Test
	public void getStartsWithMatch(){
		RadixTree<String, Boolean> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("bookshelf", Boolean.TRUE);
		map.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(1, trie.getValues("wowzacowza", RadixTree.PrefixType.PREFIXED_TO).size());
		Assertions.assertEquals(1, trie.getValues("bookshelfmania", RadixTree.PrefixType.PREFIXED_TO).size());
	}

	@Test
	public void getExactMatch(){
		RadixTree<String, Boolean> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("bookshelf", Boolean.TRUE);
		map.put("wowza", Boolean.FALSE);

		Assertions.assertEquals(Boolean.FALSE, trie.get("wowza"));
		Assertions.assertEquals(Boolean.TRUE, trie.get("bookshelf"));
		Assertions.assertNull(trie.get("bookshelf2"));
	}

	@Test
	public void removeBack(){
		RadixTree<String, Integer> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("hello", 0);
		map.put("hello world", 1);

		Assertions.assertEquals(0, trie.get("hello").intValue());
		Assertions.assertEquals(1, trie.get("hello world").intValue());

		Integer r1 = trie.remove("hello world");

		Assertions.assertNotNull(r1);
		Assertions.assertEquals(1, r1.intValue());

		Assertions.assertEquals(0, trie.get("hello").intValue());
		Assertions.assertNull(trie.get("hello world"));
	}

	@Test
	public void removeFront(){
		RadixTree<String, Integer> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("hello", 0);
		map.put("hello world", 1);

		Assertions.assertEquals(0, trie.get("hello").intValue());
		Assertions.assertEquals(1, trie.get("hello world").intValue());

		Integer r0 = trie.remove("hello world");

		Assertions.assertNotNull(r0);
		Assertions.assertEquals(1, r0.intValue());

		Assertions.assertEquals(0, trie.get("hello").intValue());
		Assertions.assertNull(trie.get("hello world"));
	}

	@Test
	public void removeFrontManyChildren(){
		RadixTree<String, Integer> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("hello", 0);
		map.put("hello world", 1);
		map.put("hello, clarice", 2);

		Assertions.assertEquals(0, trie.get("hello").intValue());
		Assertions.assertEquals(1, trie.get("hello world").intValue());
		Assertions.assertEquals(2, trie.get("hello, clarice").intValue());

		Integer r0 = trie.remove("hello world");

		Assertions.assertNotNull(r0);
		Assertions.assertEquals(1, r0.intValue());

		Assertions.assertNull(trie.get("hello world"));
		Assertions.assertEquals(0, trie.get("hello").intValue());
		Assertions.assertEquals(2, trie.get("hello, clarice").intValue());
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
	public void searchForPartialParentAndLeafKeyWhenOverlapExists(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("abcd", "abcd");
		map.put("abce", "abce");

		Assertions.assertEquals(0, trie.getValues("abe").size());
		Assertions.assertEquals(0, trie.getValues("abd").size());
	}

	@Test
	public void searchForLeafNodesWhenOverlapExists(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("abcd", "abcd");
		map.put("abce", "abce");

		Assertions.assertEquals(1, trie.getValues("abcd").size());
		Assertions.assertEquals(1, trie.getValues("abce").size());
	}

	@Test
	public void searchForStringSmallerThanSharedParentWhenOverlapExists(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("abcd", "abcd");
		map.put("abce", "abce");

		Assertions.assertEquals(2, trie.getValues("ab").size());
		Assertions.assertEquals(2, trie.getValues("a").size());
	}

	@Test
	public void searchForStringEqualToSharedParentWhenOverlapExists(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("abcd", "abcd");
		map.put("abce", "abce");

		Assertions.assertEquals(2, trie.getValues("abc").size());
	}

	@Test
	public void insert(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("bat", "bat");
		map.put("ape", "ape");
		map.put("bath", "bath");
		map.put("banana", "banana");

		Assertions.assertEquals(new RadixTreeNode<>("ple", "apple"), trie.find("apple"));
		Assertions.assertEquals(new RadixTreeNode<>("t", "bat"), trie.find("bat"));
		Assertions.assertEquals(new RadixTreeNode<>("e", "ape"), trie.find("ape"));
		Assertions.assertEquals(new RadixTreeNode<>("h", "bath"), trie.find("bath"));
		Assertions.assertEquals(new RadixTreeNode<>("nana", "banana"), trie.find("banana"));
	}

	@Test
	public void insertExistingUnrealNodeConvertsItToReal(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("applepie", "applepie");
		map.put("applecrisp", "applecrisp");

		Assertions.assertFalse(trie.containsKey("apple"));

		map.put("apple", "apple");

		Assertions.assertTrue(trie.containsKey("apple"));
	}

	@Test
	public void duplicatesAllowed(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");

		try{
			map.put("apple", "apple2");

			Assertions.assertTrue(true);
		}
		catch(DuplicateKeyException e){
			Assertions.fail("Duplicate should have been allowed");
		}
	}

	@Test
	public void duplicatesNotAllowed(){
		RadixTree<String, String> trie = AhoCorasickTree.createTreeNoDuplicates(new StringSequencer());

		map.put("apple", "apple");

		try{
			map.put("apple", "apple2");

			Assertions.fail("Duplicate should not have been allowed");
		}
		catch(DuplicateKeyException e){
			Assertions.assertEquals("Duplicate key: 'apple'", e.getMessage());
		}
	}

	@Test
	public void insertWithRepeatingPatternsInKey(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("xbox 360", "xbox 360");
		map.put("xbox", "xbox");
		map.put("xbox 360 games", "xbox 360 games");
		map.put("xbox games", "xbox games");
		map.put("xbox xbox 360", "xbox xbox 360");
		map.put("xbox xbox", "xbox xbox");
		map.put("xbox 360 xbox games", "xbox 360 xbox games");
		map.put("xbox games 360", "xbox games 360");
		map.put("xbox 360 360", "xbox 360 360");
		map.put("xbox 360 xbox 360", "xbox 360 xbox 360");
		map.put("360 xbox games 360", "360 xbox games 360");
		map.put("xbox xbox 361", "xbox xbox 361");

		Assertions.assertEquals(12, trie.size());
	}

	@Test
	public void deleteNodeWithNoChildren(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");

		Assertions.assertNotNull(trie.remove("apple"));
	}

	@Test
	public void deleteNodeWithOneChild(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("applepie", "applepie");

		Assertions.assertNotNull(trie.remove("apple"));
		Assertions.assertTrue(trie.containsKey("applepie"));
		Assertions.assertFalse(trie.containsKey("apple"));
	}

	@Test
	public void deleteNodeWithMultipleChildren(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("applepie", "applepie");
		map.put("applecrisp", "applecrisp");

		Assertions.assertNotNull(trie.remove("apple"));
		Assertions.assertTrue(trie.containsKey("applepie"));
		Assertions.assertTrue(trie.containsKey("applecrisp"));
		Assertions.assertFalse(trie.containsKey("apple"));
	}

	@Test
	public void cantDeleteSomethingThatDoesntExist(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(trie.remove("apple"));
	}

	@Test
	public void cantDeleteSomethingThatWasAlreadyDeleted(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		trie.remove("apple");

		Assertions.assertNull(trie.remove("apple"));
	}

	@Test
	public void childrenNotAffectedWhenOneIsDeleted(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("appleshack", "appleshack");
		map.put("applepie", "applepie");
		map.put("ape", "ape");

		trie.remove("apple");

		Assertions.assertTrue(trie.containsKey("appleshack"));
		Assertions.assertTrue(trie.containsKey("applepie"));
		Assertions.assertTrue(trie.containsKey("ape"));
		Assertions.assertFalse(trie.containsKey("apple"));
	}

	@Test
	public void siblingsNotAffectedWhenOneIsDeleted(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("ball", "ball");

		trie.remove("apple");

		Assertions.assertTrue(trie.containsKey("ball"));
	}

	@Test
	public void cantDeleteUnrealNode(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("ape", "ape");

		Assertions.assertNull(trie.remove("ap"));
	}

	@Test
	public void cantFindRootNode(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(trie.find(""));
	}

	@Test
	public void findSimpleInsert(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");

		Assertions.assertNotNull(trie.find("apple"));
	}

	@Test
	public void containsSimpleInsert(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");

		Assertions.assertTrue(trie.containsKey("apple"));
	}

	@Test
	public void findChildInsert(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("ape", "ape");
		map.put("appletrie", "appletrie");
		map.put("appleshackcream", "appleshackcream");

		Assertions.assertNotNull(trie.find("appletrie"));
		Assertions.assertNotNull(trie.find("appleshackcream"));
		Assertions.assertNotNull(trie.containsKey("ape"));
	}

	@Test
	public void getPrefixes(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("h", "h");
		map.put("hey", "hey");
		map.put("hell", "hell");
		map.put("hello", "hello");
		map.put("hat", "hat");
		map.put("cat", "cat");

		Assertions.assertFalse(trie.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).isEmpty());
		Assertions.assertTrue(trie.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("h"));
		Assertions.assertTrue(trie.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hell"));
		Assertions.assertTrue(trie.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hello"));
		Assertions.assertTrue(!trie.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("he"));
		Assertions.assertTrue(!trie.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hat"));
		Assertions.assertTrue(!trie.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("cat"));
		Assertions.assertTrue(!trie.getValues("helloworld", RadixTree.PrefixType.PREFIXED_TO).contains("hey"));
		Assertions.assertTrue(trie.getValues("animal", RadixTree.PrefixType.PREFIXED_TO).isEmpty());
	}

	@Test
	public void containsChildInsert(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("ape", "ape");
		map.put("appletrie", "appletrie");
		map.put("appleshackcream", "appleshackcream");

		Assertions.assertTrue(trie.containsKey("appletrie"));
		Assertions.assertTrue(trie.containsKey("appleshackcream"));
		Assertions.assertTrue(trie.containsKey("ape"));
	}

	@Test
	public void cantFindNonexistantNode(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertNull(trie.find("apple"));
	}

	@Test
	public void doesntContainNonexistantNode(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		Assertions.assertFalse(trie.containsKey("apple"));
	}

	@Test
	public void cantFindUnrealNode(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("ape", "ape");

		Assertions.assertNull(trie.find("ap"));
	}

	@Test
	public void doesntContainUnrealNode(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("ape", "ape");

		Assertions.assertFalse(trie.containsKey("ap"));
	}

	@Test
	public void searchPrefix_LimitGreaterThanPossibleResults(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("appleshack", "appleshack");
		map.put("appleshackcream", "appleshackcream");
		map.put("applepie", "applepie");
		map.put("ape", "ape");

		List<String> result = trie.getValues("app");
		Assertions.assertEquals(4, result.size());

		Assertions.assertTrue(result.contains("appleshack"));
		Assertions.assertTrue(result.contains("appleshackcream"));
		Assertions.assertTrue(result.contains("applepie"));
		Assertions.assertTrue(result.contains("apple"));
	}

	@Test
	public void searchPrefix_LimitLessThanPossibleResults(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("appleshack", "appleshack");
		map.put("appleshackcream", "appleshackcream");
		map.put("applepie", "applepie");
		map.put("ape", "ape");

		List<String> result = trie.getValues("appl");
		Assertions.assertEquals(4, result.size());

		Assertions.assertTrue(result.contains("appleshack"));
		Assertions.assertTrue(result.contains("applepie"));
		Assertions.assertTrue(result.contains("apple"));
	}

	@Test
	public void getSize(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("appleshack", "appleshack");
		map.put("appleshackcream", "appleshackcream");
		map.put("applepie", "applepie");
		map.put("ape", "ape");

		Assertions.assertTrue(trie.size() == 5);
	}

	@Test
	public void deleteReducesSize(){
		RadixTree<String, String> trie = AhoCorasickTree.createTree(new StringSequencer());

		map.put("apple", "apple");
		map.put("appleshack", "appleshack");

		trie.remove("appleshack");

		Assertions.assertTrue(trie.size() == 1);
	}*/

}
