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

package com.flaptor.indextank.index.scorer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.flaptor.indextank.index.DocId;
import com.flaptor.indextank.index.scorer.CategoryMaskManager.CategoryInfo;
import com.flaptor.indextank.index.scorer.CategoryMaskManager.CategoryValueInfo;
import com.flaptor.indextank.index.scorer.DynamicBoostsManager.DynamicBoosts;
import com.flaptor.util.Execute;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

@SuppressWarnings("deprecation")
public class DynamicDataManager implements BoostsManager {
	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	private static final String OLD_MAIN_FILE_NAME = "dynamicBoosts";
	private static final String MAIN_FILE_NAME = "dynamicData";

	private final int numberOfBoosts;
    private final ConcurrentMap<DocId, DynamicData> dynamicDataMap;
    private final File backupDir;
    private final DynamicData emptyData;
    private final CategoryMaskManager maskManager;
    private final ReentrantReadWriteLock dumpLock = new ReentrantReadWriteLock();

    /**
     * Build a {@link DynamicDataManager} with a backupDir. If the directory containts a
     * dynamic boosts file, it loads the data from it. Otherwise, creates a new boosts map  
     * 
     * @param numberOfBoosts the number of boosting doubles that this Scorer will store.
     * @param backupDir the directory to which the data stored in this Scorer shall be
     */
    @SuppressWarnings("unchecked")
	public DynamicDataManager(int numberOfBoosts, File backupDir) {
		Preconditions.checkArgument(numberOfBoosts > 0);
    	checkDirArgument(backupDir);
        this.numberOfBoosts = numberOfBoosts;
        this.backupDir = backupDir;
        this.emptyData = new DynamicData(numberOfBoosts);
        this.maskManager = new CategoryMaskManager(dumpLock);

        File oldFormatFile = new File(this.backupDir, OLD_MAIN_FILE_NAME);
        File newFormatFile = new File(this.backupDir, MAIN_FILE_NAME);
        if (!newFormatFile.exists() && oldFormatFile.exists()) {
            logger.info("Found old format file, loading it.");
            ObjectInputStream is = null;
            try {
                is = new ObjectInputStream(new BufferedInputStream(new FileInputStream(oldFormatFile)));
                int storedNumberOfBoosts = is.readInt();
                if (storedNumberOfBoosts != numberOfBoosts) {
                	throw new IllegalArgumentException("Number of boosts specified in Manager construction differ from the one stored in the backup file (" + numberOfBoosts + " vs. " + storedNumberOfBoosts +")");
                }
                try {
                    ConcurrentMap<?, ?> read = (ConcurrentMap<?, ?>) is.readObject();
                    Set<?> keys = read.keySet();
                    if (keys.isEmpty() || (keys.iterator().next() instanceof DocId && read.values().iterator().next() instanceof DynamicData)) {
                        // last version, assign directly to field
                        dynamicDataMap = (ConcurrentMap<DocId, DynamicData>) read;
                    } else {
                        // values are definitely Boosts, we'll need to transform them
                        // and check whether the keys are strings (v1) or DocIds (v2)
                        dynamicDataMap = new ConcurrentHashMap<DocId, DynamicData>();
                        boolean areDocids = keys.iterator().next() instanceof DocId;
                        for (Map.Entry<?,?> e : read.entrySet()) {
                            // convert key to docid if necessary
                            DocId docId = areDocids ? (DocId)e.getKey() : new DocId((String)e.getKey());
                            // convert value and add to the map
                            dynamicDataMap.put(docId, new DynamicData((DynamicBoosts)e.getValue()));
                        }
                    }
                    logger.info("State loaded.");
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            } catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
                Execute.close(is);
            }
        } else {
        	this.dynamicDataMap = new ConcurrentHashMap<DocId, DynamicData>();
        	if (newFormatFile.exists()) {
        	    readFromDisk();
        	}
        }
    }
    
	@Override
	public Boosts getBoosts(DocId documentId) {
	    DynamicData data = dynamicDataMap.get(documentId);
	    if (data == null) {
	        logger.warn("Failed to find boosts for document " + documentId);
	        return this.emptyData;
	    }
	    return data;
	}

    public Map<Integer, Double> getVariablesAsMap(DocId documentId) {
        DynamicData data = dynamicDataMap.get(documentId);
        if (data == null) {
            logger.warn("Failed to find variables for document " + documentId);
            return ImmutableMap.of();
        }
        return data.getVariablesAsMap(numberOfBoosts);
    }

    public Map<String, String> getCategoriesAsMap(DocId documentId) {
        try {
            return getCategoryValues(documentId);
        } catch (IllegalArgumentException e) {
            logger.warn(e.getMessage());
        }
        return ImmutableMap.of(); 
    }

	public int getNumberOfBoosts() {
		return numberOfBoosts;
	}
	
	@Override
	public int getDocumentCount() {
		return dynamicDataMap.size();
	}

	DynamicData getDynamicData(DocId docId) {
		return dynamicDataMap.get(docId);
	}
	
