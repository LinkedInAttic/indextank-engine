package com.flaptor.util.compression;

import java.io.IOException;

/**
 * Stores a compressed integer sequence, using the RLE algorithm.
 * @author jorge
 */
public class CompressedIntSequence extends EncodedIntSequence {

    private static final long serialVersionUID = 1L;
    private int size = 0;
    
    public CompressedIntSequence(int[] data) {
        super();
        size = data.length;
        int last = -1;
        int run = 0;
        for (int i = 0; i < size; i++) {
            int val = data[i];
            if (val == last) {
                run++;
            } else {
                if (run > 0) {
                    encodeRun(run,last);
                    run = 0;
                }
                encodeInt(val);
                last = val;
            }
        }
        if (run > 0) {
            encodeRun(run,last);
        }
        close();
    }

    private void encodeRun(int run, int val) {
        if (run > 3) { // encoding a run requires at least 3 bytes
            encodeMarker();
            encodeInt(run);
            encodeInt(val);
        } else {
            while (run > 0) {
                encodeInt(val);
                run--;
            }
        }
    }

    public synchronized int[] getUncompressed() {
        reset();
        int[] out = new int[size];
        int i = 0;
        while (i < size) {
            if (isMarker()) {
                int run = decodeInt();
                int val = decodeInt();
                while (run > 0) {
                    out[i++] = val;
                    run--;
                }
            } else {
                out[i++] = decodeInt();
            }
        }
        return out;
    }
    
    @Override
    protected void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(size);
        super.writeObject(out);
    }
    
    @Override
    protected void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        size = in.readInt();
        super.readObject(in);
    }
    

    
}
