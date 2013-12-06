package meliarion.ts3.ts3remote;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class that represents an unencrypted connection to the remote manager
 * Created by Meliarion on 26/11/13.
 */
public class ManagedSocketNetworkInterface extends SocketNetworkInterface  implements NetworkInterface {
    public ManagedSocketNetworkInterface(String _ip) throws IOException {
        super(_ip, 25740);
    }

    @Override
    public boolean verifyConnect(String data,String[] SCHandlers) {
        String tBuffer = data.trim();
        String patternString= "";
            patternString="TS3 Remote Manager\\s+TS3 remote connected successfully\\s+selected schandlerid=(\\d+)\\s+(.*)";
        Pattern pattern = Pattern.compile(patternString);
        Matcher m = pattern.matcher(tBuffer);
        if (m.matches()){
            SCHandlers[0]= m.group(1);
            SCHandlers[1] = m.group(2);
            return true;
        }
        else
        {
            return false;
        }
    }
}
