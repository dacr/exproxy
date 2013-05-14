/*
 * HttpMessageResponse.java
 *
 * Created on 11 août 2005, 09:49
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy;

import java.util.HashMap;
import java.util.Map;



/**
 * The Response HTTP Message class.
 * @author David Crosson
 */
public class HttpMessageResponse extends HttpMessage {
    private int    statusCode;
    private String reasonPhrase;
    
    private static final Map<Integer, String> standardReasonPhrases;
    
    static {
        standardReasonPhrases = new HashMap<Integer, String>();
        standardReasonPhrases.put(new Integer(100), "Continue");
        standardReasonPhrases.put(new Integer(101), "Switching Protocols");
        standardReasonPhrases.put(new Integer(200), "OK");
        standardReasonPhrases.put(new Integer(201), "Created");
        standardReasonPhrases.put(new Integer(202), "Accepted");
        standardReasonPhrases.put(new Integer(203), "Non-Authoritative Information");
        standardReasonPhrases.put(new Integer(204), "No Content");
        standardReasonPhrases.put(new Integer(205), "Reset Content");
        standardReasonPhrases.put(new Integer(206), "Partial Content");
        standardReasonPhrases.put(new Integer(300), "Multiple Choices");
        standardReasonPhrases.put(new Integer(301), "Moved Permanently");
        standardReasonPhrases.put(new Integer(302), "Found");
        standardReasonPhrases.put(new Integer(303), "See Other");
        standardReasonPhrases.put(new Integer(304), "Not Modified");
        standardReasonPhrases.put(new Integer(305), "Use Proxy");
        standardReasonPhrases.put(new Integer(307), "Temporary Redirect");
        standardReasonPhrases.put(new Integer(400), "Bad Request");
        standardReasonPhrases.put(new Integer(401), "Unauthorized");
        standardReasonPhrases.put(new Integer(402), "Payment Required");
        standardReasonPhrases.put(new Integer(403), "Forbidden");
        standardReasonPhrases.put(new Integer(404), "Not Found");
        standardReasonPhrases.put(new Integer(405), "Method Not Allowed");
        standardReasonPhrases.put(new Integer(406), "Not Acceptable");
        standardReasonPhrases.put(new Integer(407), "Proxy Authentication Required");
        standardReasonPhrases.put(new Integer(408), "Request Time-out");
        standardReasonPhrases.put(new Integer(409), "Conflict");
        standardReasonPhrases.put(new Integer(410), "Gone");
        standardReasonPhrases.put(new Integer(411), "Length Required");
        standardReasonPhrases.put(new Integer(412), "Precondition Failed");
        standardReasonPhrases.put(new Integer(413), "Request Entity Too Large");
        standardReasonPhrases.put(new Integer(414), "Request-URI Too Large");
        standardReasonPhrases.put(new Integer(415), "Unsupported Media Type");
        standardReasonPhrases.put(new Integer(416), "Requested range not satisfiable");
        standardReasonPhrases.put(new Integer(417), "Expectation Failed");
        standardReasonPhrases.put(new Integer(500), "Internal Server Error");
        standardReasonPhrases.put(new Integer(501), "Not Implemented");
        standardReasonPhrases.put(new Integer(502), "Bad Gateway");
        standardReasonPhrases.put(new Integer(503), "Service Unavailable");
        standardReasonPhrases.put(new Integer(504), "Gateway Time-out");
        standardReasonPhrases.put(new Integer(505), "HTTP Version not supported");
    }
    
    /**
     * Creates a new Instance of HttpMessageResponse
     */
    public HttpMessageResponse() {
    }

    /**
     * Get response status code.
     * @return status code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Set response status code.
     * @param statusCode status code
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Get response reason Phrase.
     * @return reason phrase associated to status code
     */
    public String getReasonPhrase() {
        return reasonPhrase;
    }
    /**
     * Get reason phrase as expressed in RFC for 
     * the currentstatus code.
     * @return the reason phrase or null if not available
     */
    public String getRFCReasonPhrase() {
        return standardReasonPhrases.get(new Integer(getStatusCode()));
    }

    /**
     * Set response reason Phrase
     * @param reasonPhrase reason phrase associated to status code
     */
    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    /**
     * Computes the HTTP response start line.
     * @return response start line
     */
    public String getStartLine() {
        return String.format("%s/%s %d %s", 
                getProtocol(), 
                getVersion(), 
                getStatusCode(),
                getReasonPhrase());
    }
}
