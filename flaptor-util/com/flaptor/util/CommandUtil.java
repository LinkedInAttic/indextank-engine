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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

/**
 * utility class for executing external commands
 * 
 * @author Martin Massera
 */
public class CommandUtil {
	
	/**
	 * executes an external command and waits for it to finish 
	 * @param command the command to be executed
	 * @param workingDirectory the directory to execute the command from (can be null)
	 * @return a triad of return code, stdout contents, stderr contents
	 * @throws IOException 
	 */
	public static Triad<Integer, String, String> execute(String command, File workingDirectory) throws IOException {
		int retCode;
		Runtime rt = Runtime.getRuntime(); 
		Process p = workingDirectory != null ? rt.exec(command, new String[0], workingDirectory) : rt.exec(command);
		return waitFor(p, -1);
	}
	
    /**
     * Executes a native command and logs errors if they appear. Opposite to execute(String command, File workingDirectory), it doesnt throw exceptions 
     * 
     * @param command the shell command and its arguments
	 * @param workingDirectory the directory to execute the command from (can be null)
	 * @param logger a logger to log errors to
     */
    public static Triad<Integer, String, String> execute(String command, File workingDirectory, Logger logger) {
        try {
	    	Triad<Integer, String, String> ret = execute(command, workingDirectory);
	
	    	if (ret.first() != 0) {
	            logger.error("error executing " + command + " in dir " + workingDirectory + " - stdOut: " + ret.second());
	            logger.error("error executing " + command + " in dir " + workingDirectory + " - stdErr: " + ret.third());
	    	}
	    	
	    	return ret;
         } catch (IOException e) {
            logger.error("Exception while executing shell command: " +command,e);
            return new Triad<Integer, String, String>(-1, null, null);
        }
    }
    
    /**
     * executes a remote ssh command
     * @param host 
     * @param port -1 for standard port
     * @param command
     * @param msTimeout a timeout in MS, -1 for no timeout
	 * @return a triad of return code, stdout contents, stderr contents
     * @throws IOException 
     */
    public static Triad<Integer, String, String> remoteSSHCommand(String host, int port, String command, int msTimeout) throws IOException {
    	String hostPort = host + (port > 0 ? (":"+port) : "");
    	   	
    	Process p = Runtime.getRuntime().exec("ssh -o NumberOfPasswordPrompts=0 " + hostPort);
    	p.getOutputStream().write((command+"\nexit\n").getBytes());
    	p.getOutputStream().flush();
		return waitFor(p, msTimeout);
    }

    /**
	 * waits for a process to finish and reads stdout and stderr
	 * @param p
	 * @return a triad of return code, stdout contents, stderr contents
	 * @throws IOException
	 */
	public static Triad<Integer, String, String> waitFor(Process p, int ms) throws IOException{
		int retCode = -1;
		long start = System.currentTimeMillis();
		while(true) {
			try {
				retCode = p.exitValue();
				break;
			} catch(IllegalThreadStateException e) {
				if (ms >0 && System.currentTimeMillis() - start > ms) {
					p.destroy();
					throw new IOException("process timeout, process forcedly ended"); 
				}
				ThreadUtil.sleep(50);
			}
		}

        InputStreamReader isri = new InputStreamReader(p.getInputStream());
        InputStreamReader isre = new InputStreamReader(p.getErrorStream());

        try { 
    		String stdout = IOUtil.readAll(isri);
	    	String stderr = IOUtil.readAll(isre);
		    return new Triad<Integer, String, String>(retCode, stdout, stderr);

        } catch (IOException e) {
            // LOG?
            throw e;
        } finally {
            Execute.close(isri);
            Execute.close(isre);
        }
		
	}
}
