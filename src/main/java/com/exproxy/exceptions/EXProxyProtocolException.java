/*
 * EXProxyProtocolException.java
 *
 * Created on 23 août 2005, 14:55
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.exceptions;



/**
 * Raised if received message (request or response) doesn't conform to HTTP protocol.
 * @author David Crosson
 */
public class EXProxyProtocolException extends EXProxyException {
    
    /**
     * Creates a new instance of <code>EXProxyProtocolException</code> without detail message.
     */
    public EXProxyProtocolException() {
    }
    
    
    /**
     * Constructs an instance of <code>EXProxyProtocolException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public EXProxyProtocolException(String msg) {
        super(msg);
    }
}
