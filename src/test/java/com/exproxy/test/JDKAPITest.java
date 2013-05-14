/*
 * JDKAPITest.java
 * JUnit based test
 *
 * Created on 6 juillet 2005, 14:54
 */

package com.exproxy.test;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import junit.framework.*;

/**
 *
 * @author David Crosson
 */
public class JDKAPITest extends TestCase {
    
    public JDKAPITest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
    }
    
    protected void tearDown() throws Exception {
    }

    public void testURLMethods() throws Exception {
        String urldesc = "http://ads.highmetrics.com/meteof/high_track_meteof.php?site=meteof&sec=0&pg=0|&ck=y&nu=0&rd=4229566522&vn=Netscape#toto";
        URL url = new URL(urldesc);
        System.out.println(url.getPath()+"?"+url.getQuery()+"#"+url.getRef());
        System.out.println(url.getFile());
    }
    
    public void testURINotWorking() throws Exception {
        String badURIstr = "http://ads.highmetrics.com/meteof/high_track_meteof.php?site=meteof&sec=0&pg=0|&ck=y&nu=0&rd=4229566522&vn=Netscape";
        URI uri = new URI(URLEncoder.encode(badURIstr, "ISO-8859-1"));
        System.out.println(uri);
    }

    public void testURINotWorking2() throws Exception {
        //String badURIstr = "http://tag.lemonde.fr/img/251428056.1597217.gifc=^on^&d=^www.lemonde.fr^&r=^^";
        //String badURIstr = "http://ads.highmetrics.com/meteof/high_track_meteof.php?site=meteof&sec=0&pg=0|&ck=y&nu=0&rd=4229566522&vn=Netscape#toto";
        //String badURIstr="http://g.msn.com/0AD0001Q/744946.1??PID=2638867&UIT=G&TargetID=1104588&AN=421202158&PG=CMSIE4";
        String badURIstr="http://global.msads.net/ads/53432/0000053432_000000000000000195493.gif";

//        URI uri = new URL(URLEncoder.encode(badURIstr, "ISO-8859-1")).toURI();
//
//        String file="";
//        file+=uri.getQuery();
//        if (uri.getQuery()!=null) file+="?"+uri.getQuery();
//        if (uri.getFragment()!=null) file+="#"+uri.getFragment();
        
        URL url = new URL(badURIstr);
        StringBuffer sb = new StringBuffer();
        sb.append(url.getPath());
        if (url.getQuery() != null) {
            sb.append("?");
            sb.append(url.getQuery());
        }
        if (url.getRef() != null) {
            sb.append("#");
            sb.append(url.getRef());
        }

        
        System.out.printf("**** %s ****\n", sb.toString());

    }
    
}
