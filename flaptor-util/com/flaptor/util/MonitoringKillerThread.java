//TODO poner licencia

package com.flaptor.util;




/**
 * A Thread that monitor an AStoppable, and when that is stopped,
 * it stops another AStoppable.
 *
 * This is useful for AServer,
 *
 */
public class MonitoringKillerThread extends Thread {

    private Stoppable monitoreable = null;
    private Stoppable killable = null;


    public MonitoringKillerThread(Stoppable toMonitor, Stoppable toKill) {
        this.monitoreable = toMonitor;
        this.killable = toKill;
        this.setDaemon(true);
    }


    public void run() {
        while (true) {
            if (monitoreable.isStopped()) {

                if (!killable.isStopped()) {
                    killable.requestStop();
                }
                
                while(!killable.isStopped()) {
                    Execute.sleep(100);
                }
                break;
            }
            Execute.sleep(1000);
        }
    }


}
