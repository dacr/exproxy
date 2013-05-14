/*
 * EXProxyInternalException.java
 *
 * Created on 23 août 2005, 15:12
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.exceptions;



/**
 * Raised when an Internal processing error occurred
 * @author David Crosson
 */
public class EXProxyInternalException extends EXProxyException {
    
    /**
     * Creates a new instance of <code>EXProxyInternalException</code> without detail message.
     */
    public EXProxyInternalException() {
    }
    
    
    /**
     * Constructs an instance of <code>EXProxyInternalException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public EXProxyInternalException(String msg) {
        super(msg);
    }
}
