/*
 * HttpMessageRequest.java
 *
 * Created on 11 août 2005, 09:49
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy;

import java.net.URI;

/**
 * The Request HTTP Message class.
 *
 * @author David Crosson
 */
public class HttpMessageRequest extends HttpMessage {
    private HttpMessageMethod method;
    private String uri;

    /**
     * Creates a new instance of HttpMessageRequest.
     */
    public HttpMessageRequest() {
    }

    /**
     * The HTTP method used for this request.
     * @return HTTP Method
     */
    public HttpMessageMethod getMethod() {
        return method;
    }

    /**
     * Set the HTTP method to use for this request.
     * @param method HTTP method
     */
    public void setMethod(HttpMessageMethod method) {
        this.method = method;
    }

    /**
     * Get the requested URI.
     * @return uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * Set the requested URI.
     * @param uri uri
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Computes the HTTP request start line.
     * @return request start line
     */
    public String getStartLine() {
        return String.format("%s %s %s/%s",
                getMethod().toString(),
                getUri().toString(),
                getProtocol(), 
                getVersion());
    }
}
