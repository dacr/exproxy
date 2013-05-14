/*
 * JButtonAction.java
 *
 * Created on 14 avril 2005, 12:47
 */

package com.exproxy.tools;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import javax.swing.AbstractAction;

/**
 *
 * @author dcr
 */
abstract public class JButtonAction extends AbstractAction {
    
    /**
     * Remove '_' character used to select MNEMONIC key
    */
    static String purgeText(String text) {
        return text.replaceFirst("_","");
    }
    static Integer extractKeyCode(String text) {
        if (text.length()==0) return null;
        int pos = text.indexOf('_');
        if (pos==0) return null;
        char mnemonic;
        if (pos == -1) {
            mnemonic = text.toUpperCase().charAt(0);
        } else {
            mnemonic = text.toUpperCase().charAt(pos-1);
        }
        try {
            Field codeField = KeyEvent.class.getField("VK_"+mnemonic);
            return (Integer)codeField.get(null);
        } catch(NoSuchFieldException e) {
            return null;
        } catch(IllegalAccessException e) {
            return null;
        }
    }

    public JButtonAction(String text) {
        super(purgeText(text));
        Integer code = extractKeyCode(text);
        if (code!=null) putValue(MNEMONIC_KEY, code);
    }
    public JButtonAction() {
        super();
    }
}
