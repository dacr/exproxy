/*
 * DirectSSLSocketTest.java
 * JUnit based test
 *
 * Created on 7 juin 2005, 12:56
 */

package com.exproxy.test;
import junit.framework.*;

import static com.exproxy.test.tools.TestConstants.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;



/**
 * Tests des différentes API de SOCKET 
 * @author David Crosson
 */
public class VariousSocketTest extends TestCase {
    
    public VariousSocketTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }
    
   
    /**
     * Connect to the HTTP server with New I/O
     * Uses charset conversion and mapped byte buffer
     * THIS IS THE RIGHT WAY TO DO THINGS !!!!
     */
     public void testNewIO() throws Exception {
        Charset charset = Charset.forName("ISO-8859-15");
        CharsetDecoder decoder = charset.newDecoder();
        CharsetEncoder encoder = charset.newEncoder();

        SocketChannel sc = SocketChannel.open();
        assertTrue(sc.connect(new InetSocketAddress(TEST_ADDRESS, TEST_PORT)));
        ByteBuffer request = encoder.encode(CharBuffer.wrap("GET / HTTP/1.1\nHost: localhost\nConnection: Close\n\n"));
        assertTrue(sc.isConnected());
        assertTrue(sc.write(request) == request.capacity());
        assertFalse(request.hasRemaining());

        ByteBuffer buffer = ByteBuffer.allocate(1024);
        CharBuffer cb     = CharBuffer.allocate(1024);
        buffer.clear();
        cb.clear();
        StringBuffer response = new StringBuffer();
        long previousReadTime = System.currentTimeMillis();
        while(true) {
            int l = sc.read(buffer);

            if (l == 0 && !buffer.hasRemaining()) {
                if (System.currentTimeMillis() - previousReadTime > 10*1000L) {
                    fail("Timeout during read operation");
                }
                Thread.sleep(5);
                continue;
            }
            previousReadTime = System.currentTimeMillis();
            
            buffer.flip();
            CoderResult cr = decoder.decode(buffer, cb, l == -1);
            cb.flip();
            response.append(cb); cb.position(cb.limit());  // A StringBuffer does not impact buffer position
            if (l < 0) {
                if (buffer.hasRemaining()) {
                    System.out.println("WARNING : Remaining bytes found :"+buffer.remaining());
                }
                break;
            }
            buffer.compact();
            cb.compact();
        }
        assertTrue(response.indexOf("<html>")>=0);
        assertTrue(response.indexOf("</html>")>=0);
     }


}
