package com.flaptor.indextank.api.util;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import com.ghosthack.turismo.servlet.Servlet;

public class JettyHelper {

    public static void server(int port, String mapping, String routes) throws Exception {
        server(port, handler(mapping, routes));
    }

    public static void server(int port, Handler handler) throws Exception {
        Server server = new Server(port);
        server.setHandler(handler);
        server.start();
        server.join();
    }

    public static ServletContextHandler handler(String mapping, String routes) {
        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/");
        handler.setResourceBase("./src/test/webapp/");
        ServletHolder holder = new ServletHolder(new Servlet());
        holder.setInitParameter("routes", routes);
        handler.addServlet(holder,mapping);
        return handler;
    }

    public static WebAppContext webapp() {
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setDescriptor("src/test/webapp/WEB-INF/web.xml");
        root.setResourceBase("src/test/webapp/");
        root.setParentLoaderPriority(true);
        return root;
    }

}