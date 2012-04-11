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

package com.flaptor.indextank.index;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.json.simple.JSONValue;

import com.flaptor.indextank.BoostingIndexer;
import com.flaptor.indextank.DocumentStoringIndexer;
import com.flaptor.indextank.IndexRecoverer;
import com.flaptor.indextank.LogIndexRecoverer;
import com.flaptor.indextank.blender.Blender;
import com.flaptor.indextank.dealer.Dealer;
import com.flaptor.indextank.index.lsi.LargeScaleIndex;
import com.flaptor.indextank.index.rti.RealTimeIndex;
import com.flaptor.indextank.index.scorer.BoostsScorer;
import com.flaptor.indextank.index.scorer.DynamicDataFacetingManager;
import com.flaptor.indextank.index.scorer.DynamicDataManager;
import com.flaptor.indextank.index.scorer.FacetingManager;
import com.flaptor.indextank.index.scorer.NoFacetingManager;
import com.flaptor.indextank.index.scorer.ScoreFunction;
import com.flaptor.indextank.index.scorer.UserFunctionsManager;
import com.flaptor.indextank.index.storage.InMemoryStorage;
import com.flaptor.indextank.query.IndexEngineParser;
import com.flaptor.indextank.query.analyzers.CompositeAnalyzer;
import com.flaptor.indextank.query.analyzers.FilteringAnalyzer;
import com.flaptor.indextank.rpc.IndexerServer;
import com.flaptor.indextank.rpc.IndexerStatus;
import com.flaptor.indextank.rpc.SearcherServer;
import com.flaptor.indextank.rpc.SuggestorServer;
import com.flaptor.indextank.search.DidYouMeanSearcher;
import com.flaptor.indextank.search.DocumentSearcher;
import com.flaptor.indextank.search.SnippetSearcher;
import com.flaptor.indextank.search.TrafficLimitingSearcher;
import com.flaptor.indextank.storage.alternatives.DocumentStorage;
import com.flaptor.indextank.storage.alternatives.DocumentStorageFactory;
import com.flaptor.indextank.suggest.DidYouMeanSuggestor;
import com.flaptor.indextank.suggest.NoSuggestor;
import com.flaptor.indextank.suggest.QuerySuggestor;
import com.flaptor.indextank.suggest.Suggestor;
import com.flaptor.indextank.suggest.TermSuggestor;
import com.flaptor.util.Execute;
import com.flaptor.util.FileUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 *
 * @author Flaptor Team
 */
public class IndexEngine {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	
    private BoostingIndexer indexer;
    private DocumentSearcher searcher;
    private BoostsScorer scorer;
    private DynamicDataManager boostsManager;
    private LargeScaleIndex lsi;
    private RealTimeIndex rti;
    private Suggestor suggestor;
    private DocumentStorage storage = null;
	private IndexEngineParser parser;
    private UserFunctionsManager functionsManager = null;
    private final BasicPromoter promoter;
    private IndexerStatus status;
    private final String indexCode;
    private final String environment;
    private final int basePort;
    private final File baseDir;
    
    private IndexRecoverer.IndexStorageValue recoveryStorage;
    private String cassandraClusterHosts;

    /*
     * In index configuration:
     * - log_based_storage: true/false
     * - log_server_host
     * - log_server_port
     */
    private boolean logBasedStorage = false;
    private String logServerHost;
    private int logServerPort;

    private static final int DEFAULT_BASE_PORT = 7910;
    private static final int DEFAULT_RTI_SIZE = 1000;

    public static enum SuggestValues { NO, QUERIES, DOCUMENTS};
    
    public IndexEngine( File baseDir, 
                        int basePort, 
                        int rtiSize, 
                        boolean load, 
                        int boostsSize, 
                        SuggestValues suggest, 
                        String functions, 
                        boolean facets, 
                        String indexCode, 
                        String environment ) throws IOException {
        
    	this(   baseDir, 
    	        basePort, 
    	        rtiSize, 
    	        load, 
    	        boostsSize, 
    	        suggest, 
    	        functions, 
    	        facets, 
    	        indexCode, 
    	        environment, 
    	        Maps.newHashMap()
    	     );
    }
    
