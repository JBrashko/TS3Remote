package meliarion.ts3.ts3remote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

/**
 * Created by Meliarion on 05/06/13.
 * Activity which handles the UI for the teamspeak remote
 */
public class DisplayServerActivity extends Activity  implements RemoteUserInterface {
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
        String text = (String)msg.obj;
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
        boolean remote = intent.getBooleanExtra(LauncherActivity.USE_REMOTE,false);
        setContentView(R.layout.activity_connection);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String returnMsg;
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
        { try{
            TS3ClientConnection connection = new TS3ClientConnection(client_ip, this,remote);
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
            returnMsg="Connection failed";
        }
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
    /*ServerConnection sc = currentClient.getSCHandler((msg.arg1));
    TextView serverChat = (TextView) findViewById(R.id.serverChatView);
    serverChat.setText(sc.getServerChat());
    TextView channelChat = (TextView) findViewById(R.id.channelChatView);
    channelChat.setText(sc.getChannelChat());*/
        switch (msg.arg2){//arg2 is the chat type
            case 1:

            break;
            case 2:
            //Chat.setText(sc.getChannelChat());
            break;
            case 3:
            //Chat.setText(sc.getServerChat());
            break;

        }
    }

}