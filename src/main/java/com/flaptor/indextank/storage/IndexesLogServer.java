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
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.thrift.TException;

import com.flaptor.indextank.rpc.IndexLogInfo;
import com.flaptor.indextank.rpc.LogPage;
import com.flaptor.indextank.rpc.LogPageToken;
import com.flaptor.indextank.rpc.QueueScore;
import com.flaptor.indextank.rpc.RawLogInfo;
import com.flaptor.indextank.util.FormatLogger;
import com.flaptor.indextank.util.ThriftServerThread;
import com.google.common.collect.Lists;

public class IndexesLogServer implements com.flaptor.indextank.rpc.LogReader.Iface, com.flaptor.indextank.rpc.LogManager.Iface {

    protected static final FormatLogger logger = new FormatLogger();

    protected final int readerPort;

    protected LogRoot root;
    protected LogDealer dealer;
    protected LogReader reader;
    protected LogOptimizer optimizer;
    protected LogCleaner cleaner;

    private final int managerPort;


    /**
     * Constructor for tests only. Used to run cycles without actually starting the server.
     * @throws IOException 
     */
    IndexesLogServer(LogRoot root, int liveSegmentSize, int indexSegmentSize) throws IOException {
        this.readerPort = -1;
        this.managerPort = -1;
        this.root = root;
        this.cleaner = new LogCleaner(root);
        this.dealer = new LogDealer(cleaner, root, liveSegmentSize, indexSegmentSize);
        this.reader = new LogReader(cleaner, dealer, root, liveSegmentSize, indexSegmentSize);
        this.optimizer = new LogOptimizer(cleaner, dealer, root);
    }
    
    private IndexesLogServer(int readerPort, int managerPort) throws IOException {
        this.readerPort = readerPort;
        this.managerPort = managerPort;
        this.root = new LogRoot();
        this.cleaner = new LogCleaner(root);
        this.dealer = new LogDealer(cleaner);
        this.reader = new LogReader(cleaner, dealer);
        this.optimizer = new LogOptimizer(cleaner, dealer, root);
    }

    @Override
    public LogPage read_page(String indexCode, LogPageToken token) throws TException {
        try {
            return reader.readPage(indexCode, token);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start(){
        com.flaptor.indextank.rpc.LogReader.Processor readerProcessor = new com.flaptor.indextank.rpc.LogReader.Processor(IndexesLogServer.this);
        ThriftServerThread.forProcessor(readerProcessor, "IndexesLogServer::LogReader", readerPort).start();
        com.flaptor.indextank.rpc.LogManager.Processor managerProcessor = new com.flaptor.indextank.rpc.LogManager.Processor(IndexesLogServer.this);
        ThriftServerThread.forProcessor(managerProcessor, "IndexesLogServer::LogManager", managerPort).start();
        dealer.start();
        optimizer.start();
        cleaner.start();
    }

    @SuppressWarnings("static-access")
    private static Options getOptions(){
        Option rport = OptionBuilder         .withArgName("reader_port")
                                            .hasArg()
                                            .withDescription("Server Port")
                                            .withLongOpt("reader_port")
                                            .create("rp");
        Option mport = OptionBuilder         .withArgName("reader_port")
                                            .hasArg()
                                            .withDescription("Server Port")
                                            .withLongOpt("manager_port")
                                            .create("mp");
                                            
        Option help = OptionBuilder         .withDescription("Displays this help")
                                            .withLongOpt("help")
                                            .create("h");
        
        Options options = new Options();
        options.addOption(rport);
        options.addOption(mport);
        options.addOption(help);

        return options;
    }

    private static void printHelp(Options options, String error) {
        if (null != error) {
            System.out.println("Invalid usage: " + error);
        } 
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("IndexesLogServer [options]", options);
    } 
    
    public static void main(String[] args) throws IOException, InterruptedException{
        // create the parser
        CommandLineParser parser = new PosixParser();

        int readerPort, managerPort;
        
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( getOptions(), args );
            if (line.hasOption("help")) { 
                printHelp(getOptions(), null);
                System.exit(1);
                return;
            }
            
            String val = null;
            val = line.getOptionValue("reader_port", null);
            if (null != val) {
                readerPort = Integer.valueOf(val);
            } else {
                printHelp(getOptions(), "Must specify a server port");
                System.exit(1);
                return;
            }
            val = null;
            val = line.getOptionValue("manager_port", null);
            if (null != val) {
                managerPort = Integer.valueOf(val);
            } else {
                printHelp(getOptions(), "Must specify a server port");
                System.exit(1);
                return;
            }
        } catch( ParseException exp ) {
            printHelp(getOptions(),exp.getMessage());
            System.exit(1);
            return;
        } 
        
        new IndexesLogServer(readerPort, managerPort).start();
    }


    @Override
    public IndexLogInfo get_index_log_info(String indexCode) throws TException {
        return optimizer.getIndexInfo(indexCode);
    }

    @Override
    public RawLogInfo get_raw_log_info() throws TException {
        RawLogInfo info = new RawLogInfo();
        long t = dealer.nextTimestamp;
        info.set_dealer_next_timestamp(t);
        RawLog log = new RawLog(root, RawLog.DEFAULT_SEGMENT_SIZE);
        for (Segment s : log.getLiveSegments()) {
            info.add_to_undealt_segments(s.getInfo());
        }
        return info;
    }

    @Override
    public void enqueue_for_optimization(String indexCode, int priority, double score) throws TException {
        optimizer.enqueue(indexCode, priority, (float)score);
    }

    @Override
    public void delete_index(String indexCode) throws TException {
        cleaner.deleteIndex(indexCode);
    }

    @Override
    public Map<String, QueueScore> get_optimization_queue() throws TException {
        return optimizer.getOptimizationQueue();
    }

    @Override
    public List<String> list_existing_indexes() throws TException {
        return Lists.newArrayList(root.getIndexesLogPath().list());
    }

}
