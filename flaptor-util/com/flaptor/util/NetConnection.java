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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.log4j.Logger;


/**
 * This class provides methods to send and receive messages, files and directories through the network.
 */
public class NetConnection implements Closeable {

    private static final Logger logger = Logger.getLogger(Execute.whoAmI());
    private static int BUFFER_SIZE = 2048; // The size of the buffer used to send and receive data.
    // This map holds the server sockets for each port, which must be reused in successive listening sessions.
    private static HashMap<Integer,ServerSocket> socketMap = new HashMap<Integer,ServerSocket>();
    // Specific data for the connection that this instance represents.
    private Socket socket;
    private String remoteIP;
    private BufferedInputStream inputStream;
    private BufferedOutputStream outputStream;
    private BufferedReader reader;
    private PrintWriter writer;


    /**
     * Creates an instance that is not connected.
     * After calling this constructor, the connection needs to be opened.
     * @see #open
     */
    public NetConnection () {
        socket = null;
    }


    /** 
     * Listens for an incomming connection on a given port and returns a NetConnection instance for that connection.
     * After timeout milliseconds pass without an incomming connection, it returns null. This is an
     * oportunity to check for other conditions, for example a program termination signal.
     * @param port the port where we expect the incomming connection.
     * @param timeout the timeout in milliseconds.
     * @return an NetConnection instance with the open connection to the remote party, or null if the connection timed out.
     */
    public static NetConnection listen (int port, int timeout) throws IOException, SocketException {
//System.out.println(caller()+": Listening for connections on port "+port+" with timeout="+timeout);
        NetConnection conn = null;
        ServerSocket serverSocket = socketMap.get(port);
        if (null == serverSocket) {
            serverSocket = new ServerSocket(port);
            socketMap.put(port, serverSocket);
        }
        serverSocket.setSoTimeout(timeout);
        Socket socket = null;
        try {
            socket = serverSocket.accept();
        } catch (SocketTimeoutException ste) {/*ignore*/}
        if (null != socket) {
//System.out.println(caller()+": Connection came in! ["+socket+"]");
            conn = new NetConnection();
            conn.open(socket);
        }
        return conn;
    }


    /** 
     * Opens a connection to the provided server on the provided port.
     * A NetConnection on the other side must be listening.
     * @param server the ip address of the server.
     * @param port the port where the server is listening.
     */
    public void open (String server, int port) throws UnknownHostException, IOException {
        open(new Socket(server,port));
    }


    // Opens the connection on the provided socket.
    private void open (Socket socket) throws IOException {
        this.socket = socket;
        remoteIP = socket.getInetAddress().toString();
        inputStream = new BufferedInputStream(socket.getInputStream());
        outputStream = new BufferedOutputStream(socket.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(inputStream));
        writer = new PrintWriter(new OutputStreamWriter(outputStream));
//System.out.println(caller()+": Opened connection to "+remoteIP+" ["+socket+"]");
    }


    /** 
     * Closes the connection.
     */
    public void close () {
        try { socket.shutdownOutput(); } catch (IOException e) {/*ignore*/}
        Execute.close(reader);
        Execute.close(writer);
        Execute.close(inputStream);
        Execute.close(outputStream);
//System.out.println(caller()+": Closed connection to "+remoteIP+" ["+socket+"]");
        Execute.close(socket);
    }


    /** 
     * Sends a string message through the connection.
     * @param message the message to send.
     */
    public void sendMessage (String message) throws IOException {
//System.out.println(caller()+": Sending message ["+message+"] to "+remoteIP+" ["+socket+"]");
        writer.write(message+"\n");
        writer.flush();
    }


    /** 
     * Waits for a message from the other party.
     * @return the recieved message.
     */
    public String getMessage () throws IOException {
        String message = reader.readLine();
//System.out.println(caller()+": Recieved message ["+message+"] from "+remoteIP+" ["+socket+"]");
        return message;
    }


