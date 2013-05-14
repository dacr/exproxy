/*
 * CompactFormatter.java
 *
 * Created on 23 novembre 2004, 23:01
 */

package com.exproxy.tools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Compact Log formatter to achieve log4j output
 * @author  dcr
 */
public class CompactFormatter extends java.util.logging.Formatter {
    
    /** Creates a new instance of CompactFormatter */
    public CompactFormatter() {
    }

    static SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss.S");
    
    public String getNameOnly(String classFullName) {
        int pos = classFullName.lastIndexOf('.');
        if (pos == -1) {
            return classFullName;
        } else {
            return classFullName.substring(pos+1);
        }
    }
    
    public String format(java.util.logging.LogRecord lr) {
        Date   d = new Date(lr.getMillis());
        int    t = lr.getThreadID();
        String l = lr.getLevel().toString();
        String sc = getNameOnly(lr.getSourceClassName());
        String sm = lr.getSourceMethodName();
        String m = lr.getMessage();
        // Java 1.5
        StringWriter sw = new StringWriter();
        if (lr.getThrown()!=null) {
            sw.append(" :");
            PrintWriter pw = new PrintWriter(sw);
            lr.getThrown().printStackTrace(pw);
        } else {
            sw.append("");
        }
        StringBuffer message = new StringBuffer();
        String space = "    ";
        for(String line: m.split("\\r?\\n")) {
            message.append(space);
            message.append(line);
            message.append("\n");
        }
        if (lr.getThrown() != null)
            for(String line: sw.getBuffer().toString().split("\\r?\\n")) {
                message.append(space);
                message.append(line);
                message.append("\n");
            }
        String s = String.format(
                "%1$tF %1$tT %1$tL %2$05d %3$7s %4$25s:%5$-12s %6$s",
                d, t, l, sc, sm, message);
        return s.toString();
    }
    
}
