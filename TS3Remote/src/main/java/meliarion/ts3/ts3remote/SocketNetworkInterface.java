package meliarion.ts3.ts3remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        socket = new Socket(_IP,_Port);
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

    @Override
    public boolean verifyConnect(String data, String[] SCHandlers) {
        String tBuffer = data.trim();
        String patternString="TS3 Client\\s+Welcome to the TeamSpeak 3.+\\s+selected schandlerid=(\\d+)\\s+(.*)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher m = pattern.matcher(tBuffer);
        if (m.matches()){
            SCHandlers[0]=m.group(1);
            SCHandlers[1] = m.group(2);
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
