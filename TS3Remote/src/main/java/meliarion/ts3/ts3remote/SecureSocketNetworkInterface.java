package meliarion.ts3.ts3remote;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.SocketFactory;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;


/**
 * Class that represents an encrypted connection to the remote manager
 * Created by Meliarion on 11/11/13.
 */
public class SecureSocketNetworkInterface implements NetworkInterface {
    private SSLSocket SSLsocket;
    private String IP;
    private int Port;
    private OutputStream out;
    private InputStream in;
    private String Password;
    private TrustManagerFactory tmf;
    private int connectionStage;
    private HandshakeCompletedListener listener;

    public SecureSocketNetworkInterface(String _IP, String _password, TrustManagerFactory factory) {
        this.tmf = factory;
        this.Password = _password;
        initialise(_IP, 25741);
    }

    public SecureSocketNetworkInterface(String _IP, int _port, String _password, TrustManagerFactory factory) {
        this.tmf = factory;
        this.Password = _password;
        initialise(_IP, _port);
    }


    private void initialise(String _IP, int port) {
        try {
            listener = new HandshakeCompletedListener() {
                @Override
                public void handshakeCompleted(HandshakeCompletedEvent handshakeCompletedEvent) {
                    Log.e("SecureSocketNetwork", "Handshake completed");
                }
            };
            IP = _IP;
            Port = port;
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

            SocketFactory factory = context.getSocketFactory();
            SSLsocket = (SSLSocket) factory.createSocket(IP, Port);
            SSLsocket.addHandshakeCompletedListener(listener);

            String[] suites = SSLsocket.getEnabledCipherSuites();
            SSLsocket.startHandshake();
            SSLSession session = SSLsocket.getSession();
            String s = session.toString();

            if (!SSLsocket.isConnected()) {
                SSLsocket.connect(new InetSocketAddress(IP, Port), 5000);
            }
            //SSLsocket.startHandshake();
            out = SSLsocket.getOutputStream();
            in = SSLsocket.getInputStream();
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
    public boolean verifyConnect(String data, String[] connectInfo) {
        if (connectionStage == 0) {
            connectInfo[0] = "response";
            connectInfo[1] = "serverconnectionhandlerlist";
            connectionStage++;
            return false;
        } else if (connectionStage < 4) {
            connectInfo[0] = "wait";
            connectionStage++;
            return false;
        }
        String tBuffer = data.trim();

        Pattern pattern = Pattern.compile("TS3 Remote Manager\\s+TS3 remote connected successfully\\s+selected schandlerid=(\\d+)\\s+(.*)\\s+(.*)");
        Matcher m = pattern.matcher(tBuffer);
        if (m.matches()) {
            connectInfo[0] = m.group(1);
            connectInfo[1] = m.group(2);
            return m.group(3).equals("error id=0 msg=ok");
        } else {
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        return SSLsocket.isConnected();
    }

    @Override
    public boolean usesKeepAlive() {
        return true;
    }

    @Override
    public String toString() {
        return getConnectionString();
    }
}
