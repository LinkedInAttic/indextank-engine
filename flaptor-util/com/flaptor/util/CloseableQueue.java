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


/**
 * This class extends Queue, adding "close" features.
 * 
 * This queue has 3 states: OPEN, CLOSING and CLOSED.
 * The only state that allows enqueue operations is OPEN.
 * After executing CloseableQueue.close() the enqueued elements
 * can still be retrieved. The queue will have CLOSING state after a 
 * close() call, until it is empty. In that moment, the state passes
 * to CLOSED.
 *
 *
 *  @todo review this class. Synchronization may not be fine
 *
 */
public class CloseableQueue<V> extends Queue<V>{

    private int status;
    private final static int OPEN = 0;
    private final static int CLOSING = 1;
    private final static int CLOSED = 2;
    

    /**
        Creates a queue with no size limit.
    */
    public CloseableQueue() {
         status = OPEN;
    }

    /**
        Creates a queue with a maximum size.
        @param maxSize the maximum number of elements the queue can have.
    */
    public CloseableQueue(int maxSize){
        super(maxSize);
        status = OPEN;
    }        

    /**
        Enqueues an object.
        Blocking. If the queue is size limited and the maximum size has been
        reached, it block until there is space.
        If the queue gets closed while this call is waiting for space,
        the object will be enqueued as soon as there is space

        @param o the object to enqueue
        @throws IllegalStateException if the queue is not OPEN.
    */
    public synchronized void enqueueBlock(V o){
        if (!(status == OPEN)) throw new IllegalStateException("Closed queue");
        super.enqueueBlock(o);
    }

    /**
        Enqueues an object.
        Blocking. If the queue is size limited and the maximum size has been
        reached, it block until there is space OR until the maximum time has
        elapsed.
        If the queue gets closed while this call is waiting for space,
        the object will be enqueued as soon as there is space
        @param o the object to enqueue
        @param maxWait the maximum time to wait for space to be available.
        @throws IllegalStateException if the queue is not OPEN.
        @return true if the object was enqueued, false otherwise.
    */
    public synchronized boolean enqueueBlock(V o, long maxWait){
        if (!(status == OPEN)) throw new IllegalStateException("Closed queue");
        return super.enqueueBlock(o,maxWait);     
    }
    
    /**
        Enqueues an object.
        Non blocking version. If there is enough space to enqueue the object,
        it's enqueued and true is returned. If there's no space, nothing is
        enqueued and false is returned.
        @param o the object to enqueue
        @throws IllegalStateException if the queue is not OPEN.
        @return true if the object was enqueued, false otherwise.
    */
    public synchronized boolean enqueueNoBlock(V o){
        if (!(status == OPEN)) throw new IllegalStateException("Closed queue");
        return super.enqueueNoBlock(o);
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
        if (status == CLOSED) throw new IllegalStateException("Closed queue");
        V retValue = super.dequeueNoBlock();
        // if it was closing, try to end
        if (CLOSING == status ) this.close();
        return retValue;
    }

    /**
        Dequeues an object.
        Blocking version. If there are no objects in the queue, it blocks itself
        until someone enqueues something.

        This operation is not permitted, to avoid locks. If a thread is waiting
        for the queue to have data, and the queue gets closed, the waiting
        thread will remain blocked forever. Use dequeueBlock(long) instead.
        @throws UnsupportedOperationException allways
        
    */
    public synchronized V dequeueBlock(){
        throw new UnsupportedOperationException("Closeable queues only have dequeueBlock(timeout)");
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
        @throws IllegalStateException if the queue is CLOSED.
        @return the object dequeued OR null the maximum wait time has elapsed
        with no data available.
    */
    public synchronized V dequeueBlock(long maxWait){
        if (CLOSED == status) throw new IllegalStateException("Closed queue");
        V retValue = super.dequeueBlock(maxWait);

        // if it was closing, try to end
        if (CLOSING == status) this.close();

        return retValue;
    }
    
    /**
        Return the number of objects in the queue
    */
    public synchronized int size() {
        return super.size();
    }

    /**
       Returns true if the queue is full, false otherwise. This method is not synchronized,
	   so the return value only makes sense if we know that nobody is dequeuing elements.
    */
    public boolean isFull() {
        return super.isFull();
    }


    /**
     * Closes the queue. If the queue has elements, it state will be 
     * CLOSING. If it is empty, the state will be CLOSED. 
     * */
    public synchronized void close() {
        this.status = ( 0 == size()) ? CLOSED : CLOSING;
    }

    public synchronized boolean isClosed() {
        return (CLOSED == this.status); 
    }



    /**
     * Resets a CLOSED queue to OPEN state.
     * @throws IllegalStateException if the queue is not CLOSED.
     */
    public synchronized void reset() {
        if (!(CLOSED == status)) throw new IllegalStateException("Can not reset a queue that is not closed");
        // else
        this.status = OPEN;
    }

}