    /** 
     * Sends a file through the connection.
     * @param filename the path to the file to send. The filename should not contain the '&' character.
     */
    public void sendFile (String filename) throws IOException {
//System.out.println(caller()+": Sending file "+filename+" to "+remoteIP+" ["+socket+"]");
        BufferedInputStream filedata = null;
        try {
            File file = new File(filename);
            filedata = new BufferedInputStream (new FileInputStream(file));
            sendMessage("name="+file.getName()+"&size="+filedata.available());
            if (!"start".equals(getMessage())) {
                throw new IOException("Remote party ("+remoteIP+") unable receive file "+filename);
            }

            int dataTransferred = 0;
            int count;
            byte[] data = new byte[BUFFER_SIZE];
            while ((count = filedata.read(data, 0, BUFFER_SIZE)) != -1) {
                outputStream.write(data,0,count);
                dataTransferred += count;
            }
            outputStream.flush();
            writer.flush();
            filedata.close();
        } finally {
            Execute.close(filedata);
        }
    }


    /** 
     * Waits for a file from the other party.
     * @param where the path to the local directory where the incomming file will be placed.
     * @return the path to the file that has been transfered.
     */
    public String getFile (String where) throws IOException, FileNotFoundException {
        String fileMsg = getMessage();
        if (null == fileMsg || !fileMsg.matches("^name=[a-zA-Z0-9_\\-/\\.]*&size=\\d*$")) {
            throw new IOException("Remote party ("+remoteIP+") not ready to send file");
        }
        String[] filespec = fileMsg.split("&");
        String filename = filespec[0].split("=")[1];
        long filesize = Long.parseLong(filespec[1].split("=")[1]);

        File target = new File(where,filename);
        BufferedOutputStream filedata = new BufferedOutputStream(new FileOutputStream(target));
        sendMessage("start");
        
        long dataTransferred = 0;
        int count;
        byte[] data = new byte[BUFFER_SIZE];
        while (dataTransferred < filesize) {
            if ((count = inputStream.read(data, 0, BUFFER_SIZE)) != -1) {
                filedata.write(data,0,count);
                dataTransferred += count;
            } else {
                try { Thread.sleep(1000); } catch (Exception e) { /*do nothing*/ }
            }
        }

        filedata.flush();
        filedata.close();
//System.out.println(caller()+": Recieved file "+filename+" from "+remoteIP+" ["+socket+"]");
        return target.getAbsolutePath();
    }


    /** 
     * Sends a directory through the connection.
     * @param dir the path to the dir to send.
     */
    public void sendDir (String dir) throws IOException {
//System.out.println(caller()+": Sending dir "+dir+" to "+remoteIP+" ["+socket+"]");
        String zipname = dir + ".zip";
        try {
            ZipUtil.zipDirectory(dir, zipname);
            sendFile (zipname);
        } finally {
            FileUtil.deleteFile(zipname);
        }
    }


    /** 
     * Waits for a directory from the other party.
     * @param where the path to the local directory where the incomming directory will be placed.
     * @return the path to the directory that has been transfered.
     */
    public String getDir (String where) throws IOException, FileNotFoundException {
        String dirname = null;
        String zipname = getFile(where);
        if (zipname.endsWith(".zip")) {
            try {
                ZipUtil.unzipFile(zipname);
                dirname = zipname.substring(0,zipname.length()-4);
            } finally {
                FileUtil.deleteFile(zipname);
            }
        } else {
            throw new IOException("File received is not a zip file ("+zipname+")");
        }
//System.out.println(caller()+": Recieved dir "+dirname+" from "+remoteIP+" ["+socket+"]");
        return dirname;
    }


    /**
     * Returns the IP of the remote party.
     * @return the IP of the remote party.
     */
    public String getRemoteIP () {
        return remoteIP;
    }

    private static String caller() {
        String name = "???";
        StackTraceElement[] stack = new Throwable().getStackTrace();
        for (StackTraceElement elem : stack) {
            String[] parts = elem.getClassName().split("\\.");
            String[] sub = parts[parts.length-1].split("\\$");
            name = sub[0];
            if (!"NetConnection".equals(name)) {
                break;
            }
        }
        return name;
    }

}

