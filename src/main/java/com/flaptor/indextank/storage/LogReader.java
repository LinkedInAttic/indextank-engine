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

import java.io.IOException;

import org.apache.thrift.TException;

import com.flaptor.indextank.rpc.LogBatch;
import com.flaptor.indextank.rpc.LogPage;
import com.flaptor.indextank.rpc.LogPageToken;
import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.rpc.PageType;
import com.flaptor.indextank.storage.Segment.MissingSegmentException;
import com.flaptor.indextank.util.FormatLogger;
import com.google.common.collect.Lists;

public class LogReader {

    private static final FormatLogger logger = new FormatLogger();

    private LogRoot root;
    private LogDealer dealer;

    private final int liveSegmentSize;

    private final int indexSegmentSize;
    
    public LogReader(LogCleaner cleaner, LogDealer dealer, LogRoot root, int liveSegmentSize, int indexSegmentSize) {
        this.dealer = dealer;
        this.root = root;
        this.liveSegmentSize = liveSegmentSize;
        this.indexSegmentSize = indexSegmentSize;
    }
    
    public LogReader(LogCleaner cleaner, LogDealer dealer) {
        this(cleaner, dealer, new LogRoot(), RawLog.DEFAULT_SEGMENT_SIZE, IndexLog.DEFAULT_SEGMENT_SIZE);
    }

    public LogPage readPage(String indexCode, LogPageToken token) throws TException, IOException {
        logger.info("Reading page for index %s with token %s", indexCode, token);
        RawLog liveLog = new RawLog(root, liveSegmentSize);
        IndexLog indexLog = new IndexLog(indexCode, root, indexSegmentSize);
        indexLog.markReadNow();
        Segment segment;
        long nextTimestamp = dealer.nextTimestamp;
        
        if (token.is_set_timestamp()) {
            long timestamp = token.get_timestamp();
            switch (token.get_type()) {
                case optimized:
                    segment = indexLog.getOptimizedSegment(timestamp);
                    break;
                case index:
                    segment = indexLog.getSortedSegment(timestamp);
                    break;
                case live:
                    segment = liveLog.getSegment(timestamp);
                    break;
                default:
                    throw new RuntimeException("INVALID TOKEN");
            }
        } else {
            if (!token.is_set_next_timestamp()) {
                // this is the initial token, try to find an optimized segment
                segment = indexLog.getLargestOptimizedSegment();
                if (segment == null) {
                    // no optimized segments, look for a sorted segments
                    segment = indexLog.getFirstSortedSegment();
                    if (segment == null) {
                        // no sorted segments
                        dealer.sortNow(indexCode);
                        segment = indexLog.getFirstSortedSegment();
                        if (segment == null) {
                            // this index had nothing dealt yet
                            segment = liveLog.getSegment(nextTimestamp);
                            if (segment == null) {
                                segment = liveLog.getFirstSegment();
                            }
                        }
                    }
                } else {
                    // starting the optimized segment, identify the next
                    // segment to avoid loosing a segment and not knowing about it
                    // there's a good reason for this, ask Santi if in doubt
                    Segment nextSegment = indexLog.getFirstSegment();
                    if (nextSegment != null) {
                        token.set_next_type(PageType.index);
                        token.set_next_timestamp(nextSegment.timestamp);
                    } else {
                        token.set_next_type(PageType.live);
                        token.set_next_timestamp(nextTimestamp);
                    }
                }
            } else {
                long timestamp = token.get_next_timestamp();
                token.set_type(token.get_next_type());
                token.unset_next_type();
                switch (token.get_type()) {
                    case index:
                        segment = indexLog.getSortedSegment(timestamp);
                        if (segment == null) {
                            dealer.sortNow(indexCode);
                            segment = indexLog.getSortedSegment(timestamp);
                            if (segment == null) {
                                return revertFromScratch();
                            }
                        }
                        break;
                    case live:
                        segment = liveLog.getSegment(timestamp);
                        if (segment == null) {
                            return revertFromTimestamp(timestamp);
                        }
                        break;
                    default:
                        throw new RuntimeException("INVALID TOKEN");
                }
            }
        }
        
        if (segment == null) {
            return endPage();
        } else {
            return buildPage(liveLog, indexLog, segment, token, nextTimestamp);
        }
    }


