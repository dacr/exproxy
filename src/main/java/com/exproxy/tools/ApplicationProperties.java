/*
 * ApplicationProperties.java
 *
 * Created on 14 avril 2005, 11:47
 */

package com.exproxy.tools;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dcr
 */
public class ApplicationProperties extends Properties {
    private final String RESPATH="application.properties";
    
    /** Creates a new instance of ApplicationProperties */
    private ApplicationProperties() {
        try {
            load(getClass().getResourceAsStream(RESPATH));
        } catch(IOException e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "IOException while initializing application properties", e);
        }
    }
    
    static private ApplicationProperties instance;
    static public ApplicationProperties getInstance() {
        if (instance == null) {
            instance = new ApplicationProperties();
        }
        return instance;
    }

    public String getProductName() {
        return getProperty("product.name");
    }
    public String getProductLongName() {
        return getProperty("product.longname");
    }
    public int getDefaultPort() {
        try {
            return Integer.parseInt(getProperty("default.port", "8000"));
        } catch(NumberFormatException e) {
            return 8000;
        }
    }
    public int getDefaultBacklog() {
        try {
            return Integer.parseInt(getProperty("default.backlog", "50"));
        } catch(NumberFormatException e) {
            return 50;
        }
    }
    public String getProductUrl() {
        return getProperty("product.url");
    }
    public String getProductEmail() {
        return getProperty("product.email");
    }
    public String getProductVersion() {
        return getProperty("product.version");
    }
    public String getProductAuthor() {
        return getProperty("product.author");
    }
    public String getProductAuthorEmail() {
        return getProperty("product.author.email");
    }

}
