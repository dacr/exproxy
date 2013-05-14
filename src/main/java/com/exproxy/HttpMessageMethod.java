/*
 * HttpMessageMethodType.java
 *
 * Created on 15 juin 2005, 14:59
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.exproxy;

/**
 * The 8 possible HTTP request methods.
 * @author David Crosson
 */
public enum HttpMessageMethod {
    /**
     * GET http request model.
     */
    GET,
    /**
     * POST http request model.
     */
    POST,
    /**
     * PUT http request model.
     */
    PUT,
    /**
     * TRACE http request model.
     */
    TRACE,
    /**
     * DELETE http request model.
     */
    DELETE,
    /**
     * HEAD http request model.
     */
    HEAD,
    /**
     * OPTIONS http request model.
     */
    OPTIONS,
    /**
     * CONNECT http request model.
     */
    CONNECT;
}

