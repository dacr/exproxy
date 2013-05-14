/*
 * Handler.java
 *
 * Created on 10 juin 2005, 14:01
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.impl;

import com.exproxy.listeners.EXProxyListener;
import com.exproxy.listeners.HttpMessageListener;
import com.exproxy.processors.HttpMessageProcessor;
import com.exproxy.EXProxy;
import com.exproxy.exceptions.EXProxyException;
import com.exproxy.exceptions.EXProxyIOException;
import com.exproxy.exceptions.EXProxyInternalException;
import com.exproxy.exceptions.EXProxyProtocolException;
import com.exproxy.exceptions.EXProxyReadTimeoutException;
import com.exproxy.exceptions.EXProxyUnknownHostException;
import com.exproxy.exceptions.EXProxyUnresolvedAddressException;
import com.exproxy.exceptions.EXProxyWaitTimeoutException;
import com.exproxy.HttpMessage;
import com.exproxy.tools.ChannelManager;
import com.exproxy.tools.CoolX509TrustManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import com.exproxy.tools.SSLByteChannel;
import java.net.URISyntaxException;
import com.exproxy.HttpMessageMethod;
import com.exproxy.HttpMessageRequest;
import com.exproxy.HttpMessageResponse;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.List;


/**
 *
 * @author David Crosson
 */
public class Handler implements Runnable {
    static private final Pattern rqSeparator = Pattern.compile("\\r?\\n\\r?\\n");
    static private final Pattern rqPattern = Pattern.compile("^(\\w+) +(.+) +(http.+) *\\r?\\n((?:.+\\r?\\n)*)\\r?\\n", Pattern.CASE_INSENSITIVE/*+Pattern.MULTILINE*/);

    private final Logger log = Logger.getLogger(Handler.class.getName());

    private final SocketChannel socketChannel;
    private final EXProxy exproxy;
    private ChannelManager channelManager;
    
    private String serverHost;
    private int    serverPort;
    
    private String clientHost;
    private int    clientPort;

    private final String charset = "ISO-8859-1";
    private final int readTimeout  = 300; // en secondes
    private final int waitTimeout  = 300; // en secondes
    private final int readLatency  = 10; // en millisecondes / Si rien à lire, alors on dort un peu
    private final int writeLatency = 10; // en millisecondes / Si rien à écrire, alors on dort un peu


    /**
     * Creates a new instance of Handler
     */
    public Handler(SocketChannel socketChannel, EXProxy exproxy) {
        this.socketChannel = socketChannel;
        this.exproxy = exproxy;
    }

    /**
     * Thread starting point
     */
    public void run() {
        this.channelManager = new ChannelManager();
        this.serverHost = exproxy.getIfAddress().toString();
        this.serverPort = exproxy.getPort();
        this.clientHost = socketChannel.socket().getInetAddress().getHostAddress();
        this.clientPort = socketChannel.socket().getPort();

        if (log.isLoggable(Level.INFO)) {
            log.info("Handler starts for "+getClientHost()+":"+getClientPort());
        }
        
        for(EXProxyListener listener: getExproxy().getListeners()) {
            listener.connectionStart(getClientHost(), getClientPort());
        }
        
        try {
            getSocketChannel().configureBlocking(false);
        } catch(IOException e) {
            log.log(Level.SEVERE, "Can't configure blocking mode to false", e);
            return;
        }

        try {
            processingLoop(getSocketChannel(), null);
        } catch(InterruptedIOException e) {
        } catch(InterruptedException e) {
        } finally {
            for(EXProxyListener listener: getExproxy().getListeners()) {
                listener.connectionEnd(getClientHost(), getClientPort());
            }
            if (log.isLoggable(Level.INFO)) {
                log.info("Handler finishes for "+getClientHost()+":"+getClientPort());
            }
        }
    }

    /**
     *
     */
    private void notifyFailure(EXProxyException e) {
        notifyFailure(null, null, e);
    }
    
    /**
     *
     */
    private void notifyFailure(HttpMessageRequest request, EXProxyException e) {
        notifyFailure(null, request, e);
    }

    /**
     *
     */
    private void notifyFailure(HttpMessageResponse response, HttpMessageRequest request, EXProxyException cause) {
        synchronized(getExproxy().getRequestListeners()) {
            for(HttpMessageListener l: getExproxy().getRequestListeners()) {
                if (request != null) {
                    if (response != null) {
                        l.failed(response, request, cause);
                    } else {
                        l.failed(request, cause);
                    }
                } else {
                    l.failed(cause);
                }
            }
        }
    }

