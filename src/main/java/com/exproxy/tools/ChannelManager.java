/*
 * ConnectionManager.java
 *
 * Created on 27 juin 2005, 11:33
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.tools;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author David Crosson
 */
public class ChannelManager {
    private final Logger log = Logger.getLogger(ChannelManager.class.getName());
    
    private HashMap<ChannelManagerKey, Queue<ByteChannel>> cache;
    
    /** Creates a new instance of ConnectionManager */
    public ChannelManager() {
         cache = new HashMap<ChannelManagerKey, Queue<ByteChannel>>();
    }
    
    public ByteChannel pool(String host, int port) {
        Queue<ByteChannel> queue = cache.get(new ChannelManagerKey(host, port));
        if (queue != null) {
            return queue.peek();
        }
        return null;
    }
    
    public void offer(String host, int port, ByteChannel channel) {
        Queue<ByteChannel> queue;
        ChannelManagerKey key = new ChannelManagerKey(host, port);
        if (cache.containsKey(key)) {
            queue = cache.get(key);
        } else {
            queue = new LinkedList<ByteChannel>();
            cache.put(key, queue);
        }
        queue.offer(channel);
    }
    
    public void close() {
        for(Entry<ChannelManagerKey, Queue<ByteChannel>> entry: cache.entrySet()) {
            ByteChannel channel;
            while((channel = entry.getValue().poll()) != null) {
                try {
                    channel.close();
                } catch(IOException e) {
                    log.log(Level.WARNING, "IOException while closing channel for host "+entry.getKey().getHost()+" port "+entry.getKey().getPort());
                }
            }
        }
    }
}



class ChannelManagerKey {
    final private String host;
    final private int port;
    final private int hashCode;
    public ChannelManagerKey(String host, int port) {
        this.host = host;
        this.port = port;
        this.hashCode =(host+port).hashCode(); 
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int hashCode() {
        return hashCode;
    }
}