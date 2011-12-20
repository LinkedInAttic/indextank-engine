package com.flaptor.indextank.suggest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Logger;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.scorer.Boosts;
import com.flaptor.indextank.index.scorer.DynamicDataManager;
import com.flaptor.indextank.index.storage.InMemoryStorage;
import com.flaptor.util.Execute;
import com.flaptor.util.FunctionUtils;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


class NewPopularityIndex {
    
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static final String MAIN_FILE_NAME = "autocompleteTerms";
    private static final int MAX_SUGGESTIONS = 5;

    private final File backupDir;
    private Node root;
    private int nodeCount = 0;
    private int termCount = 0;
    private int totalCount = 0;
    
    @SuppressWarnings("deprecation")
    public NewPopularityIndex(File backupDir) throws IOException {
        this.backupDir = backupDir;
        this.root = new Node("",0);
        
        File termsFile = new File(backupDir, MAIN_FILE_NAME);
        File oldFormatFile = new File(backupDir, PopularityIndex.MAIN_FILE_NAME);
        if (!termsFile.exists() && oldFormatFile.exists()) {
            logger.info("Found old format popularity index file. Converting to new format.");
            PopularityIndex old = new PopularityIndex(backupDir, true);
            old.writeNewFormat(termsFile);
            logger.info("Saved new format file");
        }
        
        if (termsFile.exists()) {
            logger.info("Loading popularity index terms from disk.");
            loadTerms(termsFile);
            logger.info("Terms loaded");
        }
        this.addTerm("text:");
    }

