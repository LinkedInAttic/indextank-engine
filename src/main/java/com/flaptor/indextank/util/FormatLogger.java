package com.flaptor.indextank.util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.flaptor.util.Execute;

public class FormatLogger {

    public static FormatLogger getAlertsLogger() {
        return new FormatLogger("ALERTS." + Execute.whoCalledMe());
    }
    
    private Logger log;

    public FormatLogger() {
        this(Execute.whoCalledMe());
    }
    
    public FormatLogger(String name) {
        log = Logger.getLogger(name);
    }
    
    public void debug(String message, Object... args) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(message, args));
        }
    }
    
    public void info(String message, Object... args) {
        if (log.isInfoEnabled()) {
            log.info(String.format(message, args));
        }
    }

    public void warn(Throwable t, String message, Object... args) {
        if (log.isEnabledFor(Level.WARN)) {
            log.warn(String.format(message, args), t);
        }
    }
    
    public void warn(String message, Object... args) {
        if (log.isEnabledFor(Level.WARN)) {
            log.warn(String.format(message, args));
        }
    }
    
    public void error(Throwable t, String message, Object... args) {
        if (log.isEnabledFor(Level.ERROR)) {
            log.error(String.format(message, args), t);
        }
    }
    
    public void error(String message, Object... args) {
        if (log.isEnabledFor(Level.ERROR)) {
            log.error(String.format(message, args));
        }
    }
    
    public void fatal(Throwable t, String message, Object... args) {
        if (log.isEnabledFor(Level.FATAL)) {
            log.fatal(String.format(message, args), t);
        }
    }
    
    public void fatal(String message, Object... args) {
        if (log.isEnabledFor(Level.FATAL)) {
            log.fatal(String.format(message, args));
        }
    }

    
}
