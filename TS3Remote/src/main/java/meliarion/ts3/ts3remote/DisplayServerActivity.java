package meliarion.ts3.ts3remote;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Meliarion on 05/06/13.
 * Activity which handles the UI for the teamspeak remote
 */
public class DisplayServerActivity extends FragmentActivity implements RemoteUserInterface, ChatFragment.OnFragmentInteractionListener {
    private PersistantFragmentTabHost mTabHost;
    private ClientConnectionInterface networkInterface;
    private Thread networkThread;
    private int rec = 0;
    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg){ //arg1 is the server connection
            int i = msg.what;
        TSMessageType mType = TSMessageType.get(i);
        TSServerView serverView = (TSServerView) findViewById(R.id.serverView);
            String origin;
            if (mType.equals(TSMessageType.ClientConnectionAltered)){
                serverView.clientConnectionClosed();
                return;
            }
            try{
            switch (mType){
                case DebugMessage:
                origin = "Debug message";
                break;
                case GeneralMessage:
                origin = "General message";
                break;
                case SetupMessage:
                origin = "Setup Message";
                break;
                case ChatMessage:
                origin = "Chat message";
                ShowChatMessage(msg);
                break;
                case ClientAltered:
                origin = "Client status changed";
                break;
                case SetupDone:
                    if(msg.arg2==1){
                origin = "Server connection id="+msg.arg1+" setup is done";
                    }
                    else
                    {
                origin = "Client connection setup is done";
                    }
                break;
                default:
                origin = "Unknown origin";
                break;
            }

                String text;
                if (msg.obj.getClass() == String.class) {
                    text = (String) msg.obj;
                } else if (msg.obj.getClass() == HashMap.class) {
                    text = (String) (((HashMap) msg.obj).get("type"));
                } else {
                    text = "Message object is not a valid class type. Object type is: " + msg.obj.getClass().getName();
                    Log.e("DisplayServerActivity", text);
                }
//            String message = origin+": "+text;//textView.getText().toString().trim();
                Log.d("MessageHandler","Message of type "+origin+" received : "+text);

                if(msg.arg1!=-1){
                    ServerConnection used = networkInterface.getSCHandler(msg.arg1);

                    if(serverView.getSCHandlerid()!=used.getID()){
                    serverView.setHandler(used);
                    }
                    if(used.isConnected())
                    {
                       switch (mType){
                        case ClientAltered:
                        serverView.clientChanged(msg.arg2);//arg2 is client id
                        break;
                        case ClientMoved:
                        serverView.clientMoved(msg.arg2);//arg2 is client id
                        break;
                        case ClientLeft:
                        serverView.clientLeft(msg.arg2);//arg2 is client id
                        break;
                        case ClientJoined:
                        serverView.clientJoined(msg.arg2);//arg2 is client id
                        break;
                        case ChannelCreated:
                        serverView.channelCreated(msg.arg2);//arg2 is channel id
                        break;
                        case ChannelAltered:
                        serverView.channelChanged(msg.arg2);//arg2 is channel id
                        break;
                        case SetupDone:
                        serverView.setHandler(used);
                        break;
                        case DisplayedServerConnectionChange:
                        break;
                        default:
                        serverView.test();
                        break;

                    }
                       // update(used.DisplayServer());
                    }
                    else
                    {   serverView.setHandler(used);
                      //  update("Displayed server connection is not connected");
                    }
                }
            }
            catch (Exception e)
            {Thread thisThread = Thread.currentThread();
                String error = "A failure occurred on thread:"+thisThread.getName()+" :"+e;
                Log.e("Handle message failure", error,e);
                update("Server updateGroup failure "+e);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the client_ip from the intent
        Intent intent = getIntent();
        String client_ip = intent.getStringExtra(LauncherActivity.CLIENT_IP);
        ClientConnectionType type = ClientConnectionType.get(intent.getIntExtra(LauncherActivity.CONNECTION_TYPE,-1));
        setContentView(R.layout.activity_connection);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String returnMsg;
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
        { try{
            TS3ClientConnection connection = new TS3ClientConnection(client_ip, this,type);
            networkThread = new Thread(connection);
            networkThread.setName("Network Thread");
            networkThread.start();
            networkInterface = connection;
            }
            catch (Exception e)
            {
                Log.e("DisplayServerActivity","An error occurred " + e.toString(),e);
            }
        }
         else
        {
            Log.e("DisplayServerActivity", "Connection failed");
        }
        //Tab setup
        addChatTab("serverChat", "Server Chat");
        addChatTab("channelChat", "Channel Chat");
    }

    private void addChatTab(String tag, String label) {
        mTabHost = (PersistantFragmentTabHost) findViewById(R.id.fragChatTabhost);
        mTabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);
        TabHost.TabSpec tabChat = mTabHost.newTabSpec(tag);
        tabChat.setIndicator(label, null);
        mTabHost.addTab(tabChat, ChatFragment.class, null);
    }

    private void addChatTab(String tag, String label, String content) {
        Bundle bundle = new Bundle();
        bundle.putString("content", content);
        mTabHost = (PersistantFragmentTabHost) findViewById(R.id.fragChatTabhost);
        mTabHost.setup(this, getSupportFragmentManager(), android.R.id.tabcontent);
        TabHost.TabSpec tabChat = mTabHost.newTabSpec(tag);
        tabChat.setIndicator(label, null);
        mTabHost.addTab(tabChat, ChatFragment.class, bundle);
    }
    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    protected void update(String data)
    {
    }

    @Override
    public void Received(String data)
    { try{
        Message msg = mHandler.obtainMessage(0,-1,0,data);// Message.obtain(this.mHandler,1,1,1,data);
       msg.sendToTarget();
    }
        catch (Exception ex){
            Log.e("DisplayServerActivity","Error sending message", ex);
        }

    }

    public Handler getHandler(){
        return mHandler;
    }



    public boolean onOptionsItemSelected (MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void TestButton (View view)
    {
        // TSServerView serverView = (TSServerView) findViewById(R.id.serverView);
        // serverView.refresh();
        try {
            FragmentManager manager = getSupportFragmentManager();
            TextView Chat = (TextView) findViewById(R.id.ChatTextView);
            ChatFragment as = (ChatFragment) manager.findFragmentByTag("serverChat");
            View v = as.getView();
            TextView f = (TextView) v.findViewById(R.id.chat);
            Chat.append(f.getText());
        } catch (NullPointerException ex) {
            Log.e("DisplayServerActivity", "An error occoured when the test button was clicked", ex);
        }
    }

    public void SendMessage (View view){
        try{
        EditText editText = (EditText) findViewById(R.id.commands);
        String message = editText.getText().toString();
        networkInterface.SendCQMessage(message);
        }
        catch (Exception ex)
        {
            Log.e("DisplayServerActivity","Error sending message",ex);
        }
   }
    private void ShowChatMessage(Message msg)throws Exception{
        ServerConnection sc = networkInterface.getSCHandler((msg.arg1));
        TextView Chat = (TextView) findViewById(R.id.ChatTextView);
        FragmentManager manager = getSupportFragmentManager();

        int a = mTabHost.getChildCount();
        List<Fragment> s = manager.getFragments();
        for (Fragment f : s) {
            Chat.append(f.getId() + ":" + f.getTag() + ":" + f.getView().findViewById(R.id.chat).getId() + "\r\n");

        }

        Chat.append("New chat message received." + a + ":" + s.size() + "\r\n");
        switch (msg.arg2){//arg2 is the chat type
            case 1://private message
                Log.i("DisplayServerActivity", "Private message recieved");
                HashMap<String, String> params = (HashMap) msg.obj;
                int clientID = sc.getClientID();
                int invokerID = Integer.parseInt(params.get("invokerid"));
                int targetID = Integer.parseInt(params.get("target"));
                if (clientID == invokerID) {
                    TSClient client = sc.getClientByCLID(targetID);
                    String tag = targetID + "Chat";
                    Fragment f = manager.findFragmentByTag(tag);
                    if (f == null) {
                        addChatTab(tag, client.getName(), sc.getPrivateChat(targetID));
                        mTabHost.setCurrentTabByTag(tag);
                    } else {
                        ChatFragment chatFragment = (ChatFragment) f;
                        chatFragment.setChat(sc.getPrivateChat(targetID));
                    }
                } else if (clientID == targetID) {
                    TSClient client = sc.getClientByCLID(invokerID);
                    String tag = invokerID + "Chat";
                    Fragment f = manager.findFragmentByTag(invokerID + "Chat");
                    if (f == null) {
                        addChatTab(tag, client.getName(), sc.getPrivateChat(invokerID));
                        mTabHost.setCurrentTabByTag(tag);
                    } else {
                        ChatFragment chatFragment = (ChatFragment) f;
                        chatFragment.setChat(sc.getPrivateChat(invokerID));
                    }
                } else {
                    Log.e("DisplayServerActivity", "Invalid private message recieved");
                }

            break;
            case 2://channel chat
                // TextView ChannelChat = (TextView) findViewById(R.id.ChannelChat);
                //    ChannelChat.setText(sc.getChannelChat());
                ChatFragment Cchat = (ChatFragment) manager.findFragmentByTag("channelChat");
                Cchat.setChat(sc.getChannelChat());
                //Chat.setText(sc.getChannelChat());
            break;
            case 3://server chat
                //  TextView ServerChat = (TextView) findViewById(R.id.ServerChat);
                //ServerChat.setText(sc.getServerChat());
                ChatFragment Schat = (ChatFragment) manager.findFragmentByTag("serverChat");
                Schat.setChat(sc.getServerChat());
                // Chat.setText(sc.getServerChat());
            break;

        }
    }

    @Override
    public void onChatMessageSend(String message) {

    }
}