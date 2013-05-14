/*
 * EXProxyUnknownHostException.java
 *
 * Created on 23 août 2005, 15:57
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.exceptions;



/**
 * Raised if an UnknownHostException occured
 * @author David Crosson
 */
public class EXProxyUnknownHostException extends EXProxyException {
    
    /**
     * Creates a new instance of <code>EXProxyUnknownHostException</code> without detail message.
     */
    public EXProxyUnknownHostException() {
    }
    
    
    /**
     * Constructs an instance of <code>EXProxyUnknownHostException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public EXProxyUnknownHostException(String msg) {
        super(msg);
    }
}
