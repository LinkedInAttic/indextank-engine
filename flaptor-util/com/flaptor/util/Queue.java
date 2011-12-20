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

import java.util.LinkedList;

/**
  This class implements a simple queue, designed to be thread-safe.
 */

public class Queue<V> {

    private LinkedList<V> q;
    private boolean sizeLimited=false;
    private int limit=0;

    /**
        Creates a queue with no size limit.
    */
    public Queue() {
        q = new LinkedList<V>();
    }

    /**
        Creates a queue with a maximum size.
        @param maxSize the maximum number of elements the queue can have.
    */
    public Queue(int maxSize){
        limit=maxSize;
        sizeLimited=true;
        q = new LinkedList<V>();
    }        

    /**
        Enqueues an object.
        Blocking. If the queue is size limited and the maximum size has been
        reached, it block until there is space.
        @param o the object to enqueue
    */
    public synchronized void enqueueBlock(V o){
        while ((sizeLimited) && (q.size() >= limit)){
            try{
                wait();
            }catch(InterruptedException e){/*We ignore the wakeup*/}
        }
        q.add(o);
        notifyAll();
    }

    /**
        Enqueues an object.
        Blocking. If the queue is size limited and the maximum size has been
        reached, it block until there is space OR until the maximum time has
        elapsed.
        @param o the object to enqueue
        @param maxWait the maximum time to wait for space to be available.
        @return true if the object was enqueued, false otherwise.
    */
    public synchronized boolean enqueueBlock(V o, long maxWait){
        long start, elapsed;
        start= System.currentTimeMillis();
        elapsed=0;
        while ((sizeLimited) && (q.size() >= limit) && (elapsed < maxWait)){
            try{
                wait(maxWait-elapsed);
            }catch(InterruptedException e){/*We ignore the wakeup*/}
            elapsed= System.currentTimeMillis()-start;
        }
        if (elapsed < maxWait){
            q.add(o);
            notifyAll();
            return true;
        }else
            return false;
    }
    
    /**
        Enqueues an object.
        Non blocking version. If there is enough space to enqueue the object,
        it's enqueued and true is returned. If there's no space, nothing is
        enqueued and false is returned.
        @param o the object to enqueue
        @return true if the object was enqueued, false otherwise.
    */
    public synchronized boolean enqueueNoBlock(V o){
        if ((q.size() < limit) || (!sizeLimited)){
            q.add(o);
            notifyAll();
            return true;
        }else
            return false;
    }
    
    /**
        Dequeues an object.
        Non blocking version. Return null if there's nothing to dequeue.
        Warning: if the queue is used to store null values there's no way
        to distinguish the "no objects left" return from the "null object
        dequeued" return.
        @return the object dequeued (if there was at least one) or null if
        the queue was empty.
    */
    public synchronized V dequeueNoBlock(){
        if (q.size() == 0)
            return null;
        V ret=q.remove(0);
        notifyAll();
        return ret;
    }

    /**
        Dequeues an object.
        Blocking version. If there are no objects in the queue, it blocks itself
        until someone enqueues something.
        @return the object dequeued.
    */
    public synchronized V dequeueBlock(){
        while (q.size() == 0){
            try{
                wait();
            }catch(InterruptedException e){/*We ignore the wakeup*/}
        }
        V ret=q.remove(0);
        notifyAll();
        return ret;
    }

    /**
        Dequeues an object.
        Blocking version. If there are no objects in the queue it block
        until someone puts something OR the maximum wait time has elapsed.
        Warning if the queue is used to store null values there's no way
        to distinguish the "no objects left" return from the "null object
        dequeue" return.
        @param maxWait the maximum time to wait for data to be available
        in milliseconds.
        @return the object dequeued OR null the maximum wait time has elapsed
        with no data available.
    */
    public synchronized V dequeueBlock(long maxWait){
        long start, elapsed;
        start= System.currentTimeMillis();
        elapsed=0;
        while ((q.size() == 0) && (elapsed < maxWait)){
            try{
                wait(maxWait-elapsed);
            }catch(InterruptedException e){/*We ignore the wakeup*/}
            elapsed= System.currentTimeMillis()-start;
        }
        if (q.size() == 0)
            return null;
        else{
            V ret=q.remove(0);
            notifyAll();
            return ret;
        }
    }
    
    /**
        Return the number of objects in the queue
    */
    public synchronized int size() {
        return q.size();
    }

    /**
       Returns true if the queue is full, false otherwise. This method is not synchronized,
	   so the return value only makes sense if we know that nobody is dequeuing elements.
    */
    public boolean isFull() {
        return q.size() == limit;
    }
}
