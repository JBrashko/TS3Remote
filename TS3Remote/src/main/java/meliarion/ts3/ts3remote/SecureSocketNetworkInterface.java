package meliarion.ts3.ts3remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLSocket;

/**
 * Stub that represents an encrypted connection to the remote manager
 * Created by Meliarion on 11/11/13.
 */
public class SecureSocketNetworkInterface implements NetworkInterface {
    private SSLSocket socket;
    private String IP;
    private int Port;
    public SecureSocketNetworkInterface(String _IP)
    {
        IP = _IP;
    }
    @Override
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public String getConnectionString() {
        return null;
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
