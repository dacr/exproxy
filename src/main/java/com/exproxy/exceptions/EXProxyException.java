/*
 * EXProxyException.java
 *
 * Created on 23 août 2005, 14:44
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.exceptions;

/**
 * Generic Exception
 * @author David Crosson
 */
public class EXProxyException extends java.lang.Exception {
    
    /**
     * Creates a new instance of <code>EXProxyException</code> without detail message.
     */
    public EXProxyException() {
    }
    
    
    /**
     * Constructs an instance of <code>EXProxyException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public EXProxyException(String msg) {
        super(msg);
    }
    
    /**
     * Get exception message (prefix the message with instance class name)
     * @return the message
     */
    public String getMessage() {
        String msg = getClass().getName();
        if (super.getMessage() != null) {
            msg+=" : "+super.getMessage();
        }
        return msg;
    }
}
