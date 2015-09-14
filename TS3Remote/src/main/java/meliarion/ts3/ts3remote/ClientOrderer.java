package meliarion.ts3.ts3remote;

import android.util.Log;

import java.util.Comparator;

/**
 * Created by Meliarion on 01/09/13.
 * Class which sorts clients into the same order as the teamspeak client
 */
public class ClientOrderer implements Comparator<Integer> {
    private final ServerConnection connection;

    public ClientOrderer(ServerConnection _connection){
        this.connection=_connection;
    }

    @Override
    public int compare(Integer clientID1, Integer clientID2) {
        try{
        TSClient client1= connection.getClientByCLID(clientID1);
        TSClient client2=connection.getClientByCLID(clientID2);
        int talk1 = client1.getTalkPower();
        int talk2 = client2.getTalkPower();

        if (talk1 < talk2)
        {   Log.d("ClientOrderer","Comparing "+client1.getName()+"("+talk1+") and "+client2.getName() + "("+talk2+"). Returning 1");
            return 1;
        }
        else if (talk1>talk2)
        {   Log.d("ClientOrderer","Comparing "+client1.getName()+"("+talk1+") and "+client2.getName() + "("+talk2+"). Returning -1");
            return -1;
        }
        else {
            return compareTalkPrivilege(client1, client2);
        }
        }
        catch (ServerConnection.SCException ex)
        {
            Log.w("ClientOrderer", "unable to retrieve client from client list. ", ex);
        return 0;
        }

    }
    private int compareTalkPrivilege(TSClient client1, TSClient client2)
    {
        Log.d("ClientOrderer","Comparing "+client1.getName()+" and "+client2.getName() + ". Now checking for talk privileges.");
        if (client1.isTalker()&&!client2.isTalker())
        {Log.d("ClientOrderer","Comparing "+client1.getName()+"("+client1.isTalker()+") and "+client2.getName() + "("+client2.isTalker()+"). Returning -1.");
            return -1;
        }
        else if(client2.isTalker()&&!client1.isTalker())
        {Log.d("ClientOrderer","Comparing "+client1.getName()+"("+client1.isTalker()+") and "+client2.getName() + "("+client2.isTalker()+"). Returning 1.");
            return 1;
        }
        else
        {
            return client1.getName().compareToIgnoreCase(client2.getName());
            //return compareIDs(client1,client2);
        }
    }
    private int compareIDs (TSClient client1, TSClient client2)
    {   int clientID1 = client1.getClientID();
        int clientID2 = client2.getClientID();
        Log.d("ClientOrderer","Comparing "+client1.getName()+"("+client1.isTalker()+") and "+client2.getName() + "("+client2.isTalker()+"). Now comparing client IDs.");
        if(clientID1<clientID2)
        {
            Log.d("ClientOrderer","Comparing "+client1.getName()+"("+clientID1+") and "+client2.getName() + "("+clientID2+"). Returning 1");
            return 1;
        }
        else if (clientID1>clientID2)
        {
            Log.d("ClientOrderer","Comparing "+client1.getName()+"("+clientID1+") and "+client2.getName() + "("+clientID2+"). Returning -1");
            return -1;
        }
        else
        {
            Log.d("ClientOrderer","Comparing "+client1.getName()+"("+clientID1+") and "+client2.getName() + "("+clientID2+"). Returning 0");
            return 0;
        }
    }
    @Override
    public String toString(){
        return "Client orderer for "+connection.toString();
    }

}
