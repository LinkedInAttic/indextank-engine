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
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.thrift.TException;

import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.rpc.SegmentInfo;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.io.PatternFilenameFilter;

public class Segment {

    private final LogRoot root;
    private final List<File> alternativeParents = Lists.newArrayList();
    
    long timestamp;
    Type type;
    Integer recordCount;
    File parent;

    public Segment(LogRoot root, File file) {
        this.root = root;
        this.parent = file.getParentFile();
        Matcher m = SEGMENT_FILE.matcher(file.getName());
        if (!m.find()) {
            throw new RuntimeException("Invalid segment filename " + file);
        }
        String suffix = m.group(2);
        for (Type type : Type.values()) {
            if (suffix.startsWith(type.key)) {
                this.type = type;
                break;
            }
        }
        if (this.type == null) {
            throw new RuntimeException("Invalid segment filename " + file);
        }
        if (isRaw()) {
            this.timestamp = Long.parseLong(m.group(1));
        } else {
            try {
                this.timestamp = dateFormat().parse(m.group(1)).getTime();
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
            this.recordCount = Integer.valueOf(suffix.substring(this.type.key.length() + 1));
        }
    }
    
    private Segment(LogRoot root, File parent, Type type, long timestamp, Integer count) {
        this.root = root;
        this.parent = parent;
        this.type = type;
        this.timestamp = timestamp;
        this.recordCount = count;
    }
    
    public long length() {
        return buildFile(false).length();
    }
    
    public boolean isRaw() {
        return type == Type.RAW;
    }

    public boolean isSorted() {
        return type == Type.SORTED || type == Type.OPTIMIZED;
    }

    public boolean isLocked() throws IOException {
        RandomAccessFile raf = new RandomAccessFile(buildFile(false ), "rw");
        FileLock lock = raf.getChannel().tryLock();
        if (lock == null) return true;
        raf.close();
        return false;
    }

    public SegmentInfo getInfo() {
        SegmentInfo info = new SegmentInfo();
        info.set_timestamp(timestamp);
        if (recordCount != null) info.set_record_count(recordCount);
        info.set_sorted(isSorted());
        return info;
    }

    public void archive(File target) {
        File oldFile = buildFile(false);
        this.parent = target;
        oldFile.renameTo(buildFile(false));
    }

    public void delete() {
        this.buildFile(false).delete();
    }

    public SegmentReader reader(int buffer) {
        return new SegmentReader(this, buffer);
    }

    public SegmentReader reader() {
        return new SegmentReader(this);
    }

    public SegmentReader portionReader(long filePosition, int readingPageSize) {
        return new SegmentReader(this, filePosition, filePosition + readingPageSize);
    }

    public Segment addAlternativeParent(File parent) {
        alternativeParents.add(parent);
        return this;
    }

    public File[] buildFileAlternatives() {
        File[] files = new File[alternativeParents.size() + 1];
        files[0] = buildFile(false);
        for (int i = 1; i < files.length; i++) {
            files[i] = buildForParent(alternativeParents.get(i-1), false);
        }
        return files;
    }

    @Override
    public String toString() {
        return buildFile(false).getAbsolutePath().substring(root.getPath().getAbsolutePath().length() + 1);
    }

    private void writeAll(Iterator<LogRecord> records) throws TException, IOException {
        Preconditions.checkState(recordCount == 0, "Cannot write records to a non-empty segment");
        File file = buildFile(true);
        // we mark the temp file for deletion in case the jvm
        // exits before we move it or delete it.
        file.deleteOnExit();
        try {
            SegmentWriter writer = new SegmentWriter(file, false);
            while (records.hasNext()) {
                LogRecord record = records.next();
                writer.write(record);
                recordCount++;
            }
            writer.release();
            // update file to non-temp and with actual record count
            file.renameTo(buildFile(false));
        } finally {
            // if something went wrong and the file is still there,
            // we delete it immediately, otherwise temp files could
            // accumulate and fill up the disk
            if (file.exists()) file.delete();
        }
    }

    private SimpleDateFormat dateFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss.SSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    private File buildFile(boolean temp) {
        return buildForParent(parent, temp);
    }
    
    private File buildForParent(File parent, boolean temp) {
        String name, kind;
        String format = "%s.%s";
        if (isRaw()) {
            name = String.valueOf(timestamp);
            kind = type.key;
        } else {
            name = dateFormat().format(new Date(timestamp));
            kind = String.format("%s_%d", type.key, recordCount);
        }
        if (temp) {
            name = String.format("temp___%s", name);
        }
        return new File(parent, String.format(format, name, kind));
    }

    public static SegmentWriter createRawHead(LogRoot root, File parent, long timestamp) {
        Segment segment = new Segment(root, parent, Type.RAW, timestamp, null);
        return new SegmentWriter(segment.buildFile(false));
    }

    public static Segment createUnsortedSegment(LogRoot root, File parent, long timestamp, Iterator<LogRecord> records) throws TException, IOException {
        return createSegment(root, parent, Type.UNSORTED, timestamp, records);
    }

    public static Segment createSortedSegment(LogRoot root, File parent, long timestamp, List<Segment> unsortedParts) throws TException, IOException {
        Iterator<LogRecord> joinedParts = Iterators.concat(Iterables.transform(unsortedParts, TO_RECORD_ITERATOR).iterator());
        return createSegment(root, parent, Type.SORTED, timestamp, RecordMerger.compactAndSort(joinedParts).iterator());
    }

    public static Segment createOptimizedSegment(LogRoot root, File parent, long timestamp, Iterator<LogRecord> records) throws TException, IOException {
        return createSegment(root, parent, Type.OPTIMIZED, timestamp, records);
    }

    public static Segment getSegment(LogRoot root, File parent, long timestamp, Boolean sorted) {
        for (Segment s : getSegments(root, parent, sorted)) {
            if (s.timestamp == timestamp) {
                return s;
            }
        }
        return null;
    }

    public static List<Segment> getSegments(final LogRoot root, final File parent) {
        return getSegments(root, parent, null);
    }
    public static List<Segment> getSegments(final LogRoot root, final File parent, Boolean sortedOnly) {
        return Lists.newArrayList(iterateSegments(root, parent, sortedOnly));
    }
    
    public static List<Segment> iterateSegments(final LogRoot root, File parent) {
        return Lists.transform(listSegmentFiles(parent), new Function<File, Segment>() {
            public Segment apply(File f) { 
                return new Segment(root, f); 
            }
        });
    }
    public static Iterable<Segment> iterateSegments(final LogRoot root, File parent, Boolean sortedOnly) {
        Iterable<Segment> segments = iterateSegments(root, parent);
        if (sortedOnly != null) {
            segments = Iterables.filter(segments, sortedOnly ? IS_SORTED : Predicates.not(IS_SORTED));
        }
        return segments;
    }

    public static final Predicate<Segment> IS_SORTED = new Predicate<Segment>() {
        @Override
        public boolean apply(Segment s) {
            return s.isSorted();
        }

    };

    public static final Function<Segment, RecordIterator> TO_RECORD_ITERATOR = new Function<Segment, RecordIterator>() {
        public RecordIterator apply(Segment s) { 
            return s.reader().iterator(); 
        }
    };

    private static Pattern SEGMENT_FILE = Pattern.compile("^([\\d-.]+)\\.(raw|unsorted_\\d+|sorted_\\d+|optimized_\\d+)?$");

    private static Segment createSegment(LogRoot root, File parent, Type type, long timestamp, Iterator<LogRecord> records) throws TException, IOException {
        Preconditions.checkArgument(type != Type.RAW);
        Segment segment = new Segment(root, parent, type, timestamp, 0);
        segment.writeAll(records);
        return segment;
    }

    private static List<File> listSegmentFiles(File parent) {
        File[] files = parent.listFiles(new PatternFilenameFilter(SEGMENT_FILE));
        Arrays.sort(files);
        return Arrays.asList(files);
    }

    public static enum Type {
        RAW("raw"), UNSORTED("unsorted"), SORTED("sorted"), OPTIMIZED("optimized");
        
        private final String key;
    
        Type(String key) {
            this.key = key;
        }
    }

    public static Segment findNextSegment(LogRoot manager, long timestamp, File archive, File main) throws MissingSegmentException {
        boolean found = false;
        Segment firstLive = null;
        for (Segment s : iterateSegments(manager, main)) {
            if (firstLive == null) firstLive = s;
            if (found) {
                return s;
            }
            if (s.timestamp == timestamp) {
                found = true;
            }           
        }
        if (!found) {
            for (Segment s : iterateSegments(manager, archive)) {
                if (found) {
                    return s;
                }
                if (s.timestamp == timestamp) {
                    found = true;
                }           
            }
            if (found) {
                return firstLive;
            } else {
                throw new MissingSegmentException(); 
            }
        }
        return null;
    }

    public static class MissingSegmentException extends Exception {
        private static final long serialVersionUID = 1L;
    }


}
