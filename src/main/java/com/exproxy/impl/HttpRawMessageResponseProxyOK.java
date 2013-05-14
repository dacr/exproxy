/*
 * ResponseProxyError.java
 *
 * Created on 14 juin 2005, 11:29
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.impl;

import com.exproxy.exceptions.EXProxyProtocolException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



/**
 *
 * @author David Crosson
 */
class HttpRawMessageResponseProxyOK extends HttpRawMessage {
    
    /** Creates a new instance of ResponseProxyError */
    public HttpRawMessageResponseProxyOK() throws EXProxyProtocolException {
        setStartLine("HTTP/1.1 200 OK");
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.UK);
        addHeader(DATE, sdf.format(new Date()));
        addHeader(SERVER, "EProxy 0.8");
        addHeader(CONTENT_LENGTH, "0");
    }
}
