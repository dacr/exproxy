/*
 * CoolHostnameVerifier.java
 *
 * Created on 20 juin 2005, 10:26
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.tools;

import javax.net.ssl.HostnameVerifier;

/**
 *
 * @author David Crosson
 */
public class CoolHostnameVerifier implements HostnameVerifier {
    
    /** Creates a new instance of CoolHostnameVerifier */
    public CoolHostnameVerifier() {
    }

    public boolean verify(String hostname, javax.net.ssl.SSLSession sSLSession) {
        return true;
    }
    
}
