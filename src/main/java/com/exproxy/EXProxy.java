/*
 * MiddleMan.java
 *
 * Created on 10 juin 2005, 13:29
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy;

import com.exproxy.impl.Handler;
import com.exproxy.tools.Logging;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.exproxy.listeners.EXProxyListener;
import com.exproxy.listeners.HttpMessageListener;
import com.exproxy.processors.HttpMessageProcessor;


/**
 * Extensible Proxy server main class, instantiate this class to get
 * a new HTTP/HTTPS proxy server. Register your message listeners or
 * processor to achieve the features you want to implement.
 * @author David Crosson
 */
public class EXProxy extends Thread {
    private final Logger log = Logger.getLogger(EXProxy.class.getName());
    //private final ExecutorService pool;
    
    private final InetAddress ifAddress;
    private final int port;
    private final int backlog;
    private final String keystoreFilename;
    private final char[] keystorePassword;
    private final char[] keystoreKeysPassword;
    
    private final List<HttpMessageListener> requestListeners;
    private final List<HttpMessageListener> responseListeners;

    private final List<HttpMessageProcessor> requestProcessors;
    private final List<HttpMessageProcessor> responseProcessors;
    
    private final List<EXProxyListener> listeners;
    
    /**
     * Creates a new EXProxy with given settings.
     *
     * @param ifAddress InetAddress where to bind the proxy server.
     * @param port TCP/IP port to use both for HTTP and HTTPS proxy.
     * @param backlog The listen backlog length.
     * @param keystoreFilename path to the keystore file to use
     * @param keystorePassword password that protect the keystore
     * @param keystoreKeysPassword password that protect the key in the keystore
     */
    public EXProxy(InetAddress ifAddress, int port, int backlog, String keystoreFilename, char[] keystorePassword, char[] keystoreKeysPassword) {
        this.ifAddress = ifAddress;
        this.port = port;
        this.backlog = backlog;
        this.keystoreFilename = keystoreFilename;
        this.keystorePassword = keystorePassword;
        this.keystoreKeysPassword = keystoreKeysPassword;
        //pool = Executors.newFixedThreadPool(30);
        requestListeners   = Collections.synchronizedList(new ArrayList<HttpMessageListener>());
        responseListeners  = Collections.synchronizedList(new ArrayList<HttpMessageListener>());
        requestProcessors  = Collections.synchronizedList(new ArrayList<HttpMessageProcessor>());
        responseProcessors = Collections.synchronizedList(new ArrayList<HttpMessageProcessor>());        
        listeners          = Collections.synchronizedList(new ArrayList<EXProxyListener>());        
    }
    /**
     * Creates a new EXProxy instance with keystore default values.
     * 
     * <p>Default values are :</p>
     * <ul>
     *    <li><p>a keystore file named "keystore" in the current directory with "changeit" as
     * password for itself and for the main key it contains.</p></li>
     * </ul>
     * @param ifAddress InetAddress where to bind the proxy server.
     * @param port TCP/IP port to use both for HTTP and HTTPS proxy
     * @param backlog The listen backlog length.
     */
    public EXProxy(InetAddress ifAddress, int port, int backlog) {
        this(ifAddress,
                port,
                backlog,
                "keystore",
                "changeit".toCharArray(),
                "changeit".toCharArray());
    }
    /**
     * Creates a new EXProxy instance with default values.
     * 
     * <p>Default values are :</p>
     * <ul>
     *    <li><p>On 127.0.0.1, port 8000 and with a backlog of 100.</p></li>
     *    <li><p>A keystore file named "keystore" in the current directory with "changeit" as
     *        password for itself and for the main key it contains.</p></li>
     * </ul>
     * @throws java.net.UnknownHostException Thrown if local host can't be found (no network ?)
     */
    public EXProxy() throws UnknownHostException {
        this(InetAddress.getLocalHost(),
                8000,
                100,
                "keystore",
                "changeit".toCharArray(),
                "changeit".toCharArray());
    }
    
    /**
     * Starts the extensible http/https proxy, invoke start method to
     * run the proxy in background.
     */
    public void run() {
        log.info(String.format("MiddleMan starts on %s %d",  ifAddress.toString(), port));
        ServerSocketChannel serverSocketChannel=null;
        try {
            try {
                log.fine("Server socket creation");
                serverSocketChannel = ServerSocketChannel.open();
                InetSocketAddress isa = new InetSocketAddress(getIfAddress(), getPort());
                serverSocketChannel.socket().bind(isa, getBacklog());
                serverSocketChannel.configureBlocking(false);
            } catch(IOException e) {
                log.log(Level.SEVERE, "Can't create server socket on port="+getPort()+" ifAddress="+getIfAddress().getHostAddress());
            }
            int count=0;
            while(true) {
                try {
                    SocketChannel socketChannel = serverSocketChannel.accept();
                    if (socketChannel != null) {
                        //pool.execute(new Handler(socketChannel, this));
                        new Thread(new Handler(socketChannel, this), "Handler-"+count++).start();
                    } else {
                        try {
                            Thread.sleep(50);
                        } catch(InterruptedException ee) {
                            log.info("MiddleMan has been interrupted");
                            break;
                        }
                    }
                } catch(SocketTimeoutException e) {
                    try {
                        Thread.sleep(50);
                    } catch(InterruptedException ee) {
                        log.info("MiddleMan has been interrupted");
                        break;
                    }
                } catch(InterruptedIOException e) {
                    log.info("MiddleMan accept operation has been interrupted");
                    break;
                } catch(ClosedByInterruptException e) {
                    log.info("Interrupt received, server socket channel has been closed");
                    break;
                } catch(ClosedChannelException e) {
                    log.info("Server socket channel has been closed");
                    break;
                } catch(IOException e) {
                    log.log(Level.SEVERE, "Can't accept requested client", e);
                    break;
                }
            }
        } finally {
            if (serverSocketChannel != null) {
                try {
                    serverSocketChannel.close();
                } catch(IOException e) {
                    log.log(Level.SEVERE, "Can't close server socket", e);
                }
            }
            //pool.shutdownNow();
            log.info("MiddleMan finishes");
        }
    }
    
