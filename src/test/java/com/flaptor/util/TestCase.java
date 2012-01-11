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


package com.flaptor.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.log4j.PropertyConfigurator;



/**
 * This class replaces the JUnit TestCase and provides test threads management, output
 * filtering, unexpected error detection and a mix of small convenience utilities.
 * 
 * By extending this class instead of the standard JUnit TestCase, any error produced 
 * through log4j will fail the test. 
 * 
 * Expected errors should be filtered using the following methods in the test setUp()
 * or anywhere in the test code:
 *
 *   filterOutput(literal)     omits any output that matches the provided string
 *   filterOutputRegex(regex)  omits any output that matches the provided regular expression
 *   unfilterOutput()          stops filtering
 *
 * Threads in the test code should extend TestThread and implement runTest() and kill().
 * This way, any uncatched exception that is thrown within a thread will fail the test.
 *
 * The kill() method will be called when any thread throws an exception. Also, threads 
 * should exit on InterruptedException and isInterrupted() returns true.
 *
 * The startTestThreads() method starts all instantiated TestThreads.
 * The waitForTestThreads() method blocks until all running TestThreads finish.
 * The runTestThreads() method does both.
 *
 * This class also takes care of the Log4J configuration.
 *
 */
public abstract class TestCase extends junit.framework.TestCase {

    private PrintStream stdOut = System.out;
    private PrintStream stdErr = System.err;

    private String fName;

    /**
     * Initialize the class.
     */
    public TestCase () {
        super();
        fName = null;
        unfilterOutput();
    }

    /**
     * Initialize the class.
     */
    public TestCase (String name) {
        super(name);
        fName = name;
        unfilterOutput();
    }

    /**
     * Configure Log4J.
     */
    private void configureLog4J () {
        String log4jConfigPath = com.flaptor.util.FileUtil.getFilePathFromClasspath("log4j.properties");
        if (null != log4jConfigPath) {
            PropertyConfigurator.configureAndWatch(log4jConfigPath);
        } else {
            System.err.println("log4j.properties not found on classpath!");
        }
    }


    // Output filters.


    /**
     * This class implements a stream that can be filtered with a regular expression.
     */
    private class FilterStream extends PrintStream {
        private Pattern pattern;

        public FilterStream (OutputStream out, String regex) {
            super(out);
            pattern = Pattern.compile(regex, Pattern.COMMENTS);
        }

        public void emit (byte[] buf, int off, int len) {
            super.write(buf,off,len);
        }

        public void match (byte[] buf, int off, int len) {
            // do not emit
        }

        public void nomatch (byte[] buf, int off, int len) {
            emit(buf,off,len);
        }

        public void write (byte[] buf, int off, int len) {
            String line = new String(buf);
            int n = line.indexOf('\n');
            if (n < 0) n = line.length();
            if (n == 0) {
                nomatch(buf,off,len);
            } else {
                line = line.substring(0,n);

                if (pattern.matcher(line).find()) {
                    match(buf,off,len);
                } else {
                    nomatch(buf,off,len);
                }
            }
        }
    }

    /**
     * This filter fails the test if it detects an error message.
     */
    private class ErrorWatchStream extends FilterStream {
        public ErrorWatchStream (OutputStream out) {
            super(out, "^(ERROR|FATAL)\\s\\[");
        }
        public void match (byte[] buf, int off, int len) {
            emit(buf,off,len);
            fail("Unexpected error detected!");
        }
    }

    /**
     * Adds a filter to the output stream (using a literal string).
     * @param literal Anything matching this string will not be shown.
     */
    protected void filterOutput(String literal) {
        filterOutputRegex(Pattern.quote(literal));
    }

    /**
     * Adds a filter to the output stream (using a regular expression).
     * @param regex Anything matching this regular expression will not be shown.
     */
    protected void filterOutputRegex(String regex) {
        System.setOut(new FilterStream(new ErrorWatchStream(stdOut), regex));
        System.setErr(new FilterStream(new ErrorWatchStream(stdErr), regex));
        configureLog4J();
    }

    /**
     * Restores the output stream to its unfiltered state (errors are still detected though).
     */
    protected void unfilterOutput() {
        System.setOut(new ErrorWatchStream(stdOut));
        System.setErr(new ErrorWatchStream(stdErr));
        configureLog4J();
    }


    // Tools for handling threads.


    /** 
     * Threads in a test should extend TestThread, implement the runTest method and exit on 
     * interrupt (isInterrupted, InterruptedException, ClosedByInterruptException).
     * Use runTestThreads to run all instantiated TestThreads (it will block until they finish).
     * Exceptions thrown by the TestThread will cause test failure.
     */

    protected ArrayList<TestThread> threads = new ArrayList<TestThread>();
    private junit.framework.TestResult testResult = null;
    protected volatile boolean error = false;

    /** 
     * Overrides the JUnit run method to take care of the test results.
     */
    public void run (final junit.framework.TestResult result) {
        testResult = result;
        super.run(testResult);
    }

    /**
     * Extend this class to implement threads in the test code.
     */ 
    protected abstract class TestThread extends Thread {

        // Register the thread and set it as daemon.
        public TestThread () {
            threads.add(this);
            setDaemon(true);
        }