    private void loadTerms(File termsFile) throws IOException {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(termsFile)));
        while (dis.available() > 0) {
            String str = dis.readUTF();
            int c = dis.readInt();
            this.incrementTermCount(str, c);
            if (logger.isDebugEnabled()) {
                logger.debug("Loaded " + str + " " + c);
            }
        }
    }
    
    private synchronized void incrementTermCount(String str, int c) {
        root.add(str, c, this);
        totalCount += c;
    }

    /**
     * Counts how many occurrences of {@code term} we've seen.
     * 
     * @param term The term to count occurrences. Never {@code null}
     * @return an int indicating how many times we saw {@code term}. 0 for never.
     */
    public int getCount(String term){
        Preconditions.checkNotNull(term);

        Node node = root.find(term);
        if (node == null) 
            return 0;

        // if there's a matching node with the same length
        // return it's count
        if (node.len == term.length())
            return node.count;

        // else
        return 0;
    }

    public List<String> getMostPopular(String prefix) {
        Node node = root.find(prefix);
        if (node == null) {
            return ImmutableList.of();
        }
        List<Node> best = Lists.newArrayList(node.best);
        Collections.sort(best, new Comparator<Node>() {
            public int compare(Node o1, Node o2) {
                return o2.count - o1.count;
            }
        });
        if (best.size() > MAX_SUGGESTIONS) {
            best = best.subList(0, MAX_SUGGESTIONS);
        }
        return Lists.transform(best, FunctionUtils.getToString());
    }
    
    public void addTerm(String term) {
        if (isAscii(term)) {
            incrementTermCount(term, 1);
        }
    }
    
    private boolean isAscii(String term) {
        for (int i = 0; i < term.length(); i++) {
            if (term.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    public void dump() throws FileNotFoundException, IOException {
        logger.info("Dumping PopularityIndex terms file.");
        File termsFile = new File(backupDir, MAIN_FILE_NAME);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(termsFile)));
        try {
            dumpNode(root, dos);
        } finally {
            Execute.close(dos);
        }
        logger.info("PopularityIndex dumped to disk.");
    }
    
    private static void dumpNode(Node node, DataOutputStream dos) throws IOException {
        if (node.count > 0) {
            dos.writeUTF(node.toString());
            dos.writeInt(node.count);
            if (logger.isDebugEnabled()) {
                logger.debug("Dumping " + node.toString() + " " + node.count);
            }
        }
        for (Node child : node.children) {
            dumpNode(child, dos);
        }
    }

    private static class Node {
		String str;
		int len;
		int count;
		Node[] children;
		List<Node> best = Lists.newArrayListWithCapacity(MAX_SUGGESTIONS);

		Node(String str, int count) {
		    this(str, str.length(), count, new Node[0]);
		    best.add(this);
		}
		
		Node(String str, int len, int count, Node[] chl) {
		    this.str = str;
		    this.len = len;
		    this.count = count;
		    this.children = chl;
		}

		/**
		 * Adds ncount to the count of nstr. And checks
		 * if the best list should be updated 
		 * @param newPopularityIndex 
		 */
		Node add(String nstr, int ncount, NewPopularityIndex index) {
		    Node node = this.insert(nstr, ncount, index);
		    this.offerBestCandidate(node);
		    return node;
		}
		
		/**
		 * Increments nstr's count by ncount, creating
		 * the necessary nodes.
		 * @param index 
		 */
		Node insert(String nstr, int ncount, NewPopularityIndex index) {
		    if (nstr.length() == len) {
		        // current nodes maches nstr, increment and return
		        this.count += ncount;
		        return this;
		    }
		    int p = len;
		    char c = nstr.charAt(p);
		    int i;
		    for (i = 0; i < children.length; i++) {
		        Node n = children[i];
		        char nc = n.str.charAt(p);
		        if (nc == c) {
		            // first char matches, insert at matching node
		            return insertAt(i, n, nstr, ncount, index);
		        }
		        if (nc > c) {
		            break;
		        }
		    }
		    // all smaller first chars have been skipped
		    // insert a new node at i
		    Node[] nchildren = new Node[children.length + 1];
		    Node newn = new Node(nstr, ncount);
		    index.termCount++;
		    index.nodeCount++;
		    index.totalCount += ncount;
		    System.arraycopy(children, 0, nchildren, 0, i);
		    nchildren[i] = newn;
		    System.arraycopy(children, i, nchildren, i+1, children.length - i);
		    children = nchildren;
		    return newn; 
        }

        private Node insertAt(int insert, Node n, String nstr, int ncount, NewPopularityIndex index) {
            int p = len;
            int minlen = Math.min(nstr.length(), n.len);
            
            // find the first non matching character betwen n and nstr
            while (p < minlen && nstr.charAt(p) == n.str.charAt(p)) {
                p++;
            }
            if (p == n.len) {
                // n is a prefix or equal to nstr
                // propagate it until the proper node
                // is found or created
                return n.add(nstr, ncount, index);
            } else if (p == nstr.length()) {
                // nstr is a prefix of n create a new node  
                // for nstr and insert it between this and n
                Node newn = new Node(nstr, nstr.length(), ncount, new Node[] {n});
                index.nodeCount++;
                index.termCount++;
                index.totalCount += ncount;
                // replace n with the new node
                children[insert] = newn;
                return newn;
            } else {
                // there a partial match between n and nstr
                // a new node for the matching part should be 
                // created with both n and nstr as its children
                Node split;
                Node newn = new Node(nstr, ncount);
                index.nodeCount++;
                index.termCount++;
                index.totalCount += ncount;
                if (nstr.charAt(p) > n.str.charAt(p)) {
                    // n is smaller than nstr
                    split = new Node(nstr, p, 0, new Node[] {n, newn} );
                    index.nodeCount++;
                } else {
                    // n is greater than nstr
                    split = new Node(nstr, p, 0, new Node[] {newn, n} );
                    index.nodeCount++;
                }
                split.best.addAll(n.best);
                split.offerBestCandidate(n);
                split.offerBestCandidate(newn);
                // replace n with the new split node
                children[insert] = split;
                return newn;
            }
            
        }
		
		@Override
		public String toString() {
		    return str.substring(0, len);
		}

        /**
         * Finds a node for the given prefix
         * If none is found, returns null
         */
        private Node find(String prefix) {
            Node[] chl = children;
            if (prefix.length() <= len) {
                if (str.startsWith(prefix)) {
                    return this;
                }
            } else if (chl.length > 0) {
                char x = prefix.charAt(len);
    		    int lo = 0;
    		    int hi = chl.length;
    		    while (hi - lo > 1) {
    		        int m = (lo+hi)/2;
    		        char cm = chl[m].str.charAt(len);
    		        if (cm > x) {
    		            hi = m; 
    		        } else {
    		            lo = m;
    		        }
    		    }
    		    Node candidate = chl[lo];
                if (candidate.str.charAt(len) == x) {
    		        return candidate.find(prefix);
    		    }
            }
            return null;
        }

        private Node find(char next) {
            Node[] chl = children;
            if (chl.length > 0) {
                int lo = 0;
                int hi = chl.length;
                while (hi - lo > 1) {
                    int m = (lo+hi)/2;
                    char cm = chl[m].str.charAt(len);
                    if (cm > next) {
                        hi = m; 
                    } else {
                        lo = m;
                    }
                }
                Node candidate = chl[lo];
                if (candidate.str.charAt(len) == next) {
                    return candidate;
                }
            }
            return null;
        }
        
        /**
         * Offer the given node as possible candidate for the best
         * suggestions list. 
         */
        public void offerBestCandidate(Node n) {
            // ignore this node and countless nodes
            best.remove(n);
            if (n.count > 0) {
                if (best.size() == MAX_SUGGESTIONS) {
                    // swap nodes with worse ones until
                    // in the end the worst one will be left out
                    for (int i = 0; i < best.size(); i++) {
                        if (best.get(i).count < n.count) {
                            Node t = n;
                            n = best.get(i);
                            best.set(i, t);
                        }
                    }
                } else {
                    // still not enough suggestions
                    this.best.add(n);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        int bc = Integer.parseInt(args[1]);
        NewPopularityIndex index = new NewPopularityIndex(dir);
        InMemoryStorage ims = new InMemoryStorage(dir, true);
        DynamicDataManager ddm = new DynamicDataManager(bc, dir);
        Scanner in = new Scanner(System.in);
        
        while (in.hasNextLine()) {
            String line = in.nextLine();
            if (line.startsWith("get ")) {
                String idStr = line.substring(4);
                DocId docId = new DocId(idStr);
                System.out.println(ims.getDocument(idStr));
                Boosts boosts = ddm.getBoosts(docId);
                System.out.println("timestamp: " + boosts.getTimestamp());
                for (int i = 0; i < bc; i++) {
                    System.out.println("var["+i+"]: " + boosts.getBoost(i));
                }
                System.out.println(ddm.getCategoryValues(docId));
            } else {
                List<String> suggestions = index.getMostPopular(line);
                for (String sugg : suggestions) {
                    System.out.print(" * ");
                    System.out.println(sugg);
                }
            }
        }
    }
    
    
    public static class PopularityIndexAutomaton extends Automaton {
    	public static class State implements Automaton.State {
    		private Node innerNode;
    		private int position;

    		public State(Node node, int position) {
				this.innerNode = node;
				this.position = position;
			}

			@Override
    		public Iterable<Automaton.Transition> getTransitions() {
				if (innerNode.len == position) {
					return Iterables.transform(Lists.newArrayList(innerNode.children), new Function<Node, Automaton.Transition>() {
						@Override
						public Automaton.Transition apply(Node node) {
							return new Transition(node, node.str.charAt(State.this.innerNode.len), State.this.innerNode.len + 1);
						}
					});
				} else {
					return Sets.<Automaton.Transition>newHashSet(new Transition(innerNode, innerNode.str.charAt(position), position + 1));
				}
    		}

    		@Override
    		public boolean isAccept() {
    			return innerNode.count > 0 && innerNode.len == position;
    		}

			@Override
			public com.flaptor.indextank.suggest.Automaton.State step(char symbol) {
				if (innerNode.len != position) {
					if (innerNode.str.charAt(position) == symbol) {
						return new State(innerNode, position + 1);
					} else {
						return null;
					}
				} else {
					Node nextNode = innerNode.find(symbol);
					
					if (nextNode == null) {
						return null;
					} else {
						return new State(nextNode, position + 1);
					}
				} 
			}
    		
    	}

    	public static class Transition implements Automaton.Transition {
    		private Node destination;
    		private char symbol;
    		private int offset;
    		
    		public Transition(Node destination, char symbol, int offset) {
				this.destination = destination;
				this.symbol = symbol;
				this.offset = offset;
			}

			@Override
			public com.flaptor.indextank.suggest.Automaton.State getState() {
    			return new State(destination, offset);
			}

			@Override
			public char getSymbol() {
				return symbol;
			}
    		
    	}
    	
    	public static PopularityIndexAutomaton adapt(NewPopularityIndex innerIndex) {
    		return new PopularityIndexAutomaton(new State(innerIndex.root.find("text:"), 5));
    	}
    	
    	private PopularityIndexAutomaton(State startState) {
    		super(startState);
    	}
    	
    }

    public Map<String, String> getStats() {
        Map<String, String> stats = Maps.newHashMap();
        stats.put("autocomplete_nodes", String.valueOf(nodeCount));
        stats.put("autocomplete_terms", String.valueOf(termCount));
        stats.put("autocomplete_total_count", String.valueOf(totalCount));
        return stats;
    }

}
