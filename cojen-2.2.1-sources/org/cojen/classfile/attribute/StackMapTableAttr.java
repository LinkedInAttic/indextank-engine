/*
 *  Copyright 2008-2010 Brian S O'Neill
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import org.cojen.classfile.Attribute;
import org.cojen.classfile.ConstantPool;
import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.TypeDesc;

import org.cojen.classfile.constant.ConstantClassInfo;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class StackMapTableAttr extends Attribute {
    private final InitialFrame mInitialFrame;
    private int mSize;
    private int mLength;

    public StackMapTableAttr(ConstantPool cp, String name, int length, DataInput din)
        throws IOException
    {
        super(cp, name);

        int size = din.readUnsignedShort();
        List<StackMapFrame> frames = new ArrayList<StackMapFrame>(size);

        InitialFrame first = new InitialFrame();
        StackMapFrame last = first;
        for (int i=0; i<size; i++) {
            StackMapFrame frame = StackMapFrame.read(last, cp, din);
            if (frame != null) {
                last = frame;
            }
        }

        mInitialFrame = first;
        mSize = size;
        mLength = length;
    }

    public int getLength() {
        if (mLength < 0) {
            if (mInitialFrame.getNext() == null) {
                mLength = 0;
            } else {
                int length = 2;
                StackMapFrame frame = mInitialFrame;
                while (frame != null) {
                    length += frame.getLength();
                    frame = frame.getNext();
                }
                mLength = length;
            }
        }
        return mLength;
    }

    @Override
    public void writeTo(DataOutput dout) throws IOException {
        if (mSize == 0) {
            return;
        }
        super.writeTo(dout);
    }

    @Override
    public void writeDataTo(DataOutput dout) throws IOException {
        dout.writeShort(mSize);
        StackMapFrame frame = mInitialFrame;
        while (frame != null) {
            frame.writeTo(dout);
            frame = frame.getNext();
        }
    }

    public StackMapFrame getInitialFrame() {
        return mInitialFrame;
    }

    public void initialStackMapFrame(MethodInfo method) {
        mInitialFrame.set(getConstantPool(), method);
    }

    public static abstract class StackMapFrame {
        static StackMapFrame read(StackMapFrame prev, ConstantPool cp, DataInput din)
            throws IOException
        {
            int frameType = din.readUnsignedByte();

            if (frameType <= 63) {
                return new SameFrame(prev, frameType);
            } else if (frameType <= 127) {
                return new SameLocalsOneStackItemFrame(prev, frameType - 64, cp, din);
            } else if (frameType <= 246) {
                // reserved frame type range
                return null;
            } else if (frameType == 247) {
                return new SameLocalsOneStackItemFrameExtended(prev, cp, din);
            } else if (frameType <= 250) {
                return new ChopFrame(prev, 251 - frameType, din);
            } else if (frameType == 251) {
                return new SameFrameExtended(prev, din);
            } else if (frameType <= 254) {
                return new AppendFrame(prev, frameType - 251, cp, din);
            } else {
                return new FullFrame(prev, cp, din);
            }
        }

        private final StackMapFrame mPrev;
        private StackMapFrame mNext;
        int mOffset = -1;

        StackMapFrame(StackMapFrame prev) {
            if ((mPrev = prev) != null) {
                prev.mNext = this;
            }
        }

        public abstract int getLength();

        public abstract int getOffsetDelta();

        public int getOffset() {
            if (mOffset < 0) {
                if (mPrev == null || mPrev instanceof InitialFrame) {
                    mOffset = getOffsetDelta();
                } else {
                    mOffset = mPrev.getOffset() + 1 + getOffsetDelta();
                }
            }
            return mOffset;
        }

        public abstract VerificationTypeInfo[] getLocalInfos();

        public abstract VerificationTypeInfo[] getStackItemInfos();

        public StackMapFrame getPrevious() {
            return mPrev;
        }

        public StackMapFrame getNext() {
            return mNext;
        }

        public abstract void writeTo(DataOutput dout) throws IOException;
    }

    private static class InitialFrame extends StackMapFrame {
        private VerificationTypeInfo[] mLocalInfos;

        InitialFrame() {
            super(null);
            mOffset = 0;
        }

        public int getLength() {
            return 0;
        }

        public int getOffsetDelta() {
            return 0;
        }

        public VerificationTypeInfo[] getLocalInfos() {
            if (mLocalInfos == null) {
                return VerificationTypeInfo.EMPTY_ARRAY;
            }
            return mLocalInfos.clone();
        }

        public VerificationTypeInfo[] getStackItemInfos() {
            return VerificationTypeInfo.EMPTY_ARRAY;
        }

        public void writeTo(DataOutput dout) {
        }

        void set(ConstantPool cp, MethodInfo info) {
            TypeDesc[] paramTypes = info.getMethodDescriptor().getParameterTypes();

            VerificationTypeInfo[] infos;
            int offset;

            if (info.getModifiers().isStatic()) {
                infos = new VerificationTypeInfo[paramTypes.length];
                offset = 0;
            } else {
                infos = new VerificationTypeInfo[1 + paramTypes.length];
                if (info.getName().equals("<init>")) {
                    infos[0] = UninitThisVariableInfo.THE;
                } else {
                    infos[0] = VerificationTypeInfo.forType(cp, info.getClassFile().getType());
                }
                offset = 1;
            }

            for (int i=0; i<paramTypes.length; i++) {
                infos[offset + i] = VerificationTypeInfo.forType(cp, paramTypes[i]);
            }

            mLocalInfos = infos;
        }
    }

    private static class SameFrame extends StackMapFrame {
        private final int mOffsetDelta;

        SameFrame(StackMapFrame prev, int offsetDelta) {
            super(prev);
            mOffsetDelta = offsetDelta;
        }

        public int getLength() {
            return 1;
        }

        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        public VerificationTypeInfo[] getLocalInfos() {
            return getPrevious().getLocalInfos();
        }

        public VerificationTypeInfo[] getStackItemInfos() {
            return VerificationTypeInfo.EMPTY_ARRAY;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(mOffsetDelta);
        }
    }

    private static class SameLocalsOneStackItemFrame extends StackMapFrame {
        private final int mOffsetDelta;
        private final VerificationTypeInfo mStackItemInfo;

        SameLocalsOneStackItemFrame(StackMapFrame prev,
                                    int offsetDelta, ConstantPool cp, DataInput din)
            throws IOException
        {
            super(prev);
            mOffsetDelta = offsetDelta;
            mStackItemInfo = VerificationTypeInfo.read(cp, din);
        }

        public int getLength() {
            return 1 + mStackItemInfo.getLength();
        }

        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        public VerificationTypeInfo[] getLocalInfos() {
            return getPrevious().getLocalInfos();
        }

        public VerificationTypeInfo[] getStackItemInfos() {
            return new VerificationTypeInfo[]{mStackItemInfo};
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(64 + mOffsetDelta);
            mStackItemInfo.writeTo(dout);
        }
    }

    private static class SameLocalsOneStackItemFrameExtended extends StackMapFrame {
        private final int mOffsetDelta;
        private final VerificationTypeInfo mStackItemInfo;

        SameLocalsOneStackItemFrameExtended(StackMapFrame prev, ConstantPool cp, DataInput din)
            throws IOException
        {
            super(prev);
            mOffsetDelta = din.readUnsignedShort();
            mStackItemInfo = VerificationTypeInfo.read(cp, din);
        }

        public int getLength() {
            return 3 + mStackItemInfo.getLength();
        }

        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        public VerificationTypeInfo[] getLocalInfos() {
            return getPrevious().getLocalInfos();
        }

        public VerificationTypeInfo[] getStackItemInfos() {
            return new VerificationTypeInfo[]{mStackItemInfo};
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(257);
            dout.writeShort(mOffsetDelta);
            mStackItemInfo.writeTo(dout);
        }
    }

    private static class ChopFrame extends StackMapFrame {
        private final int mOffsetDelta;
        private final int mChop;

        private transient VerificationTypeInfo[] mLocalInfos;

        ChopFrame(StackMapFrame prev, int chop, DataInput din) throws IOException {
            super(prev);
            mOffsetDelta = din.readUnsignedShort();
            mChop = chop;
        }

        public int getLength() {
            return 3;
        }

        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        public VerificationTypeInfo[] getLocalInfos() {
            if (mLocalInfos == null) {
                VerificationTypeInfo[] prevInfos = getPrevious().getLocalInfos();
                VerificationTypeInfo[] infos = new VerificationTypeInfo[prevInfos.length - mChop];
                System.arraycopy(prevInfos, 0, infos, 0, infos.length);
                mLocalInfos = infos;
            }
            return mLocalInfos.clone();
        }

        public VerificationTypeInfo[] getStackItemInfos() {
            return VerificationTypeInfo.EMPTY_ARRAY;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(251 - mChop);
            dout.writeShort(mOffsetDelta);
        }
    }

    private static class SameFrameExtended extends StackMapFrame {
        private final int mOffsetDelta;

        SameFrameExtended(StackMapFrame prev, DataInput din) throws IOException {
            super(prev);
            mOffsetDelta = din.readUnsignedShort();
        }

        public int getLength() {
            return 3;
        }

        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        public VerificationTypeInfo[] getLocalInfos() {
            return getPrevious().getLocalInfos();
        }

        public VerificationTypeInfo[] getStackItemInfos() {
            return VerificationTypeInfo.EMPTY_ARRAY;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(251);
            dout.writeShort(mOffsetDelta);
        }
    }

    private static class AppendFrame extends StackMapFrame {
        private final int mOffsetDelta;
        private final VerificationTypeInfo[] mAppendInfos;

        private transient VerificationTypeInfo[] mLocalInfos;

        AppendFrame(StackMapFrame prev, int numLocals, ConstantPool cp, DataInput din)
            throws IOException
        {
            super(prev);
            mOffsetDelta = din.readUnsignedShort();
            mAppendInfos = VerificationTypeInfo.read(cp, din, numLocals);
        }

        public int getLength() {
            int length = 3;
            for (VerificationTypeInfo info : mAppendInfos) {
                length += info.getLength();
            }
            return length;
        }

        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        public VerificationTypeInfo[] getLocalInfos() {
            if (mLocalInfos == null) {
                VerificationTypeInfo[] prevInfos = getPrevious().getLocalInfos();
                VerificationTypeInfo[] infos =
                    new VerificationTypeInfo[prevInfos.length + mAppendInfos.length];
                System.arraycopy(prevInfos, 0, infos, 0, prevInfos.length);
                System.arraycopy(mAppendInfos, 0, infos, prevInfos.length, mAppendInfos.length);
                mLocalInfos = infos;
            }
            return mLocalInfos.clone();
        }

        public VerificationTypeInfo[] getStackItemInfos() {
            return VerificationTypeInfo.EMPTY_ARRAY;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(251 + mAppendInfos.length);
            dout.writeShort(mOffsetDelta);
            for (VerificationTypeInfo info : mAppendInfos) {
                info.writeTo(dout);
            }
        }
    }

    private static class FullFrame extends StackMapFrame {
        private final int mOffsetDelta;
        private final VerificationTypeInfo[] mLocalInfos;
        private final VerificationTypeInfo[] mStackItemInfos;

        FullFrame(StackMapFrame prev, ConstantPool cp, DataInput din) throws IOException {
            super(prev);
            mOffsetDelta = din.readUnsignedShort();
            int numLocals = din.readUnsignedShort();
            mLocalInfos = VerificationTypeInfo.read(cp, din, numLocals);
            int numStackItems = din.readUnsignedShort();
            mStackItemInfos = VerificationTypeInfo.read(cp, din, numStackItems);
        }

        public int getLength() {
            int length = 7;
            for (VerificationTypeInfo info : mLocalInfos) {
                length += info.getLength();
            }
            for (VerificationTypeInfo info : mStackItemInfos) {
                length += info.getLength();
            }
            return length;
        }

        public int getOffsetDelta() {
            return mOffsetDelta;
        }

        public VerificationTypeInfo[] getLocalInfos() {
            return mLocalInfos.clone();
        }

        public VerificationTypeInfo[] getStackItemInfos() {
            return mStackItemInfos.clone();
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(255);
            dout.writeShort(mOffsetDelta);
            dout.writeShort(mLocalInfos.length);
            for (VerificationTypeInfo info : mLocalInfos) {
                info.writeTo(dout);
            }
            dout.writeShort(mStackItemInfos.length);
            for (VerificationTypeInfo info : mStackItemInfos) {
                info.writeTo(dout);
            }
        }
    }

    public static abstract class VerificationTypeInfo {
        static final VerificationTypeInfo[] EMPTY_ARRAY = new VerificationTypeInfo[0];

        static VerificationTypeInfo read(ConstantPool cp, DataInput din)
            throws IOException
        {
            int type = din.readUnsignedByte();
            switch (type) {
            case 0:
                return TopVariableInfo.THE;
            case 1:
                return IntegerVariableInfo.THE;
            case 2:
                return FloatVariableInfo.THE;
            case 3:
                return DoubleVariableInfo.THE;
            case 4:
                return LongVariableInfo.THE;
            case 5:
                return NullVariableInfo.THE;
            case 6:
                return UninitThisVariableInfo.THE;
            case 7:
                return new ObjectVariableInfo(cp, din);
            case 8:
                return new UninitVariableInfo(cp, din);
            }
            return null;
        }

        static VerificationTypeInfo forType(ConstantPool cp, TypeDesc type) {
            switch (type.getTypeCode()) {
            default:
                return TopVariableInfo.THE;
            case TypeDesc.OBJECT_CODE:
                return new ObjectVariableInfo(cp, type);
            case TypeDesc.BOOLEAN_CODE:
            case TypeDesc.BYTE_CODE:
            case TypeDesc.CHAR_CODE:
            case TypeDesc.SHORT_CODE:
            case TypeDesc.INT_CODE:
                return IntegerVariableInfo.THE;
            case TypeDesc.LONG_CODE:
                return LongVariableInfo.THE;
            case TypeDesc.FLOAT_CODE:
                return FloatVariableInfo.THE;
            case TypeDesc.DOUBLE_CODE:
                return DoubleVariableInfo.THE;
            }
        }

        private static VerificationTypeInfo[] read(ConstantPool cp, DataInput din, int num)
            throws IOException
        {
            VerificationTypeInfo[] infos = new VerificationTypeInfo[num];
            for (int i=0; i<num; i++) {
                infos[i] = read(cp, din);
            }
            return infos;
        }

        VerificationTypeInfo() {
        }

        public int getLength() {
            return 1;
        }

        public abstract TypeDesc getType();

        /**
         * When true, type is unassigned or unused.
         */
        public boolean isTop() {
            return false;
        }

        /**
         * When true, type is the "this" reference.
         */
        public boolean isThis() {
            return false;
        }

        /**
         * When true, type is an object whose constructor has not been called.
         */
        public boolean isUninitialized() {
            return false;
        }

        public abstract void writeTo(DataOutput dout) throws IOException;

        @Override
        public String toString() {
            return getType().getFullName();
        }
    }

    private static class TopVariableInfo extends VerificationTypeInfo {
        static final TopVariableInfo THE = new TopVariableInfo();

        private TopVariableInfo() {
        }

        public TypeDesc getType() {
            return null;
        }

        @Override
        public boolean isTop() {
            return true;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(0);
        }

        @Override
        public String toString() {
            return "<top>";
        }
    }

    private static class IntegerVariableInfo extends VerificationTypeInfo {
        static final IntegerVariableInfo THE = new IntegerVariableInfo();

        private IntegerVariableInfo() {
        }

        public TypeDesc getType() {
            return TypeDesc.INT;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(1);
        }
    }

    private static class FloatVariableInfo extends VerificationTypeInfo {
        static final FloatVariableInfo THE = new FloatVariableInfo();

        private FloatVariableInfo() {
        }

        public TypeDesc getType() {
            return TypeDesc.FLOAT;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(2);
        }
    }

    private static class LongVariableInfo extends VerificationTypeInfo {
        static final LongVariableInfo THE = new LongVariableInfo();

        private LongVariableInfo() {
        }

        public TypeDesc getType() {
            return TypeDesc.LONG;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(4);
        }
    }

    private static class DoubleVariableInfo extends VerificationTypeInfo {
        static final DoubleVariableInfo THE = new DoubleVariableInfo();

        private DoubleVariableInfo() {
        }

        public TypeDesc getType() {
            return TypeDesc.DOUBLE;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(3);
        }
    }

    private static class NullVariableInfo extends VerificationTypeInfo {
        static final NullVariableInfo THE = new NullVariableInfo();

        private NullVariableInfo() {
        }

        public TypeDesc getType() {
            return null;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(5);
        }

        @Override
        public String toString() {
            return "<null>";
        }
    }

    private static class UninitThisVariableInfo extends VerificationTypeInfo {
        static final UninitThisVariableInfo THE = new UninitThisVariableInfo();

        private UninitThisVariableInfo() {
        }

        public TypeDesc getType() {
            return null;
        }

        @Override
        public boolean isThis() {
            return true;
        }

        @Override
        public boolean isUninitialized() {
            return true;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(6);
        }

        @Override
        public String toString() {
            return "<uninitialized this>";
        }
    }

    private static class ObjectVariableInfo extends VerificationTypeInfo {
        private final ConstantClassInfo mClassInfo;

        ObjectVariableInfo(ConstantPool cp, DataInput din) throws IOException {
            mClassInfo = (ConstantClassInfo) cp.getConstant(din.readUnsignedShort());
        }

        ObjectVariableInfo(ConstantPool cp, TypeDesc desc) {
            mClassInfo = cp.addConstantClass(desc);
        }

        @Override
        public int getLength() {
            return 3;
        }

        public TypeDesc getType() {
            return mClassInfo.getType();
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(7);
            dout.writeShort(mClassInfo.getIndex());
        }
    }

    private static class UninitVariableInfo extends VerificationTypeInfo {
        private final int mOffset;

        UninitVariableInfo(ConstantPool cp, DataInput din) throws IOException {
            mOffset = din.readUnsignedShort();
        }

        @Override
        public int getLength() {
            return 3;
        }

        public TypeDesc getType() {
            return null;
        }

        @Override
        public boolean isUninitialized() {
            return true;
        }

        public void writeTo(DataOutput dout) throws IOException {
            dout.writeByte(8);
            dout.writeShort(mOffset);
        }

        @Override
        public String toString() {
            return "<uninitialized>";
        }
    }
}
