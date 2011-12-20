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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.cojen.classfile.Attribute;
import org.cojen.classfile.ConstantPool;
import org.cojen.classfile.FixedLocation;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.Location;
import org.cojen.classfile.LocationRange;
import org.cojen.classfile.LocationRangeImpl;
import org.cojen.classfile.TypeDesc;
import org.cojen.classfile.constant.ConstantUTFInfo;

/**
 * This class corresponds to the LocalVariableTable_attribute structure as 
 * defined in section 4.7.7 of <i>The Java Virtual Machine Specification</i>.
 * 
 * @author Brian S O'Neill
 */
public class LocalVariableTableAttr extends Attribute {

    private List<Entry> mEntries = new ArrayList<Entry>(10);
    private List<Entry> mCleanEntries;
    private int mRangeCount;
    
    public LocalVariableTableAttr(ConstantPool cp) {
        super(cp, LOCAL_VARIABLE_TABLE);
    }
    
    public LocalVariableTableAttr(ConstantPool cp, String name) {
        super(cp, name);
    }

    public LocalVariableTableAttr(ConstantPool cp, String name, int length, DataInput din)
        throws IOException
    {
        super(cp, name);

        int size = din.readUnsignedShort();
        for (int i=0; i<size; i++) {
            int start_pc = din.readUnsignedShort();
            int end_pc = start_pc + din.readUnsignedShort() + 1;
            int name_index = din.readUnsignedShort();
            int descriptor_index = din.readUnsignedShort();
            final int index = din.readUnsignedShort();

            final ConstantUTFInfo varName = (ConstantUTFInfo)cp.getConstant(name_index);
            final ConstantUTFInfo varDesc = (ConstantUTFInfo)cp.getConstant(descriptor_index);

            if (varDesc == null) {
                continue;
            }

            final Location startLocation = new FixedLocation(start_pc);
            final Location endLocation = new FixedLocation(end_pc);

            final Set<LocationRange> ranges = Collections
                .singleton((LocationRange) new LocationRangeImpl(startLocation, endLocation));

            LocalVariable localVar = new LocalVariable() {
                private String mName;
                private TypeDesc mType;

                {
                    mName = varName == null ? null : varName.getValue();
                    mType = TypeDesc.forDescriptor(varDesc.getValue());
                }

                public String getName() {
                    return mName;
                }

                public void setName(String name) {
                    mName = name;
                }

                public TypeDesc getType() {
                    return mType;
                }
                
                public boolean isDoubleWord() {
                    return mType.isDoubleWord();
                }
                
                public int getNumber() {
                    return index;
                }

                public Set<LocationRange> getLocationRangeSet() {
                    return ranges;
                }
            };

            mEntries.add(new Entry(localVar, varName, varDesc));
        }
    }

    public LocalVariable getLocalVariable(Location useLocation, int number) {
        return getLocalVariable(useLocation.getLocation(), number);
    }

    public LocalVariable getLocalVariable(int useLocation, int number) {
        // TODO: Build some sort of index to improve performance.
        for (Entry entry : mEntries) {
            LocalVariable var = entry.mLocalVar;
            if (var.getNumber() == number) {
                for (LocationRange range : var.getLocationRangeSet()) {
                    int start = range.getStartLocation().getLocation();
                    int end = range.getEndLocation().getLocation();
                    if (start >= 0 && end >= 0) {
                        if (start <= useLocation && useLocation < end) {
                            return var;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Add an entry into the LocalVariableTableAttr.
     */
    public void addEntry(LocalVariable localVar) {
        String varName = localVar.getName();
        if (varName == null) {
            int num = localVar.getNumber();
            varName = num < 0 ? "_" : ("v" + num + '$');
        }

        ConstantUTFInfo name = getConstantPool().addConstantUTF(varName);
        ConstantUTFInfo descriptor = 
            getConstantPool().addConstantUTF(localVar.getType().getDescriptor());
        mEntries.add(new Entry(localVar, name, descriptor));

        mCleanEntries = null;
    }
    
    public int getLength() {
        clean();
        return 2 + 10 * mRangeCount;
    }
    
    public void writeDataTo(DataOutput dout) throws IOException {
        dout.writeShort(mRangeCount);

        int size = mCleanEntries.size();
        for (int i=0; i<size; i++) {
            Entry entry = mCleanEntries.get(i);
            LocalVariable localVar = entry.mLocalVar;

            Set<LocationRange> ranges = localVar.getLocationRangeSet();
            if (ranges == null) {
                continue;
            }

            int name_index = entry.mName.getIndex();
            int descriptor_index = entry.mDescriptor.getIndex();
            int index = localVar.getNumber();

            check("local variable table entry name index", name_index);
            check("local variable table entry descriptor index", descriptor_index);
            check("local variable table entry index", index);

            for (LocationRange range : ranges) {
                Location startLocation = range.getStartLocation();
                Location endLocation = range.getEndLocation();

                int start_pc = startLocation.getLocation();
                int length = endLocation.getLocation() - start_pc - 1;

                check("local variable table entry start PC", start_pc);

                dout.writeShort(start_pc);
                dout.writeShort(length);
                dout.writeShort(name_index);
                dout.writeShort(descriptor_index);
                dout.writeShort(index);
            }
        }
    }

    private void check(String type, int addr) throws IllegalStateException {
        if (addr < 0 || addr > 65535) {
            throw new IllegalStateException("Value for " + type + " out of " +
                                            "valid range: " + addr);
        }
    }

    private void clean() {
        if (mCleanEntries != null) {
            return;
        }

        // Clean out entries that are incomplete or bogus.

        int size = mEntries.size();
        mCleanEntries = new ArrayList<Entry>(size);
        mRangeCount = 0;

        outer: for (int i=0; i<size; i++) {
            Entry entry = mEntries.get(i);
            LocalVariable localVar = entry.mLocalVar;

            Set<LocationRange> ranges = localVar.getLocationRangeSet();
            if (ranges == null || ranges.size() == 0) {
                continue;
            }

            for (LocationRange range : ranges) {
                Location startLocation = range.getStartLocation();
                Location endLocation = range.getEndLocation();

                if (startLocation == null || endLocation == null) {
                    continue outer;
                }

                int start_pc = startLocation.getLocation();
                int length = endLocation.getLocation() - start_pc - 1;

                if (length < 0) {
                    continue outer;
                }
            }
            
            mCleanEntries.add(entry);
            mRangeCount += entry.getRangeCount();
        }
    }

    private static class Entry {
        public LocalVariable mLocalVar;
        public ConstantUTFInfo mName;
        public ConstantUTFInfo mDescriptor;

        public Entry(LocalVariable localVar,
                     ConstantUTFInfo name, ConstantUTFInfo descriptor) {

            mLocalVar = localVar;
            mName = name;
            mDescriptor = descriptor;
        }

        public int getRangeCount() {
            return mLocalVar.getLocationRangeSet().size();
        }
    }
}
