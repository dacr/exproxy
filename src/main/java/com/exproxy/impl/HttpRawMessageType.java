/*
 * HttpMessageType.java
 *
 * Created on 15 juin 2005, 14:55
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.impl;

/**
 * HttpMessage are Request or Response
 * @author David Crosson
 */
enum HttpRawMessageType {
    /**
     * For http request message
     */
    REQUEST,
    /**
     * For http response message
     */
    RESPONSE
}
