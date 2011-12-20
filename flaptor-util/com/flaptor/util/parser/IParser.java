/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.flaptor.util.parser;

/**
 *
 * @author jorge
 */
public interface IParser {

    /**
     * Parse the given html document.
     * @param content the html document to parse.
     * @return the parsed string.
     */
    public ParseOutput parse(String url, byte[] content) throws Exception;

}
