package com.flaptor.indextank.index.scorer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;

public class CategoryMaskManager implements Serializable {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    private AtomicInteger nextbit;
    private ConcurrentMap<String, CategoryInfo> categoryInfoMap;

    private final ReentrantReadWriteLock dumpLock;

    public CategoryMaskManager(ReentrantReadWriteLock dumpLock) {
        this.nextbit = new AtomicInteger(0);
        this.categoryInfoMap = new MapMaker().makeMap();
        this.dumpLock = dumpLock;
    }

    public int getMaxMaskSize() {
    	int nextbitValue = nextbit.get();
    	
    	if (nextbitValue == 0) {
    		return 0;
    	} else {
    		return (nextbitValue - 1) / 32 + 1;
    	}
    	
    }
    
    public CategoryValueInfo getCategoryValueInfo(final String category, String value) {
        CategoryInfo catInfo = categoryInfoMap.get(category);
        if (value.isEmpty()) {
        	value = null;
        }
        if (null == catInfo) {
        	if (value == null) {
        		return null;
        	}
            dumpLock.readLock().lock();
            try {
                catInfo = new CategoryInfo();
                CategoryInfo previous = categoryInfoMap.putIfAbsent(category, catInfo);
                if (previous != null) {
                    // somebody beat us to it
                    catInfo = previous;
                }
            } finally {
                dumpLock.readLock().unlock();
            }
        }

        return catInfo.getCategoryValueInfo(value);        
    }
    
	public Map<String, CategoryInfo> getCategoryInfos() {
		return categoryInfoMap;
	}

    private static int getBitMaskSize(int[] bitmask) {
    	int counter = 0;
    	for (int i = 0; i < bitmask.length; i++) {
    		int mask = 1;
			for (int j = 0; j < 32; j++) {
				if ((bitmask[i] & mask) != 0) {
					counter++;
				}
				mask = mask << 1;
			}
		}
    	
    	return 1 << counter;
    }

    private int[] enlargeBitMask(int[] bitmask) {
    	int bit = nextbit.getAndIncrement();
    	int position = bit / 32;
    	if (bitmask.length <= position) {
    		int[] newBitmask = new int[position + 1];
    		System.arraycopy(bitmask, 0, newBitmask, 0, bitmask.length);
    		bitmask = newBitmask;
    	}
    	
    	bitmask[position] = bitmask[position] + (int)(1 << bit % 32);
    	return bitmask;
    }
    
    /**
     * Private method to save state to disk.
     * Blocking.
     */
    synchronized void writeData(DataOutputStream dos) throws IOException {
        dos.writeInt(nextbit.get());
        
        for (Entry<String, CategoryInfo> entry : categoryInfoMap.entrySet()) {
            dos.writeUTF(entry.getKey());
            entry.getValue().writeData(dos);
        }
        
        dos.writeUTF("\u0000");
    }

    synchronized void readData(DataInputStream dis) throws IOException {
        nextbit.set(dis.readInt());
        
        while (true) {
            String value = dis.readUTF();
            if (value.equals("\u0000")) break;
            CategoryInfo info = readCategoryInfoData(dis);
            categoryInfoMap.put(value, info);
        }
    }

    CategoryInfo readCategoryInfoData(DataInputStream dis) throws IOException {
        CategoryInfo ci = new CategoryInfo();
        int len = dis.readInt();
        ci.bitmask = new int[len];
        for (int i = 0; i < len; i++) {
            ci.bitmask[i] = dis.readInt();
        }
        while (true) {
            int code = dis.readInt();
            if (code == -1) break;
            String value = dis.readUTF();
            ci.valueCodes.put(value, code);
            ci.codeValues.put(code, value);
        }
        return ci;
    }

	//--------------------------------------------------------------------
    //Internal classes.
    public final class CategoryInfo implements Serializable {
    	private ConcurrentMap<String, Integer> valueCodes;
    	private ConcurrentMap<Integer, String> codeValues;
    	private int[] bitmask;

    	public CategoryInfo() {
			this.valueCodes = new ConcurrentHashMap<String, Integer>();
			this.codeValues = new ConcurrentHashMap<Integer, String>();
			this.bitmask = new int[0];
		}
    	
    	void writeData(DataOutputStream dos) throws IOException {
    	    int[] bm = bitmask;
    	    dos.writeInt(bm.length);
    	    for (int i = 0; i < bm.length; i++) {
    	        dos.writeInt(bm[i]);
            }
            for (Entry<String, Integer> entry : valueCodes.entrySet()) {
                dos.writeInt(entry.getValue());
                dos.writeUTF(entry.getKey());
            }
            
            dos.writeInt(-1);
        }
    	
        public CategoryValueInfo getCategoryValueInfo(String value) {
            // optimize lock
            synchronized (bitmask) {
            	Integer intValue;
            	if (value != null) {
            		intValue = valueCodes.get(value);
            	} else {
            		intValue = 0;
            	}
        		
        		if (intValue == null) {
        				intValue = valueCodes.size() + 1;
        				if (getBitMaskSize(bitmask) - 1 < intValue) {
        				    dumpLock.readLock().lock();
        				    try {
        				        bitmask = enlargeBitMask(bitmask);
        				    } finally {
        				        dumpLock.readLock().unlock();
        				    }
        				}
        				valueCodes.put(value, intValue);
        				codeValues.put(intValue, value);
    
    			}
        		return new CategoryValueInfo(bitmask, intValue);
    		}
    	}
    	
    	public int[] getBitmask() {
    		return bitmask;
    	}
    	
    	public Integer getValueCode(String value) {
    		return valueCodes.get(value);
    	}
    	
    	public String getValue(int code) {
    		return codeValues.get(code);
    	}
    	
    	public int getSize() {
    		return valueCodes.size();
    	}

    }

    public static final class CategoryValueInfo {
        private final int[] bitmask;
        private final int valueCode;

        public CategoryValueInfo(int[] bitmask, int valueCode) {
            this.bitmask = bitmask;
            this.valueCode = valueCode;
        }

        public int[] getBitmask() {
            return bitmask;
        }

        public int getValueCode() {
            return valueCode;
        }
    }

    public Map<String, String> getStats() {
        HashMap<String, String> stats = Maps.newHashMap();
        stats.put("categories_bits", String.valueOf(nextbit.get()));
        stats.put("categories_count", String.valueOf(categoryInfoMap.size()));
        return stats;
    }


}