    public IndexEngine( File baseDir, 
                        int basePort, 
                        int rtiSize, 
                        boolean load, 
                        int boostsSize, 
                        SuggestValues suggest, 
                        String functions, 
                        boolean facets, 
                        String indexCode, 
                        String environment, 
                        Map<Object, Object> configuration) throws IOException {
        
        Preconditions.checkNotNull(indexCode);
        Preconditions.checkNotNull(environment);
        Preconditions.checkArgument(basePort > 0);
        Preconditions.checkNotNull(baseDir);

        this.indexCode = indexCode;
        this.environment = environment;
        this.basePort = basePort;
        this.baseDir = baseDir;
        
        
    	String defaultField = "text";
    	
    	
    	if (configuration.containsKey("log_based_storage")) {
    	    logBasedStorage = (Boolean)configuration.get("log_based_storage");
    	    if (logBasedStorage) {
    	        logServerHost = (String) configuration.get("log_server_host");
    	        
    	        logServerPort = ((Long) configuration.get("log_server_port")).intValue();
    	    }
    	}
    	
    	Map<Object, Object> analyzerConfiguration = (Map<Object, Object>) configuration.get("analyzer_config");

    	if (analyzerConfiguration != null) {
			Analyzer analyzer;
			
			if (analyzerConfiguration.containsKey("perField")) {
				Map<Object, Object> perfieldConfiguration = (Map<Object, Object>) analyzerConfiguration.get("perField");
				Map<String, Analyzer> perfieldAnalyzers = Maps.newHashMap();
				for (Entry<Object, Object> entry : perfieldConfiguration.entrySet()) {
					String field = (String) entry.getKey();
					Map<Object, Object> config = (Map<Object, Object>) entry.getValue();
					
					perfieldAnalyzers.put(field, buildAnalyzer(config));
				}
				
				analyzer = new CompositeAnalyzer(buildAnalyzer((Map<Object, Object>) analyzerConfiguration.get("default")), perfieldAnalyzers);
				
			} else {
				analyzer = buildAnalyzer(analyzerConfiguration);
			}
			
			parser = new IndexEngineParser(defaultField, analyzer);
		} else {
			parser = new IndexEngineParser(defaultField);
		}
    	
    	boostsManager = new DynamicDataManager(boostsSize, baseDir);
        
    	scorer = new BoostsScorer(boostsManager, Maps.<Integer, ScoreFunction>newHashMap());
        
        functionsManager = new UserFunctionsManager(scorer);
        boolean someFunctionDefined = false;
        String def0 = "0-A";
        try {
            functionsManager.addFunction(0, def0); // Default timestamp function
        } catch (Exception ex) {
            logger.error("Defining scoring function (spec '"+def0+"')", ex);
        }
        if (null != functions && !"".equals(functions)) {
            String[] specs = functions.split("\\|");
            for (String spec : specs) {
                try {
                    String[] parts = spec.split(":",2);
                    if (parts.length == 2) {
                        int id = Integer.parseInt(parts[0].trim());
                        String def = parts[1].trim();
                        functionsManager.addFunction(id, def);
                        someFunctionDefined = true;
                    } else {
                        logger.error("Function should be defined as <id>:<definition> (found '"+spec+"').");
                    }
                } catch (Exception ex) {
                    logger.error("Defining scoring function (spec '"+spec+"')", ex);
                }
            }
        }

        FacetingManager facetingManager;
        
        if (facets) {
        	facetingManager = new DynamicDataFacetingManager(boostsManager);
        } else {
        	facetingManager = new NoFacetingManager();
        }
        
        lsi = new LargeScaleIndex(scorer, parser, baseDir, facetingManager);
		rti = new RealTimeIndex(scorer, parser, rtiSize, facetingManager);
        switch (suggest) {
            case NO:
                suggestor = new NoSuggestor();
                break;
            case DOCUMENTS:
                IndexEngineParser suggestorParser = new IndexEngineParser(defaultField);
                suggestor = new TermSuggestor(suggestorParser, baseDir);
                break;
            case QUERIES:
                suggestor = new QuerySuggestor(parser, baseDir);
                break;
        }
        
        this.cassandraClusterHosts = (String) configuration.get("cassandra_cluster_hosts");
        // index recovery configuration
        String recoveryConf = (String) configuration.get("index_recovery");
        if ("cassandra".equals(recoveryConf)) {
            if (this.cassandraClusterHosts == null || this.cassandraClusterHosts.trim().length() == 0)
                throw new IllegalArgumentException("Invalid cassandra servers for index recovery");
            this.recoveryStorage = IndexRecoverer.IndexStorageValue.CASSANDRA;
            logger.info("Index recovery configuration set to recover index from cassandra servers: " + this.cassandraClusterHosts);

        } else {
            logger.info("Index recovery configuration set to recover index from simpleDB");
            this.recoveryStorage = IndexRecoverer.IndexStorageValue.SIMPLEDB;
        }
       
        storage = buildStorage(configuration); 
 
        promoter = new BasicPromoter(baseDir, load);
        searcher = new Blender(lsi, rti, suggestor, promoter, boostsManager);
        indexer = new Dealer(lsi, rti, suggestor, boostsManager, rtiSize, promoter, functionsManager);
        status = IndexerStatus.started;

    }

