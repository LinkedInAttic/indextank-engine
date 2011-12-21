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

package com.flaptor.indextank.index.lsi;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.MMapDirectory;

import com.flaptor.indextank.index.QueryMatcher;
import com.flaptor.indextank.index.lsi.term.IndexReaderTermMatcher;
import com.flaptor.indextank.index.scorer.FacetingManager;
import com.flaptor.indextank.index.scorer.Scorer;
import com.flaptor.indextank.index.term.TermMatcher;
import com.flaptor.indextank.index.term.query.TermBasedQueryMatcher;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;


public class LsiIndex {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    public static final int SEARCHER_POOL_SIZE = 2;
    public static final String PAYLOAD_TERM_FIELD = "docidpayload";
    public static final String PAYLOAD_TERM_TEXT = "docidpayload";
    public static final Term PAYLOAD_TERM = new Term(PAYLOAD_TERM_FIELD, PAYLOAD_TERM_TEXT);
    
    private final File dirLocation;
    private /*final*/ Directory directory;
    private final Scorer scorer;
    private final AtomicReference<Pair<BlockingDeque<IndexSearcher>, BlockingDeque<QueryMatcher>>> searchObjects;
    private volatile IndexWriter indexWriter;

    private final FacetingManager facetingManager;
	private final IndexEngineParser parser;
	
	private final Map<String, String> stats = new ConcurrentHashMap<String, String>();


    public LsiIndex(IndexEngineParser parser, String directoryPath, Scorer scorer, FacetingManager facetingManager) throws IOException {
        this.parser = parser;
		this.scorer = scorer;
		this.facetingManager = facetingManager;
		Preconditions.checkNotNull(directoryPath);
        dirLocation = new File(directoryPath);
        if (!dirLocation.exists() || !dirLocation.isDirectory()) {
            throw new IllegalArgumentException("Wrong directory path.");
        }
        directory = new MMapDirectory(dirLocation);
        reopenWriter();
        searchObjects = new AtomicReference<Pair<BlockingDeque<IndexSearcher>, BlockingDeque<QueryMatcher>>>();
        reopenSearcher();
    }

    private void reopenSearcher() { 
        BlockingDeque<IndexSearcher> searcherPool= new LinkedBlockingDeque<IndexSearcher>();
        BlockingDeque<QueryMatcher> matcherPool = new LinkedBlockingDeque<QueryMatcher>();
        for (int i=0; i < SEARCHER_POOL_SIZE; i++) {
            try { 
                IndexSearcher searcher = new IndexSearcher(directory, true); //read-only for better concurrent performance.
                TermMatcher termMatcher = new IndexReaderTermMatcher(searcher.getIndexReader(), PAYLOAD_TERM);
                QueryMatcher matcher = new TermBasedQueryMatcher(scorer, termMatcher, this.facetingManager);		
                searcherPool.addFirst(searcher); //no blocking, throws exception.
                matcherPool.addFirst(matcher);
            } catch (CorruptIndexException cie) {
                logger.fatal("HORROR!!! corrupted index. unable to reopen", cie);
            } catch (IOException ioe) { 
                logger.fatal("HORROR!!! IO exception. unable to reopen", ioe);
            } 
        }
        searchObjects.set(new Pair<BlockingDeque<IndexSearcher>, BlockingDeque<QueryMatcher>>(searcherPool, matcherPool));
    }

    private void reopenWriter() throws CorruptIndexException, LockObtainFailedException, IOException {
        indexWriter = new IndexWriter(this.directory, getAnalyzer(), IndexWriter.MaxFieldLength.UNLIMITED);
    }

	private Analyzer getAnalyzer() {
		Analyzer analyzer = parser.getAnalyzer();
		Analyzer payloadAnalyzer = new PayloadAnalyzer();
		return new PerFieldAnalyzerWrapper(analyzer, ImmutableMap.of(LsiIndex.PAYLOAD_TERM_FIELD, payloadAnalyzer));
	} 

    public BlockingDeque<IndexSearcher> getLuceneIndexSearcherPool() {
        return searchObjects.get().first();
    }
    
    public BlockingDeque<QueryMatcher> getQueryMatcherPool() {
        return searchObjects.get().last();
    }

	public IndexWriter getLuceneIndexWriter() {
        return indexWriter;
    }

    public void flush(){
        try {
            long t = System.currentTimeMillis();
            int before = indexWriter.maxDoc();
            indexWriter.commit();
            int after = indexWriter.maxDoc();
            int total = indexWriter.numDocs();
            
            double commitTime = (System.currentTimeMillis() - t) / 1000.0;
            stats.put("lucene_doc_count", String.valueOf(total));
            stats.put("commit_time", String.valueOf(commitTime));
            stats.put("lucene_max_doc", String.valueOf(after));
            stats.put("lucene_previous_max_doc", String.valueOf(before));
            
            logger.info(String.format("Commited index to disk in %.3fs. Document count is %d. MaxDoc from %d to %d", commitTime, total, before, after));
        } catch (IOException e) {
            logger.fatal("unexpected exception while commiting the index: ", e);
            System.exit(1);
        }
        reopenSearcher();
    }
    
    public Map<String, String> getStats() {
        return stats;
    }

}
