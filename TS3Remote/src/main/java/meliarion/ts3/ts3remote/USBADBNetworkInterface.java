package meliarion.ts3.ts3remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Stub for the future implementation of USBADB connectivity
 * Created by Meliarion on 21/11/13.
 */
public class USBADBNetworkInterface implements NetworkInterface {
    private ServerSocket serverSocket;
    private Socket client;
    private int port=38300;
    private int timeout = 10000;
    public USBADBNetworkInterface() throws IOException
    {
    serverSocket = new ServerSocket(port);
    serverSocket.setSoTimeout(timeout);
        client = serverSocket.accept();
        client.setTcpNoDelay(true);
        serverSocket.close();
    }
    @Override
    public OutputStream getOutputStream() throws IOException {
        return client.getOutputStream();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return client.getInputStream();
    }

    @Override
    public String getConnectionString() {
        return "USB ADB bridge";
    }
    //TODO Complete verifyConnect
    @Override
    public boolean verifyConnect(String data, String[] SCHandlers) {
        return false;
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public boolean usesKeepAlive() {
        return true;
    }
}
