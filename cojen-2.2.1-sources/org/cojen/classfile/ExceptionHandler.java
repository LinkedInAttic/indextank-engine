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

package org.cojen.classfile;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.cojen.classfile.constant.ConstantClassInfo;

/**
 * This class corresponds to the exception_table structure as defined in
 * section 4.7.4 of <i>The Java Virtual Machine Specification</i>.
 *
 * @author Brian S O'Neill
 */
public class ExceptionHandler<L extends Location> implements LocationRange<L> {
    private L mStart;
    private L mEnd;
    private L mCatch;
    private ConstantClassInfo mCatchType;
    
    /**
     * @param startLocation
     * @param endLocation
     * @param catchLocation
     * @param catchType if null, then catch every object.
     */
    public ExceptionHandler(L startLocation,
                            L endLocation,
                            L catchLocation,
                            ConstantClassInfo catchType) {
        mStart = startLocation;
        mEnd = endLocation;
        mCatch = catchLocation;
        mCatchType = catchType;
    }
    
    public L getStartLocation() {
        return mStart;
    }
    
    public L getEndLocation() {
        return mEnd;
    }
    
    public L getCatchLocation() {
        return mCatch;
    }
    
    /**
     * Returns null if every object is caught by this handler.
     */
    public ConstantClassInfo getCatchType() {
        return mCatchType;
    }

    public void writeTo(DataOutput dout) throws IOException {
        int start_pc = getStartLocation().getLocation();
        int end_pc = getEndLocation().getLocation();
        int handler_pc = getCatchLocation().getLocation();
        int catch_type;
        ConstantClassInfo catchType = getCatchType();
        if (catchType == null) {
            catch_type = 0;
        }
        else {
            catch_type = catchType.getIndex();
        }

        check("exception start PC", start_pc);
        check("exception end PC", end_pc);
        check("exception handler PC", handler_pc);

        dout.writeShort(start_pc);
        dout.writeShort(end_pc);
        dout.writeShort(handler_pc);
        dout.writeShort(catch_type);
    }

    private void check(String type, int addr) throws IllegalStateException {
        if (addr < 0 || addr > 65535) {
            throw new IllegalStateException("Value for " + type + " out of " +
                                            "valid range: " + addr);

        }
    }

    public static ExceptionHandler<FixedLocation> readFrom(ConstantPool cp, DataInput din)
        throws IOException
    {
        int start_pc = din.readUnsignedShort();
        int end_pc = din.readUnsignedShort();
        int handler_pc = din.readUnsignedShort();
        int catch_type = din.readUnsignedShort();

        ConstantClassInfo catchTypeConstant;
        if (catch_type == 0) {
            catchTypeConstant = null;
        } else {
            catchTypeConstant = (ConstantClassInfo)cp.getConstant(catch_type);
        }

        return new ExceptionHandler<FixedLocation>(new FixedLocation(start_pc),
                                                   new FixedLocation(end_pc),
                                                   new FixedLocation(handler_pc),
                                                   catchTypeConstant);
    }
}
