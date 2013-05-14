/*
 * MiddleManTest.java
 * JUnit based test
 *
 * Created on 10 juin 2005, 15:46
 */

package com.exproxy.test;
import com.exproxy.EXProxy;
import com.exproxy.test.tools.IOToolBox;
import java.net.Proxy;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import junit.framework.*;

import static com.exproxy.test.tools.TestConstants.*;
import com.exproxy.tools.CoolHostnameVerifier;
import com.exproxy.tools.CoolX509TrustManager;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;



/**
 *
 * @author David Crosson
 */
public class EXProxyTest extends TestCase {
    
    public EXProxyTest(String testName) throws Exception {
        super(testName);

        KeyManager[] km = null;
        CoolX509TrustManager rtm = new CoolX509TrustManager();
        TrustManager[] tm = {rtm};
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, tm, new java.security.SecureRandom());
        SSLSocketFactory sslSF = sslContext.getSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(sslSF);
        HttpsURLConnection.setDefaultHostnameVerifier(new CoolHostnameVerifier());
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public void testBasic() throws Exception {
        EXProxy mm = new EXProxy(
                Inet4Address.getByName(PROXY_ADDRESS),
                PROXY_PORT, 
                PROXY_BACKLOG,
                PROXY_KEYSTORE_FILENAME,
                PROXY_KEYSTORE_PASSWORD,
                PROXY_KEYSTOREKEYS_PASSWORD
                );
        try {
            mm.start();
            Thread.sleep(1000);

            try {
                Socket s = new Socket(PROXY_ADDRESS, PROXY_PORT);
                s.close();
            } catch(Exception e) {
                fail("Can't connect the proxy");
            }

            URL url = new URL("https", TESTSSL_ADDRESS, TESTSSL_PORT, "/");
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_ADDRESS, PROXY_PORT));
            HttpURLConnection cnx = (HttpURLConnection)url.openConnection(proxy);
            cnx.setConnectTimeout(0);
            cnx.setReadTimeout(0);

            String result = IOToolBox.getString(cnx.getInputStream(), "ISO-8859-1");
        
            assertTrue("returns "+cnx.getResponseCode()+" instead of 200", cnx.getResponseCode() == 200);
            
            assertTrue(result.contains("<html>"));
            assertTrue(result.contains("</html>"));
        } finally {
            mm.interrupt();
            while(mm.isAlive()) Thread.sleep(10);
        }
    }

/*
    public void testBasicNormalWithHttpClient() throws Exception {
        MiddleMan mm = new MiddleMan(PROXY_PORT, 20, Inet4Address.getByName(PROXY_ADDRESS));
        try {
            mm.start();
            Thread.sleep(2000);

            try {
                Socket s = new Socket(PROXY_ADDRESS, PROXY_PORT);
                s.close();
            } catch(Exception e) {
                fail("Can't connect the proxy");
            }

            HttpClient httpclient = new HttpClient();
            httpclient.getHostConfiguration().setProxy(PROXY_ADDRESS, PROXY_PORT);
            GetMethod httpget = new GetMethod("http://"+TEST_ADDRESS+":"+TEST_PORT+"/");
            httpclient.executeMethod(httpget);
            assertTrue("returns "+httpget.getStatusCode()+" instead of 200", httpget.getStatusCode() == 200);
            String result = httpget.getResponseBodyAsString();
            httpget.releaseConnection();
            assertTrue(result.contains("<html>"));
            assertTrue(result.contains("</html>"));
        } finally {
            mm.interrupt();
            while(mm.isAlive()) Thread.sleep(10);
        }
    }

    public void testBasicSSLWithHttpClient() throws Exception {
        MiddleMan mm = new MiddleMan(PROXY_PORT, 20, Inet4Address.getByName(PROXY_ADDRESS));
        try {
            mm.start();
            Thread.sleep(2000);

            try {
                Socket s = new Socket(PROXY_ADDRESS, PROXY_PORT);
                s.close();
            } catch(Exception e) {
                fail("Can't connect the proxy");
            }

            HttpClient httpclient = new HttpClient();
            httpclient.getHostConfiguration().setProxy(PROXY_ADDRESS, PROXY_PORT);
            fail("Récuperer une version de commons httpclient qui supporte la combinaison proxy et https");
            Protocol coolhttps = new Protocol("https", new CoolSSLProtocolSocketFactory(), 443);
            Protocol.registerProtocol("https", coolhttps);
            httpclient.getHostConfiguration().setHost(TESTSSL_ADDRESS, TESTSSL_PORT , coolhttps);

            GetMethod httpget = new GetMethod("/");
            httpget.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new MyRetryHandler());
            httpclient.executeMethod(httpget);
            assertTrue("returns "+httpget.getStatusCode()+" instead of 200", httpget.getStatusCode() == 200);
            String result = httpget.getResponseBodyAsString();
            httpget.releaseConnection();
            assertTrue(result.contains("<html>"));
            assertTrue(result.contains("</html>"));
        } finally {
            mm.interrupt();
            while(mm.isAlive()) Thread.sleep(10);
        }
    }

class MyRetryHandler  implements HttpMethodRetryHandler {
    public boolean retryMethod(final HttpMethod method, final IOException exception, int executionCount) {
        if (executionCount >= 2) {
            return false;
        }
        return false;
    }
};

*/
}