    /**
     * Starts a default proxy on the first available network interface using IPv4.
     * Uses default keystores parameters.
     * @param args Not used
     * @throws java.io.IOException Error while looking for available network interfaces
     */
    public static void main(String[] args) throws IOException {
        Logging.logSystemInit();
        Logger log = Logger.getLogger(EXProxy.class.getName());
        
        InetAddress nif=null;
        Enumeration<InetAddress> addrs =  NetworkInterface.getByName("lo").getInetAddresses();
        while(addrs.hasMoreElements()) {
            InetAddress cur = addrs.nextElement();
            if (cur instanceof Inet4Address) {
                nif=cur;
                break;
            }
        }
        //nif = InetAddress.getLocalHost();
        if (nif != null) {
            EXProxy mm = new EXProxy(nif, 8000, 50);
            mm.start();
        }
    }

    /**
     * Get the InetAddress on which the proxy server is running.
     * @return InetAddress
     */
    public InetAddress getIfAddress() {
        return ifAddress;
    }

    /**
     * Get the port on which the proxy server is running.
     * @return port number
     */
    public int getPort() {
        return port;
    }

    /**
     * Get listen backlog length.
     * @return backlog length
     */
    public int getBacklog() {
        return backlog;
    }

    /**
     * Get keystore path.
     * @return String containing the keystore path
     */
    public String getKeystoreFilename() {
        return keystoreFilename;
    }

    /**
     * Get keystore password.
     * @return password
     */
    public char[] getKeystorePassword() {
        return keystorePassword;
    }

    /**
     * Get keystore key password.
     * @return key password
     */
    public char[] getKeystoreKeysPassword() {
        return keystoreKeysPassword;
    }


    
    
    
    
    
    /**
     * Get current request listeners.
     * @return listeners list
     */
    public List<HttpMessageListener> getRequestListeners() {
        return requestListeners;
    }

    /**
     * Add a new listener to receive incoming http requests.
     * @param listener the listener instance.
     */
    public void addRequestListener(HttpMessageListener listener) {
        getRequestListeners().add(listener);
    }
    
    /**
     * Remove the given listener.
     * @param listener the listener to remove.
     */
    public void removeRequestListener(HttpMessageListener listener) {
        getRequestListeners().remove(listener);
    }
    
    
    
    
    
    /**
     * Get current response listeners.
     * @return listeners list
     */
    public List<HttpMessageListener> getResponseListeners() {
        return responseListeners;
    }

    /**
     * Add a new listener to receive incoming http responses, you will also
     * receive the associated requests.
     * @param listener The listener instance.
     */
    public void addResponseListener(HttpMessageListener listener) {
        getResponseListeners().add(listener);
    }

    /**
     * Remove the given listener.
     * @param listener the listener to remove.
     */
    public void removeResponseListener(HttpMessageListener listener) {
        getResponseListeners().remove(listener);
    }

    
    

    
    /**
     * Get current listeners for exproxy events.
     * @return listeners list
     */
    public List<EXProxyListener> getListeners() {
        return listeners;
    }

    /**
     * Add a new listener to EXProxy events.
     * @param listener The listener instance.
     */
    public void addListener(EXProxyListener listener) {
        getListeners().add(listener);
    }

    /**
     * Remove the given listener.
     * @param listener the listener to remove.
     */
    public void removeListener(EXProxyListener listener) {
        getListeners().remove(listener);
    }

    
    

    
    /**
     * Get current request processors.
     * @return processors list
     */
    public List<HttpMessageProcessor> getRequestProcessors() {
        return requestProcessors;
    }
    /**
     * Add a new request processor.
     * @param processor The request processor instance.
     */
    public void addRequestProcessor(HttpMessageProcessor processor) {
        getRequestProcessors().add(processor);
    }
    
    /**
     * Remove the given request processor.
     * @param processor The processor instance to remove.
     */
    public void removeRequestProcessor(HttpMessageProcessor processor) {
        getRequestProcessors().remove(processor);
    }

    
    
    
    
    

    /**
     * Get current response processors.
     * @return processors list.
     */
    public List<HttpMessageProcessor> getResponseProcessors() {
        return responseProcessors;
    }

    /**
     * Add a new response processor.
     * @param processor The request processor instance.
     */
    public void addResponseProcessor(HttpMessageProcessor processor) {
        getResponseProcessors().add(processor);
    }

    /**
     * Remove the given response processor.
     * @param processor The processor instance to remove.
     */
    public void removeResponseProcessor(HttpMessageProcessor processor) {
        getResponseProcessors().remove(processor);
    }

}
