/*
 * HttpMessageListener.java
 *
 * Created on 6 juillet 2005, 12:18
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.listeners;

import com.exproxy.exceptions.EXProxyException;
import com.exproxy.*;


/**
 * Listen requests before they are sent, and/or
 * responses with associated requests once they are received.
 * @author David Crosson
 */
public interface HttpMessageListener {

    /**
     * get just received HTTP request message, it is called before
     * this request is sent. Processing must be achieved very quickly.
     * @param request Received request message
     */
    public void received(HttpMessageRequest request);

    /**
     * Fail to get a request.
     * @param cause describes what happened
     */
    public void failed(EXProxyException cause);
    
    /**
     * Fail to get a response for the given request.
     * @param request Received request message
     * @param cause describes what happened
     */
    public void failed(HttpMessageRequest request, EXProxyException cause);

    /**
     * Fail to send the given response for the given request.
     * @param response Received response message
     * @param request Received request message
     * @param cause describes what happened
     */
    public void failed(HttpMessageResponse response, HttpMessageRequest request, EXProxyException cause);

    
    /**
     * get just received HTTP response Message for the associated request message.
     * This method is called once the entire message body is read.
     * Processing must be achieved very quickly.
     * @param response Received response
     * @param request Associated response request.
     */
    public void received(HttpMessageResponse response, HttpMessageRequest request);
}
