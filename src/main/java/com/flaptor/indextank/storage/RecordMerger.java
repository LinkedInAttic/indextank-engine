package com.flaptor.indextank.storage;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.util.CollectionsUtil.PeekingIterator;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class RecordMerger {
    
    public static final Comparator<LogRecord> MERGE_ORDER = new Comparator<LogRecord>() {
        @Override
        public int compare(LogRecord o1, LogRecord o2) {
            return o1.get_docid().compareTo(o2.get_docid());
        }
    };
    
    public static List<LogRecord> compactAndSort(Iterator<LogRecord> source) {
        Map<String, LogRecord> target = Maps.newHashMap();
        while (source.hasNext()) {
            LogRecord record = source.next();
            LogRecord compacted = target.get(record.get_docid());
            if (compacted == null) {
                target.put(record.get_docid(), new LogRecord(record));
            } else {
                mergeRecordInto(record, compacted);
            }
        }
        List<LogRecord> records = Lists.newArrayList(target.values());
        Collections.sort(records, RecordMerger.MERGE_ORDER);
        return records;
    }

    public static void mergeRecordInto(LogRecord source, LogRecord target) {
        target.set_id(source.get_id());
        if (source.is_deleted()) {
            target.set_deleted(source.is_deleted());
            target.set_fields(source.get_fields());
            target.set_variables(source.get_variables());
            target.set_categories(source.get_categories());
        } else {
            if (source.is_set_fields()) {
                // fields are overwritten (or first set)
                target.set_fields(source.get_fields()); 
            }
            if (source.is_set_variables()) {
                if (!target.is_set_variables()) {
                    // first time variables are set
                    target.set_variables(source.get_variables());
                } else {
                    // variables are updated
                    target.get_variables().putAll(source.get_variables()); 
                }
            }
            if (source.is_set_categories()) {
                if (!target.is_set_categories()) {
                    // first time categories are set
                    target.set_categories(source.get_categories());
                } else {
                    // categories are updated
                    target.get_categories().putAll(source.get_categories()); 
                }
            }
        }
    }

    public static Iterator<LogRecord> merge(final Iterable<PeekingIterator<LogRecord>> cursors) {
        return new AbstractIterator<LogRecord>() {
            @Override
            protected LogRecord computeNext() {
                while (true) {
                    String docid = null;
                    for (PeekingIterator<LogRecord> it : cursors) {
                        if (it.hasNext()) {
                            LogRecord peek = it.peek();
                            if (docid == null || peek.get_docid().compareTo(docid) < 0) {
                                docid = peek.get_docid();
                            }
                        }
                    }
                    if (docid != null) {
                        LogRecord merged = null;
                        for (PeekingIterator<LogRecord> it : cursors) {
                            if (it.hasNext()) {
                                LogRecord peek = it.peek();
                                if (peek.get_docid().equals(docid)) {
                                    it.next();
                                    if (merged == null) {
                                        merged = peek;
                                    } else if (peek.is_deleted()) {
                                        if (peek.is_set_fields() || peek.is_set_variables() || peek.is_set_categories()) {
                                            merged = peek;
                                            merged.set_deleted(false);
                                        } else {
                                            merged = null;
                                        }
                                    } else {
                                        mergeRecordInto(peek, merged);
                                    }
                                }
                            }
                        }
                        if (merged != null) {
                            return merged;
                        }
                    } else {
                        return endOfData();
                    }
                }
            }
        };
    }
    
}
