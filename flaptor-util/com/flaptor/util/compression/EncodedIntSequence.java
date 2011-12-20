package com.flaptor.util.compression;

import com.flaptor.util.FileUtil;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Stores integers in byte sequence, using fewer than four bytes for smaller integers.
 * A special convenience marker can be encoded that cannot be produced by encoding regular 
 * integers.
 * @author jorge
 */
public class EncodedIntSequence implements Serializable {

    private static final long serialVersionUID = 1L;
    private ArrayList<Integer> data = null;
    protected byte[] enc = null;
    private int pos = 0;
    private int MARKER = (byte) 0xFC;

    public EncodedIntSequence() {
        data = new ArrayList<Integer>();
    }

    public void encodeInt(int val) {
        if (val < 0xFC) {
            data.add(val);
        } else if (val <= 0xFFFF) {
            data.add(0xFD); // 2 bytes
            data.add((val & 0xFF00) >> 8);
            data.add(val & 0x00FF);
        } else if (val <= 0xFFFFFF) {
            data.add(0xFE); // 3 bytes
            data.add((val & 0xFF0000) >> 16);
            data.add((val & 0x00FF00) >> 8);
            data.add(val & 0x0000FF);
        } else {
            data.add(0xFF); // 4 bytes
            data.add((val & 0xFF000000) >> 24);
            data.add((val & 0x00FF0000) >> 16);
            data.add((val & 0x0000FF00) >> 8);
            data.add(val & 0x000000FF);
        }
    }

    public void close() {
        if (null != data) {
            enc = new byte[data.size()];
            for (int i = 0; i < data.size(); i++) {
                enc[i] = (byte)(int)data.get(i);
            }
            data.clear();
            data = null;
        }
    }
    
    public void reset() {
        if (null != data) {
            close();
        }
        pos = 0;
    }
    
    @SuppressWarnings("fallthrough")
    public int decodeInt() {
        int val = 0;
        if (null == enc) {
            throw new IllegalStateException("Trying to read sequence before closing it.");
        }
//System.out.println("enc["+pos+"]="+valAt(pos)+" ("+toHexa(enc[pos])+")");
        if (valAt(pos) < 0xFC) {
            val = valAt(pos++);
        } else {
            switch (enc[pos++]) {
                case (byte) 0xFF: // 4 bytes
                    val = valAt(pos++) << 24;
                case (byte) 0xFE: // 3 bytes
                    val |= valAt(pos++) << 16;
                case (byte) 0xFD: // 2 bytes
                    val |= valAt(pos++) << 8;
                    val |= valAt(pos++);
                    break;
                default:
                    throw new IllegalStateException("Found unexpected marker");
                }
        }
        return val;
    }

    public int valAt(int i) {
        byte b = enc[i];
        if (b < 0) {
            return b+256;
        } else {
            return b;
        }
    }
    
    public void encodeMarker() {
        if (null == data) {
            throw new IllegalStateException("Trying to write on closed sequence.");
        }
        data.add(MARKER);
    }

    public boolean isMarker() {
        if (null == enc) {
            throw new IllegalStateException("Trying to read sequence before closing it.");
        }
        if (MARKER == enc[pos]) {
            pos++;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EncodedIntSequence)) { return false; }
        return java.util.Arrays.equals(enc, ((EncodedIntSequence)other).enc);
    }
    
    @Override
    public int hashCode() {
        return null == enc ? 0 : enc.hashCode();
    }
    
    static String hexa = "0123456789ABCDEF";
    private String toHexa(int v) {
        if (v < 0) { v += 256; }
        return ""+hexa.charAt(v / 16)+hexa.charAt(v % 16)+" ";
    }
    
    @Override
    public String toString() {
        if (null == enc) {
            close();
        }
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<pos; i++) {
            buf.append(toHexa(enc[i]));
        }
        return buf.toString();
    }
    
    protected void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if (null == enc) {
            close();
        }
        out.writeInt(enc.length);
        for (int i=0; i<enc.length; i++) {
            out.writeByte(enc[i]);
        }
    }
    
    protected void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        int len = in.readInt();
        for (int i=0; i<len; i++) {
            data.add((int)in.readByte());
        }
        close();
    }
    
    // For testing purposes
    public static void main(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException {
        EncodedIntSequence seq1 = new EncodedIntSequence();
        int max = 500000000, step = 100;
        for (int i=0; i<max; i+=step) {
            seq1.encodeInt(i);
        }
        seq1.close();
        
        File dir = FileUtil.createTempDir("","");
        File file = new File(dir,"encode.tst");
        ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file, false), 16000));
        seq1.writeObject(outputStream);
        outputStream.close();
        
        ObjectInputStream inputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file), 16000));
        EncodedIntSequence seq2 = new EncodedIntSequence();
        seq2.readObject(inputStream);
        FileUtil.deleteDir(dir);
        
        for (int i=0; i<max; i+=step) {
            int d = seq2.decodeInt();
            if (i != d) {
                System.out.println("ERROR: "+i+" <> "+d);
                System.exit(-1);
            }
        }
        System.out.println("Ok!");
    }

}
