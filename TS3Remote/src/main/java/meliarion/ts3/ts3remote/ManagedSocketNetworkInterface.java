package meliarion.ts3.ts3remote;

import java.io.IOException;


/**
 * Class that represents an unencrypted connection to the remote manager
 * Created by Meliarion on 26/11/13.
 */
public class ManagedSocketNetworkInterface extends SocketNetworkInterface  implements NetworkInterface {
    //String patternString="TS3 Remote Manager\\s+TS3 remote connected successfully\\s+selected schandlerid=(\\d+)\\s+(.*)\\s+(.*)";
    String password;
    public ManagedSocketNetworkInterface(String _ip,String _password) throws IOException {
        super(_ip, 25740);
        password = _password;
        patternString="TS3 Remote Manager\\s+TS3 remote connected successfully\\s+selected schandlerid=(\\d+)\\s+(.*)\\s+(.*)";
    }

    @Override
    public boolean verifyConnect(String data, String[] connectInfo) {
        if (password.equals("")) {
        return super.verifyConnect(data, connectInfo);
        }
        else
        {
            return false;
        }
    }

    @Override
    public String getConnectionString() {
        return "Managed connection to " + this.getIP() + ":" + this.getPort();
    }

    @Override
    public String toString() {
        return getConnectionString();
    }
}
