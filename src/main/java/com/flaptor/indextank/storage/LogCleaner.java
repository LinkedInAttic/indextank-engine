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

package com.flaptor.indextank.storage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.flaptor.indextank.util.FormatLogger;
import com.flaptor.indextank.util.IndexTankUtil;
import com.flaptor.util.Execute;
import com.google.common.io.Files;

public class LogCleaner implements Runnable {

    private static final FormatLogger logger = new FormatLogger();

    private static final long SAFE_PERIOD_AFTER_READ = 30 * 60 * 1000;
    private LogRoot root;
    private LinkedBlockingQueue<String> deletedIndexes = new LinkedBlockingQueue<String>();
    private ReentrantReadWriteLock indexesLock = new ReentrantReadWriteLock();

    public LogCleaner(LogRoot root) {
        this.root = root;
    }

    public void deleteIndex(String code) {
        deletedIndexes.offer(code);
    }
    
    @Override
    public void run() {
        while (true) {
            File path = root.getIndexesLogPath();
            while (true) {
                if (IndexTankUtil.isMaster() && root.getSafeToReadFile().exists()) {
                    while (!deletedIndexes.isEmpty()) {
                        indexesLock.writeLock().lock();
                        try {
                            String code = deletedIndexes.poll();
                            File indexPath = root.getIndexLogPath(code);
                            if (indexPath.exists()) {
                                try {
                                    Files.deleteRecursively(indexPath);
                                } catch (IOException e) {
                                    logger.error(e, "Failed to delete index %s", code);
                                }
                            }
                        } finally {
                            indexesLock.writeLock().unlock();
                        }
                    }
                    
                    try {
                        for (File file : path.listFiles()) {
                            if (!IndexLog.isLog(file)) continue;
                            String code = file.getName();
                            IndexLog log = new IndexLog(code, root, IndexLog.DEFAULT_SEGMENT_SIZE);
                            if (log.getLastRead() < System.currentTimeMillis() - SAFE_PERIOD_AFTER_READ) {
                                List<Segment> segments = log.getOptimizedSegments();
                                if (segments.size() > 1) {
                                    for (Segment s : segments.subList(0, segments.size()-1)) {
                                        logger.info("Purging %s", s);
                                        s.delete();
                                    }
                                }
                                Segment optimized = log.getLargestOptimizedSegment();
                                if (optimized != null) {
                                    for (Segment s : log.getSortedSegments()) {
                                        if (s.timestamp <= optimized.timestamp) {
                                            logger.info("Purging %s", s);
                                            s.delete();
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable t) {
                        logger.error(t, "Failed while trying to clean up logs");
                    }
                }
                Execute.sleep(25000);
            }
        }
    }
    
    public void start() {
        new Thread(this).start();
    }
    
    public void lockIndexesForRead() {
        indexesLock.readLock().lock();
    }
    public void unlockIndexesForRead() {
        indexesLock.readLock().unlock();
    }
    
}
