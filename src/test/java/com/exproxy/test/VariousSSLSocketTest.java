/*
 * DirectSSLSocketTest.java
 * JUnit based test
 *
 * Created on 7 juin 2005, 12:56
 */

package com.exproxy.test;

import com.exproxy.tools.CoolX509TrustManager;
import java.net.ServerSocket;
import javax.net.ServerSocketFactory;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import junit.framework.*;

import static com.exproxy.test.tools.TestConstants.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;


/**
 * Tests des différentes API de SOCKET 
 * @author David Crosson
 */
public class VariousSSLSocketTest extends TestCase {
    
    public VariousSSLSocketTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }
    
    /**
     * Connect to a SSL HTTP server. Will fail as the server certificate
     * is self signed.
     */
    public void testSocketConnect() throws Exception {
        SSLSocketFactory sslSF = (SSLSocketFactory)SSLSocketFactory.getDefault();
        Socket s = sslSF.createSocket(TESTSSL_ADDRESS, TESTSSL_PORT);
        try {
            s.getOutputStream().write("GET / HTTP/1.1\nHost: localhost\nConnection: Close\n\n".getBytes());
            fail();
        } catch(SSLHandshakeException e) {
        }
    }

    
    /**
     * Connect to a SSL HTTP server with a self signed certificate
     * Use old I/O. This code does no charset conversion
     * BAD CODE
     */
    public void testSelfCertSocketConnect() throws Exception {
        KeyManager[] km = null;
        CoolX509TrustManager rtm = new CoolX509TrustManager();
        TrustManager[] tm = {rtm};
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, tm, new java.security.SecureRandom());
        SSLSocketFactory sslSF = sslContext.getSocketFactory();
        
        Socket s = sslSF.createSocket(TESTSSL_ADDRESS, TESTSSL_PORT);
        s.getOutputStream().write("GET / HTTP/1.1\nHost: localhost\nConnection: Close\n\n".getBytes());
        InputStream in = s.getInputStream();
        int c;
        StringBuffer sb = new StringBuffer();
        while( (c = in.read()) != -1) {
            sb.append((char)c);
        }
        assertTrue(sb.indexOf("<html>")>=0);
        assertTrue(sb.indexOf("</html>")>=0);
    }
    
     /**
      * How to do SSL on a SocketChannel
      * We have to upgrade the socket channel with
      * a new one thanks to a SSLSocketFactory.createSocket
      * (The new socket is like a tunnel over the old one)
      */
     public void testSSLOverNIO() throws Exception {
        Charset charset = Charset.forName("ISO-8859-15");
        CharsetDecoder decoder = charset.newDecoder();
        CharsetEncoder encoder = charset.newEncoder();

        KeyManager[] km = null;
        CoolX509TrustManager rtm = new CoolX509TrustManager();
        TrustManager[] tm = {rtm};
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, tm, new java.security.SecureRandom());
        SSLSocketFactory sslSF = sslContext.getSocketFactory();
        SocketChannel sc = SocketChannel.open();
        assertTrue(sc.connect(new InetSocketAddress(TEST_ADDRESS, TEST_PORT)));

        // Upgrade the socketChannel to SSL
        Socket upgradedSocket = sslSF.createSocket(sc.socket(), TEST_ADDRESS, TEST_PORT, true);
        assertTrue(sc != null);
        SocketChannel sslsc = upgradedSocket.getChannel();
        assertTrue(sslsc != null);
        
        // Now Let's discuss in SSL with our server
        ByteBuffer request = encoder.encode(CharBuffer.wrap("GET / HTTP/1.1\nHost: localhost\nConnection: Close\n\n"));
        assertTrue(sslsc.isConnected());
        assertTrue(sslsc.write(request) == request.capacity());
        assertFalse(request.hasRemaining());
        
        // Just use old NIO as the purpose of this test was just to test what is been done before this line
        InputStream in = sslsc.socket().getInputStream();
        StringBuffer sb = new StringBuffer();
        int c;
        while( (c = in.read()) != -1) {
            sb.append((char)c);
        }
        assertTrue(sb.indexOf("<html>")>=0);
        assertTrue(sb.indexOf("</html>")>=0);
     }

     /**
      * How to create SSL server, a keystore must have been correctly initialized
      * Using old IO mechanisms
      */
     public void testSSLClientAndServerSocket() throws Exception {        
        SSLServerSocket ss=null;
        final String host = "localhost";
        final int port = 9999;
        final String message="Hello world !!";
        try {
            // ------------- SSL Initialization
            KeyStore ks = KeyStore.getInstance("JKS");
            File kf = new File(TEST_KEYSTORE_FILENAME);
            ks.load(new FileInputStream(kf), TEST_KEYSTORE_PASSWORD);
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, TEST_KEYSTORE_PASSWORD);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // ------------- 
            
            SSLServerSocketFactory sslSrvFact = sslContext.getServerSocketFactory();
            ss = (SSLServerSocket)sslSrvFact.createServerSocket(port, 5);

            Thread th = new Thread("Client (accepting selfcert) Request") {
                public void run() {
                    SSLSocket sc=null;
                    try {
                        KeyManager[] km = null;
                        CoolX509TrustManager rtm = new CoolX509TrustManager();
                        TrustManager[] tm = {rtm};
                        SSLContext sslContext = SSLContext.getInstance("SSL");
                        sslContext.init(null, tm, new java.security.SecureRandom());
                        SSLSocketFactory sslSF = sslContext.getSocketFactory();
                        //SSLSocketFactory sslSF = (SSLSocketFactory)SSLSocketFactory.getDefault();
                        sc =(SSLSocket)sslSF.createSocket(host, port); 
                        sc.setSoTimeout(5*1000);
                        OutputStream cout = sc.getOutputStream();
                        InputStream cin = sc.getInputStream();
                        cout.write(message.getBytes());
                    } catch(Exception e) {
                        e.printStackTrace();
                        fail();
                    } finally {
                        if (sc!=null) try {sc.close();} catch(Exception e) {}
                    }
                }
            };
            th.start();
            
            SSLSocket c = (SSLSocket)ss.accept();
            OutputStream sout = c.getOutputStream();
            InputStream sin = c.getInputStream();
            int r;
            StringBuffer sb = new StringBuffer();
            while((r = sin.read()) != -1) sb.append((char)r);
            assertTrue(sb.toString().equals(message));
            
        } finally {
            if (ss!=null) ss.close();
        }
     }
     
     /**
      * How to start normal comm between a client and a server and
      * then continuing using SSL protocol without modifying created
      * sockets
      */
     public void testNormalFollowedBySSLClientAndServerComSocket() throws Exception {        
        ServerSocket    s=null;
        final String host = "localhost";
        final int port = 9999;
        final String message="je ne suis pas un message confidentiel\n";
        final String messageSSL="Ceci est un message confidentiel\n";
        try {
            // ------------- Normal Server socker initialization
            s = ServerSocketFactory.getDefault().createServerSocket(port);            

            Thread th = new Thread("Client (accepting selfcert) Request") {
                public void run() {
                    Socket nc=null;
                    SSLSocket sc=null;
                    try {
                        // -- First send message in clear
                        nc = new Socket(host, port);
                        nc.setSoTimeout(5*1000);
                        OutputStream ncout = nc.getOutputStream();
                        InputStream ncin = nc.getInputStream();
                        ncout.write(message.getBytes());

                        // -- Switching to SSL
                        KeyManager[] km = null;
                        CoolX509TrustManager rtm = new CoolX509TrustManager();
                        TrustManager[] tm = {rtm};
                        SSLContext sslContext = SSLContext.getInstance("SSL");
                        sslContext.init(null, tm, new java.security.SecureRandom());
                        SSLSocketFactory sslSF = sslContext.getSocketFactory();
                        sc =(SSLSocket)sslSF.createSocket(nc, host, port, false);

                        // -- Second send message using SSL
                        sc.setSoTimeout(5*1000);
                        OutputStream scout = sc.getOutputStream();
                        InputStream scin = sc.getInputStream();
                        scout.write(messageSSL.getBytes());

                    } catch(Exception e) {
                        e.printStackTrace();
                        fail();
                    } finally {
                        if (nc!=null) try {nc.close();} catch(Exception e) {}
                        if (sc!=null) try {sc.close();} catch(Exception e) {}
                    }
                }
            };
            th.start();
            
            // ------------- Accepting client connection
            Socket client = s.accept();
            
            // ------------- Reading normal message
            int ch;
            StringBuffer msgRcv1 = new StringBuffer();
            InputStream in = client.getInputStream();
            while(true) {
                ch = in.read();
                if (ch==-1) fail("A secured message must follow");
                msgRcv1.append((char)ch);
                if (ch==(int)'\n') break;
            }
            assertTrue(msgRcv1.toString().equals(message));
                        
            // ------------- Switching client socket to SSL
            KeyStore ks = KeyStore.getInstance("JKS");
            File kf = new File(TEST_KEYSTORE_FILENAME);
            ks.load(new FileInputStream(kf), TEST_KEYSTORE_PASSWORD);
            
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ks, TEST_KEYSTORE_PASSWORD);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            // ------------- 
            SSLSocketFactory sslFact = sslContext.getSocketFactory();
            
            SSLSocket c = (SSLSocket)sslFact.createSocket(client, host, port, true);
            c.setUseClientMode(false);
            OutputStream sout = c.getOutputStream();
            InputStream sin = c.getInputStream();
            int r;
            StringBuffer sb = new StringBuffer();
            while((r = sin.read()) != -1) sb.append((char)r);
            assertTrue(sb.toString().equals(messageSSL));
            
        } finally {
            if (s!=null) s.close();
        }
     }

}
