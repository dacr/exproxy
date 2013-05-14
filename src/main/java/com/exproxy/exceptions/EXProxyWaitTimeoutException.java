/*
 * EXProxyWaitTimeoutException.java
 *
 * Created on 23 août 2005, 14:47
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.exceptions;



/**
 * Raised when waiting for the next message timeout is reached
 * @author David Crosson
 */
public class EXProxyWaitTimeoutException extends EXProxyException {
    
    /**
     * Creates a new instance of <code>EXProxyWaitTimeoutException</code> without detail message.
     */
    public EXProxyWaitTimeoutException() {
    }
    
    
    /**
     * Constructs an instance of <code>EXProxyWaitTimeoutException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public EXProxyWaitTimeoutException(String msg) {
        super(msg);
    }
}
