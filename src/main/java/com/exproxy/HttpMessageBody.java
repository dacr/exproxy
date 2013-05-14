/*
 * HttpMessageBody.java
 *
 * Created on 11 août 2005, 13:27
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy;

import java.io.ByteArrayInputStream;

/**
 * An interface to hide how is managed http message content.
 * @author David Crosson
 */
public interface HttpMessageBody {
    /**
     * Get all content bytes.
     * @return array of bytes
     */
    public byte[] getContent();
    /**
     * Get a stream to content bytes.
     * @return content bytes stream
     */
    public ByteArrayInputStream getContentStream();
}
