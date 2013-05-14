/*
 * TestConstants.java
 *
 * Created on 6 juin 2005, 14:37
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.test.tools;

import com.exproxy.*;

/**
 *
 * @author dcr
 */
public class TestConstants {
    public static String PROXY_ADDRESS = "127.0.0.1";
    public static int    PROXY_PORT    = 8000;
    public static int    PROXY_BACKLOG = 20;
    public static String PROXY_KEYSTORE_FILENAME = "keystore";
    public static char[] PROXY_KEYSTORE_PASSWORD = "changeit".toCharArray();
    public static char[] PROXY_KEYSTOREKEYS_PASSWORD = "changeit".toCharArray();


    public static String TEST_ADDRESS = "127.0.0.1";
    public static int    TEST_PORT=8080;

    public static String TESTSSL_ADDRESS = "127.0.0.1";
    public static int    TESTSSL_PORT=8443;

    public static String TEST_KEYSTORE_FILENAME = "keystoretest";
    public static char[] TEST_KEYSTORE_PASSWORD = "changeit".toCharArray();

    
    /** Creates a new instance of TestConstants */
    private TestConstants() {
    }
    
}
