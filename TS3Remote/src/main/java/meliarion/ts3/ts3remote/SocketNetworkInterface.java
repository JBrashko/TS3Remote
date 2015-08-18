package meliarion.ts3.ts3remote;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that represents a direct connection to the TS3 client
 * Created by Meliarion on 11/11/13.
 */
public class SocketNetworkInterface implements NetworkInterface {
    private Socket socket;
    private int Port;
    private String IP;
    protected int connectionStage =0;
    protected String patternString="TS3 Client\\s+Welcome to the TeamSpeak 3.+\\s+selected schandlerid=(\\d+)\\s+(.*)\\s+(.*)";

    public SocketNetworkInterface(String _IP) throws IOException
    {
       initialise(_IP, 25639);
    }
    protected SocketNetworkInterface(String _IP, int _Port) throws IOException
    {
        initialise(_IP, _Port);
    }
    protected void initialise(String _IP, int _Port) throws IOException
    {
        IP = _IP;
        Port = _Port;
        Log.i("ClientConnection", "Creating socket");
        socket = new Socket();
        Log.i("ClientConnection", "Created socket");
        this.socket.connect(new InetSocketAddress(_IP, _Port), 5000);
        Log.i("ClientConnection", "Connected socket");
        this.socket.setTcpNoDelay(true);
    }

    @Override
    public OutputStream getOutputStream() throws IOException{
        return socket.getOutputStream();
    }

    @Override
    public InputStream getInputStream()throws IOException {
        return socket.getInputStream();
    }

    @Override
    public String getConnectionString() {
        return IP+":"+Port;
    }
    private boolean stageZero(String data)
    {
        return data.equals("TS3 Client");
    }
    @Override
    public boolean verifyConnect(String data, String[] connectInfo) {
        if (connectionStage==0)
        {   connectInfo[0]="response";
            connectInfo[1]="serverconnectionhandlerlist";
            connectionStage++;
            return false;
        }
        else if (connectionStage<4)
        {
            connectInfo[0]="wait";
            connectionStage++;
            return false;
        }
        String tBuffer = data.trim();

        Pattern pattern = Pattern.compile(patternString);
        Matcher m = pattern.matcher(tBuffer);
        if (m.matches()){
            connectInfo[0]=m.group(1);
            connectInfo[1] = m.group(2);
            if(!m.group(3).equals("error id=0 msg=ok"))
            {
                return false;
            }
            return true;
        }
        else {
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    public boolean usesKeepAlive() {
        return true;
    }
}
