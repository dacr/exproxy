/*
 * EXProxyListener.java
 *
 * Created on 26 août 2005, 10:31
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.listeners;

/**
 * Listener to receive EXProxy server events such as
 * new client connection.
 * @author David Crosson
 */
public interface EXProxyListener {
    /**
     * New client connection.
     * @param host client host
     * @param port client port
     */
    public void connectionStart(String host, int port);
    
    /**
     * Client connection finishes.
     * @param host client host
     * @param port client port
     */
    public void connectionEnd(String host, int port);
}