	private Analyzer buildAnalyzer(Map<Object, Object> configuration) {
		Analyzer analyzer;
		String factoryClassString = (String) configuration.get("factory");
		Map<Object, Object> factoryConfig = (Map<Object, Object>) configuration.get("configuration");
		
		try {
			Class<?> factoryClass = Class.forName(factoryClassString);
			Method method = factoryClass.getMethod("buildAnalyzer", new Class[] {Map.class});
			
			analyzer = (Analyzer) method.invoke(null, factoryConfig);
			
			if (factoryConfig.containsKey("filters")) {
				analyzer = new FilteringAnalyzer(analyzer, factoryConfig);
			}
			
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Analyzer factory class not found", e);
		} catch (SecurityException e) {
			throw new RuntimeException("Analyzer factory class not instantiable", e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Analyzer factory class does not have the required static method buildAnalyzer", e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Analyzer factory class does not have the required static method buildAnalyzer", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Analyzer factory class does not have the required static method buildAnalyzer or it is not accessible", e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Analyzer factory class threw an exception for the give configuration", e);
		}
		return analyzer;
	};

    public DocumentStorage buildStorage(Map<?, ?> configuration) {

        DocumentStorage storage = null;

        // parse storage from configuration
        String storageClassStr = (String) configuration.get("storage");
        if (null == storageClassStr || "".equals(storageClassStr.trim()) ) {
            logger.info("NOT Using storage");
        } else {
		        try {
                // it is either a factory, or a concrete class
                Class storageClass = Class.forName(storageClassStr);

                if (DocumentStorageFactory.class.isAssignableFrom(storageClass)) {
                // if it was a factory .. 
                    logger.info("got a DocumentStorageFactory: " + storageClass);
                    DocumentStorageFactory storageFactory = (DocumentStorageFactory) storageClass.newInstance();
                    Map<?, ?> storageConfig = (Map<?, ?>)configuration.get("storage_config");
                    if (null == storageConfig) { 
                        logger.warn("no 'storage_config' entry on configuration .. will try an empty Map");
                        storageConfig = Maps.newHashMap();
                    }

                    storage = storageFactory.fromConfiguration(storageConfig);
                } else if (DocumentStorage.class.isAssignableFrom(storageClass)) {
                // if it was a storage .. 
                    logger.info("got a DocumentStorage: " + storageClass + ". Trying default Constructor.");
                    storage = (DocumentStorage) storageClass.newInstance();
                } else {
                // if it was anything else .. complain
                    logger.error("got a storage option that is NOT a DocumentStorageFactory nor a DocumentStorage. I don't know what to do with it!");
                    throw new IllegalArgumentException("Configuration 'storage' class is not a DocumentStorageFactory nor a DocumentStorage.");
                }
		        } catch (ClassNotFoundException e) {
        			throw new RuntimeException("DocumentStorage(Factory?) class not found", e);
        		} catch (SecurityException e) {
        			throw new RuntimeException("DocumentStorage(Factory?) class not instantiable", e);
        		} catch (InstantiationException e) {
        			throw new RuntimeException("DocumentStorage(Factory?) class threw an exception for the given configuration", e);
		        } catch (IllegalAccessException e) {
			        throw new RuntimeException("DocumentStorage(Factory?) class is not accessible", e);
        		}
        }


        return storage;
    }


