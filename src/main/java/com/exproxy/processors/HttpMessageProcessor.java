/*
 * HttpMessageProcessor.java
 *
 * Created on 6 juillet 2005, 12:27
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.processors;

import com.exproxy.*;



/**
 * Process requests before they are sent and/or responses once they are received.
 * @author David Crosson
 */

public interface HttpMessageProcessor {
    /**
     * Process the given message. null can be returned if doSend is going to return false.
     * The message can be a request or a response; everything can be done on the message,
     * content changes, headers removed or added, ... You may return the given message or
     * create a new http message instance, the http message type (request or response) MUST
     * be preserved.
     * @param input HttpMessage to process (request or response)
     * @return The processed message
     */
    public HttpMessage process(HttpMessage input);
    
    /**
     * Shall we send this message ? If the message is a request it means
     * shall we send the message to the remote host ? If the message is a
     * response it means shall we send back the response to the user browser ?
     * If the processor returns false then no more processors will
     * be executed and the HttpMessage will be trashed.
     * @param input The message
     * @return true if we can send the message
     */

    public boolean doSend(HttpMessage input);

    /**
     * Shall we continue to run next processors ?
     * @param input The message
     * @return true to allow exproxy to continue with other processors; false
     * to break processors execution.
     */
    public boolean doContinue(HttpMessage input);
    
}