	@Override
	public void removeBoosts(String documentId) {
		dynamicDataMap.remove(new DocId(documentId));
	}

	@Override
	public void setBoosts(String documentId, Map<Integer, Float> boosts) {
		setBoosts(documentId, null, boosts);
	}
	
    public void setCategoryValues(String documentId, Map<String, String> categories) {
    	DynamicData data = getOrCreateData(documentId);
        for (Map.Entry<String, String> entry : categories.entrySet()) {
            CategoryValueInfo catInfo = maskManager.getCategoryValueInfo(entry.getKey(), entry.getValue());
            if (catInfo != null) {
            	data.setCategoryValue(catInfo.getBitmask(), catInfo.getValueCode());
            }
        }
    }

    public Map<String, String> getCategoryValues(DocId documentId) {
    	DynamicData data = dynamicDataMap.get(documentId);
    	if (null == data) {
    		throw new IllegalArgumentException("no data for document " + documentId);
    	}
    	Map<String, String> results = Maps.newHashMap();
    	
    	Map<String, CategoryInfo> categoryInfos = maskManager.getCategoryInfos();
    	
    	for (Entry<String, CategoryInfo> entry : categoryInfos.entrySet()) {
    		CategoryInfo categoryInfo = entry.getValue();
    		int valueCode = data.getCategoryValue(categoryInfo.getBitmask());
    		if (valueCode != 0) {
    			results.put(entry.getKey(), categoryInfo.getValue(valueCode));
    		}
    	}
    	
    	return results;
    }

    public interface FacetsCollector {
    	public void addCategoryValue(String category, Integer valueCode);
    }
    
    public void populateCollector(DocId documentId, FacetsCollector collector) {
    	DynamicData data = dynamicDataMap.get(documentId);
    	if (null == data) {
    		throw new IllegalArgumentException("no data for document " + documentId);
    	}
    	Map<String, CategoryInfo> categoryInfos = maskManager.getCategoryInfos();
    	
    	for (Entry<String, CategoryInfo> entry : categoryInfos.entrySet()) {
    		CategoryInfo categoryInfo = entry.getValue();
    		int valueCode = data.getCategoryValue(categoryInfo.getBitmask());
    		if (valueCode != 0) {
    			collector.addCategoryValue(entry.getKey(), valueCode);
    		}
    	}
    	
    }
    
    public CategoryMaskManager getMaskManager() {
    	return maskManager;
    }
    
    
	@Override
	public void setBoosts(String documentId, Integer timestamp, Map<Integer, Float> boosts) {
		Preconditions.checkNotNull(documentId);

		for (Integer index : boosts.keySet()) {
			if (index >= numberOfBoosts || index < 0) {
				throw new IllegalArgumentException("Invalid boost index (" + index + " for a Scorer with a maximum of " + numberOfBoosts + " boosts)");
			}
		}
		DynamicData data = getOrCreateData(documentId);
		for (Entry<Integer, Float> entry : boosts.entrySet()) {
			data.setBoost(entry.getKey(), entry.getValue());
		}
		if (timestamp != null) {
            data.setTimestamp(timestamp);
		}
	}

    private DynamicData getOrCreateData(String docid) {
        DocId key = new DocId(docid);
        DynamicData data = dynamicDataMap.get(key);
		if (data == null) {
			data = new DynamicData(numberOfBoosts);
			
			DynamicData previousValue = dynamicDataMap.putIfAbsent(key, data);
			if (previousValue != null) {
				data = previousValue;
			}
		}
        return data;
    }

    /*
     * Check the synching block 
     */
    
    @Override
    public void dump() throws IOException {
        logger.info("Starting DynamicDataManager's dump.");
        dumpLock.writeLock().lock();
        try {
            newSyncToDisk();
        } finally {
            dumpLock.writeLock().unlock();
        }
        logger.info("DynamicDataManager's dump completed.");
    }

    /**
     * Syncs the stored data to disk.
     * This method is non-blocking, and does not ensure that the operation will be completed
     * in time, or at all.
     */
    public void nonBlockingSync() {
        (new SyncerThread()).start();
    }

    private static final int SERIALIZATION_VERSION = 1;
    private synchronized void newSyncToDisk() throws IOException {
        File f = new File(backupDir, MAIN_FILE_NAME);
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        try {
            dos.writeInt(SERIALIZATION_VERSION);
            dos.writeInt(numberOfBoosts);
            
            for (Entry<DocId, DynamicData> entry : dynamicDataMap.entrySet()) {
                entry.getKey().writeData(dos);
                entry.getValue().writeData(dos);
            }
            DocId.writeNull(dos);
            
            maskManager.writeData(dos);
        } finally {
            Execute.close(dos);
        }
    }
    
