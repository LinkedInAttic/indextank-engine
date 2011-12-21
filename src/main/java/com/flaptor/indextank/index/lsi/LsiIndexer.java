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

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;

import com.flaptor.indextank.Indexer;
import com.flaptor.indextank.index.Document;
import com.flaptor.util.Execute;
import com.google.common.base.Preconditions;

/**
 * @author Flaptor Development Team
 */
public final class LsiIndexer implements Indexer {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());

    //Lucene related variables.
    private LsiIndex workIndex;
    private IndexWriter writer;

    /**
     * Default constructor. 
     */
    public LsiIndexer(LsiIndex index) {
        Preconditions.checkNotNull(index);
        workIndex = index;
        openWriter();
    }

    /**
     *@inheritDoc
     */
    public synchronized void add(final String docId, final Document itdoc) {
        if (null == docId) {
            logger.error("No documentId specified. Ignoring addition.");
            return;
        }

        org.apache.lucene.document.Document doc = asLuceneDocument(itdoc);
        org.apache.lucene.document.Field docidPayloadField = new org.apache.lucene.document.Field(LsiIndex.PAYLOAD_TERM_FIELD, docId, Field.Store.NO, Field.Index.ANALYZED);
        doc.add(docidPayloadField);

        doc.add(new Field("documentId",docId,Field.Store.NO,Field.Index.NOT_ANALYZED));
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Adding document with docId=" + docId + ". Doc is " + itdoc.getFieldNames());
            }
            writer.updateDocument(docIdTerm(docId), doc);
        } catch (IOException e) {
            logger.error(e);
        }
    }



    /**
     *@inheritDoc
     */
    public synchronized void del(final String docId) {
        try {
            writer.deleteDocuments(docIdTerm(docId));
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private static Term docIdTerm(final String docId) {
        return new Term("documentId", docId);
    }


    /**
     * Opens the writer.
     * @throws RuntTimeException if there was a problem opening the writer.
     */
    private void openWriter() {
        writer = workIndex.getLuceneIndexWriter();
    }

    /**
     * Closes the writer to be sure that all changes are in the directory and
     * then calls the shell command that makes a copy of it. Used to make a copy
     * of the directory while it's in a consistent state. If there is an index
     * optimization scheduled, it'll be performed here.
     * @throws IllegasStateException if the index copy couldn't be made.
     * @throws RuntimeException if there was a problem opening the index.
     */
    public synchronized void makeDirectoryCheckpoint() {
        workIndex.flush();
    }

    // Helper method to transform an IndexTank Document to a Lucene Document
    private static org.apache.lucene.document.Document asLuceneDocument(Document itd){
        org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
        for (String field: itd.getFieldNames()) {
            doc.add(new Field(field, itd.getField(field), Field.Store.NO, Field.Index.ANALYZED));
        }

        return doc;
    }


}
