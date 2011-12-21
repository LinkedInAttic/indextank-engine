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

package com.flaptor.indextank.rpc;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TBinaryProtocol.Factory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;

import com.flaptor.indextank.index.scorer.DynamicDataManager;
import com.flaptor.indextank.index.scorer.FunctionRangeFilter;
import com.flaptor.indextank.index.scorer.IntersectionMatchFilter;
import com.flaptor.indextank.index.scorer.MatchFilter;
import com.flaptor.indextank.index.scorer.Scorer;
import com.flaptor.indextank.index.scorer.VariablesRangeFilter;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.MatchAllQuery;
import com.flaptor.indextank.query.NoSuchQueryVariableException;
import com.flaptor.indextank.query.ParseException;
import com.flaptor.indextank.query.Query;
import com.flaptor.indextank.query.QueryVariables;
import com.flaptor.indextank.query.QueryVariablesImpl;
import com.flaptor.indextank.search.DocumentSearcher;
import com.flaptor.indextank.search.SearchResult;
import com.flaptor.indextank.search.SearchResults;
import com.flaptor.util.Execute;
import com.flaptor.util.Pair;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

public class SearcherServer { 
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private DocumentSearcher searcher;
    private int port;
	private final IndexEngineParser parser;
	private final DynamicDataManager dynamicDataManager;
	private final Scorer scorer;


    public SearcherServer(DocumentSearcher searcher, IndexEngineParser parser, DynamicDataManager dynamicDataManager, Scorer scorer, int port){
        this.scorer = scorer;
		this.dynamicDataManager = dynamicDataManager;
		this.searcher = searcher;
		this.parser = parser;  
        this.port = port;
    }

    public void start(){
        Thread t = new Thread() { 
        
            public void run() { 
                try {
                    TServerSocket serverTransport = new TServerSocket(SearcherServer.this.port);
                    Searcher.Processor processor = new Searcher.Processor(new SearcherImpl(SearcherServer.this.searcher));
                    Factory protFactory = new TBinaryProtocol.Factory(true, true);
                    TServer server = new TThreadPoolServer(processor, serverTransport, protFactory);
                    System.out.println("Starting searcher server on port " + SearcherServer.this.port + " ...");
                    server.serve();
                } catch( TTransportException tte ){
                    tte.printStackTrace();
                }
            } 
        };
        t.start();
    }

    /**
     * Converts a ISearchResults to Thrift ResultSet.
     */ 
    private static ResultSet toResultSet(SearchResults results) {
        ResultSet rs = new ResultSet();
        rs.set_status("OK");
        rs.set_matches(results.getMatches());
        rs.set_facets(toFacetsMap(results.getFacets()));
        rs.set_didyoumean(results.getDidYouMean());

        rs.set_docs(Lists.<Map<String,String>>newArrayList());
        rs.set_scores(Lists.<Double>newArrayList());
        rs.set_variables(Lists.<Map<Integer,Double>>newArrayList());
        rs.set_categories(Lists.<Map<String,String>>newArrayList());

        for (SearchResult sr : results.getResults()) {
            Map<String,String> doc = Maps.newHashMap();
            doc.putAll(sr.getFields());
            doc.put("docid", sr.getDocId());
            rs.add_to_docs(doc);
            rs.add_to_scores(sr.getScore());
            rs.add_to_variables(sr.getVariables());
            rs.add_to_categories(sr.getCategories()); 
        }
        
        return rs;
    }


    private static Map<String, Map<String, Integer>> toFacetsMap(Map<String, Multiset<String>> facets) {
    	Map<String, Map<String, Integer>> results = Maps.newHashMap();
    	
    	for (Entry<String, Multiset<String>> entry : facets.entrySet()) {
			Map<String, Integer> value = Maps.newHashMap();
			
			for (String catValue : entry.getValue()) {
				value.put(catValue, entry.getValue().count(catValue));
			}
			
			results.put(entry.getKey(), value);
		}
    	
    	return results;
	}

	private Query generateQuery(String str, int start, int len, QueryVariables vars, Multimap<String, String> facetsFilter, MatchFilter rangeFilters) throws ParseException {
        return new Query(parser.parseQuery(str), str, vars, facetsFilter, rangeFilters);
    }


    private class SearcherImpl implements Searcher.Iface {
        private DocumentSearcher searcher;

        // constructor
        private SearcherImpl(DocumentSearcher searcher){
            this.searcher = searcher ;
        }

