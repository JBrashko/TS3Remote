package meliarion.ts3.ts3remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Stub for the future implementation of USB connectivity
 * Created by Meliarion on 21/11/13.
 */
public class USBNetworkInterface implements NetworkInterface {
    public USBNetworkInterface()
    {

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
        return false;
    }
}
