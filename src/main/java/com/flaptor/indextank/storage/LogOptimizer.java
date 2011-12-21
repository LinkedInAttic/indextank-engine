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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;

import org.apache.thrift.TException;

import com.flaptor.indextank.rpc.IndexLogInfo;
import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.rpc.QueueScore;
import com.flaptor.indextank.util.FormatLogger;
import com.flaptor.indextank.util.IndexTankUtil;
import com.flaptor.util.Execute;
import com.flaptor.util.CollectionsUtil.PeekingIterator;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;


public class LogOptimizer implements Runnable {
    private static final FormatLogger logger = new FormatLogger();
    
    private LogRoot root;
    private final LogDealer dealer;

    private PriorityQueue<String> queue;
    private Map<String, Score> scores = Maps.newHashMap();

    private final LogCleaner cleaner;
    
    public LogOptimizer(LogCleaner cleaner, LogDealer dealer, LogRoot root) {
        this.cleaner = cleaner;
        this.dealer = dealer;
        this.root = root;
        this.queue = new PriorityQueue<String>(1000, new Comparator<String>() {
            @Override
            public int compare(String code1, String code2) {
                Score s1 = scores.get(code1), s2 = scores.get(code2);
                int c = s1.priority - s2.priority;
                if (c == 0) c = Float.compare(s2.score, s1.score);
                return c;
            }
        });
    }
    
    private static class Score {
        public Score(int priority, float score) {
            this.priority = priority;
            this.score = score;
        }
        private int priority;
        private float score;
    }

    private class DiscoveryThread extends Thread {
        private static final int DISCOVERY_PRIORITY = 10;

        @Override
        public void run() {
            File path = root.getIndexesLogPath();
            while (true) {
                if (IndexTankUtil.isMaster() && root.getSafeToReadFile().exists()) {
                    try {
                        for (File file : path.listFiles()) {
                            if (!IndexLog.isLog(file)) continue;
                            String code = file.getName();
                            IndexLogInfo info = getIndexInfo(code);
                            int segments = info.get_unoptimized_segments();
                            long opt = info.get_optimized_record_count();
                            long unopt = info.get_unoptimized_record_count();
                            if (segments > 4) {
                                float ratio;
                                ratio = opt == 0 ? 2 : unopt / opt;
                                // flat ratios at two to avoid aberrations at the beginning of an index history
                                ratio = Math.max(ratio, 2);
                                if (ratio > .1) {
                                    // DISCOVERED! indexes are not optimized for less than 5 segments
                                    // or less than 10% size in those segments.
                                    float score = ratio * segments;
                                    enqueue(code, DISCOVERY_PRIORITY, score);
                                }
                            }
                        }
                    } catch (Throwable t) {
                        logger.error(t, "Failed while trying to discover indexes to optimize");
                    }
                }
                Execute.sleep(30000);
            }
        }

    }
    public synchronized void enqueue(String code, int priority, float score) {
        Score current = scores.get(code);
        if (current != null) {
            if (current.priority < priority) return;
            queue.remove(code);
        }
        scores.put(code, new Score(priority, score));
        queue.add(code);
    }
    
    public void run() {
        while (true) {
            try {
                if (IndexTankUtil.isMaster() && root.getSafeToReadFile().exists()) {
                    String code;
                    synchronized (this) {
                        code = queue.poll();
                    }
                    if (code != null) {
                        optimize(code);
                        synchronized (this) {
                            if (scores.remove(code) != null) {
                                queue.remove(code);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                logger.error(t, "Failed to run optimizer's cycle");
            }
            Execute.sleep(5000);
        }
    }
    
    public void start() {
        new Thread(this).start();
        new DiscoveryThread().start();
    }
    
    public IndexLogInfo getIndexInfo(String code) {
        IndexLogInfo info = new IndexLogInfo();
        IndexLog log = new IndexLog(code, root, IndexLog.DEFAULT_SEGMENT_SIZE);
        List<Segment> optimizedSegments = log.getOptimizedSegments();
        int optimizedCount = 0;
        long lastTimestamp = 0;
        for (Segment segment : optimizedSegments) {
            info.add_to_optimized_segments(segment.getInfo());
            optimizedCount = segment.recordCount;
            lastTimestamp = segment.timestamp;
        }
        info.set_optimized_record_count(optimizedCount);
        info.set_last_optimization_timestamp(lastTimestamp);
        
        long unoptimizedCount = 0;
        int unoptimizedSegments = 0;
        boolean unsortedSegments = false;
        for (Segment segment : log.getSegments()) {
            if (segment.timestamp > lastTimestamp) {
                unoptimizedCount += segment.recordCount;
                if (segment.isSorted()) {
                    unoptimizedSegments++;
                    info.add_to_sorted_segments(segment.getInfo());
                } else {
                    unsortedSegments = true;
                    info.add_to_unsorted_segments(segment.getInfo());
                }
            }
        }
        if (unsortedSegments) unoptimizedSegments++;
        info.set_unoptimized_record_count(unoptimizedCount);
        info.set_unoptimized_segments(unoptimizedSegments);
        
        return info;
    }
    
    public void optimize(String code) throws TException, IOException {
        cleaner.lockIndexesForRead();
        dealer.sortNow(code);
        IndexLog log = new IndexLog(code, root, IndexLog.DEFAULT_SEGMENT_SIZE);
        List<Segment> segments = Lists.newArrayList();
        Segment optimizedSegment = log.getLargestOptimizedSegment();
        long lastTimestamp = 0;
        int optimizedCount = 0;
        if (optimizedSegment != null) {
            segments.add(optimizedSegment);
            optimizedCount = optimizedSegment.recordCount;
            lastTimestamp = optimizedSegment.timestamp;
        }
        int totalCount = optimizedCount;
        for (Segment segment : log.getSortedSegments()) {
            if (segment.timestamp > lastTimestamp) {
                segments.add(segment);
                totalCount += segment.recordCount;
            }
            if (segments.size() == 40) {
                break;
            }
        }
        
        logger.info("Optimizing %d segments for index %s. Last optimized segment had %d records. Total records to merge: %d", segments.size(), log.code, optimizedCount, totalCount);

        long t = System.currentTimeMillis();
        
        Iterator<LogRecord> merged = mergeSegments(segments);
        
        long timestamp = segments.get(segments.size()-1).timestamp;
        Segment optimized = Segment.createOptimizedSegment(root, log.getOptimizedPath(), timestamp, merged);
        
        double ratio = 100.0 * optimized.recordCount / totalCount;
        logger.info("Finished optimization. New segment: %s - ratio: %.2f%% - time: %.2fs", optimized, ratio, (System.currentTimeMillis() - t) / 1000.0);
    }


    private static Iterator<LogRecord> mergeSegments(List<Segment> sortedSegments) {
        Function<Segment, PeekingIterator<LogRecord>> toPeakingIterators = new Function<Segment, PeekingIterator<LogRecord>>() {
            @Override
            public PeekingIterator<LogRecord> apply(Segment s) {
                return new PeekingIterator<LogRecord>(s.reader(1024*1024).iterator());
            }
        };
        List<PeekingIterator<LogRecord>> cursors = Lists.newArrayList(Iterables.transform(sortedSegments, toPeakingIterators));
        Iterator<LogRecord> merged = RecordMerger.merge(cursors);
        return merged;
    }

    public Map<String, QueueScore> getOptimizationQueue() {
        Map<String, QueueScore> map = Maps.newHashMap();
        for (Entry<String, Score> e : scores.entrySet()) {
            Score score = e.getValue();
            map.put(e.getKey(), new QueueScore(score.priority, score.score));
        }
        return map;
    }

}
