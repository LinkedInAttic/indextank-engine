/*
 * Copyright (c) 2011 LinkedIn, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

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
