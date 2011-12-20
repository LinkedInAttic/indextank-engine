package com.flaptor.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.log4j.Logger;

public class WikipediaArticleFinder {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static final String UTF_8_ENCODING = "UTF-8";

    /**
     * Looks up a wikipedia page for a given concept. 
     * 
     * @param concept the concept to look up for
     * @param language the two letter language code for the article to look
     * @return the wikipedia article url or null if there is no article related 
     * @throws IOException if there is a problem with the connectivity to wikipedia.org 
     * @throws MalformedURLException if the language argument is invalid  
     */
    public static URL getWikipediaPage(String concept, String language) throws MalformedURLException, IOException {
        //Possible TODO: make a keepalive connection for multiple requests
        
        String url = "http://" + language + 
                     ".wikipedia.org/wiki/Special:Search?search=" +
                     URLEncoder.encode(concept, UTF_8_ENCODING) + 
                     "&go=Go";
        
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection httpConnection = null;
        try {
            httpConnection = (HttpURLConnection)new URL(url).openConnection();
            httpConnection.connect();
            
            int responseCode = httpConnection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // It returned a results page, hence there is no specific article for the concept
                return null;
            } else if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                    responseCode == HttpURLConnection.HTTP_MOVED_PERM) {
                // It returned a redirect, hence there is a specific article for the concept 
                return new URL(httpConnection.getHeaderField("Location"));
            } else { 
                logger.warn("Unexpected response code (" + responseCode + ").");
                return null;
            }
        } finally {
            if (httpConnection != null) {
                httpConnection.disconnect();
            }
        }
    }
}