    public BoostingIndexer getIndexer(){
        return this.indexer;
    }

    public DocumentSearcher getSearcher(){
        return this.searcher;
    }

    public Suggestor getSuggestor() {
        return this.suggestor;
    }
   
    public DocumentStorage getStorage() {
		return storage;
	}

    public IndexEngineParser getParser() {
		return parser;
	}

    public void setStatus(IndexerStatus status) {
        this.status = status;
    }

    public IndexerStatus getStatus() {
        return status;
    }
    
    public void setIndexer(BoostingIndexer indexer) {
        this.indexer = indexer;
    }

    public synchronized void startFullRecovery() {
        if (getStatus() != IndexerStatus.started && getStatus() != IndexerStatus.error) {
            logger.error("startFullRecovery requested, but I'm in the wrong state (" + getStatus() + ')');
        } else {
            setStatus(IndexerStatus.recovering);
            logger.info("startFullRecovery requested. Creating and starting a recovery thread.");
            
            Runnable recoverer;
            if (logBasedStorage) {
                recoverer = new LogIndexRecoverer(this, indexCode, logServerHost, logServerPort);
            } else {
                recoverer = new IndexRecoverer(this, "127.0.0.1", basePort, baseDir, indexCode, environment, recoveryStorage, cassandraClusterHosts);
                ((IndexRecoverer)recoverer).resetTimestamp();
            }
            
            Thread recovererThread = new Thread(recoverer) {
                                            public void run() {
                                                try {
                                                    super.run();
                                                    setStatus(IndexerStatus.ready);
                                                } catch (Exception e) {
                                                    logger.error("Exception while recovering.", e);
                                                    setStatus(IndexerStatus.error);
                                                }
                                            }
                                        };
            recovererThread.start();
                
        }
    }

