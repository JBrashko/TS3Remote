package meliarion.ts3.ts3remote;

/**
 * Created by Meliarion on 04/11/13.
 * Interface to abstract the TS3 client connection
 */
public interface RemoteNetworkInterface {
    public ServerConnection getSCHandler(int SCID)throws TSRemoteException;
    public void SendCQMessage(String message);
}
