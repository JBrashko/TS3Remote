package meliarion.ts3.ts3remote;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


/**
 * Stub that represents an encrypted connection to the remote manager
 * Created by Meliarion on 11/11/13.
 */
public class SecureSocketNetworkInterface implements NetworkInterface {
    private SSLSocket SSLsocket;
    private String IP;
    private int Port;
    private OutputStream out;
    private InputStream in;
    public SecureSocketNetworkInterface(String _IP)
    {
        initialise(_IP, 25741);
    }

    public SecureSocketNetworkInterface(String _IP, int _port) {
        initialise(_IP, _port);
    }

    private void initialise(String _IP, int port) {
        try {
        IP = _IP;
            SocketFactory factory = SSLSocketFactory.getDefault();
            SSLsocket = (SSLSocket) factory.createSocket(IP, Port);
            SSLsocket.connect(new InetSocketAddress(IP, Port), 5000);
            SSLsocket.startHandshake();
            out = SSLsocket.getOutputStream();
            in = SSLsocket.getInputStream();
        } catch (Exception ex) {
            Log.e("SecureSocketNetwork", "Error establishing connection", ex);
        }
    }
    @Override
    public OutputStream getOutputStream() throws IOException {
        return out;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return in;
    }

    @Override
    public String getConnectionString() {
        return "Secure connection to " + IP + ":" + Port;
    }

    @Override
    public boolean verifyConnect(String data, String[] SCHandlers) {
        return false;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean usesKeepAlive() {
        return true;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
