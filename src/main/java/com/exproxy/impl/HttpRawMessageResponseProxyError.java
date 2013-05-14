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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;



/**
 *
 * @author David Crosson
 */
class HttpRawMessageResponseProxyError extends HttpRawMessage {
    
    /** Creates a new instance of ResponseProxyError */
    public HttpRawMessageResponseProxyError(String message) throws EXProxyProtocolException {
        this(message, null);
    }
    public HttpRawMessageResponseProxyError(String message, Throwable t) throws EXProxyProtocolException {
        if (t!= null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw, true));
            message+="\n"+sw.getBuffer().toString();
        }
        message=message.replaceAll("\\n", "<br>");
        byte[] content = ("<html><body><h1>Error encountered by MiddleMan</h1>"+message+"</body></html>").getBytes();
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.UK);

        setStartLine("HTTP/1.1 501 Internal Proxy Error");
        addHeader(DATE, sdf.format(new Date()));
        addHeader(SERVER, "EProxy 1.0");
        addHeader(CONNECTION, "close");
        addHeader(CONTENT_LENGTH, Integer.toString(content.length));
        addHeader(CONTENT_TYPE, "text/html");
        setContent(content);
    }
}
