/*
 * ApacheHttpClientTest.java
 * JUnit based test
 *
 * Created on 7 juin 2005, 12:00
 */

package com.exproxy.test;

import com.exproxy.test.tools.CoolSSLProtocolSocketFactory;
import static com.exproxy.test.tools.TestConstants.*;

import junit.framework.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;

/**
 * Test the Apache HTTPClient library for direct connection to tomcat test server
 * @author David Crosson
 */
public class VariousHttpClientTest extends TestCase {
    
    public VariousHttpClientTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }
    
   public void testGet() throws Exception {
        HttpClient httpclient = new HttpClient();
        GetMethod httpget = new GetMethod("http://"+TEST_ADDRESS+":"+TEST_PORT+"/");
        httpclient.executeMethod(httpget);
        assertTrue("returns "+httpget.getStatusCode()+" instead of 200", httpget.getStatusCode() == 200);
        String result = httpget.getResponseBodyAsString();
        httpget.releaseConnection();
    }


    public void testSSLGet() throws Exception {
        Protocol coolhttps = new Protocol("https", new CoolSSLProtocolSocketFactory(), 443);
        Protocol.registerProtocol("https", coolhttps);
        HttpClient httpclient = new HttpClient();
        GetMethod httpget = new GetMethod("https://"+TESTSSL_ADDRESS+":"+TESTSSL_PORT+"/");
        httpclient.executeMethod(httpget);
        assertTrue("returns "+httpget.getStatusCode()+" instead of 200", httpget.getStatusCode() == 200);
        String result = httpget.getResponseBodyAsString();
        httpget.releaseConnection();
    }

}
