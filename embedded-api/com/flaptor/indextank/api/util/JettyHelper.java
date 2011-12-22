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

package com.flaptor.indextank.api.util;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.ghosthack.turismo.servlet.Servlet;

public class JettyHelper {

    public static void server(int port, String mapping, String routes, Object api) throws Exception {
        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/");
        handler.setAttribute("api", api);
        ServletHolder holder = new ServletHolder(new Servlet());
        holder.setInitParameter("routes", routes);
        handler.addServlet(holder, mapping);
        server(port, handler);
    }
    
    public static void server(int port, Handler handler) throws Exception {
        Server server = new Server(port);
        server.setHandler(handler);
        server.start();
        server.join();
    }

}