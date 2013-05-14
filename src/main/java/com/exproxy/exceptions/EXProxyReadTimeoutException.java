/*
 * EXProxyReadTimeoutException.java
 *
 * Created on 23 août 2005, 14:46
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.exceptions;



/**
 * Raised when a read timeout occured while reading a message
 * @author David Crosson
 */
public class EXProxyReadTimeoutException extends EXProxyException {
    
    /**
     * Creates a new instance of <code>EXProxyReadTimeoutException</code> without detail message.
     */
    public EXProxyReadTimeoutException() {
    }
    
    
    /**
     * Constructs an instance of <code>EXProxyReadTimeoutException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public EXProxyReadTimeoutException(String msg) {
        super(msg);
    }
}
