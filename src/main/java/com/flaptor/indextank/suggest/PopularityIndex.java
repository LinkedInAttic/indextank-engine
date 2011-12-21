/*
 * Copyright (c) 2011 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.flaptor.indextank.suggest;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;


@Deprecated
class PopularityIndex {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    static final String MAIN_FILE_NAME = "popularityIndex";
    private static final int TREE_SIZE = 36;
    private static final int SUGGESTION_LIMIT = 3;

    private final File backupDir;
    private Leaf root;

    public PopularityIndex(File backupDir, boolean load) throws IOException {
        Preconditions.checkNotNull(backupDir);
        checkDirArgument(backupDir);
        this.backupDir = backupDir;
        if (!load) {
            logger.info("Starting a new(empty) PopularityIndex.");
            root = new Leaf();
        } else {
            File f = new File(this.backupDir, MAIN_FILE_NAME);
            ObjectInputStream is = null;
            try {
                is = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)));
                try {
                    root = (Leaf) is.readObject();
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
                logger.info("State loaded.");
            } finally {
                Execute.close(is);
            }
        }
    }

    public void dump() throws IOException {
        syncToDisk();
    }

    /**
     * Serializes this instance content to disk.
     * Blocking method.
     */
    private synchronized void syncToDisk() throws IOException {
        logger.info("Starting dump to disk.");
        File f = new File(backupDir, MAIN_FILE_NAME);
        ObjectOutputStream os = null;
        try {
            os = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
            os.writeObject(root);
            os.flush();
            logger.info("Dump to disk completed.");
        } finally {
            Execute.close(os);
        }
    }

    /**
     * @throws IllegalArgumentException
     */
    private static void checkDirArgument(File backupDir) {
        Preconditions.checkNotNull(backupDir);
        if (!backupDir.canRead()) {
            String s = "Don't have read permission over the backup directory(" + backupDir.getAbsolutePath() + ").";
            logger.error(s);
            throw new IllegalArgumentException(s);
        }
        if (!backupDir.canWrite()) {
            String s = "Don't have write permission over the backup directory(" + backupDir.getAbsolutePath() + ").";
            logger.error(s);
            throw new IllegalArgumentException(s);
        }
    }

    private String normalize(String str) {
        return str.toLowerCase();
    }

    public List<String> getMostPopular(String str) {
        str = normalize(str);
        Leaf leaf;
        try {
            leaf = search(str, root);
        } catch (UnsupportedCharacterException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("String with unsupported character, returning no suggestions. Problematic string is \"" + str + "\", character is \"" + e.getCharacter() + "\"");
            }
            return new ArrayList<String>(0);
        }

        if (leaf == null) {
            return ImmutableList.of();
        }

        List<Leaf> sorted;
        if (leaf.suggestions == null) {
            sorted = ImmutableList.of();
        } else {
            sorted = Ordering.natural().sortedCopy(leaf.suggestions);
        }

        return Lists.transform(sorted, Functions.toStringFunction());
    }

    public void add(String str) {
        str = normalize(str);
        try {
            add(0, root, str);
        } catch (UnsupportedCharacterException e) {
        	if (logger.isDebugEnabled()) {
        		logger.debug("Trying to store string with unsupported character, skipping it. String is \"" + str + "\", character is \"" + e.getCharacter() + "\"");
        	}
        }
    }

    private Leaf search(CharSequence chars, Leaf leaf) throws UnsupportedCharacterException {
        if (chars.length() == 0) {
            return leaf;
        }
        int point = mf(chars.charAt(0));
        Leaf nextLeaf = leaf.map[point];
        if (null == nextLeaf) {
            return null;
        }
        return search(chars.subSequence(1, chars.length()), nextLeaf);
    }

    /**
     * @returns the number of times this string is stored.
     */
    private Leaf add(int position, Leaf leaf, String originalStr) throws UnsupportedCharacterException {
        if (position == originalStr.length()) {
            // consumed the entire sequence, increment count
            leaf.counter.incrementAndGet();
            if (leaf.str == null) {
                leaf.str = originalStr;
            }
            return leaf;
        }

        int point = mf(originalStr.charAt(position));

        if (leaf.map[point] == null) {
            // if we need to create the child leaf, we lock the parent 
            synchronized (leaf) {
                // if its still null, we create it, now locked
                if (leaf.map[point] == null) {
                    leaf.map[point] = new Leaf();
                }
            }
        }

        // increase the prefix counter
        //leaf.prefixCounter.incrementAndGet();

        Leaf s = add(position+1, leaf.map[point], originalStr);

        if (s != null) {
            if (!leaf.offerSuggestion(s)) {
                s = null;
            }
        }

        return s;
    }


    private static class SuggestionEntry implements Iterable<Leaf>, Serializable {
		private static final long serialVersionUID = 1L;

		SuggestionEntry(Leaf suggestion) {
            this.suggestion = suggestion;
        }
        SuggestionEntry next = null;
        SuggestionEntry prev = null;
        Leaf suggestion;

        @Override
            public Iterator<Leaf> iterator() {
                return new AbstractIterator<Leaf>() {
                    SuggestionEntry current = SuggestionEntry.this;
                    @Override
                        protected Leaf computeNext() {
                            if (current == null) {
                                return endOfData();
                            }
                            try {
                                return current.suggestion;
                            } finally {
                                current = current.next;
                            }
                        }
                };
            }

    }

    private static class Leaf implements Comparable<Leaf>, Serializable {
		private static final long serialVersionUID = 1L;
		
		AtomicInteger counter = new AtomicInteger(0);
        Leaf[] map = new Leaf[TREE_SIZE];
        SuggestionEntry suggestions = null;
        String str;
        public boolean offerSuggestion(Leaf s) {
            SuggestionEntry ss = suggestions;
            int countSuggestions = 0;
            boolean shouldAdd = false;
            while (ss != null) {
                if (ss.suggestion == s) return true;
                if (ss.suggestion.counter.get() < s.counter.get()) {
                    shouldAdd = true;
                }
                countSuggestions++;
                ss = ss.next;
            }
            if (shouldAdd || countSuggestions < SUGGESTION_LIMIT) {
                synchronized (this) {
                    // add new suggestion on top
                    SuggestionEntry e = new SuggestionEntry(s);
                    e.next = this.suggestions;
                    if (e.next != null) e.next.prev = e;
                    this.suggestions = e;
                    // remove worst suggestion
                    SuggestionEntry minE = e;
                    countSuggestions = 0;
                    while (e != null) {
                        countSuggestions++;
                        if (e.suggestion.counter.get() < minE.suggestion.counter.get()) {
                            minE = e;
                        }
                        e = e.next;
                    }
                    if (countSuggestions > SUGGESTION_LIMIT) {
                        if (minE == this.suggestions) {
                            // removing the head of the list
                            if (minE.next != null) minE.next.prev = null;
                            this.suggestions = minE.next;
                        } else {
                            if (minE.prev != null) minE.prev.next = minE.next;
                            if (minE.next != null) minE.next.prev = minE.prev;
                        }
                    }
                }
                return true;
            }
            return false;
        }

        @Override
            public int compareTo(Leaf o) {
                return o.counter.get() - counter.get();
            }

        @Override
            public String toString() {
                return str;
            }
    }

    /**
     * Mapping function.
     */
    private static int mf(char c) throws UnsupportedCharacterException {
        int i = (int) c;
        if (i >= 48 && i <= 57) {
            return (i - 48);
        }
        if (i >= 97 && i <= 122) {
            return (i - 87);
        }
        throw new UnsupportedCharacterException("Unsupported character: \"" + c + "\"", c);
    }

    private static class UnsupportedCharacterException extends Exception {
        private static final long serialVersionUID = 1L;
        private final char character;

        public UnsupportedCharacterException(String msg, char character) {
            super(msg);
            this.character = character;
        }

		public char getCharacter() {
            return character;
        }
    }

    private static void writeNewFormatLeaf(Leaf l, DataOutputStream dos) throws IOException {
        if (l.str != null) {
            dos.writeUTF(l.str);
            dos.writeInt(l.counter.get());
        }
        for (int i = 0; i < l.map.length; i++) {
            Leaf ch = l.map[i];
            if (ch != null) {
                writeNewFormatLeaf(ch, dos);
            }
        }
    }

    public void writeNewFormat(File file) throws IOException {
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        try {
            writeNewFormatLeaf(this.root, dos);
        } finally {
            Execute.close(dos);
        }
    }

}
