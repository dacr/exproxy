/*
 * HttpMessage.java
 *
 * Created on 11 août 2005, 09:48
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy;
import com.exproxy.exceptions.EXProxyProtocolException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * The Generic HTTP Message class. It provides common 
 * caracteristics of both HTTP Request and Response messages.
 * @author David Crosson
 */
public abstract class HttpMessage {
    private String charset = "ISO-8859-1";

    /**
     * Connection header string constant value.
     */
    static public final String HEADER_CONNECTION           ="connection";          // keep-alive | close
    /**
     * Proxy-Connection header string constant value.
     */
    static public final String HEADER_PROXY_CONNECTION     ="proxy-connection";    // keep-alive | close
    /**
     * Keep-Alive header string constant value.
     */
    static public final String HEADER_KEEP_ALIVE           ="keep-alive";          // 300
    /**
     * User-Agent header string constant value.
     */
    static public final String HEADER_USER_AGENT           ="user-agent";          // Mozilla/5.0 (X11; U; Linux i686; fr-FR; rv:1.7.6) Gecko/20050318 Firefox/1.0.2
    /**
     * Accept header string constant value.
     */
    static public final String HEADER_ACCEPT               ="accept";              // image/png,*/*;q=0.5
    /**
     * Accept-Language header string constant value.
     */
    static public final String HEADER_ACCEPT_LANGUAGE      ="accept-language";     // fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3
    /**
     * Accept-Encoding header string constant value.
     */
    static public final String HEADER_ACCEPT_ENCODING      ="accept-encoding";     // gzip, deflate
    /**
     * Accept-Charset header string constant value.
     */
    static public final String HEADER_ACCEPT_CHARSET       ="accept-charset";      // ISO-8859-1,utf-8;q=0.7,*;q=0.7
    /**
     * Date header string constant value.
     */
    static public final String HEADER_DATE                 ="date";                // Fri, 10 Jun 2005 13:33:39 GMT
    /**
     * Server header string constant value.
     */
    static public final String HEADER_SERVER               ="server";              // Apache
    /**
     * Content-Type header string constant value.
     */
    static public final String HEADER_CONTENT_TYPE         ="content-type";        // text/html
    /**
     * Content-Length header string constant value.
     */
    static public final String HEADER_CONTENT_LENGTH       ="content-length";      // 200
    /**
     * Transfer-Encoding header string constant value.
     */
    static public final String HEADER_TRANSFER_ENCODING    ="transfer-encoding";   //     
    /**
     * Cookie header string constant value.
     */
    static public final String HEADER_COOKIE               ="cookie";
    /**
     * Set-Cookie header string constant value.
     */
    static public final String HEADER_SET_COOKIE           ="set-cookie";
    

    private Map<String, List<String>> headers = new HashMap<String, List<String>>();
    private HttpMessageBody body;

    private String fromHost;
    private int fromPort;

    private String toHost;
    private int toPort;
    
    private String protocol;
    private String version;


    /**
     * Get message headers.
     * @return Map whose keys are header unique names and
     * whose values are a list of String with each
     * string corresponding to different occurences
     * of the header in the message.
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * Get header values.
     * @param header header name
     * @return values set by multiple occurence of this header 
     * in the message
     */
    public List<String> getHeaderValues(String header) {
        return headers.get(header);
    }

    /**
     * Set header values.
     * @param headers header values list. Each value will appear
     * in the message as follow :
     * <p><code>"headername: headervalue\n"</code></p>
     */
    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    /**
     * Get the host sending that message.
     * @return host
     */
    public String getFromHost() {
        return fromHost;
    }

    /**
     * Set the host sending that message.
     * @param fromHost host
     */
    public void setFromHost(String fromHost) {
        this.fromHost = fromHost;
    }

    /**
     * Get the host's port sending that message.
     * @return port number
     */
    public int getFromPort() {
        return fromPort;
    }

    /**
     * Set the host's port sending that message.
     * @param fromPort port number
     */
    public void setFromPort(int fromPort) {
        this.fromPort = fromPort;
    }

    /**
     * Get the host receiving that message.
     * @return host
     */
    public String getToHost() {
        return toHost;
    }

    /**
     * Set the host receiving that message.
     * @param toHost host
     */
    public void setToHost(String toHost) {
        this.toHost = toHost;
    }

    /**
     * Get the host's port receiving that message.
     * @return port number
     */
    public int getToPort() {
        return toPort;
    }

    /**
     * Set the host's port receiving that message.
     * @param toPort port number
     */
    public void setToPort(int toPort) {
        this.toPort = toPort;
    }

    /**
     * Get message protocol (i.e. HTTP).
     * @return protocol name
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Set message protocol.
     * @param protocol protocol name
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Get protocol version.
     * @return protocol version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set protocol version.
     * @param version Protocol version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Get the message body (the content) if one is available.
     * @return message body or null if no body available
     */
    public HttpMessageBody getBody() {
        return body;
    }

    /**
     * Set the message body (the content).
     * @param body message body
     */
    public void setBody(HttpMessageBody body) {
        this.body = body;
    }
    
    /**
     * Get HTTP Message start line
     * @return String containing the first line of the
     * HTTP request without \r or \n
     */
    abstract public String getStartLine();


    /**
     * get headers string in http format.
     * @return string containing headers
     */
    public String getHeadersString() {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, List<String>> entry: headers.entrySet()) {
            String key = entry.getKey();
            for(String value: entry.getValue()) {
                sb.append(key);
                sb.append(": ");
                sb.append(value);
                sb.append("\r\n");
            }
        }
        return sb.toString();
    }

    /**
     * return a ByteBuffer containing this HTTPMessage bytes
     * position is set to 0 and limit to the message length
     * @return ByteBuffer containing this message
     * @throws com.exproxy.exceptions.EXProxyProtocolException raised if an character encoding problem occured
     */
    public ByteBuffer getData() throws EXProxyProtocolException {
        try {
            String head = String.format("%s\r\n%s\r\n", getStartLine(), getHeadersString());
            byte[] headBytes    = head.getBytes(charset);
            byte[] contentBytes = getBody().getContent();
            int dataLength = headBytes.length+contentBytes.length;
            ByteBuffer bb = ByteBuffer.allocate(dataLength);
            bb.put(headBytes);
            bb.put(contentBytes);
            bb.flip();
            return bb;
        } catch(UnsupportedEncodingException e) {
            throw new EXProxyProtocolException("Unsupported Charset encoding operation");
        }
    }
    
}
