/*
 * VariousLowLevelSSLTest.java
 * JUnit based test
 *
 * Created on 8 juin 2005, 10:09
 */

package com.exproxy.test;

import junit.framework.*;

import static com.exproxy.test.tools.TestConstants.*;
import com.exproxy.tools.CoolX509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.KeyStore;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 *
 * @author David Crosson
 */
public class VariousSSLLowLevelTest extends TestCase {
    
    public VariousSSLLowLevelTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }
    
    
    /**
     * Discussion SSL avec un serveur en utilisant
     * l'API bas niveau SSLEngine de Sun.
     */
    public void testSSLLowLevelClientConnection() throws Exception {
        Charset charset = Charset.forName("ISO-8859-15");
        CharsetDecoder decoder = charset.newDecoder();
        CharsetEncoder encoder = charset.newEncoder();

        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);
        sc.connect(new InetSocketAddress(TESTSSL_ADDRESS, TESTSSL_PORT));
        //sc.connect(new InetSocketAddress("www.verisign.com", 443));
        while(sc.finishConnect() == false) Thread.sleep(10);
        
        KeyManager[] km = null;
        CoolX509TrustManager rtm = new CoolX509TrustManager();
        TrustManager[] tm = {rtm};
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, tm, new java.security.SecureRandom());
        SSLEngine e = sslContext.createSSLEngine();
        e.setUseClientMode(true);
        e.beginHandshake();
        
        ByteBuffer inAppData  = ByteBuffer.allocate(1000*1024);
        ByteBuffer outAppData = ByteBuffer.allocate(100*1024);
        ByteBuffer inNetData  = ByteBuffer.allocate(100*1024);
        ByteBuffer outNetData = ByteBuffer.allocate(100*1024);
        inAppData.clear();
        outAppData.clear();
        inNetData.clear();
        outNetData.clear();

        SSLEngineResult ser=null;

        // Negotiate Handshake
        outAppData.flip();
        ser = e.wrap(outAppData,  outNetData);
        assertTrue(ser.getStatus().toString()+" - "+ser.getHandshakeStatus().toString(), ser.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP);
        outAppData.compact();

        outNetData.flip();
        while(outNetData.hasRemaining()) sc.write(outNetData);
        outNetData.compact();

        while(ser != null && ser.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.FINISHED) {
            Thread.sleep(1000);
            //System.out.format("%s - %s\n", ser.getStatus().toString(), ser.getHandshakeStatus().toString());
            switch(ser.getHandshakeStatus()) {
                case NEED_TASK:
                    //Executor exec = Executors.newSingleThreadExecutor();
                    Runnable task;
                    while ((task=e.getDelegatedTask()) != null) {
                        //exec.execute(task);
                        task.run();
                    }
                    // Must continue with wrap as data must be sent
                case NEED_WRAP:
                    outAppData.flip();
                    ser = e.wrap(outAppData,  outNetData);
                    outAppData.compact();
                    outNetData.flip();
                    while(outNetData.hasRemaining()) sc.write(outNetData);
                    outNetData.compact();
                    break;
                case NEED_UNWRAP:
                    while(sc.read(inNetData) > 0) {
                        //System.out.println("sleep");
                        Thread.sleep(50);
                    }
                    inNetData.flip();
                    ser = e.unwrap(inNetData, inAppData);
                    inNetData.compact();
                    break;
                case NOT_HANDSHAKING:
                    
            }
        }
        assertTrue(ser.getStatus().toString()+" - "+ser.getHandshakeStatus().toString(), ser.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED);

        // ----------- SEND REQUEST
        String rq = "GET / HTTP/1.1\nHost: localhost\nConnection: Close\n\n";
        outAppData.put(encoder.encode(CharBuffer.wrap(rq)));
        
        outAppData.flip();
        ser = e.wrap(outAppData,  outNetData);
        outAppData.compact();

        outNetData.flip();
        while(outNetData.hasRemaining()) sc.write(outNetData);
        outNetData.compact();


        // ----------- READ RESPONSE
        while(sc.read(inNetData) >= 0) {
            //System.out.println("sleep");
            Thread.sleep(50);
        }
        //System.out.format("%d - %d - %d\n", inNetData.position(), inNetData.limit(), inNetData.capacity());
        inNetData.flip();
        while(inNetData.hasRemaining()) {
            ser = e.unwrap(inNetData, inAppData);
        }
        //System.out.format("%d - %d - %d\n", inAppData.position(), inAppData.limit(), inAppData.capacity());
        inNetData.compact();

        
        e.closeOutbound();
        assertFalse(e.isOutboundDone());

        outAppData.flip();
        ser = e.wrap(outAppData,  outNetData);
        outAppData.compact();
        outNetData.flip();
        while(outNetData.hasRemaining()) sc.write(outNetData);
        outNetData.compact();
        // It is acceptable to close the socket without having read response to the close command sent
        sc.close();
        assertTrue(e.isOutboundDone());
        
        // ----------- CHECK RESPONSE
        inAppData.flip();
        CharBuffer cb = decoder.decode(inAppData);
        String content = cb.toString();
        //System.out.println(content.substring(content.length()-100));
        //System.out.println(content);
        assertTrue(content.contains("<html>"));
        assertTrue(content.contains("</html>"));
        
    }

}
