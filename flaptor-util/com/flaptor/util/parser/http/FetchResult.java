/**
 * 
 */
package com.flaptor.util.parser.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import com.flaptor.util.Execute;
import com.flaptor.util.Pair;

public class FetchResult {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    
    private String cleanText;
    private Document document;
	private List<Pair<String,String>> links = Collections.emptyList();
	private String title = null;
    private String url = null;
    private URI baseUri = null;

	private String documentType;

	public FetchResult(String url) throws URISyntaxException {
        this.url = url;
        if (url.length() > 0) {
            baseUri = getURI(url);
        }
        links = new ArrayList<Pair<String,String>>();
    }

    protected void setTitle(String title) {
        if (null != title) {
            this.title = title.trim();
        }
    }

    protected void setBaseUrl(URI baseUri) throws URISyntaxException {
    	this.baseUri = baseUri;
    }
    protected void setBaseUrl(String baseUrl) throws URISyntaxException {
        baseUri = getURI(baseUrl);
    }
    
    public URI getBaseUri() {
		return baseUri;
	}

    protected void setText(String cleanText) {
    	this.cleanText = cleanText;
    }
    
    public String getText() {
        return cleanText;
    }

    public String getUrl() {
        return url;
    }

    public List<Pair<String,String>> getLinks() {
        return links;
    }

    protected void setLinks(List<Pair<String, String>> links) {
		this.links = links;
	}

    public String getTitle() {
        return title;
    }
    
    public Document getDocument() {
		return document;
	}

	protected void setDocument(Document document) {
		this.document = document;
	}

    public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

    public static URI getURI(String url) throws URISyntaxException {
    	URI uri = null;
    	url = url.trim();
    	if (url.startsWith("file:") || url.startsWith("javascript:")) {
    		logger.debug("Can't handle url: "+url);
    	} else {
    		int p = url.indexOf('?');
    		if (p < 0) {
    			try {
    				uri = new URI(url.replace(" ", "%20"));
    			} catch (java.net.URISyntaxException e) {
    				logger.debug("Malformed URI: "+url);
    			}
    		} else {
    			String base, query;
    			int q = url.lastIndexOf('#');
    			if (q < 0) q = url.length();
    			if (p < q) { 
    				base = url.substring(0,p+1);
    				query = url.substring(p+1,q);
    			} else {
    				base = url.substring(0,q)+"?";
    				query = url.substring(p+1);
    			}            
    			// Encode any space in the url. Can't use a url encoder because it would encode stuff like '/' and ':'.
    			base = base.replace(" ", "%20");
    			try {
    				// Re-encode the query part, to handle partially encoded urls.
    				query = java.net.URLEncoder.encode(java.net.URLDecoder.decode(query,"UTF-8"),"UTF-8");
    				query = query.replace("%3D","=").replace("%26","&");
    			} catch (java.io.UnsupportedEncodingException e) {
    				logger.debug("encoding a url", e);
    			}
    			url = base + query;
    			uri = new URI(url);
    		}
    	}
        return uri;
    }

}