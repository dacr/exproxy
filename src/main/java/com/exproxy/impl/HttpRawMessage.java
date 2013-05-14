/*
 * ReqRes.java
 *
 * Created on 14 juin 2005, 10:21
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.impl;

import com.exproxy.exceptions.EXProxyProtocolException;
import com.exproxy.HttpMessageBody;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.exproxy.HttpMessageMethod;


/**
 * Http message model.
 * @author David Crosson
 */
class HttpRawMessage implements HttpMessageBody {
    /**
     * Connection header string constant value.
     */
    static public final String CONNECTION           ="connection";          // keep-alive | close
    /**
     * Proxy-Connection header string constant value.
     */
    static public final String PROXY_CONNECTION     ="proxy-connection";    // keep-alive | close
    /**
     * Keep-Alive header string constant value.
     */
    static public final String KEEP_ALIVE           ="keep-alive";          // 300
    /**
     * User-Agent header string constant value.
     */
    static public final String USER_AGENT           ="user-agent";          // Mozilla/5.0 (X11; U; Linux i686; fr-FR; rv:1.7.6) Gecko/20050318 Firefox/1.0.2
    /**
     * Accept header string constant value.
     */
    static public final String ACCEPT               ="accept";              // image/png,*/*;q=0.5
    /**
     * Accept-Language header string constant value.
     */
    static public final String ACCEPT_LANGUAGE      ="accept-language";     // fr,fr-fr;q=0.8,en-us;q=0.5,en;q=0.3
    /**
     * Accept-Encoding header string constant value.
     */
    static public final String ACCEPT_ENCODING      ="accept-encoding";     // gzip, deflate
    /**
     * Accept-Charset header string constant value.
     */
    static public final String ACCEPT_CHARSET       ="accept-charset";      // ISO-8859-1,utf-8;q=0.7,*;q=0.7

    /**
     * Date header string constant value.
     */
    static public final String DATE                 ="date";                // Fri, 10 Jun 2005 13:33:39 GMT
    /**
     * Server header string constant value.
     */
    static public final String SERVER               ="server";              // Apache
    /**
     * Content-Type header string constant value.
     */
    static public final String CONTENT_TYPE         ="content-type";        // text/html
    /**
     * Content-Length header string constant value.
     */
    static public final String CONTENT_LENGTH       ="content-length";      // 200
    /**
     * Transfer-Encoding header string constant value.
     */
    static public final String TRANSFER_ENCODING    ="transfer-encoding";   // 
    
    /**
     * Cookie header string constant value.
     */
    static public final String COOKIE               ="cookie";
    /**
     * Set-Cookie header string constant value.
     */
    static public final String SET_COOKIE           ="set-cookie";

    private String charset = "ISO-8859-1";
    private Logger log = Logger.getLogger(getClass().getName());

    private String                method        = null; // Deduced from startLine
    private HttpMessageMethod methodType    = null; // Deduced from startLine
    private HttpRawMessageType       type          = null; // Deduced from startLine
    private int                   code          = -1;   // Deduced from startLine
    private String                codeMessage   = null; // Deduced from startLine
    private String                protocol      = null; // Deduced from startLine
    private String                dest          = null; // Deduced from startLine
    private String                host          = null; // Initialized by the engine 
    private int                   port          = -1;   // Initialized by the engine 

    private final Map<String, List<String>> headers = new HashMap<String, List<String>>();
    private byte[] content = new byte[0];


    /**
     * Creates an empty httpMessage
     */
    public HttpRawMessage() {
    }

    /**
     * Set http message headers.
     * @param rawHeaders a string containing headers in the same form as in raw http message
     */
    public void setHeaders(String rawHeaders) {
        headers.clear();
        addHeaders(rawHeaders);
    }
    /**
     * Add the given headers
     * @param rawHeaders a string containing headers in the same form as in raw http message
     */
    public void addHeaders(String rawHeaders) {
        if (rawHeaders == null) return;
        if (rawHeaders.length()==0) return;
        for(String header: rawHeaders.split("\\r?\\n")) {
            if (header.length()==0) continue;
            String[] parts = header.split(":", 2);
            String key=parts[0];
            String value=parts[1].trim();
            addHeader(key, value);
        }
    }
    /**
     * add a new header
     * @param key header name
     * @param value header value
     */
    public void addHeader(String key, String value) {
        key = key.toLowerCase().trim();
        if (!getHeaders().containsKey(key)) {
            getHeaders().put(key, new ArrayList<String>());
        }
        getHeaders().get(key).add(value);        
    }
    /**
     * Remove the given header
     * @param key header name
     */
    public void removeHeader(String key) {
        getHeaders().remove(key);
    }

