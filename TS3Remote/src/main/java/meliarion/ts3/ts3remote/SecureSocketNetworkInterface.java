package meliarion.ts3.ts3remote;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
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
    private String Password;
    private HandshakeCompletedListener listener;

    public SecureSocketNetworkInterface(String _IP, String _password) {
        this.Password = _password;
        listener = new HandshakeCompletedListener() {
            @Override
            public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
                Log.e("SecureSocketNetwork", "Handshake completed");
            }
        };
        initialise(_IP, 25741);
    }

    public SecureSocketNetworkInterface(String _IP, int _port) {
        initialise(_IP, _port);
    }

    private void initialise(String _IP, int port) {
        try {
            IP = _IP;
            Port = port;
            SocketFactory factory = SSLSocketFactory.getDefault();
            SSLsocket = (SSLSocket) factory.createSocket(IP, Port);
            SSLSession session = SSLsocket.getSession();
            String s = session.toString();
            SSLsocket.addHandshakeCompletedListener(listener);
            if (!SSLsocket.isConnected()) {
                SSLsocket.connect(new InetSocketAddress(IP, Port), 5000);
            }
            //SSLsocket.startHandshake();
            out = SSLsocket.getOutputStream();
            //in = SSLsocket.getInputStream();
        } catch (SocketTimeoutException ex) {
            Log.e("SecureSocketNetwork", "The socket timed out trying to connect", ex);
        } catch (SocketException ex) {
            Log.e("SecureSocketNetwork", "A socket exception occurred", ex);
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