    @SuppressWarnings("static-access")
    private static Options getOptions(){
        Option baseDir = OptionBuilder  .withArgName("base-dir")
                                        .hasArg()
                                        .isRequired()
                                        .withDescription("The basint e dir")
                                        .withLongOpt("dir")
                                        .create("d");

        Option basePort = OptionBuilder .withArgName("base-port")
                                        .hasArg()
                                        .withDescription("The base port")
                                        .withLongOpt("port")
                                        .create("p");

        Option boostSize = OptionBuilder .withArgName("boosts-size")
        								.hasArg()
        								.withDescription("Number of available boosts")
        								.withLongOpt("boosts")
        								.create("b");

        Option rtiSize = OptionBuilder .withArgName("rti-size")
                                        .hasArg()
                                        .withDescription("The size limit for the RTI")
                                        .withLongOpt("rti-size")
                                        .create("rs");

        Option help     = OptionBuilder .withDescription("displays this help")
                                        .withLongOpt("help")
                                        .create("h");

        Option snippets = OptionBuilder .withDescription("Allow snippet generation and field fetching.")
                                        .withLongOpt("snippets")
                                        .create("sn");
        
        Option recover = OptionBuilder  .withDescription("Recover documents from the storage.")
                                        .withLongOpt("recover")
                                        .create("r");

        Option indexCode = OptionBuilder.withArgName("code")
                                        .hasArg()
                                        .isRequired()
                                        .withDescription("the index code this indexengine has")
                                        .withLongOpt("index-code")
                                        .create("ic");

        Option environment = OptionBuilder.withArgName("environment")
								        .hasArg()
								        .isRequired()
								        .withDescription("environment prefix")
								        .withLongOpt("environment-prefix")
								        .create("env");
        /*
         * Analyzer argument should receive a JSON string with the following root structure:
         *    - factory: a java type that implements the following static method: org.apache.lucene.analysis.Analyzer buildAnalyzer(Map).
         *    - configuration: a JSON object to be passed to the buildAnalyzer method. 
         */
        Option analyzer = OptionBuilder.withArgName("analyzer")
								        .hasArg()
								        .withDescription("specific analyzer")
								        .withLongOpt("analyzer")
								        .create("an");

        Option configFile = OptionBuilder.withArgName("conf-file")
								        .hasArg()
								        .withDescription("configuration file")
								        .withLongOpt("conf-file")
								        .create("cf");

        Option loadState = OptionBuilder.withArgName("load")
                                        .withDescription("if present, the index engine will try to restore its state"
                                                + "from the serialized form.")
                                        .withLongOpt("load-state")
                                        .create("l");

        Option suggest = OptionBuilder.withArgName("suggest")
                                        .hasArg()
                                        .withDescription("if present, loads the suggest/autocomplete system.")
                                        .withLongOpt("suggest")
                                        .create("su");
        Option facets = OptionBuilder.withArgName("facets")
								        .withDescription("if present, performs facetings queries.")
								        .withLongOpt("facets")
								        .create("fa");

        Option functions = OptionBuilder.withArgName("functions")
                                        .hasArg()
                                        .withDescription("list of '|' separated scoring functions, each of which has the form <id>:<definition>.")
                                        .withLongOpt("functions")
                                        .create("fn");
        Option didyoumean = OptionBuilder.withLongOpt("didyoumean")
                                        .withDescription("if present, performs 'did you mean?' suggestions on queries. Requires --suggest documents.")
                                        .create("dym");
        
        Option storage  = OptionBuilder.withLongOpt("storage")
                                        .hasArg()
                                        .withDescription("DEPRECATED! if present, specifies a storage backend. Only 'ram' is supported on command line. USE JSON CONFIGURATION!.")
                                        .create("st");

        Options options = new Options();
        options.addOption(baseDir);
        options.addOption(basePort);
        options.addOption(boostSize);
        options.addOption(help);
        options.addOption(snippets);
        options.addOption(recover);
        options.addOption(indexCode);
        options.addOption(rtiSize);
        options.addOption(loadState);
        options.addOption(suggest);
        options.addOption(facets);
        options.addOption(functions);
        options.addOption(environment);
        options.addOption(analyzer);
        options.addOption(didyoumean);
        options.addOption(configFile);
        options.addOption(storage);

        return options;
    }

    private static void printHelp(Options options, String error) {
        if (null != error) {
            System.out.println("Parsing failed.  Reason: " + error);
        } 
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("IndexEngine",options);
    } 

    //--------------------------------------------------------------------------------
    //PRIVATE CLASSES
    private static class ShutdownThread extends Thread {
        private final BoostingIndexer server;
        public ShutdownThread(BoostingIndexer server) {
            this.server = server;
            setName("IndexEngine's ShutdownThread");
        }
        @Override
        public void run() {
            try {
                logger.info("Shutdown hook started.");
                server.dump();
                logger.info("Shutdown hook ended.");
            } catch (Exception e) {
                logger.error("Exception caught while saving state to disk. This probably means that some data was lost.", e);
            }
        }
    }

    //--------------------------------------------------------------------------------
    //STATIC METHODS
    public static void main(String[] args) throws IOException{
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            org.apache.log4j.PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            logger.warn("log4j.properties not found on classpath!");
        }
        // create the parser
        CommandLineParser parser = new PosixParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( getOptions(), args );
            if (line.hasOption("help")) { 
                printHelp(getOptions(),null);
                System.exit(1);
            }
            

            Map<Object, Object> configuration = Maps.newHashMap();

            File baseDir = new File(line.getOptionValue("dir"));
            int basePort = Integer.parseInt(line.getOptionValue("port", String.valueOf(DEFAULT_BASE_PORT)));
            int boostsSize = Integer.parseInt(line.getOptionValue("boosts", String.valueOf(1)));
            int rtiSize = Integer.parseInt(line.getOptionValue("rti-size", String.valueOf(DEFAULT_RTI_SIZE)));
            boolean loadState = line.hasOption("load-state");