    /**
     * get http message headers as a map whose keys are headernames and
     * value a list of values.
     * @return headers map.
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }
    /**
     * Get header values list.
     * @param key header name.
     * @return values list.
     */
    public List<String> getHeaderValues(String key) {
        return headers.get(key);
    }

    /**
     * Does this message contain a body ?
     * @return true if the message has a body.
     */
    public boolean hasMessageBody() {
        switch(type) {
            case REQUEST:
                // ------------------ RFC 2616 4.3 - Message body
                return headers.containsKey(CONTENT_LENGTH) || 
                        headers.containsKey(TRANSFER_ENCODING);
            case RESPONSE:
                // ------------------ RFC 2616 4.3 - Message body
                if (getCode()==204 || getCode()==304 ||
                        (getCode()>100 && getCode()<199)) return false;
                if (getMethodType() == HttpMessageMethod.HEAD) return false;

                return true;    
        }
        return false;
    }

    /**
     * Checks if body is using chunked transfer encoding
     * @return true if the body is encoded using the ChunkedTransferEncoding
     */
    public boolean hasChunkedTransferEncoding() {
        if (headers.containsKey(TRANSFER_ENCODING)) {
            List<String> values = getHeaderValues(TRANSFER_ENCODING);
            if (values.size() > 1) {
                log.warning("Transfer-Encoding has been defined several times, will take first value");
            }
            String transferEncoding = values.get(0).toLowerCase();
            if (! "identity".equals(transferEncoding)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Shall the connection be closed ?
     * @return true if the message asks the connection to be closed.
     */
    public boolean hasCloseConnection() {
        if (headers.containsKey(CONNECTION)) {
            List<String> values = getHeaderValues(CONNECTION);
            if (values.size() > 1) {
                log.warning("Connection has been defined several times, will take first value");
            }
            String connection = values.get(0).toLowerCase();
            if ("close".equals(connection)) {
                return true;
            }
        }
        return false;
    }

    /**
     * returns -1 if Chunked transfer OR end is reached with EOF
     * otherwise returns the number of bytes read
     * @return Message body length
     */
    public long getMessageBodyLength() {
        // ------------------ RFC 2616 4.4 - Message length - case 2
        if (headers.containsKey(TRANSFER_ENCODING)) {
            List<String> values = getHeaderValues(TRANSFER_ENCODING);
            if (values.size() > 1) {
                log.warning("Transfer-Encoding has been defined several times, will take first value");
            }
            String transferEncoding = values.get(0).toLowerCase();
            if (! "identity".equals(transferEncoding)) {
                return -1;
            }
        }
        // ------------------ RFC 2616 4.4 - Message length - case 3
        if (headers.containsKey(CONTENT_LENGTH)) {
            List<String> values = getHeaderValues(CONTENT_LENGTH);
            if (values.size() > 1) {
                log.warning("Content-Length has been defined several times, will take first value");
            }
            try {
                return Long.parseLong(values.get(0));
            } catch(NumberFormatException e) {
                log.severe("Couln't parse Content-Length numeric value, returning 0");
                return 0;
            }
        }
        return -1;
    }


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
     * Return a friendly representation of this object
     * @return HttpMessage string representation
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getHead());
        sb.append("\nhasMessageBody    = "+hasMessageBody());
        sb.append("\nMessageBodyLength = "+getMessageBodyLength());
        sb.append("\nChunkEncoding     = "+hasChunkedTransferEncoding());
        sb.append("\nConnectionClose   = "+hasCloseConnection());
        if (getContent().length > 0) {
            sb.append("\n\n"+getContent().length+" bytes of message body to follow...");
            //sb.append("\n\n"+new String(getContent()));
        }
        
        return sb.toString();
    }

    /**
     * return a ByteBuffer containing this HTTPMessage bytes
     * position is set to 0 and limit to the message length
     * @return ByteBuffer containing this message
     * @throws com.exproxy.EXProxyProtocolException Thrown if a charset encoding problem occurs
     */
    public ByteBuffer getData() throws EXProxyProtocolException {
        try {
            byte[] headBytes    = getHead().getBytes(charset);
            byte[] contentBytes = getContent();
            int dataLength = headBytes.length+contentBytes.length;
            ByteBuffer bb = ByteBuffer.allocate(dataLength);
            bb.put(headBytes);
            bb.put(contentBytes);
            bb.flip();
            return bb;
        } catch(UnsupportedEncodingException e) {
            log.log(Level.SEVERE, "Can't encode string to bytes", e);
            throw new EXProxyProtocolException("Unsupported Charset encoding operation");
        }
    }

    /**
     * Get content body.
     * @return Array of bytes containing http message content body.
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Set message content body.
     * @param content Array of bytes containg message body.
     */
    public void setContent(byte[] content) {
        this.content = content;
    }

    /**
     * Get the first line of this message
     * @return Http request or response first line.
     */
    public String getStartLine() {
        if (getType() == HttpRawMessageType.RESPONSE) {
            return String.format("%s %d %s", getProtocol(), getCode(), getCodeMessage());
        } else {
            return String.format("%s %s %s", getMethod(), getDest(), getProtocol());
        }
        
    }

    /**
     *  Set the first line of this message
     * @param startLine Http Request or Response first line
     * @throws com.exproxy.EXProxyProtocolException Thrown if the given first line for the message is not HTTP RFC compliant.
     */
    public void setStartLine(String startLine) throws EXProxyProtocolException {
        if (startLine.startsWith("HTTP/")) {
            // --------------------------- Il s'agit d'une réponse
            setType(HttpRawMessageType.RESPONSE);
            String parts[] = startLine.split("\\s+", 3);
            if (parts.length < 2) {
                throw new EXProxyProtocolException("Invalid response start line : "+startLine);
            }
            setProtocol(parts[0]);
            try {
                setCode(Integer.parseInt(parts[1]));
            } catch(NumberFormatException e) {
                String error = "Coudn't parse HTTP response code :"+parts[1];
                log.log(Level.SEVERE, error, e);
                throw new EXProxyProtocolException(error);
            }
            if (parts.length==3) {
                setCodeMessage(parts[2]);
            } else {
                setCodeMessage("");
            }
        } else {
            // --------------------------- Il s'agit d'une requête
            setType(HttpRawMessageType.REQUEST);
            String parts[] = startLine.split("\\s+");
            if (parts.length != 3) {
                throw new EXProxyProtocolException("Invalid request start line : "+startLine);
            }
            setMethod(parts[0]);
            setMethodType(HttpMessageMethod.valueOf(parts[0]));
            setDest(parts[1]);
            setProtocol(parts[2]);
        }
    }

    /**
     * Get the complete head part (first line followed by the headers) for this message.
     * @return String containing the message head part (without body)
     */
    public String getHead() {
        return String.format("%s\r\n%s\r\n", getStartLine(), getHeadersString());
    }

    /**
     * Get used http method if the message is a request
     * @return method identifier
     */
    public String getMethod() {
        return method;
    }

    /**
     * Set http request message method
     * @param method HTTP request method
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * get the HTTP Response message status code
     * @return http return code
     */
    public int getCode() {
        return code;
    }

    /**
     * set the HTTP Response message status code
     * @param code http return code
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * get the HTTP Response message status code message
     * @return code message
     */
    public String getCodeMessage() {
        return codeMessage;
    }

    /**
     * set the HTTP Response message status code
     * @param codeMessage code message
     */
    public void setCodeMessage(String codeMessage) {
        this.codeMessage = codeMessage;
    }

    /**
     * Get protocol
     * @return protocol string
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Set protocol
     * @param protocol protocol string
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Get Requested URI
     * @return uri
     */
    public String getDest() {
        return dest;
    }

    /**
     * Set Requested URI
     * @param dest uri
     */
    public void setDest(String dest) {
        this.dest = dest;
    }

    /**
     * Get message type (request or response)
     * @return message type
     */
    public HttpRawMessageType getType() {
        return type;
    }

    /**
     * Set message type (request or response)
     * @param type message type
     */
    public void setType(HttpRawMessageType type) {
        this.type = type;
    }

    /**
     * Get used http method type if the message is a request
     * @return method type identifier
     */
    public HttpMessageMethod getMethodType() {
        return methodType;
    }

    /**
     * Set http request message method type
     * @param methodType method type identifier
     */
    public void setMethodType(HttpMessageMethod methodType) {
        this.methodType = methodType;
    }

    /**
     * Get request host (both for request or response)
     * @return host name (IP or DNS name)
     */
    public String getHost() {
        return host;
    }

    /**
     * Set request host (both for request or response)
     * @param host host name (IP or DNS name)
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Get request port (both for request or response)
     * @return port
     */
    public int getPort() {
        return port;
    }

    /**
     *  Set request port (both for request or response)
     * @param port port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Get content body stream.
     * @return Array of bytes containing http message content body.
     */
    public java.io.ByteArrayInputStream getContentStream() {
        return new ByteArrayInputStream(getContent());
    }

}
