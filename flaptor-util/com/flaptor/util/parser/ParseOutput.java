/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.flaptor.util.parser;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.flaptor.util.Execute;
import com.flaptor.util.Pair;


/**
 * The result of the parser is stored in an object of this class.
 * It contains the extracted text, the title and the outlinks.
 */
public class ParseOutput {
    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private String text;
    private List<Pair<String,String>> links;
    private String title = "";
    private String url = null;
    private URI baseUri = null;
    // map field - content
    private Map<String,StringBuffer> fields;
    public static final String CONTENT = "PARSER_CONTENT_FIELD";

    public ParseOutput(String url) throws URISyntaxException {
        this.url = url;
        if (url.length() > 0) {
            baseUri = getURI(url);
        }
        links = new ArrayList<Pair<String,String>>();
        fields = new HashMap<String,StringBuffer>();
        fields.put(CONTENT,new StringBuffer());
    }

    public void addFieldString(String field, String str) {
        // check that the str is valid.
        if (null == str || "".equals(str)) { 
            logger.debug("field " + field + " is empty");
            return;
        }

        // So, find field.
        StringBuffer buffer = fields.get(field);
        if (null == buffer) {
            buffer = new StringBuffer();
            fields.put(field,buffer);
        } 
        str = collapseWhiteSpace(str);
        if (str.length() > 0) {
            if (buffer.length() > 0) buffer.append(' ');
            buffer.append(str.trim());
        }
    }

    public void addString(String str) {
        addFieldString(CONTENT,str);
    }


    public void addLink(String url, String anchor) throws URISyntaxException {
        URI target = getURI(url);
        if (null != target) {
            if (null != baseUri) {
                if (baseUri.getPath() == null || baseUri.getPath().length() == 0) {
                    baseUri = baseUri.resolve(URI.create("/"));
                }
                target = baseUri.resolve(target);
            }
            links.add(new Pair<String,String>(target.toString(),anchor.trim()));
        }
    }

    public void setTitle(String title) {
        if (null != title) {
            this.title = title.trim();
        }
    }

    public void setBaseUrl(String baseUrl) throws URISyntaxException {
        baseUri = getURI(baseUrl);
    }

    public void close(){
        text = fields.get(CONTENT).toString();
        text = text.replaceAll("(\\.\\s)+", ". ");
        text = text.replaceAll("\\s\\.", ". ");           
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public List<Pair<String,String>> getLinks() {
        return links;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Gets the content of the given fieldname.
     * 
     * @param fieldName
     * @return The String content of fieldName if present,
     *         null otherwise.
     */
    public String getField(String fieldName) {
        StringBuffer sb = fields.get(fieldName);
        if (null != sb) { 
            return sb.toString();
        } else {
            return null;
        }
    }

            // This method tries to create an URI from a possibly malformed url.
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
    				query = query.replaceAll("%([^0-9])","%25$1");
    				query = query.replaceAll("%$","%25");
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


    /**
     * Replace all whitespace with one space character and trim.
     * This runs more than 4 times faster than 
     * <code>str.replaceAll("\\s+"," ").trim()</code>
     */
    private String collapseWhiteSpace(String str) {
        StringBuffer buf = new StringBuffer();
        boolean inspace = false;
        for (int n=0; n<str.length(); n++) {
            char ch = str.charAt(n);
            if (ch==' ' || ch=='\t' || ch=='\n' || ch=='\f' || ch=='\r' || ch==10 || ch==160) {  // 160 is &nbsp; in utf
                if (!inspace) {
                    buf.append(' ');
                    inspace = true;
                }
            } else {
                buf.append(ch);
                inspace = false;
            }
        }
        return buf.toString().trim();
    }


        
}

