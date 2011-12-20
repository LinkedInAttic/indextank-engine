/*
 *  Copyright 2004-2010 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.cojen.classfile.attribute;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.cojen.classfile.Attribute;
import org.cojen.classfile.ConstantPool;
import org.cojen.classfile.FixedLocation;
import org.cojen.classfile.Location;

/**
 * This class corresponds to the LineNumberTable_attribute structure as
 * defined  in section 4.7.6 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
public class LineNumberTableAttr extends Attribute {

    private List<Entry> mEntries = new ArrayList<Entry>();
    private boolean mClean = false;
    
    public LineNumberTableAttr(ConstantPool cp) {
        super(cp, LINE_NUMBER_TABLE);
    }
    
    public LineNumberTableAttr(ConstantPool cp, String name) {
        super(cp, name);
    }
    
    public LineNumberTableAttr(ConstantPool cp, String name, int length, DataInput din)
        throws IOException
    {
        super(cp, name);

        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            int start_pc = din.readUnsignedShort();
            int line_number = din.readUnsignedShort();

            try {
                addEntry(new FixedLocation(start_pc), line_number);
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public int getLineNumber(Location start) {
        clean();
        int index = Collections.binarySearch(mEntries, new Entry(start, 0));
        if (index < 0) {
            if ((index = -index - 2) < 0) {
                return -1;
            }
        }
        return mEntries.get(index).mLineNumber;
    }

    public void addEntry(Location start, int line_number) throws IllegalArgumentException {
        if (line_number < 0 || line_number > 65535) {
            throw new IllegalArgumentException("Value for line number out of " +
                                               "valid range: " + line_number);
        }
        mEntries.add(new Entry(start, line_number));
        mClean = false;
    }
    
    public int getLength() {
        clean();
        return 2 + 4 * mEntries.size();
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        int size = mEntries.size();
        dout.writeShort(size);
        for (int i=0; i<size; i++) {
            Entry entry = mEntries.get(i);
            
            int start_pc = entry.mStart.getLocation();

            if (start_pc < 0 || start_pc > 65535) {
                throw new IllegalStateException
                    ("Value for line number table entry start PC out of " +
                     "valid range: " + start_pc);
            }

            dout.writeShort(start_pc);
            dout.writeShort(entry.mLineNumber);
        }
    }

    private void clean() {
        if (!mClean) {
            mClean = true;

            // Clean things up by removing multiple mappings of the same
            // start_pc to line numbers. Only keep the last one.
            // This has to be performed now because the Labels should have
            // a pc location, but before they did not. Since entries must be
            // sorted ascending by start_pc, use a sorted set.

            Set<Entry> reduced = new TreeSet<Entry>();
            for (int i = mEntries.size(); --i >= 0; ) {
                reduced.add(mEntries.get(i));
            }

            mEntries = new ArrayList<Entry>(reduced);
        }
    }

    private static class Entry implements Comparable<Entry> {
        public final Location mStart;
        public final int mLineNumber;
        
        public Entry(Location start, int line_number) {
            mStart = start;
            mLineNumber = line_number;
        }

        public int compareTo(Entry other) {
            int thisLoc = mStart.getLocation();
            int thatLoc = other.mStart.getLocation();
            
            if (thisLoc < thatLoc) {
                return -1;
            } else if (thisLoc > thatLoc) {
                return 1;
            } else {
                return 0;
            }
        }

        public boolean equals(Object other) {
            if (other instanceof Entry) {
                return mStart.getLocation() == ((Entry)other).mStart.getLocation();
            }
            return false;
        }

        public String toString() {
            return "start_pc=" + mStart.getLocation() + " => " +
                "line_number=" + mLineNumber;
        }
    }
}