            SuggestValues suggest;
            if (line.hasOption("suggest")) {
                String value = line.getOptionValue("suggest");
                if ( value.equalsIgnoreCase("queries")) {
                    suggest = SuggestValues.QUERIES;
                } else if ( value.equalsIgnoreCase("documents")) {
                    suggest = SuggestValues.DOCUMENTS;
                } else {
                    throw new IllegalArgumentException("Invalid value for suggest: can only be \"queries\" or \"documents\".");
                }
            } else {
                suggest = SuggestValues.NO;
            }
            
            if (line.hasOption("storage")){
                logger.warn("command-line option 'storage' is deprecated. write it on the JSON configuration!");
                logger.warn("I'll try to do that for you this time .. ");

                String storageType = line.getOptionValue("storage");
                if ("ram".equals(storageType)) {
                    Map<String, String> storageConfig = Maps.newHashMap();
                    storageConfig.put(InMemoryStorage.Factory.DIR, baseDir.getPath());
                    storageConfig.put(InMemoryStorage.Factory.LOAD, "true");
                    
                    configuration.put("storage", InMemoryStorage.class.getName());
                    configuration.put("storage_config", storageConfig);
                } else {
                    throw new IllegalArgumentException("DEPRECATED command line storage got an Illegal value: " + storageType + ". Please migrate to JSON configuration!");
                }
            }

            String functions = null;
            if (line.hasOption("functions")) {
                functions = line.getOptionValue("functions");
            }
            
            String environment;
            String val = line.getOptionValue("environment-prefix", null);
            if (null != val) {
            	environment = val;
            } else {
            	environment = "";
            }
            logger.info("Command line option 'environment-prefix' set to " + environment);
			
            boolean facets = line.hasOption("facets");
            logger.info("Command line option 'facets' set to " + facets);
            String indexCode = line.getOptionValue("index-code");
            logger.info("Command line option 'index-code' set to " + indexCode);

        	
            String configFile = line.getOptionValue("conf-file", null); 
        	  logger.info("Command line option 'conf-file' set to " + configFile);
            
        	if (configFile != null) {
        		configuration = (Map<Object, Object>) JSONValue.parse(FileUtil.readFile(new File(configFile)));
        	}
            IndexEngine ie = new IndexEngine(
                                               baseDir, 
                                               basePort, 
                                               rtiSize, 
                                               loadState, 
                                               boostsSize, 
                                               suggest, 
                                               functions, 
                                               facets, 
                                               indexCode, 
                                               environment, 
                                               configuration);

            BoostingIndexer indexer = ie.getIndexer(); 
            DocumentSearcher searcher = ie.getSearcher();
            Suggestor suggestor = ie.getSuggestor();
            DocumentStorage storage = ie.getStorage();


            if (line.hasOption("snippets")) {
                // shouldn't this be set based on storageValue? 
            	indexer = new DocumentStoringIndexer(indexer, storage);
            	ie.setIndexer(indexer);
                searcher = new SnippetSearcher(searcher, storage, ie.getParser());
            }

            if (line.hasOption("didyoumean")) {
                if (suggest != SuggestValues.DOCUMENTS) {
                    throw new IllegalArgumentException("didyoumean requires --suggest documents");
                }
                DidYouMeanSuggestor dym = new DidYouMeanSuggestor((TermSuggestor)ie.getSuggestor());
                searcher = new DidYouMeanSearcher(searcher, dym);
            }

            searcher = new TrafficLimitingSearcher(searcher);
            Runtime.getRuntime().addShutdownHook(new ShutdownThread(indexer));

            new SearcherServer(searcher, ie.getParser(), ie.boostsManager, ie.scorer, basePort + 2).start();
			new SuggestorServer(suggestor, basePort + 3).start();
            IndexerServer indexerServer = new IndexerServer(ie, indexer, basePort + 1);
            indexerServer.start();

        } catch( ParseException exp ) {
            printHelp(getOptions(),exp.getMessage());
        } 

    }

}
