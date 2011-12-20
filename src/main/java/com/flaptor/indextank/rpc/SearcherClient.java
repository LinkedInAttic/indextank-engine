package com.flaptor.indextank.rpc;

import java.util.Collections;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import com.google.common.collect.Lists;

public class SearcherClient {

    @SuppressWarnings("static-access")
    private static Options getOptions(){
        Option hostName = OptionBuilder .withArgName("server-host")
                                        .hasArg()
                                        .withDescription("the host where the server is running")
                                        .withLongOpt("host")
                                        .create("h");
        Option basePort = OptionBuilder .withArgName("base-port")
                                        .hasArg()
                                        .withDescription("The base port where the server is running")
                                        .withLongOpt("port")
                                        .create("p");
        
        Option function = OptionBuilder .withArgName("scoring-function")
								        .hasArg()
								        .withDescription("The scoring function index")
								        .withLongOpt("function")
								        .create("f");
        
        Option help     = OptionBuilder .withDescription("displays this help")
                                        .withLongOpt("help")
                                        .create("help");

        Options options = new Options();
        options.addOption(hostName);
        options.addOption(basePort);
        options.addOption(function);
        options.addOption(help);

        return options;
    }

    private static void printHelp(Options options, String error) {
        if (null != error) {
            System.out.println("Usage error: " + error);
        }
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("SearcherClient",options);
    }

    public static void main(String[] args) {

 // create the parser
        CommandLineParser parser = new PosixParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( getOptions(), args );
            if (line.hasOption("help")) {
                printHelp(getOptions(),null);
                System.exit(1);
            }

            if (!line.hasOption("function")) {
            	   printHelp(getOptions(),null);
                   System.exit(1);
            }
            
            Integer scoringFunctionIndex = Integer.valueOf(line.getOptionValue("function"));
            
            String host = line.getOptionValue("host", "localhost");
            String basePort = line.getOptionValue("port","7910");
            int port = Integer.valueOf(basePort) + 2;

            TTransport transport;
            transport = new TSocket(host, port);
            TProtocol protocol = new TBinaryProtocol(transport);
            Searcher.Client client = new Searcher.Client(protocol);

            transport.open();
            ResultSet rs = client.search(line.getArgs()[0], 0, 10, scoringFunctionIndex, Collections.<Integer, Double>emptyMap(), Lists.<CategoryFilter>newArrayList(), Lists.<RangeFilter>newArrayList(), Lists.<RangeFilter>newArrayList(), Collections.<String, String>emptyMap()); // TODO join getArgs
            transport.close();
            System.out.println(rs);
        } catch (InvalidQueryException e) {
            e.printStackTrace();
        } catch (MissingQueryVariableException e) {
            e.printStackTrace();
		} catch (IndextankException e) {
			e.printStackTrace();
        } catch( ParseException exp ) {
            printHelp(getOptions(),exp.getMessage());
        } catch (TTransportException e) {
            e.printStackTrace();
        } catch (TException e) {
            e.printStackTrace();
        } 
    }

}