        // Here is where the test thread does its thing.
        public abstract void runTest () throws Throwable;

        // Here is where the test thread should stop doing its thing.
        public abstract void kill () throws Throwable;

        // We override run so we can save the test result.
        public void run () {
            try {
                runTest();
            } catch (Throwable t) {
                stopThreads();
                handleException(t);
            }
        } 

        // Start the thread if we are not aborting.
        public void start () {
            synchronized (threads) {
                if (!error) {
                    super.start();
                }
            }
        }
    }

    /**
     * Start all registered threads
     */
    protected void startTestThreads () {
        for (TestThread tt : threads) {
            if (tt.getState() == Thread.State.NEW || tt.getState() == Thread.State.TERMINATED) {
                tt.start();
            }
        }
    }

    /**
     * Wait until all registered threads finish.
     */
    protected void waitForTestThreads () {
        for (TestThread tt : threads) {
            if (tt.getState() != Thread.State.NEW && tt.getState() != Thread.State.TERMINATED) {
                try {
                    tt.join();
                } catch (InterruptedException e) { 
                    /* stop waiting */
                } catch (ThreadDeath t) { 
                    /* stop waiting */
                }
            }
        }

    }

    /**
     * Start all registered threads and wait until they finish.
     */
    protected void runTestThreads () {
        startTestThreads();
        waitForTestThreads();
    }

    /**
     * Report an exception to JUint.
     * @param t the exception to report.
     */
    private void handleException (Throwable t) {
        error = true;
        if (t instanceof junit.framework.AssertionFailedError) {
            testResult.addFailure(this, (junit.framework.AssertionFailedError)t);
        } else {
            testResult.addError(this, t);
        }
    }
    
    
    @Override
    public void runBare() throws Throwable {
        assertNotNull("TestCase.fName cannot be null", fName); // Some VMs crash when calling getMethod(null,null);
        Method runMethod= null;
        try {
            // use getMethod to get all public inherited
            // methods. getDeclaredMethods returns all
            // methods of this class but excludes the
            // inherited ones.
            runMethod= getClass().getMethod(fName, (Class[])null);
        } catch (NoSuchMethodException e) {
            fail("Method \""+fName+"\" not found");
        }
        TestInfo info = runMethod.getAnnotation(TestInfo.class);

        if (hasToRun(info)) {
            //This fragment of code is the original (super.runBare)
            Throwable exception= null;
            setUp();
            try {
                runTest();
            } catch (Throwable running) {
                exception= running;
            }
            finally {
                try {
                    tearDown();
                } catch (Throwable tearingDown) {
                    if (exception == null) exception= tearingDown;
                }
            }
            //This part is also new
            try {
                checkCleanExit(info);
            } catch (Throwable checkingExit) {
                exception = checkingExit;
            }
            
            if (exception != null) throw exception;
        }    
    }
    
    
    private void checkCleanExit(TestInfo info) {
        if (null == info) return;
        if (isSomePortInUse(info.requiresPort())) {
            fail("This test did not clean up some of it ports.");
        }
    }
    
    private boolean hasToRun(TestInfo info) {
        String ttr = System.getProperty("TestsToRun");
        if (null != ttr) {
            assert(ttr.equals(TestInfo.TestType.UNIT.toString())
                    || ttr.equals(TestInfo.TestType.INTEGRATION.toString())
                    || ttr.equals(TestInfo.TestType.SYSTEM.toString())
                    || ttr.equals("ALL"));
            if (null != info) {
                return annotatedTestHasToRun(info, ttr);
            } else {
                System.out.println("Warning: test \"" + fName + "\" without test info. Running it anyway.");
                return true;
            }
        } else {
            return true;
        }
    }
    
    private boolean annotatedTestHasToRun(TestInfo info, String runlevel) {
        if (!runlevel.equals("ALL") && !runlevel.equals(info.testType().toString())) {
            System.out.println("Skipping test  \"" + fName + "\": it is not of the type specified.");
            return false;
        }
        return !isSomePortInUse(info.requiresPort());
    }

    private boolean isSomePortInUse(int[] ports) {
        for (int p : ports) {
            ServerSocket s = null;
            try {
                s = new ServerSocket(p);
            } catch (IOException e) {
                System.out.println("Port " + p + " is in use. " + e);
                return true;
            } finally {
                Execute.close(s);
            }
        }
        return false;
    }

    /**
     * Gets the name of a TestCase
     * @return the name of the TestCase
     */
    @Override
    public String getName() {
        return fName;
    }
    /**
     * Sets the name of a TestCase
     * @param name the name to set
     */
    @Override
    public void setName(String name) {
        fName= name;
        super.setName(name);
    }

    /**
     * Do everything possible to stop all running threads.
     */
    @SuppressWarnings("deprecation")
    private void stopThreads () {
        for (TestThread tt : threads) {
            try {
                tt.interrupt();
                tt.kill();
                tt.stop();
            } catch (ThreadDeath t) { 
                /* expected, ignore */
            } catch (Throwable t) { 
                System.err.println("Exception thrown while stopping a TestThread: "+t);
            }
        }
    }

}
