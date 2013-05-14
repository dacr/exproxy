/*
 * CoolX509TrustManager.java
 *
 * Created on 7 juin 2005, 12:36
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.tools;

import javax.net.ssl.X509TrustManager;

public class CoolX509TrustManager implements X509TrustManager {
    public boolean checkClientTrusted(java.security.cert.X509Certificate[] chain) {
        return true;
    }
    public boolean isServerTrusted(java.security.cert.X509Certificate[] chain) {
        return true;
    }
    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
    }
    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {}
    public boolean isClientTrusted(java.security.cert.X509Certificate[] chain) {
        return true;
    }
}