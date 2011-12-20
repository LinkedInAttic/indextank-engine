package com.flaptor.util.xml;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.flaptor.util.Pair;

abstract public class SaxStackHandler extends DefaultHandler {
    
    protected Stack<Pair<String,StringBuffer>> elementStack = new Stack<Pair<String,StringBuffer>>();

    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
        elementStack.push(new Pair<String, StringBuffer>(name,new StringBuffer()));
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        StringBuffer sb = elementStack.peek().last();
        sb.append(ch, start, length);
    }

    public final void endElement(String uri, String localName, String name) throws SAXException {
        endElement(uri, localName, name, elementStack.peek().last().toString());
        elementStack.pop();
    }
    
    abstract public void endElement(String uri, String localName, String name, String textContent) throws SAXException ;
}
