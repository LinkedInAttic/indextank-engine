package com.flaptor.indextank.index.scorer;

import java.util.Arrays;

import com.flaptor.indextank.util.FormatLogger;


public class CategoryEncoder {
    private static final FormatLogger logger = new FormatLogger();

    /**
     * Precondition bitmask.length >= data.length
     */
    public static int decode(int[] data, int dataOffset, int[] bitmask) {
        int retval = 0;
        for (int i = bitmask.length - 1; i >= 0 ; i--) {
            for (int j = 31; j >=0; j--) {
                if ((bitmask[i] & (1<<j)) != 0) {
                    retval = retval << 1;
                    if ((data.length - dataOffset > i) && ( (data[i+dataOffset] & (1<<j)) != 0)) {
                        retval = retval | 1;
                    }
                }
            }
        }
        return retval;
    }

    public static int[] encode(int[] oldData, int dataOffset, int[] bitmask, int value) {
        int[] data;
        if (oldData.length - dataOffset < bitmask.length) {
            data = new int[bitmask.length + dataOffset];
            System.arraycopy(oldData, 0, data, 0, oldData.length);
        } else {
            data = oldData;
        }
        for (int i =dataOffset; i < data.length &&  i < bitmask.length + dataOffset; i++) {
            for (int j = 0; j < 32; j++) {
                if ((bitmask[i - dataOffset] & (1<<j)) != 0) {
                    data[i] = data[i] & (~(1<<j));
                    if ((value & 1) != 0) {
                        data[i] = data[i] | (1 << j);
                    }
                    value = value >>> 1;
                }
            }
        }
        if (value != 0) {
            throw new IllegalArgumentException("value too big to encode in bitmasked space");
        }
        return data;

    }

    /**
     * Check if the dataBitmap masked with the categoriesBitmask is compliant with the valuesBitmap.
     * This implies anding every int in the dataBitmap with its correspoding int in the categoriesBitmask
     * and checking the result against the correspoding int in the valuesBitmap. In case the valuesBitmap
     * is larger than the dataBitmap, every extra position must be zero. 
     * 
     * @param dataBitmap the document data
     * @param offset the offset of the dataBitmap where the actual facets data begins (after the variables data)
     * @param categoriesBitmask mask to apply to the data for the category/ies to consider
     * @param valuesBitmap values to match
     * @return <code>true</code> if the values match 
     */
    public static boolean matches(int[] dataBitmap, int offset, int[] categoriesBitmask, int[] valuesBitmap) {
    	if (valuesBitmap.length != categoriesBitmask.length) {
    		throw new IllegalArgumentException("Values and mask must have the same size");
    	}
    	
    	for (int i = 0; i < valuesBitmap.length; i++) {
			if (i >= dataBitmap.length - offset) {
				if (valuesBitmap[i] != 0) {
					return false;
				}
			} else if ((dataBitmap[i + offset] & categoriesBitmask[i]) != valuesBitmap[i]) {
				return false;
			}
		}
    	
    	return true;
    }
    

}