        public ResultSet search(String queryStr, int start, int len, int scoringFunctionIndex, Map<Integer, Double> queryVariables, List<CategoryFilter> facetsFilter, List<RangeFilter> variableRangeFilters, List<RangeFilter> functionRangeFilters, Map<String,String> extraParameters) throws IndextankException, InvalidQueryException, MissingQueryVariableException {
            logger.debug("Searching: start: " + start + ", len: " + len +", query: \"" + queryStr + "\"");
            try { 
                Query query = generateQuery(queryStr,start,len, QueryVariablesImpl.fromMap(queryVariables), convertToMultimap(facetsFilter), new IntersectionMatchFilter(convertToVariableRangeFilter(variableRangeFilters), convertToFunctionRangeFilter(functionRangeFilters)));
                ResultSet resultSet = toResultSet(this.searcher.search(query, start, len, scoringFunctionIndex, extraParameters));
                logger.info("Search found " + resultSet.get_matches() + " results - start: " + start + ", len: " + len +", query: \"" + queryStr + "\"");
                return resultSet;
            } catch (NoSuchQueryVariableException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Some query variables (" + queryVariables + ") are missing for evaluating functions",e); 
                }
                MissingQueryVariableException ite = new MissingQueryVariableException();
                ite.set_message("Missing query variable with index '" + e.getMissingVariableIndex() + "'");
                throw ite;
            } catch (ParseException pe) {
                if (logger.isDebugEnabled()){
                    logger.debug("Parsing '" + queryStr + "' failed with " + pe,pe); 
                }
                InvalidQueryException ite = new InvalidQueryException();
                ite.set_message("Invalid query");
                throw ite;
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing search. Will throw an IndexTankException. Original Exception is:", e);
                throw new IndextankException();
            } catch (InterruptedException e) {
                logger.error("Interrupted while searching.");
                IndextankException ite = new IndextankException();
                ite.set_message("Interrupted while searching.");
                throw ite;
            }
        }
        
        private VariablesRangeFilter convertToVariableRangeFilter(List<RangeFilter> rangeFilters) {
        	Multimap<Integer, Pair<Float, Float>> filters = HashMultimap.create();
        	for (RangeFilter filter : rangeFilters) {
				filters.put(filter.get_key(), new Pair<Float, Float>(filter.is_no_floor() ? null : (float)filter.get_floor(), filter.is_no_ceil() ? null : (float)filter.get_ceil()));
			}
        	
			return new VariablesRangeFilter(dynamicDataManager, filters);
        }

        private FunctionRangeFilter convertToFunctionRangeFilter(List<RangeFilter> rangeFilters) {
        	Multimap<Integer, Pair<Float, Float>> filters = HashMultimap.create();
        	for (RangeFilter filter : rangeFilters) {
				filters.put(filter.get_key(), new Pair<Float, Float>(filter.is_no_floor() ? null : (float)filter.get_floor(), filter.is_no_ceil() ? null : (float)filter.get_ceil()));
        	}
        	
        	return new FunctionRangeFilter(scorer, dynamicDataManager, filters);
        }

		private Multimap<String, String> convertToMultimap(List<CategoryFilter> facetsFilter) {
        	Multimap<String, String> result = HashMultimap.create();
        	for (CategoryFilter facetFilter : facetsFilter) {
				result.put(facetFilter.get_category(), facetFilter.get_value());
			}
        	
        	return result;
		}

		@Override
        public int count(String queryStr) throws IndextankException {
            logger.debug("Counting: query: \"" + queryStr + "\"");
            try { 
                Query query = generateQuery(queryStr,0,1,null, ImmutableMultimap.<String, String>of(), VariablesRangeFilter.NO_FILTER);
                int count = this.searcher.countMatches(query);
                logger.info("Counted " + count + " for query: \"" + queryStr + "\"");
                return count;
            } catch (ParseException pe) {
                if (logger.isDebugEnabled()){
                    logger.debug("Parsing '" + queryStr + "' failed with " + pe,pe); 
                }
                
                IndextankException ite = new IndextankException();
                ite.set_message("Invalid query");
                throw ite;
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing count. Will throw an IndexTankException. Original Exception is:", e);
                throw new IndextankException();
            } catch (InterruptedException e) {
                logger.error("Interrupted while searching.");
                IndextankException ite = new IndextankException();
                ite.set_message("Interrupted while searching.");
                throw ite;
            }
        }
        
        @Override
        public int size() throws IndextankException {
            try {
                logger.debug("Fetching size");
                Query query = new Query(new MatchAllQuery(),null,null);
                int size = this.searcher.countMatches(query);
                logger.info("Fetched size: " + size);
                return size;
            } catch (RuntimeException e) {
                logger.error("RuntimeException while processing size. Will throw an IndexTankException. Original Exception is:", e);
                throw new IndextankException();
            } catch (InterruptedException e) {
                logger.error("Interrupted while searching.");
                IndextankException ite = new IndextankException();
                ite.set_message("Interrupted while searching.");
                throw ite;
            }
        }

        public SearcherStats stats(){
            return new SearcherStats();
        }

    } 

}
