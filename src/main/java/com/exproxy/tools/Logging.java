/*
 * Logging.java
 *
 * Created on 6 juillet 2005, 11:24
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.tools;

import java.io.IOException;
import java.util.logging.LogManager;

/**
 *
 * @author David Crosson
 */
public class Logging {
    public static void logSystemInit() throws IOException {
        LogManager lm = LogManager.getLogManager();
        lm.readConfiguration(Logging.class.getResourceAsStream("logging.properties"));
    }
}
