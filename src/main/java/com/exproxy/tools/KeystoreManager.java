/*
 * KeystoreManager.java
 *
 * Created on 26 août 2005, 18:11
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy.tools;
import com.exproxy.tools.ThreadedStreamStoreAndLog;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author David Crosson
 */
public class KeystoreManager {
    private Logger log;
    private File keystore;
    
    /** Creates a new instance of KeystoreManager */
    public KeystoreManager(File keystore, String alias, String dName, String storepass, String keypass, int validity/*, String algo, String storetype*/) {
        log = Logger.getLogger(getClass().getName());
        if (!keystore.exists()) {
            generate(keystore, alias, dName, storepass, keypass, validity/*, algo, storetype*/);
        }
    }
    
    public void generate(File keystore, String alias, String dName, String storepass, String keypass, int validity/*, String algo, String storetype*/) {
        String javaHome = System.getProperty("java.home");
        String keytoolcmd = javaHome+File.separator+"bin"+File.separator+"keytool";
        
        String[] keytoolCMD = {
            keytoolcmd,
            "-genkey",
            "-alias", alias,
            "-dname", dName,
            "-keypass", keypass,
            "-keystore", keystore.getAbsolutePath(),
//            "-storetype", storetype,
//            "-keyalg", algo,
            "-storepass", storepass,
            "-validity", Integer.toString(validity)
        };
        try {
            genericCommandExecute(keytoolCMD);
        } catch(IOException e) {
            log.log(Level.WARNING, "CanKeytool command execution error", e);
        }
    }

    
    private int genericCommandExecute(String[] command) throws IOException  {
        Process commandproc;
        String cmdString="";
        for(int i=0; i<command.length; i++) cmdString+=command[i]+" ";
        commandproc = Runtime.getRuntime().exec(command);
        ThreadedStreamStoreAndLog stdout = new ThreadedStreamStoreAndLog(commandproc.getInputStream(), "OUT", Level.FINE);
        ThreadedStreamStoreAndLog stderr = new ThreadedStreamStoreAndLog(commandproc.getErrorStream(), "ERR", Level.FINE);
        stdout.start();
        stderr.start();
        int rc=-1;
        long startTime = System.currentTimeMillis();
        while(true) {
            try {
                rc = commandproc.exitValue();
                break;
            } catch(IllegalThreadStateException e) {
                try {Thread.sleep(100);} catch(InterruptedException e2) {}
                if (System.currentTimeMillis()-startTime > 60*1000) {
                    log.log(Level.SEVERE, "Interrupting command execution, timeout has been reached");
                    stdout.interrupt();
                    stderr.interrupt();
                    commandproc.destroy();
                    break;
                }
            }
        }
        // Waiting Threads to finish themselves
        while( stdout.isAlive() || stderr.isAlive()) {
           try {Thread.sleep(100);} catch(InterruptedException e2) {break;}
        }
        if (log.isLoggable(Level.FINE)) {
            log.info("Exit code ="+rc+" for command "+cmdString);
            log.finest("STDOUT log:\n"+stdout.getBuffer().toString());
            log.finest("STDERR log:\n"+stderr.getBuffer().toString());
        }
        return rc;
    }

}
