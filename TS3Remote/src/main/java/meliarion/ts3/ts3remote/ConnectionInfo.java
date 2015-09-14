package meliarion.ts3.ts3remote;

import javax.net.ssl.TrustManagerFactory;

/**
 * Class designed to hold the info required to connect
 * Created by James on 14/09/2015.
 */
public class ConnectionInfo {
    private static String IP;

    public String getIP() {
        return IP;
    }

    public String getPassword() {
        return Password;
    }

    public TrustManagerFactory getTrustManagerFactory() {
        return trustManagerFactory;
    }

    public ClientConnectionType getType() {
        return type;
    }

    private static String Password;
    private static TrustManagerFactory trustManagerFactory;
    private static ClientConnectionType type;

    //Todo: Add some validation
    public ConnectionInfo(String _IP, String _Password, TrustManagerFactory _TMF, ClientConnectionType _type) {
        IP = _IP;
        Password = _Password;
        trustManagerFactory = _TMF;
        type = _type;
    }
}