    private synchronized void readFromDisk() {
        File f = new File(backupDir, MAIN_FILE_NAME);
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));
            int version = dis.readInt();
            if (version > SERIALIZATION_VERSION) {
                throw new IllegalStateException(String.format("File version is newer than known by this class: %d > %d", version, SERIALIZATION_VERSION));
            }
            
            int fileBoosts = dis.readInt();
            if (numberOfBoosts != fileBoosts) {
                throw new IllegalStateException(String.format("Incorrect number of boosts in file. Actual: %d, Expected: %d", fileBoosts, numberOfBoosts));
            }

            while (true) {
                DocId docid = DocId.readData(dis);
                if (docid == null) {
                    break;
                }
                DynamicData data = DynamicData.readData(numberOfBoosts, dis);
                dynamicDataMap.put(docid, data);
            }
            
            maskManager.readData(dis);
        } catch (IOException e) {
            logger.fatal("Error while loading dynamic data", e);
            throw new RuntimeException(e);
        } finally {
            Execute.close(dis);
        }
        
    }
    
    //----------------------------------------------------------------------------------------
    //STATIC METHODS
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

    //----------------------------------------------------------------------------------------
    //PRIVATE CLASSES

    static class DynamicData implements Serializable, Boosts {
        private static final long serialVersionUID = 1L;
        
        private int[] data; // timestamp = data[0], variables = data[1-n], categories = data[n+1, m]
        private int dataBoundary;

        DynamicData(int numberOfBoosts) {
            data = new int[1 + numberOfBoosts];
            dataBoundary = numberOfBoosts + 1;
        }

        DynamicData(DynamicBoosts oldBoosts) {
            data = new int[1 + oldBoosts.boosts.length];
            data[0] = oldBoosts.timestamp;
            for (int i = 0; i < oldBoosts.boosts.length; i++) {
                data[1 + i] = Float.floatToRawIntBits(oldBoosts.boosts[i]);
            }
        }
    	
        public DynamicData(int numberOfBoosts, int[] data) {
            this.data = data;
            this.dataBoundary = numberOfBoosts + 1;
        }

        public int[] getData() {
        	return data;
        }

        public Map<Integer, Double> getVariablesAsMap(int numberOfVariables) {
            HashMap<Integer, Double> map = new HashMap<Integer, Double>(numberOfVariables);
            for(int id = 0; id < numberOfVariables; id++) {
                map.put(id, Double.valueOf(getBoost(id)));
            }
            return map;
        }
        
        @Override
		public float getBoost(int boostIndex) {
			return Float.intBitsToFloat(data[1 + boostIndex]);
		}

        public void setBoost(int boostIndex, float boostValue) {
            data[1 + boostIndex] = Float.floatToRawIntBits(boostValue);
        }

        public void setCategoryValue(int[] bitmask, int value) {
        	if (data.length - dataBoundary < bitmask.length) {
        		int[] newData = new int[bitmask.length + dataBoundary];
        		System.arraycopy(data, 0, newData, 0, data.length);
        		data = newData;
        	}
            data = CategoryEncoder.encode(data, dataBoundary, bitmask, value);
        }

        public int getCategoryValue(int[] bitmask) {
            return CategoryEncoder.decode(data, dataBoundary, bitmask);
        }

        @Override
		public int getTimestamp() {
			return data[0];
		}

        public void setTimestamp(int timestamp) {
            data[0] = timestamp;
        }
        
        void writeData(DataOutputStream dos) throws IOException {
            int len = data.length;
            dos.writeInt(len);
            for (int i = 0; i < len; i++) {
                dos.writeInt(data[i]);
            }
        }

        static DynamicData readData(int numberOfBoosts, DataInputStream dis) throws IOException {
            int len = dis.readInt();
            int[] data = new int[len];
            for (int i = 0; i < len; i++) {
                data[i] = dis.readInt();
            }
            return new DynamicData(numberOfBoosts, data);
        }
        
        
    }
    
    private class SyncerThread extends Thread {
        public SyncerThread() {
            setName("DynamicDataManager's syncer thread");
        }
        @Override
        public void run() {
            try {
                newSyncToDisk();
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }
    
    public static void main(String[] args) {
        int boosts = Integer.parseInt(args[0]);
        DynamicDataManager ddm = new DynamicDataManager(boosts, new File(args[1]));
        System.out.println("Count: " + ddm.getDocumentCount());
        
        Scanner in = new Scanner(System.in);

        while (in.hasNextLine()) {
            String line = in.nextLine();
            DocId docId = new DocId(line);
            DynamicData data = ddm.getDynamicData(docId);
            System.out.println("timestamp: " + data.getTimestamp());
            for (int i = 0; i < boosts; i++) {
                System.out.println("var["+i+"]: " + data.getBoost(i));
            }
            System.out.println(ddm.getCategoryValues(docId));
        }

    }

    public Map<String, String> getStats() {
        HashMap<String, String> stats = Maps.newHashMap();
        stats.putAll(maskManager.getStats());
        stats.put("dynamic_data_count", String.valueOf(this.dynamicDataMap.size()));
        stats.put("dynamic_data_variables", String.valueOf(this.numberOfBoosts));
        return stats;
    }

}
