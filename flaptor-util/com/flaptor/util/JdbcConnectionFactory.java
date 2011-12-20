/*
Copyright 2008 Flaptor (flaptor.com) 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License.
*/
package com.flaptor.util;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.log4j.Logger;


/**
 * JDBC connection factory
 * 
 * @author Martin Massera
 */
public class JdbcConnectionFactory {

	private static final Logger logger = Logger.getLogger(Execute.whoAmI());
	
    private String driver;
    private String dburl;
    private String user;
    private String pass;
    
    public JdbcConnectionFactory(String driverClass, String url, String user, String pass) {
        this.driver = driverClass;
        this.dburl = url;
        this.user = user;
        this.pass = pass;
    }
    
    public JdbcConnectionFactory(Config config) {
        this(   config.getString("database.driver"),
                config.getString("database.url"), 
                config.getString("database.user"),
                config.getString("database.pass"));
    }
    
    public JdbcConnectionFactory(String configfile) {
    	this(Config.getConfig(configfile));
    }
    
    public Connection connect() throws Exception {
        Class.forName(driver).newInstance();
        return DriverManager.getConnection(dburl, user, pass);
    }
}
