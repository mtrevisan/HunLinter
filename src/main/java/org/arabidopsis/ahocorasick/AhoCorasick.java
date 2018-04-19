package org.arabidopsis.ahocorasick;

import java.util.Iterator;

/**
 <p>An implementation of the Aho-Corasick string searching
 automaton.  This implementation of the <a
 href="http://portal.acm.org/citation.cfm?id=360855&dl=ACM&coll=GUIDE"
 target="_blank">Aho-Corasick</a> algorithm is optimized to work
 with chars.</p>

 <p>
 Example usage:
 <code><pre>
 AhoCorasick tree = new AhoCorasick();
 tree.add("hello", "hello");
 tree.add("world", "world");
 tree.prepare();

 Iterator searcher = tree.search("hello world".getBytes());
 while(searcher.hasNext()){
   SearchResult result = searcher.next();
   System.out.println(result.getOutputs());
   System.out.println("Found at index: " + result.getLastIndex());
 }
 </pre></code>
 </p>
 */
public class AhoCorasick {
  private State root;
  private boolean prepared;

  public AhoCorasick() {
    this.root = new State(0);
    this.prepared = false;
  }

  /**
    Adds a new keyword with the given output.  During search, if
    the keyword is matched, output will be one of the yielded
    elements in SearchResults.getOutputs().
   */
  public void add(String keyword, Object output) {
    if (this.prepared)
      throw new IllegalStateException("can't add keywords after prepare() is called");
    State lastState = this.root.extendAll(keyword.toCharArray());
    lastState.addOutput(output);
  }

  /**
    Prepares the automaton for searching.  This must be called
    before any searching().
   */
  public void prepare() {
    this.prepareFailTransitions();
    this.prepared = true;
  }

  /**
    Starts a new search, and returns an Iterator of SearchResults.
   */
  public Iterator<SearchResult> search(char[] chars) {
    return new Searcher(this, this.startSearch(chars));
  }

  /**
   * DANGER DANGER: dense algorithm code ahead. Very order dependent. Initializes the fail
   * transitions of all states except for the root.
   */
  private void prepareFailTransitions() {
    Queue<State> q = new Queue<State>();

    for (int i = 0; i < 256; i++)
      if (this.root.get((char) i) != null) {
        this.root.get((char) i).setFail(this.root);
        q.add(this.root.get((char) i));
      }

    this.prepareRoot();
    while(!q.isEmpty()){
      State node = q.pop();

      char[] keys = node.keys();
      for(int i = 0; i < keys.length; i ++){
        char transition = keys[i];
        State targetState = node.get(transition);

        node = node.getFail();
        while(node.get(transition) == null)
          node = node.getFail();

        targetState.setFail(node.get(transition));
        targetState.getOutputs().addAll(node.get(transition).getOutputs());

		  
		  q.add(targetState);
      }
    }
  }

  /** Sets all the out transitions of the root to itself, if no
    transition yet exists at this point.
   */
  private void prepareRoot() {
    for (int i = 0; i < 256; i++)
      if (this.root.get((char) i) == null)
        this.root.put((char) i, this.root);
  }

  /**
   * Returns the root of the tree.
   */
  State getRoot() {
    return this.root;
  }

  /**
   * Begins a new search using the raw interface.
   */
  SearchResult startSearch(char[] chars) {
    if (!this.prepared)
      throw new IllegalStateException ("can't start search until prepare()");
    return continueSearch (new SearchResult(this.root, chars, 0));
  }

  /**
   * Continues the search, given the initial state described by the lastResult.
   */
  SearchResult continueSearch(SearchResult lastResult) {
    char[] chars = lastResult.chars;
    State state = lastResult.lastMatchedState;
    for (int i = lastResult.lastIndex; i < chars.length; i++) {
      char b = chars[i];
      while (state.get(b) == null)
        state = state.getFail();
      state = state.get(b);
      if (state.getOutputs().size() > 0)
        return new SearchResult(state, chars, i+1);
    }

    return null;
  }
}