    /**
     * Main loop : Read request, process request, send back response to the client
     */
    private void processingLoop(ByteChannel source, URL context) throws InterruptedIOException, InterruptedException {
        try {
            BufferedReader in;
            if (log.isLoggable(Level.FINE)) {
                log.fine("Entering processing loop, context="+context);
            }
            HttpRawMessage rawRequest, rawResponse;
            HttpMessageRequest request;
            HttpMessageResponse response;
            boolean doSend;
            while(true) {
                
                // -------------- 1. Read next request --------------
                try {
                    rawRequest = readRawHttpMessage(source, readTimeout, waitTimeout);
                } catch(EXProxyException e) {
                    log.log(Level.WARNING, e.getMessage(), e);
                    notifyFailure(e);
                    break;
                }
                if (rawRequest == null) break;
                if (!source.isOpen()) break;
                
                if (rawRequest.getMethodType() == HttpMessageMethod.CONNECT) {
                    try {
                        processRequestConnect(source, rawRequest, context);
                    } catch(EXProxyException e) {
                        log.log(Level.WARNING, "EXProxyException", e);
                        notifyFailure(e);
                        try {
                            sendReqres(source, new HttpRawMessageResponseProxyError(e.getMessage()));
                        } catch(EXProxyException e2) {
                            log.log(Level.SEVERE, "Was unable to send the error http message", e2);
                        }
                    } catch(MalformedURLException e) {
                        String errmsg = String.format("BAD URL FOR CONNECT METHOD : %s context=%s",rawRequest.getDest(), context);
                        log.log(Level.SEVERE, errmsg, e);
                        EXProxyException exp = new EXProxyProtocolException(errmsg);
                        notifyFailure(exp);
                        try {
                            sendReqres(source, new HttpRawMessageResponseProxyError(exp.getMessage()));
                        } catch(EXProxyException e2) {
                            log.log(Level.SEVERE, "Was unable to send the error http message", e2);
                        }
                    }
                    break;
                }

                // -------------- 2. correct message (from rawRequest URL and/or current context URL deduce proxied host and port)
                try {
                    correctRequestMessage(rawRequest, context);
                } catch(MalformedURLException e) {
                    log.log(Level.SEVERE, "BAD URL : "+rawRequest.getDest()+" context="+context+" request="+new String(rawRequest.getContent()), e);
                    break;
                }
                
                if (log.isLoggable(Level.FINER)) {
                    log.log(Level.FINER,
                            "\n"  +rawRequest.getStartLine()+
                            "\n"+rawRequest.getHeadersString()
                            );
                }

                
                request = (HttpMessageRequest)getHttpMessageFromRawHttpMessage(rawRequest);

                // -------------- 3. Apply request processors --------------
                doSend=true;
                synchronized(getExproxy().getRequestProcessors()) {
                    for(HttpMessageProcessor p: getExproxy().getRequestProcessors()) {
                        if (!p.doSend(request)) {
                            doSend=false;
                            break;
                        }
                        request = (HttpMessageRequest)p.process(request);
                        if (!p.doContinue(request)) {
                            break;
                        }
                    }
                }
                if (!doSend) {
                    // TODO : Il vaudrait mieux retourner une page d'erreur indiquant qu'on a été filtré
                    break;
                }

                // -------------- 4. Apply request listeners --------------
                synchronized(getExproxy().getRequestListeners()) {
                    for(HttpMessageListener l: getExproxy().getRequestListeners()) {
                        l.received(request);
                    }
                }

                // -------------- 5. Send request and get response --------------
                try {
                    rawResponse = processRequestDefault(source, request, context);
                } catch(EXProxyException e) {
                    log.log(Level.WARNING, "EXProxyException", e);
                    notifyFailure(request, e);
                    try {
                        sendReqres(source, new HttpRawMessageResponseProxyError(e.getMessage()));
                    } catch(EXProxyException e2) {
                        log.log(Level.SEVERE, "Was unable to send the error http message", e2);
                    }
                    break;
                }

                if (rawResponse == null) break;

                
                if (log.isLoggable(Level.FINER)) {
                    log.log(Level.FINER,
                            "\n"  +rawResponse.getStartLine()+
                            "\n"+rawResponse.getHeadersString()
                            );
                }
                
                
                response = (HttpMessageResponse)getHttpMessageFromRawHttpMessage(rawResponse);

                // -------------- 6. Apply response processors
                doSend=true;
                synchronized(getExproxy().getResponseProcessors()) {
                    for(HttpMessageProcessor p: getExproxy().getResponseProcessors()) {
                        if (!p.doSend(response)) {
                            doSend=false;
                            break;
                        }
                        response = (HttpMessageResponse)p.process(response);
                        if (!p.doContinue(response)) {
                            break;
                        }
                    }
                }
                if (!doSend) {
                    // TODO : Il vaudrait mieux retourner une page d'erreur indiquant qu'on a été filtré
                    break;
                }

                // -------------- 7. Apply response listeners --------------
                synchronized(getExproxy().getResponseListeners()) {
                    for(HttpMessageListener l: getExproxy().getResponseListeners()) {
                        l.received(response, request);
                    }
                }
                // -------------- 8. Send response --------------
                try {
                    sendReqres(source, response);
                    if (rawResponse.hasCloseConnection()) {
                        break;
                    }
                } catch(EXProxyException e) {
                    log.log(Level.WARNING, "EXProxyProtocolException", e);
                    notifyFailure(response, request, e);
                    try {
                        sendReqres(source, new HttpRawMessageResponseProxyError(e.getMessage()));
                    } catch(EXProxyException e2) {
                        log.log(Level.SEVERE, "Was unable to send the error http message", e2);
                    }
                    break;                    
                }
            }
        } finally {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Exiting processing loop, context="+context);
            }
            try {
                getSocketChannel().socket().close();
            } catch(IOException e) {
                log.log(Level.WARNING, "IOException during socket close operation", e);
            }
            channelManager.close();
            
            readBuffers.clear();
        }
    }


    enum HttpMessageReadState {
        NOTHING_READ,
        READING_STARTLINE,
        READING_HEADERS,
        READING_BODY,
        READING_BODY_CHUNK,
        READING_BODY_CHUNK_TRAILER,
        FINISHED
    };
    
    // Mémorisation des buffers, car des caractères recues en trop
    // seront traités lors du prochain appel pour prendre en compte
    // la requête ou la réponse suivante suivante.
    private Map<ByteChannel, ByteBuffer> readBuffers = new HashMap<ByteChannel, ByteBuffer>();
    
    /**
     * Read an HTTP message on the given input stream
     * @param channel where to read the HTTP message
     * @param readTimeout read message timeout in seconds (Used for keepAlive)
     * @param waitTimeout maximum amount of time waiting for the next message
     * @throws java.io.InterruptedIOException 
     * @throws java.lang.InterruptedException 
     * @throws com.exproxy.EXProxyProtocolException 
     * @return The message read
     */
    private HttpRawMessage readRawHttpMessage(ByteChannel channel, int readTimeout, int waitTimeout) throws InterruptedIOException, InterruptedException, EXProxyException  {
        
        HttpRawMessage message = new HttpRawMessage();
        HttpMessageReadState state = HttpMessageReadState.NOTHING_READ;

        
        ByteBuffer readBuffer;
        if (readBuffers.containsKey(channel)) {
            readBuffer = readBuffers.get(channel);
        } else {
            readBuffer = ByteBuffer.allocate(256*1024);
            readBuffers.put(channel,  readBuffer);
        }
        
        ByteBuffer startLineByteBuffer  = ByteBuffer.allocate(64*1024);
        ByteBuffer headersByteBuffer    = ByteBuffer.allocate(64*1024);
        ByteBuffer bodyByteBuffer       = ByteBuffer.allocate(64*1024);
        ByteBuffer chunkedStatusBuffer  = ByteBuffer.allocate(64*1024);
        ByteBuffer chunkedTrailerBuffer = ByteBuffer.allocate(64*1024);
        CharsetDecoder decoder = Charset.forName(charset).newDecoder();

        byte[]   body;
        int      endOfLineCount=0;
        long     contentBytesToRead=-1;
        boolean  eof=false;
        long     nothingHasBeenDoneTime=System.currentTimeMillis();
        try {
            while(state != HttpMessageReadState.FINISHED) {
                int l = channel.read(readBuffer);
                if (l==-1) {
                    eof=true;
                }
                if (l==0) {
                    Thread.sleep(readLatency);
                    if (System.currentTimeMillis() - nothingHasBeenDoneTime >= readTimeout*1000) {
                        if (state != state.NOTHING_READ) {
                            if (log.isLoggable(Level.FINE)) {
                                log.fine("Read timeout reached while reading HTTP message : state="+state);
                            }
                            throw new EXProxyReadTimeoutException();
                        }
                    }
                    if (System.currentTimeMillis() - nothingHasBeenDoneTime >= waitTimeout*1000) {
                        if (state == state.NOTHING_READ) {
                            if (log.isLoggable(Level.FINE)) {
                                log.fine("Wait timeout reached while waiting for a HTTP message :");
                            }
                            throw new EXProxyWaitTimeoutException();
                        }
                    }
                } 
                if (l > 0) {
                    nothingHasBeenDoneTime=System.currentTimeMillis();
                }
                readBuffer.flip();
                
                int readBufferAvailableBytes = readBuffer.limit();

                switch(state) {
                    // -----------------------------------------------------
                    case NOTHING_READ:
                        while(readBuffer.hasRemaining()) {
                            byte b = readBuffer.get();
                            // Ignoring any empty lines before startLine
                            if (b == '\r' || b == '\n') continue;
                            readBuffer.position(readBuffer.position()-1);
                            state = HttpMessageReadState.READING_STARTLINE;
                            endOfLineCount=0;
                            break;
                        }
                    // -----------------------------------------------------
                    case READING_STARTLINE:
                        while(readBuffer.hasRemaining()) {
                            byte b = readBuffer.get();
                            if (b=='\r') continue;
                            if (b=='\n') {
                                if (log.isLoggable(Level.FINEST)) {
                                    log.finest("HTTP Protocol StartLine read");
                                }
                                try {
                                    startLineByteBuffer.flip();
                                    CharBuffer startLine = decoder.decode(startLineByteBuffer);
                                    message.setStartLine(startLine.toString()); // limit and position are OK
                                } catch(CharacterCodingException e) {
                                    String error = "Character decoding error within startLine";
                                    log.log(Level.SEVERE, error, e);
                                    throw new EXProxyProtocolException(error);
                                }
                                state = HttpMessageReadState.READING_HEADERS;
                                endOfLineCount=1;
                                break;
                            } else {
                                try {
                                    startLineByteBuffer.put(b);
                                } catch(BufferOverflowException e) {
                                    String error = "BufferOverFlow while reading startLine";
                                    log.log(Level.SEVERE, error, e);
                                    throw new EXProxyProtocolException(error);
                                }
                            }
                        }
                    // -----------------------------------------------------
                    case READING_HEADERS: 
                        // ****** ATTENTION SI CORRECTION ICI ALORS CORRECTION DANS READING_BODY_CHUNK_TRAILER ***************
                        while(readBuffer.hasRemaining()) {
                            byte b = readBuffer.get();
                            if (b =='\n') endOfLineCount++;
                            else if (b != '\r') endOfLineCount=0;
                            
                            try {
                                headersByteBuffer.put(b);
                            } catch(BufferOverflowException e) {
                                String error = "BufferOverFlow while reading headers";
                                log.log(Level.SEVERE, error, e);
                                throw new EXProxyProtocolException(error);
                            }

                            if (endOfLineCount ==2) {
                                if (log.isLoggable(Level.FINEST)) {
                                    log.finest("HTTP Protocol Headers read");
                                }
                                try {
                                    headersByteBuffer.flip();
                                    CharBuffer headers = decoder.decode(headersByteBuffer);
                                    message.setHeaders(headers.toString()); // limit and position are OK
                                } catch(CharacterCodingException e) {
                                    String error = "Character decoding error within headers";
                                    log.log(Level.SEVERE, error, e);
                                    throw new EXProxyProtocolException(error);
                                }
                                if (message.hasMessageBody()) {
                                    if (message.hasChunkedTransferEncoding()) {
                                        state = HttpMessageReadState.READING_BODY_CHUNK;
                                        contentBytesToRead = -1;
                                    } else {
                                        state = HttpMessageReadState.READING_BODY;
                                        contentBytesToRead = message.getMessageBodyLength();
                                    }
                                } else {
                                    state = HttpMessageReadState.FINISHED;
                                }
                                break;
                            }
                        }
                        break;
                    // -----------------------------------------------------
                    case READING_BODY:
                        if ((contentBytesToRead == 0) ||       // When content-length is known
                                (contentBytesToRead==-1 && eof==true && !readBuffer.hasRemaining())) {  // When connection-Close required 
                            if (log.isLoggable(Level.FINEST)) {
                                log.finest("HTTP Protocol content read");
                            }
                            bodyByteBuffer.flip();
                            byte[] data = stripByteBuffer(bodyByteBuffer).array();
                            message.setContent(data);
                            state = HttpMessageReadState.FINISHED;
                        } else {
                            while(readBuffer.hasRemaining()) {
                                if (bodyByteBuffer.remaining()==0) {
                                    if (log.isLoggable(Level.FINEST)) {
                                        log.finest("Increasing reading body capacity");
                                    }
                                    ByteBuffer nbb = ByteBuffer.allocate(bodyByteBuffer.capacity()+bodyByteBuffer.capacity()/2);
                                    bodyByteBuffer.flip();
                                    nbb.put(bodyByteBuffer);
                                    bodyByteBuffer = nbb;      
                                }
                                byte b = readBuffer.get();
                                try {
                                    bodyByteBuffer.put(b);
                                    if (contentBytesToRead > 0) contentBytesToRead--;
                                    if (contentBytesToRead == 0) break;
                                } catch(BufferOverflowException e) {
                                    // Can't arise as we have dynamically increased the buffer
                                }
                            }
                        }
                        break;
                    // -----------------------------------------------------
                    case READING_BODY_CHUNK:
                        if (contentBytesToRead==-1) {
                            // We don't known how many bytes to read
                            // Let's read the line giving us that information
                            while(readBuffer.hasRemaining()) {
                                byte b = readBuffer.get();
                                if (b=='\r') continue;
                                if (b=='\n' && chunkedStatusBuffer.position()==0) continue; // D'un chunk à l'autre nous avons : chunk CRLF status CRLF
                                if (b=='\n') {
                                    chunkedStatusBuffer.flip();
                                    CharBuffer chunkedStatus = decoder.decode(chunkedStatusBuffer);
                                    String parts[] = chunkedStatus.toString().split("\\s+", 2);
                                    String hexLengthValue = parts[0];
                                    try {
                                        contentBytesToRead = Integer.valueOf(hexLengthValue, 16).intValue();
                                    } catch(NumberFormatException e) {
                                        String error = "couldn't parse chunk hexadecimal length value";
                                        log.log(Level.SEVERE, error, e);
                                        throw new EXProxyProtocolException(error);
                                    }
                                    chunkedStatusBuffer.clear();
                                    if (log.isLoggable(Level.FINEST)) {
                                        log.finest("HTTP Protocol Chunk Statusline read : bytes to read now = "+contentBytesToRead+" ("+chunkedStatus.toString()+")");
                                    }
                                    break;
                                } else {
                                    try {
                                        chunkedStatusBuffer.put(b);
                                    } catch(BufferOverflowException e) {
                                        String error = "BufferOverFlow while reading Chunk statusLine";
                                        log.log(Level.SEVERE, error, e);
                                        throw new EXProxyProtocolException(error);
                                    }
                                }
                            }
                        }
                        if (contentBytesToRead == 0) {  // Chunk reading end
                            if (log.isLoggable(Level.FINEST)) {
                                log.finest("HTTP Protocol content read");
                            }
                            bodyByteBuffer.flip();
                            byte[] data = stripByteBuffer(bodyByteBuffer).array();
                            message.setContent(data);
                            // Cleaning message headers as we transform chunked transfert encoding to
                            // normal encoding : body reconstitution
                            message.removeHeader(HttpRawMessage.TRANSFER_ENCODING);
                            message.addHeader(HttpRawMessage.CONTENT_LENGTH, Integer.toString(data.length));

                            // Now after body chunks we may have new headers to append
                            state = HttpMessageReadState.READING_BODY_CHUNK_TRAILER;
                            endOfLineCount=1;
                        }
                        if (contentBytesToRead > 0) {
                            while(readBuffer.hasRemaining()) {
                                if (bodyByteBuffer.remaining()==0) {
                                    if (log.isLoggable(Level.FINEST)) {
                                        log.finest("Increasing reading body capacity");
                                    }
                                    ByteBuffer nbb = ByteBuffer.allocate(bodyByteBuffer.capacity()+bodyByteBuffer.capacity()/2);
                                    bodyByteBuffer.flip();
                                    nbb.put(bodyByteBuffer);
                                    bodyByteBuffer = nbb;      
                                }
                                byte b = readBuffer.get();
                                try {
                                    bodyByteBuffer.put(b);
                                    contentBytesToRead--;
                                    if (contentBytesToRead == 0) {
                                        // Next chunk of data
                                        contentBytesToRead=-1;
                                        break;
                                    }
                                } catch(BufferOverflowException e) {
                                    // Can't arise as we have dynamically increased the buffer
                                    String errmsg = "CAN'T ARISE AS WE HAVE DYNAMICALLY INCREASED THE BUFFER TO THE CORRECT SIZE";
                                    log.log(Level.SEVERE, errmsg);
                                    throw new EXProxyInternalException(errmsg);
                                }
                            }
                        }
                        break;
                    // -----------------------------------------------------
                    case READING_BODY_CHUNK_TRAILER:
                        while(readBuffer.hasRemaining()) {
                            byte b = readBuffer.get();
                            if (b =='\n') endOfLineCount++;
                            else if (b != '\r') endOfLineCount=0;
                            
                            try {
                                chunkedTrailerBuffer.put(b);
                            } catch(BufferOverflowException e) {
                                String error = "BufferOverFlow while reading chunk trailer headers";
                                log.log(Level.SEVERE, error, e);
                                throw new EXProxyProtocolException(error);
                            }

                            if (endOfLineCount ==2) {
                                if (log.isLoggable(Level.FINEST)) {
                                    log.finest("HTTP Protocol Chunk trailer Headers read");
                                }
                                chunkedTrailerBuffer.flip();
                                CharBuffer headersBuffer = decoder.decode(chunkedTrailerBuffer);
                                String headers = headersBuffer.toString();
                                message.addHeaders(headers); // limit and position are OK
                                state = HttpMessageReadState.FINISHED;
                                break;
                            }
                        }
                        break;
                    // -----------------------------------------------------
                    case FINISHED:
                        break;
                }




                if (eof==true && readBufferAvailableBytes==0) {
                    switch(state) {
                        case NOTHING_READ:
                            // No more message, connection has been closed
                            return null;
                        case FINISHED:
                            break;
                        default:
                            String errmsg = String.format(
                                    "HTTP Message truncated, EOF encountered :\n" +
                                    "***** ReadState *****\n%s\n" +
                                    "***** StartLine *****\n%s\n" +
                                    "***** Headers *****\n%s\n"+
                                    "***** Content read byte count *****\n%d\n",
                                    state.toString(),
                                    message.getStartLine(),
                                    message.getHeadersString(),
                                    bodyByteBuffer.position());
                            throw new EXProxyProtocolException(errmsg);
                    }
                }

                readBuffer.compact();
                
            }
        } catch(InterruptedException e) {
            if (log.isLoggable(Level.INFO)) {
                log.info("Handler interrupted");
            }
            throw e;
        } catch(InterruptedIOException e) {
            if (log.isLoggable(Level.INFO)) {
                log.info("Handler IO interrupted");
            }
            throw e;
        } catch(ClosedByInterruptException e) {
            if (log.isLoggable(Level.INFO)) {
                log.info("Handler interrupted during read, channel got closed");
            }
            message = null;
        } catch(IOException e) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.SEVERE, "IOException during request response read", e);
            }
            throw new EXProxyIOException(e.getMessage());
        }
        return message;
    }


    private HttpMessage getHttpMessageFromRawHttpMessage(HttpRawMessage rawMessage) {
        HttpMessage message;
        if (rawMessage.getType() == HttpRawMessageType.REQUEST) {
            HttpMessageRequest request;
            message = request = new HttpMessageRequest();
            request.setMethod(HttpMessageMethod.valueOf(rawMessage.getMethod()));
            request.setUri(rawMessage.getDest());
            request.setFromHost(getClientHost());
            request.setFromPort(getClientPort());
            request.setToHost(rawMessage.getHost());
            request.setToPort(rawMessage.getPort());
        } else {
            HttpMessageResponse response;
            message = response = new HttpMessageResponse();
            response.setStatusCode(rawMessage.getCode());
            response.setReasonPhrase(rawMessage.getCodeMessage());
            response.setFromHost(rawMessage.getHost());
            response.setFromPort(rawMessage.getPort());
            response.setToHost(getClientHost());
            response.setToPort(getClientPort());
        }
        String[] parts = rawMessage.getProtocol().split("/", 2);
        message.setProtocol(parts[0]);
        message.setVersion(parts[1]);
        message.setHeaders(rawMessage.getHeaders());
        message.setBody(rawMessage);
        return message;
    }

    
    /**
     * Process the HTTP CONNECT METHOD
     * Initiate a "man in the middle" SSL processing
     * Send back the processing to processingLoop to process requests as non-ssl ones.
     */
    private HttpRawMessage processRequestConnect(ByteChannel channel, HttpRawMessage request, URL context) throws InterruptedException, InterruptedIOException, MalformedURLException, EXProxyProtocolException, EXProxyIOException {
        HttpRawMessage response = null;
        String[] parts = request.getDest().split(":");
        String remoteHost = parts[0];
        int remotePort = Integer.parseInt(parts[1]);
        try {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Saying OK to CONNECT method");
            }
            sendReqres(channel, new HttpRawMessageResponseProxyOK());
            if (remotePort == 443 || remotePort == 8443) {
                channel = upgradeChannel2ServerSSLChannel(channel);
                context = new URL("https://"+remoteHost+":"+remotePort);
                processingLoop(channel, context);
            } else {
                log.severe("Don't known what to do with this CONNECT request ... :"+request);
                response = new HttpRawMessageResponseProxyError("Invalid CONNECT Request");
            }
        } catch(IOException e) {
            log.log(Level.SEVERE, "IOException", e);
        }
        return response;
    }
    
    
    
    /**
     * Correct a received message : 
     * initialize host, port data, remove headers, ...
     */
    private void correctRequestMessage(HttpRawMessage input, URL context) throws MalformedURLException {
        URL url;
        if (context == null) {
            // Assume input is an absolute URL
            url = new URL(input.getDest());
        } else {
            // Assume dest is a relative URL part
            // Inside the SSL tunnel, dest becomes relative
            url = new URL(context, input.getDest());
        }
        // Building the request to send to the proxied http server
        String proxiedHost = url.getHost();
        int proxiedPort = url.getPort();
        if (proxiedPort == -1) proxiedPort = url.getDefaultPort();

//        try {
            String dest = extractFileFromURL(url);
            input.setDest( dest );
            input.setHost( proxiedHost );
            input.setPort( proxiedPort );
            input.getHeaders().remove(HttpRawMessage.ACCEPT_ENCODING);  // Pour ne pas obtenir des contenus compressés et donc non filtrable
            input.getHeaders().remove(HttpRawMessage.PROXY_CONNECTION);
            if (!input.getHeaders().containsKey(HttpRawMessage.CONNECTION)) {
                input.getHeaders().put(HttpRawMessage.CONNECTION, varargs2list("Keep-Alive"));
            }
//        } catch(URISyntaxException e) {
//            log.log(Level.SEVERE, "Invalid URI has been given :"+input.getDest());
//        }
    }
    
    private List<String> varargs2list(String... args) {
        List<String> l = new ArrayList<String>();
        for (String arg: args) {
            l.add(arg);
        }
        return l;
    }
    
    /**
     * Il vaut mieux être souple dans le traitement de l'URI à cause de
     * sites qui ne respectent pas tout à faire la norme.
     * 
     */
    private String extractFileFromURL(URL url)  /* throws URISyntaxException */ {
        StringBuffer sb = new StringBuffer();
        sb.append(url.getPath());
        if (url.getQuery() != null) {
            sb.append("?");
            sb.append(url.getQuery());
        }
        if (url.getRef() != null) {
            sb.append("#");
            sb.append(url.getRef());
        }
        return sb.toString();

//        try {
//            URI uri = new URI(URLEncoder.encode(url.toString(), charset));
//            String file= uri.getRawPath();
//            if (uri.getRawQuery()!=null) file+="?"+uri.getRawQuery();
//            if (uri.getRawFragment()!=null) file+="#"+uri.getRawFragment();
//            return file;
//        } catch(UnsupportedEncodingException e) {
//            log.log(Level.SEVERE, "Unsupported charset to encode URL", e);
//            return null;
//        }
//        try {            
//            URI uri = new URI(dest);
//            String file= uri.getRawPath();
//            if (uri.getRawQuery()!=null) file+="?"+uri.getRawQuery();
//            if (uri.getRawFragment()!=null) file+="#"+uri.getRawFragment();
//            return file;
//        } catch(URISyntaxException e) {
//            // On essaye d'une autre manière car certain sites respectent pas les standards
//            String parts[] = dest.split("\\?", 2);
//            URI uri = new URI(parts[0]);
//            String file= uri.getRawPath();
//            if (parts.length ==2) {
//                file+="?"+parts[1];
//            }
//            return file;
//        }
    }

    
    
    /**
     * send immediately the given HTTP response
     */
    private void sendReqres(ByteChannel dest, HttpMessage message) throws InterruptedException, InterruptedIOException, EXProxyProtocolException, EXProxyIOException  {
        // TODO Voir pour ajouter un timeout en écriture
        try {
            ByteBuffer buffer = message.getData();
            while(buffer.hasRemaining()) {
                int l = dest.write(buffer);
                Thread.sleep(writeLatency); // TEMPO because we are in non-blocking mode
            }
        } catch(InterruptedIOException e) {
            throw e;
        } catch(IOException e) {
            log.log(Level.SEVERE, "Couldn't send response", e);
            throw new EXProxyIOException(e.getMessage());
        }
    }
    // TODO - Unifier ces deux méthodes en supprimant la suivante
    private void sendReqres(ByteChannel dest, HttpRawMessage message) throws InterruptedException, InterruptedIOException, EXProxyProtocolException, EXProxyIOException  {
        try {
            ByteBuffer buffer = message.getData();
            while(buffer.hasRemaining()) {
                int l = dest.write(buffer);
                Thread.sleep(writeLatency); // TEMPO because we are in non-blocking mode
            }
        } catch(InterruptedIOException e) {
            throw e;
        } catch(IOException e) {
            log.log(Level.SEVERE, "Couldn't send response", e);
            throw new EXProxyIOException(e.getMessage());
        }
    }

    /**
     * Processing GET, POST, HEAD, OPTIONS, ... (but no CONNECT) default HTTP protocol methods
     */
    private HttpRawMessage processRequestDefault(ByteChannel channel, HttpMessageRequest request, URL context)  throws InterruptedException, InterruptedIOException, EXProxyException {
        if (log.isLoggable(Level.FINE)) {
            log.fine("Processing "+request.getUri());
        }
        try {
            HttpRawMessage proxiedResponse =  getProxiedResponse(request.getToHost(), request.getToPort(), request, context);
            if (proxiedResponse != null) {
                proxiedResponse.setHost(request.getToHost());
                proxiedResponse.setPort(request.getToPort());
            }
            return proxiedResponse;
        } catch(UnknownHostException e) {
            EXProxyException exp = new EXProxyUnknownHostException(e.getMessage());
            throw exp;
        } catch(UnresolvedAddressException e) {
        EXProxyException exp = new EXProxyUnresolvedAddressException(e.getMessage());
            throw exp;            
        } catch(IOException e) {
            EXProxyException exp = new EXProxyIOException(e.getMessage());
            throw exp;
        }
    }

    /**
     * Get a response from the proxied HTTP server
     */
    private HttpRawMessage getProxiedResponse(String host, int port, HttpMessageRequest request, URL context) throws InterruptedException, InterruptedIOException, UnknownHostException, UnresolvedAddressException, EXProxyException, IOException {
        HttpRawMessage response = null;
        Socket s;
        ByteChannel channel;

        if ((channel = channelManager.pool(host, port)) == null) {
            if (log.isLoggable(Level.FINE)) {
                log.fine(String.format("Connecting to remote host %s port %d", host, port));
            }
            SocketChannel sc = SocketChannel.open(new InetSocketAddress(host, port));
            sc.finishConnect();
            sc.configureBlocking(false);
            channel = sc;
            if (context != null && "https".equals(context.getProtocol().toLowerCase())) {
                channel = upgradeChannel2ClientSSLChannel(channel);
            }
        }

        sendReqres(channel, request);
        response = readRawHttpMessage(channel, readTimeout, waitTimeout);

        channelManager.offer(host, port, channel);

        return response;
    }



    /**
     * Build a SSL client channel on top of an existing one
     */
    private ByteChannel upgradeChannel2ClientSSLChannel(ByteChannel channel) {
        try {
            KeyManager[] km = null;
            CoolX509TrustManager rtm = new CoolX509TrustManager();
            TrustManager[] tm = {rtm};
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, tm, new java.security.SecureRandom());

            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(true);
            engine.beginHandshake();
            
            return new SSLByteChannel(channel, engine);
        } catch(Exception e) {
            log.log(Level.SEVERE, "Exception during client SSL channel upgrade", e);

        }
        return null;
    }
    
    /**
     * Build a SSL server channel on top of an existing one
     */
    private ByteChannel upgradeChannel2ServerSSLChannel(ByteChannel channel) {
        try {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Switching socket to SSL");
            }
            
            KeyStore ks = KeyStore.getInstance("JKS");
            File kf = new File(getExproxy().getKeystoreFilename()); 
            ks.load(new FileInputStream(kf), getExproxy().getKeystorePassword());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, getExproxy().getKeystoreKeysPassword());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
            
            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
            engine.beginHandshake();

            return new SSLByteChannel(channel, engine);
        } catch(Exception e) {
            log.log(Level.SEVERE, "Exception during server SSL channel upgrade", e);
        }
        return null;
    }


    /**
     * Create a new Buffer from the content (between current position and limit)
     * of the given buffer. 
     * New buffer position and limit are set to its capacity
     * Given buffer position is now equal to the limit (which is below the capacity)
     */
    private ByteBuffer stripByteBuffer(ByteBuffer buffer) {
        ByteBuffer newBuffer = ByteBuffer.allocate(buffer.limit());
        newBuffer.put(buffer);
        return newBuffer;
    }


    /**
     * get the socket channel on which this handler is reading request
     * 
     */
    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    /**
     * get the EXProxy instance to which belongs this handler
     */
    public EXProxy getExproxy() {
        return exproxy;
    }    

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getClientHost() {
        return clientHost;
    }

    public int getClientPort() {
        return clientPort;
    }
}
