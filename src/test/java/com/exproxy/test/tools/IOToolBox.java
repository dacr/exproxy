/*
 * IOToolBox.java
 *
 * Created on 20 juin 2005, 10:02
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.test.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *
 * @author David Crosson
 */
public class IOToolBox {
    
    /** Creates a new instance of IOToolBox */
    private IOToolBox() {
    }
    
    static public String getString(InputStream in, String charset) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in, charset));
        StringBuffer sb = new StringBuffer(); String line;
        while((line = r.readLine()) != null) sb.append(line);
        return sb.toString();
    }
    
}
