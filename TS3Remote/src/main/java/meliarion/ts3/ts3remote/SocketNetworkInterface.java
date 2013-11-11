package meliarion.ts3.ts3remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by Meliarion on 11/11/13.
 */
public class SocketNetworkInterface implements NetworkInterface {
    private Socket socket;
    private int Port;
    private String IP;
    public SocketNetworkInterface(String _IP, int _Port) throws IOException
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
    public boolean isConnected() {
        return socket.isConnected();
    }
}
