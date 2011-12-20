/*
Copyright 2008 Flaptor (flaptor.com) 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License.
*/

package com.flaptor.util;

import java.util.Vector;

/**
 * This class implements a simple Trie tree.
 * It is both the main interface class and the tree node element.
 * A Trie tree efficiently stores and finds strings that share prefixes.
 * The strings are used as keys to stored objects. Each node represents 
 * a character in the key strings.
 * All strings that start with the same prefix share the same starting nodes.
 * At the point where the strings differ, a branch is created, one for the 
 * rest of each string.
 */
public class TrieTree<Type> {

    private char keyCh;            // the key character for this node
    private Type value;            // the stored value
    private TrieTree<Type> next;   // the next sibling at this level
    private TrieTree<Type> child;  // the child of this node


    /** 
     * Constructs an empty Trie tree. 
     */
    public TrieTree () {
        keyCh = 0;
        value = null;
        next = null;
        child = null;
    }

    /**
     * Puts a key/value pair in the tree. 
     * If the tree previously contained a value for this key, the old value is overwritten.
     * @param key the key to insert into the tree.
     * @param value the value associated with the provided key.
     */
    public void put (String key, Type value) {
        if (null != key && key.length() > 0) {  // dont store empty or null keys
            char ch = key.charAt(0);
            if (keyCh == ch || keyCh == 0) {  // if the node has the key char or is a new node
                keyCh = ch;  // write the key char in case it is a new node
                if (key.length() == 1) {  // if the key is complete
                    this.value = value;  // write the value here
                } else {
                    if (null == child) {  // if there is no child yet, create one
                        child = new TrieTree<Type>();
                    }
                    child.put(key.substring(1), value);  // recursively continue down the tree
                }
            } else {
                if (null == next) {  // if there is no sibling yet, create one
                    next = new TrieTree<Type>();
                }
                next.put(key, value);  // recursively continue across the tree
            }
        }
    }

    /**
     * Adds a key/value pair to the tree. 
     * If the tree previously contained a value for this key, an exception is thrown.
     * @param key the key to insert into the tree.
     * @param value the value associated with the provided key.
     */
    public void add (String key, Type value) {
        if (null != key && key.length() > 0) {  // dont store empty or null keys
            char ch = key.charAt(0);
            if (keyCh == ch || keyCh == 0) {  // if the node has the key char or is a new node
                keyCh = ch;  // write the key char in case it is a new node
                if (key.length() == 1) {  // if the key is complete
                    if (this.value == null) {
                        this.value = value;  // write the value here
                    } else {
                        throw new DuplicateKeyException("Adding a key/value pair");
                    }
                } else {
                    if (null == child) {  // if there is no child yet, create one
                        child = new TrieTree<Type>();
                    }
                    child.add(key.substring(1), value);  // recursively continue down the tree
                }
            } else {
                if (null == next) {  // if there is no sibling yet, create one
                    next = new TrieTree<Type>();
                }
                next.add(key, value);  // recursively continue across the tree
            }
        }
    }

    /**
     * Returns the object stored at the provided key.
     * @param key the key to search for.
     * @return the stored object associated with the key if found, null otherwise.
     */
    public Type get (String key) {
        if (null == key || key.length() == 0) {  // empty or null keys are not stored
            return null;
        }
        TrieTree<Type> node = getNode(key);
        if (null == node) { return null; }
        return node.value;
    }

    /**
     * Returns the node stored at the provided key.
     * @param key the key to search for.
     * @return the stored node associated with the key if found, null otherwise.
     */
    private TrieTree<Type> getNode (String key) {
        if (null == key || key.length() == 0) {  // empty or null keys are not stored
            return null;
        }
        if (keyCh == key.charAt(0)) {  // if this node has the key char
            if (key.length() == 1) {  // and if the key is complete
                return this;  // we found the key, return the stored value
            } else {
                if (null == child) {
                    return null;  // the key is not over, yet the tree is
                } else {
                    return child.getNode(key.substring(1));  // recursively search down the tree
                }
            }
        } else {
            if (null == next) {
                return null;  // the node does not have the key char, and there are no more siblings
            } else {
                return next.getNode(key);  // recursively search across the tree
            }
        }
    }

    /**
     * Returns a list of objects stored at the provided key or any inicial part of the key.
     * @param key the key to search for.
     * @return a vector of partial matches, containing the objects associated with the key if found
     * and the length of the key portion that was matched.
     */
    public Vector<PartialMatch<Type>> getPartialMatches (String key) {
        Vector<PartialMatch<Type>> res = new Vector<PartialMatch<Type>>();
        if (null != key && key.length() > 0) {  // empty or null keys are not stored
            TrieTree<Type> node = this;
            int keypos = 0;
            boolean treeFinished = false;
            while (keypos < key.length() && ! treeFinished) {
                if (node.keyCh == key.charAt(keypos)) {  // if this node has the key char
                    if (null != node.value) {  // and if the key is complete
                        res.add (new PartialMatch<Type>(keypos, node.value));  // we found a value, add it to the list
                    }
                    if (null == node.child) {
                        treeFinished = true;  // the tree is over
                    } else {
                        node = node.child;  // iteratively search down the tree
                    }
                    keypos++;
                } else {
                    if (null == node.next) {
                        treeFinished = true;  // the node does not have the key char, and there are no more siblings
                    } else {
                        node = node.next;  // iteratively search across the tree
                    }
                }
            }
        }
        return res;
    }

    /**
     * Returns a list of objects stored at the nodes starting at the provided prefix.
     * @param prefix the prefix to search for.
     * @return a vector of the stored objects that start at the provided prefix.
     */
    public Vector<Type> getPrefixMatches(String prefix) {
        Vector<Type> res = new Vector<Type>();
        if (null != prefix && prefix.length() > 0) {  // empty or null prefixes are not considered
            TrieTree<Type> node = getNode(prefix);
            if (null != node) {
                if (null != node.value) {
                    res.add(node.value);
                }
                if (null != node.child) {
                    node.child.collectValues(res);
                }
            }
        }
        return res;
    }

    private void collectValues(Vector<Type> bag) {
        if (null != value) {
            bag.add(value);
        }
        if (null != next) {
            next.collectValues(bag);
        }
        if (null != child) {
            child.collectValues(bag);
        }
    }
    
    /**
     * Searches the tree for the provided key.
     * @param key the key to search for.
     * @return true if there is a value associated with the key, false otherwise.
     */
    public boolean hasKey (String key) {
        return null != get(key);
    }



    /**
     * Internal class for storing partial matches.
     */
    public class PartialMatch<T> {

        private int pos;
        private T value;

        /** Class Constructor. */
        public PartialMatch (int pos, T value) {
            this.pos = pos;
            this.value = value;
        }

        /** Get the partial match position */
        public int getPosition () {
            return pos;
        }

        /** Get the partial match value */
        public T getValue () {
            return value;
        }
    }


    /**
     * Thrown if a duplicate key is added to a Trie tree.
     */
    public static class DuplicateKeyException extends RuntimeException {

        private static final long serialVersionUID = 1L;

        /** Class Constructor. */
        public DuplicateKeyException(String message) {
            super(message);
        }

        /** Get the exception message. */
        public String toString() {
            return("DuplicateKeyException: " + getMessage());
        }
    }


}

