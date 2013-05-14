/*
 */

package com.exproxy.tools;

import java.io.IOException;
import java.io.BufferedReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author  David Crosson
 */

public class ThreadedStreamStoreAndLog extends Thread {
    private Logger       log;
    private Level        level;
    private InputStream  inputStream;
    private String       type;
    private StringBuffer buffer;

    public ThreadedStreamStoreAndLog(InputStream inputStream, String type, Level level) {
        this.inputStream = inputStream;
        this.log = Logger.getLogger(this.getClass().getName());
        this.level = level;
        setType(type);
        setBuffer(new StringBuffer());
    }

    public void run() {
        try {
            InputStreamReader isr = new InputStreamReader(inputStream);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null) {
                getBuffer().append(line+"\n");
                if (log.isLoggable(level)) log.log(level, type+": "+line);
            }
            Thread.sleep(1); // To makes this thread interruptable
        } catch (IOException ioe) {
            log.log(Level.SEVERE, "IOException thrown", ioe);
        } catch(InterruptedException e) {
            log.log(Level.WARNING, "InterruptedException thrown", e);
        }
    }
    
    /**
     * Getter for property type.
     * @return Value of property type.
     */
    public String getType() {
        return type;
    }
    
    /**
     * Setter for property type.
     * @param type New value of property type.
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * Getter for property buffer.
     * @return Value of property buffer.
     */
    public StringBuffer getBuffer() {
        return buffer;
    }
    
    /**
     * Setter for property buffer.
     * @param buffer New value of property buffer.
     */
    public void setBuffer(StringBuffer buffer) {
        this.buffer = buffer;
    }
    
}