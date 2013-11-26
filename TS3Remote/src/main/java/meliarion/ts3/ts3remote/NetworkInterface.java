package meliarion.ts3.ts3remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Meliarion on 11/11/13.
 * Provides an interface for the connection to communicate with the client/remote
 */
public interface NetworkInterface {
    OutputStream getOutputStream()throws IOException;
    InputStream getInputStream()throws IOException;
    String getConnectionString();
    boolean verifyConnect(String data,String[] SCHandlers);
    boolean isConnected();
    boolean usesKeepAlive();
}
