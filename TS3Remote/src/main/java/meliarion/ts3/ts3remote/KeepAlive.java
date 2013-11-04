package meliarion.ts3.ts3remote;

import android.util.Log;

/**
 * Created by Meliarion on 19/09/13.
 * Class that is designed to keep the connection alive by sending a message to the client
 * after a certain amount of time has passed since the last message
 */
class KeepAlive implements Runnable {
    private TS3ClientConnection parent;
    private long keepAliveTime;
    private Thread thisThread;
    private boolean running = true;
    private boolean initialising = true;
    public KeepAlive(TS3ClientConnection _parent, long _keepAliveTime)
    {
        this.parent = _parent;
        keepAliveTime = _keepAliveTime;
    }
    public void addSleepTime(){
        try{
            if (running&&(thisThread !=null)&&(thisThread.getState()== Thread.State.WAITING))
            {
            Log.i("KeepAlive", "Extending sleep time");
            thisThread.interrupt();
            }

        }
        catch (Exception ex)
        {
            Log.e("KeepAlive", "Error adding sleep time. "+ex,ex);
        }
    }
    public void initialisingDone(){
        initialising=false;
    }
    public void close(){
        running = false;
        Log.e("KeepAlive","Closing keep alive thread");
    }
    @Override
    public void run() {
    thisThread = Thread.currentThread();
    String message ="currentschandlerid";
    while (running)
    {  if(initialising)
        {
        continue;
        }
        try{
        Log.i("KeepAlive", "sending message: " + message+" to keep connection alive");
        parent.SendMessage(message);
        }
        catch (Exception ex)
        {
            Log.e("KeepAlive","error sending keep alive message: "+ex,ex);
        }
        finally {
        resetSleepTime();

        }
    }
    Log.e("KeepAlive", "Keep alive thread closed");
    }
    private void resetSleepTime(){
        if (!running)
        {
            return;
        }
        try{
            Thread.sleep(keepAliveTime);
        }
        catch (InterruptedException ex)
        {   Log.e("KeepAlive","sleep event interrupted. "+ex,ex);
            resetSleepTime();
        }
    }

}
