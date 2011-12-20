package com.flaptor.util.parser.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.flaptor.util.Execute;
import com.flaptor.util.parser.ParseException;

/**
 * This abstract class provides methods for parsing http requests but leaves unimplemented
 * the actual parsing of the response. Subclasses should implement the {@link #parse(String url, InputStream is, HttpURLConnection connection)}
 * method and should defined the return type by fixing the type parameter T. The strategy pattern
 * is used to define a handler for the response ({@link HttpResponseHandler}) code of an http connection, implementations may choose
 * to throw runtime exceptions in some cases, or decide whether the parser should proceed to invoke 
 * the {@link #fecthAndParse(InputStream)} method or not.
 * 
 * Subclasses may modify the default HTTP method (GET) by overriding the {@link #getMethod()} method.
 * 
 * The abstract class handles http connection, and takes care of handling timeouts with specific
 * methods.  
 * 
 * @author Santiago Perez (santip), Ignacio Perez (iperez)
 *
 * @param <T>
 */
public abstract class HttpUrlFetcher<T> {
	public interface HttpResponseHandler {
	    /**
	     * Handles response codes for http requests
	     * 
	     * @param responseCode the code to be handled
	     * 
	     * @return true iff the input should be processed, if false,
	     * input will not be processed and a null value will be 
	     * returned by the parser 
	     */
	    public boolean handleResponse(int responseCode);
	}
	
    private static final String USER_AGENT_HEADER = "User-Agent";
    private final HttpResponseHandler responseHandler;
    private final String userAgent;
    
    /**
     * Create a new HttpParser with the given handler.
     * 
     * @param responseHandler the handler for HTTP response codes.
     * @param userAgent the User-Agent for HTTP request.
     */
    public HttpUrlFetcher(HttpResponseHandler responseHandler, String userAgent) {
        this.responseHandler = responseHandler;
        this.userAgent = userAgent;
    }

    public HttpUrlFetcher(HttpResponseHandler responseHandler) {
        this(responseHandler, null);
    }

    /**
     * Parses the content of the given url.
     * 
     * @param url the url to parse.
     * @param timeoutMillis maximum amount of milliseconds to wait before aborting the parsing.
     * 
     * @return the result of the parsing, for specific implentations see documentation on {@link #fecthAndParse(InputStream)} 
     * of the specific class.
     * 
     * @throws ParseException if an exception occurred during the parsing of the content.
     * @throws IOException if there's a problem in the connection with the given url 
     * @throws TimeoutException if the connection and/or parsing takes longer than the given timeout
     * @throws InterruptedException if the thread gets interrupted while processing.
     */
    public T fetchAndParse(final String url, long timeoutMillis) throws ParseException, IOException, TimeoutException, InterruptedException {
        return fetchAndParse(url, timeoutMillis, null);
    }
    
    /**
     * Parses the content of the given url.
     * 
     * @param url the url to parse.
     * @param timeoutMillis maximum amount of milliseconds to wait before aborting the parsing.
     * @param followRedirects override HttpURLConnection followRedirects parameter.
     * 
     * @return the result of the parsing, for specific implentations see documentation on {@link #fecthAndParse(InputStream)} 
     * of the specific class.
     * 
     * @throws ParseException if an exception occurred during the parsing of the content.
     * @throws IOException if there's a problem in the connection with the given url 
     * @throws TimeoutException if the connection and/or parsing takes longer than the given timeout
     * @throws InterruptedException if the thread gets interrupted while processing.
     */
    public T fetchAndParse(final String url, long timeoutMillis, final Boolean followRedirects) throws ParseException, IOException, TimeoutException, InterruptedException {
        Callable<T> parsingTask = new Callable<T>() { 
            public T call() throws Exception { 
                return fecthAndParse(url, followRedirects); 
            }
        };
        try {
            return Execute.executeWithTimeout(parsingTask, timeoutMillis, TimeUnit.MILLISECONDS, "httpparse");
        } catch (ExecutionException e) {
            Execute.checkAndThrow(ParseException.class, e.getCause());
            Execute.checkAndThrow(IOException.class, e.getCause());
            Execute.checkAndThrow(RuntimeException.class, e.getCause());
            Execute.checkAndThrow(Error.class, e.getCause());
            throw new RuntimeException(e.getCause());
        }
    }
    
    /**
     * Parses the content of the given url.
     * 
     * @param url the url to parse
     * 
     * @return the result of the parsing, for specific implentations see documentation on {@link #fecthAndParse(InputStream)} 
     * of the specific class.
     * 
     * @throws ParseException if an exception occurred during the parsing of the content.
     * @throws IOException if there's a problem in the connection with the given url 
     */
    public T fecthAndParse(String url) throws ParseException, IOException {
        return fecthAndParse(url, null);
    }
    
    /**
     * Parses the content of the given url.
     * 
     * @param url the url to parse
     * @param followRedirects override HttpURLConnection followRedirects parameter.
     * 
     * @return the result of the parsing, for specific implentations see documentation on {@link #fecthAndParse(InputStream)} 
     * of the specific class.
     * 
     * @throws ParseException if an exception occurred during the parsing of the content.
     * @throws IOException if there's a problem in the connection with the given url 
     */
    public T fecthAndParse(String url, Boolean followRedirects) throws ParseException, IOException {
    	if (followRedirects == null) {
    		followRedirects = true;
    	}
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setDoInput(true);
            prepareConnection(connection, followRedirects);
            if (shouldDoOutput()) {
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                try {
                    writeOutput(os);
                } finally {
                    Execute.close(os);
                }
            }
            
            if (responseHandler == null || responseHandler.handleResponse(connection.getResponseCode())) {
                InputStream is = connection.getInputStream();
                try {
                    return parse(url, is, connection);
                } finally {
                    Execute.close(is);
                }
            } else {
                return null;
            }
        } finally {
            connection.disconnect();
        }
    }


    /**
     * Method to be overriden.
     * 
     * @param is the input stream to parse
     * @param connection the {@link HttpURLConnection} to be used
     * @return
     * @throws ParseException
     */
    protected abstract T parse(String url, InputStream is, HttpURLConnection connection) throws ParseException;

    /**
     * May be overriden to change the HTTP method.
     * 
     * @return the HTTP method to use for establishing connections 
     */
    protected String getMethod() {
        return "GET";
    }

    /**
     * May be overriden to do additional work when establishing a connection, such 
     * as setting specific headers. Subclasses should invoke it's parent implementation 
     * of this method.
     * 
     * @param connection the connection being established
     * 
     * @throws ProtocolException
     */
    protected void prepareConnection(HttpURLConnection connection, Boolean followRedirects) throws ProtocolException {
        if (followRedirects != null) {
            connection.setInstanceFollowRedirects(followRedirects);
        }
        connection.setRequestMethod(getMethod());
        if (userAgent != null) {
            connection.addRequestProperty(USER_AGENT_HEADER, userAgent);
        }
    }

    /**
     * May be overriden by implementations to define an output in http connections.
     * Default implementation returns false.
     * 
     * @return true iff output should be used
     */
    protected boolean shouldDoOutput() {
        return false;
    }

    /**
     * May be overriden in conjunction with {@link #shouldDoOutput()} to write the
     * necessary output in the http connection
     * 
     * @param os the stream where the output should be written
     */
    protected void writeOutput(OutputStream os) {
    }
    
}
