package meliarion.ts3.ts3remote;

import android.util.Log;

import java.util.Comparator;

/**
 * Created by Meliarion on 09/09/13.
 * Class which sorts channels into the same order as the teamspeak client
 */
public class ChannelOrderer implements Comparator<Integer> {
    private ServerConnection connection;
    public ChannelOrderer (ServerConnection _connection)
    {this.connection = _connection;

    }
    @Override
    public int compare(Integer cid1, Integer cid2) {
        try {
        TSChannel channel1 = connection.getChannelByID(cid1);
        TSChannel channel2 = connection.getChannelByID(cid2);
        int cOrder1 = channel1.getOrder();
        int cOrder2 = channel2.getOrder();
        if(cOrder1>cOrder2)
        {
            return 1;
        }
        else if (cOrder1<cOrder2)
        {
            return -1;
        }
        else
        {
            return compareNames(channel1, channel2);
        }
        }
        catch (Exception e)
        {
            Log.e("ChannelOrderer","Error sorting channels",e);
            return 0;
        }
    }
    private int compareNames (TSChannel channel1,TSChannel channel2){
        return channel1.getName().compareToIgnoreCase(channel2.getName());
    }
    @Override
    public String toString(){
    return "Channel orderer for "+connection.toString();
    }
}