    private LogPage revertFromTimestamp(long timestamp) {
        throw new RuntimeException("REVERT UNIMPLEMENTED " + timestamp);
    }


    private LogPage revertFromScratch() {
        throw new RuntimeException("REVERT UNIMPLEMENTED");
    }


    private LogPage endPage() {
        return new LogPage(new LogBatch(Lists.<LogRecord>newArrayList()));
    }


    private LogPage buildPage(RawLog liveLog, IndexLog log, Segment segment, LogPageToken token, long nextTimestamp) {
        long position = token.get_file_position();
        
        // get a portion reader for the current segment and the position denoted by the token
        SegmentReader reader = segment.portionReader(position, root.getReadingPageSize());
        
        LogPage page = new LogPage(new LogBatch(Lists.<LogRecord>newArrayList()));

        // iterate the portion and load the records in the returned page 
        RecordIterator iterator = reader.iterator();
        while (iterator.hasNext()) {
            LogRecord next = iterator.next();
            if ((!next.is_set_index_code()) || next.get_index_code().equals(log.code)) {
                page.get_batch().add_to_records(next);
            }
        }
        page.set_next_page_token(token);

        long newPosition = position + iterator.getSafelyRead();
        boolean finished = newPosition == segment.length();

        if (finished) {
            switch (segment.type) {
                case OPTIMIZED:
                    // the optimized case is special because we already figured out the 
                    // next step at the beggining. still, if we were going to continue 
                    // with a live segment and there's an index segment for it already
                    // let's switch to speed up the read process.
                    if (token.get_next_type() == PageType.live) {
                        Segment indexSegment = log.getSegment(token.get_next_timestamp());
                        if (indexSegment != null) {
                            // there's an index segment to read, much better than live :)
                            token.set_next_type(PageType.index);
                        }
                    }
                    break;
                case SORTED:
                    boolean foundCurrent = false;
                    Segment nextIndexSegment = null;

                    // let's look for the first index segment after the one we just read
                    for (Segment s : log.getSegments()) {
                        if (foundCurrent) {
                            nextIndexSegment = s;
                            break;
                        }
                        if (s.timestamp == segment.timestamp) {
                            foundCurrent = true;
                        }
                    }
                    if (!foundCurrent) {
                        // the file we just read to build the page has dissapeared. we can't
                        // guarantee that the next one didn't dissappear as well. ABORT!!!
                        logger.error("Unable to find sorted segment for timestamp %s", token.get_timestamp());
                        return revertFromScratch();
                    }
                    if (nextIndexSegment != null) {
                        // we know we should continue with another index segment
                        token.set_next_type(PageType.index);
                        token.set_next_timestamp(nextIndexSegment.timestamp);
                    } else {
                        // we are out of index segments. we can safely assume that we have read
                        // all dealt data, so next segment should be the first undealt live segment
                        token.set_next_type(PageType.live);
                        token.set_next_timestamp(nextTimestamp);
                    }
                    break;
                case RAW:
                    token.set_next_type(PageType.live);
                    try {
                        Segment s;
                        s = liveLog.findFollowingSegment(segment.timestamp);
                        if (s == null) {
                            // end of read process
                            page.unset_next_page_token();
                        } else {
                            token.set_next_timestamp(s.timestamp);
                        }
                    } catch (MissingSegmentException e) {
                        throw new RuntimeException("INVALID TOKEN", e);
                    }
                    break;
            }
            token.unset_timestamp();
            token.unset_type();
            token.unset_file_position();
        } else {
            switch (segment.type) {
                case OPTIMIZED:   token.set_type(PageType.optimized);   break;
                case SORTED:      token.set_type(PageType.index);       break;
                case RAW:         token.set_type(PageType.live);        break;
            }
            token.set_timestamp(segment.timestamp);
            token.set_file_position(newPosition);
        }

        logger.info("Returning page with %d records for index %s for segment %s from %d to %d, next token %s", page.get_batch().get_records_size(), log.code, segment, position, newPosition, token);
        return page;
    }

}
