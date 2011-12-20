package com.flaptor.indextank.storage;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.thrift.TException;
import org.apache.thrift.TProcessor;
import org.apache.thrift.transport.TTransportException;

import com.flaptor.indextank.rpc.LogBatch;
import com.flaptor.indextank.rpc.LogRecord;
import com.flaptor.indextank.rpc.LogWriter;
import com.flaptor.indextank.util.FormatLogger;
import com.flaptor.indextank.util.ThriftServerThread;
import com.flaptor.util.Execute;
import com.google.common.base.Preconditions;

public class LogWriterServer implements LogWriter.Iface {

    static final FormatLogger logger = new FormatLogger();
    static final FormatLogger alertLogger = FormatLogger.getAlertsLogger();

    protected RawLog liveLog; 
    protected final int port;
    protected LogRoot root;

    private final boolean testMode;

    /**
     * Constructor for tests only. Used to run cycles without actually starting the server.
     * @throws IOException 
     */
    LogWriterServer(LogRoot root, int liveSegmentSize, boolean testMode) throws IOException {
        Preconditions.checkArgument(testMode, "This constructor is only intended for test mode");
        this.testMode = testMode;
        this.port = -1;
        this.liveLog = new RawLog(root, liveSegmentSize);
        this.root = root;
        if (!root.lockComponent("writer")) {
            throw new RuntimeException("Unable to get lock, there's another server running writing to this log.");
        }
    }
    
    private LogWriterServer(int port) throws IOException {
        this(LogRoot.DEFAULT_PATH, port);
    }

    private LogWriterServer(File path, int port) throws IOException {
        this.port = port;
        this.root = new LogRoot(path, LogRoot.DEFAULT_READING_PAGE_SIZE);
        this.liveLog = new RawLog(root, RawLog.DEFAULT_SEGMENT_SIZE);
        this.testMode = false;
        if (!root.lockComponent("writer")) {
            throw new RuntimeException("Unable to get lock, there's another server running writing to this log.");
        }
    }
    
    @Override
    public void send_batch(LogBatch batch) throws TException {
        for (LogRecord record : batch.get_records()) {
            try {
                liveLog.write(record);
            } catch (IOException e) {
                alertLogger.error(e, "Unable to write batch of %d records.", batch.get_records_size());
                throw new RuntimeException(e);
            }
        }
        if (testMode) {
            liveLog.flush();
        }
    }

    public void start(){
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    logger.info("Flushing live log to disk");
                    liveLog.flush();
                    logger.info("Successfully flushed to disk");
                } catch (TTransportException e) {
                }
            }
        });
        TProcessor processor = new LogWriter.Processor(LogWriterServer.this);
        ThriftServerThread.forProcessor(processor, "LogWriterServer", port).start();
        new FlushEverySecond().start();
    }

    @SuppressWarnings("static-access")
    private static Options getOptions(){
        Option port = OptionBuilder         .withArgName("port")
                                            .hasArg()
                                            .withDescription("Server Port")
                                            .withLongOpt("port")
                                            .create("p");
        
        Option path = OptionBuilder         .withArgName("path")
                                            .hasArg()
                                            .withDescription("Logs Path")
                                            .withLongOpt("path")
                                            .isRequired(false)
                                            .create("f");
        
        Option help = OptionBuilder         .withDescription("Displays this help")
                                            .withLongOpt("help")
                                            .create("h");
        
        Options options = new Options();
        options.addOption(port);
        options.addOption(path);
        options.addOption(help);

        return options;
    }

    private static void printHelp(Options options, String error) {
        if (null != error) {
            System.out.println("Invalid usage: " + error);
        } 
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("LogWriterServer [options]", options);
    } 
    
    public static void main(String[] args) throws IOException, InterruptedException{
        // create the parser
        CommandLineParser parser = new PosixParser();

        int port;
        
        LogWriterServer server;
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( getOptions(), args );
            if (line.hasOption("help")) { 
                printHelp(getOptions(), null);
                System.exit(1);
                return;
            }
            
            String val = null;
            val = line.getOptionValue("port", null);
            if (null != val) {
                port = Integer.valueOf(val);
            } else {
                printHelp(getOptions(), "Must specify a server port");
                System.exit(1);
                return;
            }
            
            String path = null;
            path = line.getOptionValue("path", null);
            if (null != path) {
                server = new LogWriterServer(new File(path), port);
            } else {
                server = new LogWriterServer(port);
            }
        } catch( ParseException exp ) {
            printHelp(getOptions(),exp.getMessage());
            System.exit(1);
            return;
        } 
        
        server.start();
    }
    
    private class FlushEverySecond extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Execute.sleep(1000);
                    liveLog.flush();
                } catch (Throwable t) {
                    alertLogger.error(t, "Failed while flushing!!!");
                }
            }
        }
    }

    
}
