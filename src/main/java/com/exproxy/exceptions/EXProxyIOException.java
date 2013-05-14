/*
 * EXProxyIOException.java
 *
 * Created on 23 août 2005, 14:48
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.exceptions;



/**
 * Raised if an IOException occured
 * @author David Crosson
 */
public class EXProxyIOException extends EXProxyException {
    
    /**
     * Creates a new instance of <code>EXProxyIOException</code> without detail message.
     */
    public EXProxyIOException() {
    }
    
    
    /**
     * Constructs an instance of <code>EXProxyIOException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public EXProxyIOException(String msg) {
        super(msg);
    }
}
