package meliarion.ts3.ts3remote;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Meliarion on 01/07/13.
 * Class handling the connection to the teamspeak client
 */
public class TS3ClientConnection implements Runnable, ClientConnectionInterface {

   // private Socket requestSocket;
    ClientConnectionType connectionType;
    private NetworkInterface networkInterface;
    private StringBuilder returnStringBuffer = new StringBuilder();
    private InputStreamReader isr;
    private Writer osw;
    private String TSClientIP;
    private RemoteUserInterface UI;
    private Handler mHandler;
    private KeepAlive keepAlive;
    public int DisplayedSCHandler=-1;
    private String sentCommand="";
    private int UsedSCHandlerIndex =-1;
    private boolean connected=false;
    private boolean notifyRegistered=false;
    private List<ServerConnection> SCHandlers = new ArrayList<ServerConnection>();


    public TS3ClientConnection(String _ip, RemoteUserInterface _ui, ClientConnectionType _type)
    {
      this.TSClientIP = _ip;
      this.UI = _ui;
      this.mHandler = _ui.getHandler();
      this.connectionType = _type;

    }
    public boolean usingNetwork(){
        return (connectionType.equals(ClientConnectionType.DirectNetwork)||connectionType.equals(ClientConnectionType.ManagedNetwork)||connectionType.equals(ClientConnectionType.SecureNetwork));
    }

    public ServerConnection getSCHandler(int scid)throws TSRemoteException{
        for (ServerConnection serverConnection : SCHandlers) {
            if (serverConnection.getID() == scid) {
                return serverConnection;
            }
        }
        throw new TSRemoteException("SCHandler id:"+scid+" not found");
    }


    private void Connect()throws IOException, TSRemoteException{
    if(this.networkInterface==null)
    {
        switch (connectionType)
        {   case DirectNetwork:
            networkInterface = new SocketNetworkInterface(TSClientIP);
            break;
            case ManagedNetwork:
            networkInterface = new ManagedSocketNetworkInterface(TSClientIP);
            break;
            case SecureNetwork:
            networkInterface = new SecureSocketNetworkInterface(TSClientIP);
            break;
            case USB:
            networkInterface = new USBNetworkInterface();
            break;
            default:
            throw new TSRemoteException("Invalid client connection type");
        }
    }
    else
    {
        throw new IOException();
    }
        Log.i("Connect", "Making ISR");
        this.isr = new InputStreamReader(this.networkInterface.getInputStream());
        Log.i("Connect","Making OSR");
        this.osw = new BufferedWriter(new OutputStreamWriter(this.networkInterface.getOutputStream()));
        if(this.networkInterface.usesKeepAlive())
        {
            startKeepAlive();
        }
        Log.i("Connect", "Initialisation done");
    }
    private void startKeepAlive()
    {
        Log.i("Connect","Making keepAlive");
        long keepAliveTime = 5*60*1000;
        keepAlive =    new KeepAlive(this,keepAliveTime);
        Thread keepAliveThread = new Thread(keepAlive);
        keepAliveThread.setName("Keep alive thread");
        Log.i("Connect", "Starting keepAlive");
        keepAliveThread.start();
        keepAlive.addSleepTime();
    }

    /*private void NetworkConnect() throws IOException{
        if (this.networkInterface==null){
        Log.i("Connect","Making Socket");
            if(remote)
            {
            this.networkInterface = new SocketNetworkInterface(this.TSClientIP, 25740);
            }
            else
            {
            this.networkInterface = new SocketNetworkInterface(this.TSClientIP, 25639);
            }
        }
    }*/
    public void SendCQMessage(String message)
    {
        SendMessage(message);
    }
    private void SendMessage(String message){
        try{
        Log.i("CQMessage","sending message: "+message);
        sentCommand = message;
        String processedmessage = message+"\r\n";
        this.osw.write(processedmessage.toCharArray());
        this.osw.flush();
        if (keepAlive !=null){
            keepAlive.addSleepTime();
        }
        }
        catch (Exception e)
        {
            Log.e("CQMessage","Error sending message: " + e,e);
        }
    }

    public void run(){
        Log.i("ClientConnection","Thread started");
        Thread thisThread = Thread.currentThread();
        try{
        Connect();
        Read();
        }
        catch (SocketException ex)
        {   Message msg = mHandler.obtainMessage(TSMessageType.ClientConnectionAltered.showCode(),0,0,"A socket exception error occoured");
            msg.sendToTarget();
            String stuff = thisThread.getName()+":"+"connection timed out "+ex.toString();
            UI.Received(stuff);
        }
        catch (Exception ex)
        {   Message msg = mHandler.obtainMessage(TSMessageType.ClientConnectionAltered.showCode(),0,0,"An unrecoverable error occoured");
            msg.sendToTarget();
            String stuff = thisThread.getName()+":"+"an error happened "+ex.toString();
            Log.e("TS3ClientConnection", stuff,ex);

        }
        finally
        {   Log.e("TS3ClientConnection", "Read finished, thread closing.");
            if (keepAlive !=null)
            {
            keepAlive.close();
            }
        }
    }

    private void Read() throws Exception{
        if (this.networkInterface.isConnected()){
            Log.i("Initialisation","connected to the server " + this.networkInterface.getConnectionString() + "\n");
            //ConnectionStage stage = ConnectionStage.ConnectionStarted;
            String buffer = "";
            int ch;
            Pattern response = Pattern.compile("error\\sid=(\\d+)\\smsg=([^$]*)");
            Pattern notify = Pattern.compile("(\\S+)\\sschandlerid=(\\d+)\\s*(.*)");
            while ((ch = isr.read()) != -1){
                //UI.Received(((char) this.ch)+"");
                char c = (char)ch;
                this.returnStringBuffer.append(c);
                if (c == '\n'){//(this.returnStringBuffer.indexOf("\n") != -1){
                    String receivedLine = this.returnStringBuffer.toString();
                    if (receivedLine.equals(""))
                    {
                        continue;
                    }
                    Log.i("Meliarion.TS3.TS3Remote.CQMessage", receivedLine);
                    UI.Received(receivedLine);
                    Matcher match = response.matcher(receivedLine);
                    if(match.find()){
                        if (Integer.valueOf(match.group(1)) != 0)
                        {
                            try{
                            ManageError(Integer.valueOf(match.group(1)),match.group(2));
                            }
                            catch (ClientQueryMessageException ex)
                            {
                                if(ex.getCode()==1794)
                                {
                                    ServerConnection curr =SCHandlers.get(UsedSCHandlerIndex);
                                    curr.Disconnect();
                                    Message msg =  mHandler.obtainMessage(TSMessageType.ServerConnectionChange.showCode(),curr.getID(),0,"Server connection is not connected");
                                    msg.sendToTarget();
                                    SendMessage("currentschandlerid");
                                }
                                else
                                {
                                    throw ex;
                                }
                            }
                        }
                        else
                        {   if (!connected)
                            {
                            handleVerifyConnect(buffer);
                            buffer = "";
                            continue;
                            }
                            if(!commandResponse(buffer))
                            {
                                Log.e("CommandResponse","Error processing command response for command:"+sentCommand+" and resoponse:"+buffer);
                            }
                            if (!(UsedSCHandlerIndex >=0&&SCHandlers.get(UsedSCHandlerIndex).getStage()==ConnectionStage.SetupDone))
                            {
                            setupResponse(buffer);
                            }
                                buffer = "";
                        }
                    }
                    else
                    {
                        if(SCHandlers.size()==0&&!sentCommand.equals("serverconnectionhandlerlist"))
                        {
                            SendMessage("serverconnectionhandlerlist");
                        }
                        else
                        {
                                Matcher nMatch = notify.matcher(receivedLine);
                                if (nMatch.find()&&(DisplayedSCHandler>=0)){
                                    String type = nMatch.group(1);
                                    int SCHandler = Integer.valueOf(nMatch.group(2));
                                    String params = nMatch.group(3);
                                    notifyResponce(type,SCHandler,params);
                                    this.returnStringBuffer.delete(0,this.returnStringBuffer.length());
                                    continue;
                                }
                        }
                        buffer +=receivedLine;
                    }
                    this.returnStringBuffer.delete(0,this.returnStringBuffer.length());
                }
            }
            Message msg = mHandler.obtainMessage(TSMessageType.ClientConnectionAltered.showCode(),0,0,"Client connection closed");
            msg.sendToTarget();
             Log.e("TS3ClientConnection","Client connection closed");
        }
    }
    private void handleVerifyConnect(String buffer) throws  TSRemoteException
    {       String[] SCHandlerInfo = new String[2];
            if(networkInterface.verifyConnect(buffer,SCHandlerInfo))
            {   DisplayedSCHandler=Integer.valueOf(SCHandlerInfo[0]);
                parseSCHandlers(SCHandlerInfo[1]);
                SendMessage("clientnotifyregister schandlerid=0 event=any");
                connected=true;
            }
            else
            {throw new TSRemoteException("Connection failed");}
    }

    private void setupResponse(String buffer)throws TSRemoteException {
        if(connected&& UsedSCHandlerIndex ==-1)
        {   SendMessage("use " + SCHandlers.get(0).getID());
            return;
        }
        ServerConnection curr = SCHandlers.get(UsedSCHandlerIndex);
        if (!curr.Connecting())
        {
            String asdf = "";
        }
        ConnectionStage stage = curr.getStage();
        switch (stage){
            case Disconnected:
                SCHandlerAdvance();
                break;
            case VerifyConnection:
                SendMessage("whoami");
              //  curr.advanceConnectionStage();
            break;
            case RequestServerGroups:
                SendMessage("servergrouplist");
               // curr.advanceConnectionStage();
            break;
            case RequestChannelGroups:
                SendMessage("channelgrouplist");//serverGroupsPopulate();
                break;
            case RequestClients:
                SendMessage("clientlist -uid -away -voice -groups -icon -country");//channelGroupsPopulate();
                break;
            case RequestChannels:
                SendMessage("channellist -topic -flags -voice -icon -limits");//clientPopulate (buffer);
                break;
            //case FinaliseSetup:
              //  SCHandlerAdvance();//channelPopulate (buffer);
            //break;
            case FinaliseSetup:
                SendMessage("serverconnectinfo");
                break;
            case PopulatingDone:
            case SetupDone:
                Message msg = UI.getHandler().obtainMessage(TSMessageType.SetupDone.showCode(),curr.getID(),1,"Setup done");
                msg.sendToTarget();
                SCHandlerAdvance();//curr.advanceConnectionStage();
            break;
            default:
            UI.Received(stage.toString()+":"+buffer);
            break;
        }
    }
    private boolean parseChannelList(String buffer){
        ServerConnection curr=SCHandlers.get(UsedSCHandlerIndex);
        curr.ClearChannelList();
        Pattern channelList = Pattern.compile("\\|*\\s*([^\\|]+)");
        Matcher m = channelList.matcher(buffer.trim());
        TSChannel channel;
        if (m.find()){
            try{
                channel = new TSChannel(m.group(1));
                curr.AddChannel(channel);
            }
            catch (Exception e){
                new TSRemoteException("error making channel:"+m.group(1)+". "+e);
            }
            while(m.find()){
                try{
                    channel = new TSChannel(m.group(1));
                    curr.AddChannel(channel);
                }catch (Exception e){
                    new TSRemoteException("error making channel:"+m.group(1)+". "+e);
                }
            }
            curr.processChannels();
            curr.ChannelsReceived();
            return true;}
            return false;
    }
    private boolean parseClientList(String buffer){
        ServerConnection curr=SCHandlers.get(UsedSCHandlerIndex);
        curr.ClearClientList();
        Pattern clientlist = Pattern.compile("\\|*\\s*([^\\|]+)");
        Matcher m = clientlist.matcher(buffer.trim());
        if (m.find()){
            try{
                TSClient client = new TSClient(m.group(1));
                curr.AddClient(client);
                while(m.find()){
                    client = new TSClient(m.group(1));
                    curr.AddClient(client);
                }
            }
            catch (Exception ex){
                new TSRemoteException("error making client:"+m.group(1),ex);
            }
            curr.ClientListReceived();
            return true;
        }
        return false;
    }
    /*private boolean VerifyClientConnect(String buffer) throws TSRemoteException{
        String tBuffer = buffer.trim();
        String patternString= "";
        if (remote)
        {
            patternString="TS3 Remote Manager\\s+TS3 remote connected successfully\\s+selected schandlerid=(\\d+)\\s+(.*)";
        }
        else
        {
            patternString="TS3 Client\\s+Welcome to the TeamSpeak 3.+\\s+selected schandlerid=(\\d+)\\s+(.*)";
        }
        Pattern pattern = Pattern.compile(patternString);
        Matcher m = pattern.matcher(tBuffer);
        if (m.matches()){
            DisplayedSCHandler=Integer.valueOf(m.group(1));
            parseSCHandlers(m.group(2));
            return true;
        }
        else {
        return false;
    }
    }*/
    private void SCHandlerAdvance(){

        if (UsedSCHandlerIndex == SCHandlers.size()-1)
        {
            Message msg = UI.getHandler().obtainMessage(TSMessageType.SetupDone.showCode(),DisplayedSCHandler,0,"Setup done");
            msg.sendToTarget();
            if(!sentCommand.equals("currentschandlerid")){
            SendMessage("currentschandlerid");}
            if(keepAlive != null){
            keepAlive.initialisingDone();
            }
        }
        else
        {   UsedSCHandlerIndex++;
            SendMessage("use " + SCHandlers.get(UsedSCHandlerIndex).getID());
        }
    }
    private void parseSCHandlers(String handlerBlock) throws TSRemoteException{
        Pattern pattern = Pattern.compile("schandlerid=(\\d+)");
        Matcher m = pattern.matcher(handlerBlock);
        while (m.find())
        {int i = Integer.valueOf(m.group(1));
            if(!AddSCHandler(i))
            {
                throw new TSRemoteException("Error adding SCHandler to list");
            }
        }
        if(SCHandlers.size()==0)
        {
            throw new TSRemoteException("No SCHandlers found in list");
        }
    }
    private void ManageError(Integer code, String message) throws ClientQueryMessageException{
        Log.e("Meliarion.TS3.TS3Remote.CQMessage","server connection error id="+code+" with message="+message);
        switch (code)
        {
            case 0:
            return;
            case 1794:
            SCHandlers.get(UsedSCHandlerIndex).Disconnect();
            SendMessage("currentschandlerid");
            break;
            default:
            throw new ClientQueryMessageException(code,message);
        }

    }
    private boolean whoamiResponce(String responce){
        ServerConnection curr = SCHandlers.get(UsedSCHandlerIndex);
        curr.connectionVerified();
        Pattern pattern = Pattern.compile(".*clid=(\\d+) cid=(\\d+).*");
        Matcher m = pattern.matcher(responce);
        boolean a = m.find();
        if (a)
        {
            curr.setClientID(Integer.valueOf(m.group(1)),Integer.valueOf(m.group(2)));
            return true;
        }
        return false;
    }
    private boolean currentSCHandlerIDResponce(String responce){
        Pattern pattern = Pattern.compile("schandlerid=(\\d+)");
        Matcher m = pattern.matcher(responce);
        if(m.find()){
            DisplayedSCHandler = Integer.valueOf(m.group(1));
            return true;
        }
        return false;
    }
    private boolean commandResponse(String responce) {//returns true if the command was handled
    String lastCommand = sentCommand;
    if (sentCommand.equals("currentschandlerid"))
    {
            return currentSCHandlerIDResponce(responce);
    }
    else if (sentCommand.equals("channelgrouplist")||sentCommand.equals("servergrouplist")||sentCommand.substring(0,3).equals("use"))
    {   //handled by notifyresponce
        return true;
    }
    else if (sentCommand.equals("whoami"))
    {   return whoamiResponce(responce);

    }
    else if (sentCommand.equals("clientlist -uid -away -voice -groups -icon -country"))
    {
            return parseClientList(responce);

    }
    else if(sentCommand.equals("clientnotifyregister schandlerid=0 event=any"))
    {
        notifyRegistered = true;
        return true;
    }
    else if (sentCommand.equals("channellist -topic -flags -voice -icon -limits"))
    {
        return parseChannelList(responce);

    }
    else if (sentCommand.equals("serverconnectinfo"))
    {   ServerConnection server = SCHandlers.get(UsedSCHandlerIndex);
        server.UpdateConnectionInfo(responce);
        return true;
    }
    return false;
    }
    private boolean AddSCHandler(int _scid) {
    ServerConnection connection = new ServerConnection(_scid);
    Log.i("SCHandler","Adding SCHandler id:"+_scid+" to list");
    return SCHandlers.add(connection);
    }
    private void notifyResponce (String type, int scid, String parameters) throws Exception{
         if(scid>SCHandlers.size())
         {
             AddSCHandler(scid);
         }
         Map<String, String> params = new LinkedHashMap<String, String>();
         Pattern parampattern = Pattern.compile("\\s*\\|*([^=\\s]+)=?([^\\|\\s]*)");
         Matcher mparams = parampattern.matcher(parameters);
         int i=0;
         while (mparams.find()){
             if(params.containsKey(i+mparams.group(1))){
             i++;
             params.put(i+mparams.group(1),mparams.group(2));
             }
             else if (params.containsKey(mparams.group(1)))
             {
             params.put(i+mparams.group(1),mparams.group(2));
             }
             else
             {
             params.put(mparams.group(1),mparams.group(2));
            }
         }
         Log.d("Notify","Notify message of "+type+" received");
         try {
         ServerConnection server = getSCHandler(scid);
         handleNotify(type,server,params);
         }
         catch (ServerConnection.SCException ex){
             Log.e("Notify","Server connection excetion of type:"+ex.getType()+"occured in scid:"+scid+". "+ex, ex);
         }
         catch (TSRemoteException ex){
         Log.e("Notify","Failed to find SCHandler id:"+scid+". "+ex, ex);
         }
         catch (NullPointerException ex){
         Log.e("Notify","null pointer exception in scid:"+scid+". "+ex, ex);
         }
         catch (IndexOutOfBoundsException ex){
         Log.e("Notify","Index out of bounds exception in scid:"+scid+". "+ex, ex);
         }
        catch (NumberFormatException ex){
            Log.e("Notify","Number format exception in scid:"+scid+". "+ex, ex);
        }
        catch (Exception ex){
            Log.e("Notify","An unhandled exception occured in scid:"+scid+". "+ex, ex);
        }

    }



    private void handleNotify(String type, ServerConnection server, Map<String, String> params)throws Exception{
        if (type.equals("selected"))
        {
            UsedSCHandlerIndex = SCHandlers.indexOf(getSCHandler(server.getID()));

        }
        else if (type.equals("notifytalkstatuschange")){
            int clid=Integer.valueOf(params.get("clid"));
            try{
            server.getClientByCLID(clid).SetTalkStatus(params.get("status").equals("1"), params.get("isreceivedwhisper").equals("1"));
            Message msg = mHandler.obtainMessage(TSMessageType.ClientAltered.showCode(),server.getID(),clid,"Client status changed");
            msg.sendToTarget();
            }
            catch (ServerConnection.SCNotFoundException ex)
            {
                Log.e("Notify","Failed to update client talk status, clid:"+clid+". "+ex.getErrorDescription(), ex);
            }

        }else if (type.equals("notifytextmessage")){
            int mtype = Integer.valueOf(params.get("targetmode"));
            String s = params.get("msg");
            server.AddTextMessage(s,mtype,params.get("invokername"),params.get("invokeruid"));
            Message msg =  mHandler.obtainMessage(TSMessageType.ChatMessage.showCode(),server.getID(),mtype,"New chat message received ");
            msg.sendToTarget();

        }else if (type.equals("notifyservergrouplist"))
        {
        Set<String> keys = params.keySet();
        Iterator<String> iterKeys = keys.iterator();
        Map<String,String> grpMap = new HashMap<String, String>();
        Pattern pattern = Pattern.compile("(\\d+)?(.+)");
        Matcher m;
        String s;
        TSGroup group;
        int i = -1;
            while (iterKeys.hasNext())
            {s = iterKeys.next();
             m = pattern.matcher(s);
             if(m.find())
             {  if((m.group(1)!=null)&&(Integer.valueOf(m.group(1))>i))
                {   group = new TSGroup(grpMap);
                    server.AddServerGroupByParams(grpMap);
                    grpMap.clear();
                    i++;
                }

                  grpMap.put(m.group(2),params.get(s));
                }
                else
                {
                     throw new TSRemoteException("Unable to build group parameter map");
                }
             }
            //group=new TSGroup(grpMap);
            server.AddServerGroupByParams(grpMap);
        }
        else if (type.equals("notifychannelgrouplist"))
        {   int i = 0;
            if (server.hasChannelGroups()){
                server.ClearChannelGroups();
            }
            if (params.containsKey("name"))
            {
                TSGroup group = new TSGroup(Integer.valueOf(params.get("cgid")),params.get("name"),Integer.valueOf(params.get("type")),Integer.valueOf(params.get("iconid")));
                server.AddChannelGroup(group);
            }
            while (params.containsKey(i+"name")){
                TSGroup group = new TSGroup(Integer.valueOf(params.get(i+"cgid")),params.get(i+"name"),Integer.valueOf(params.get(i+"type")),Integer.valueOf(params.get(i+"iconid")));
                server.AddChannelGroup(group);
                i++;
            }
        }
        else if (type.equals("notifyclientupdated"))
        { int clid=Integer.valueOf(params.get("clid"));
            try{

            TSClient client = server.getClientByCLID(clid);
            boolean needresort = params.containsKey("client_talk_power")||params.containsKey("client_nickname")||params.containsKey("client_is_talker");
            client.updateClient(params);
            if(needresort){
                 server.sortChannelClients(client.getChannelID());
            }
            Message msg = mHandler.obtainMessage(TSMessageType.ClientAltered.showCode(),server.getID(),clid,"Client status changed");
            msg.sendToTarget();
            }
            catch (ServerConnection.SCNotFoundException ex)
            {
                Log.e("Notify","Failed to updateGroup client clid:"+clid+". "+ex.getErrorDescription(), ex);
            }

        }
        else if(type.equals("notifyclientmoved"))
        {   int clid=Integer.valueOf(params.get("clid"));

            try{
            server.moveClient(clid, params);
            Message msg = mHandler.obtainMessage(TSMessageType.ClientMoved.showCode(),server.getID(),clid,"Client moved");
            msg.sendToTarget();
            }
            catch (ServerConnection.SCNotFoundException ex)
            {
                Log.e("Notify","Failed to move client clid:"+clid+". "+ex.getErrorDescription(), ex);
            }
        }
        else if(type.equals("notifyclientchannelgroupchanged"))
        {
            int cgid = Integer.valueOf(params.get("cgid"));
            int cid = Integer.valueOf(params.get("cid"));
            int clid = Integer.valueOf(params.get("clid"));
            try{
                server.getClientByCLID(clid).changeChannelGroup(cgid,cid);
                Message msg = mHandler.obtainMessage(TSMessageType.ClientAltered.showCode(),server.getID(),clid,"Client channel group changed");
                msg.sendToTarget();
                }
            catch (ServerConnection.SCNotFoundException ex)
            {
                Log.e("Notify","Failed to change client channel group. "+ex.getErrorDescription(), ex);
            }
        }
        else if(type.equals("notifycurrentserverconnectionchanged"))
        {
            DisplayedSCHandler = server.getID();
            Message msg = mHandler.obtainMessage(TSMessageType.DisplayedServerConnectionChange.showCode(),server.getID(),0,"Displayed server connection changed");
            msg.sendToTarget();
        }
        else if(type.equals("notifyservergroupclientadded"))
        {
          //  int clid = Integer.valueOf(params.get("clid"));
          //  int sgid = Integer.valueOf(params.get("sgid"));
          //  getSchandler(scid).getClientByCLID(clid).addServerGroup(sgid);
          //  Message msg = mHandler.obtainMessage(TSMessageType.ClientAltered.showCode(),scid,clid,"Client server group changed");
          //  msg.sendToTarget();
        }
        else if(type.equals("notifyclientleftview"))
        {
            int clid = Integer.valueOf(params.get("clid"));
            try{
            server.removeClientByCLID(clid);
            Message msg = mHandler.obtainMessage(TSMessageType.ClientLeft.showCode(),server.getID(),clid,"Client left view");
            msg.sendToTarget();
            }
            catch (ServerConnection.SCNotFoundException ex)
            {
                Log.e("Notify","Failed to remove client from view, client id:"+clid+" not found in clientlist. "+ex.getErrorDescription(), ex);
            }
        }
        else if (type.equals("notifycliententerview"))
        {   Set<String> keys = params.keySet();
            Iterator<String> iterKeys = keys.iterator();
            Pattern pattern = Pattern.compile("(\\d+)?(.+)");
            TSClient client;
            Matcher m;
            String s;
            int i = -1;
            while (iterKeys.hasNext())
            {s = iterKeys.next();
                m = pattern.matcher(s);
                if(m.find())
                {  if((m.group(1)!=null)&&(Integer.valueOf(m.group(1))>i)){
                    client = new TSClient(params,i);
                    server.AddClient(client);
                    Message msg = mHandler.obtainMessage(TSMessageType.ClientJoined.showCode(),server.getID(),client.getClientID(),"Client entered view");
                    msg.sendToTarget();
                    i++;
                }
                }
                else
                {
                    throw new TSRemoteException("Unable to build client parameter map");
                }
            }
            //group=new TSGroup(grpMap);
            if (i>-1)
            {
                server.ClientListReceived();
            }
            client = new TSClient(params,i);
            server.AddClient(client);
            Message msg = mHandler.obtainMessage(TSMessageType.ClientJoined.showCode(),server.getID(),client.getClientID(),"Client entered view");
            msg.sendToTarget();
        }
        else if(type.equals("notifychannelcreated"))
        {
            TSChannel newChannel = new TSChannel(params);
            server.AddChannel(newChannel);
            Message msg = mHandler.obtainMessage(TSMessageType.ChannelCreated.showCode(),server.getID(),0,"Channel created");
            msg.sendToTarget();

        }
        else if (type.equals("notifychanneledited"))
        {
            int cid = Integer.valueOf(params.get("cid"));
            try{
            TSChannel channel = server.getChannelByID(cid);
            boolean resortNeeded = (params.containsKey("channel_order"))||(params.containsKey("channel_name"));
            channel.updateChannel(params);
            if (resortNeeded){//if you change something that effects its sort order
                TSChannel parent = server.getChannelByID(channel.getParentID());//find its parent
                parent.sortSubchannels(new ChannelOrderer(server));//and resort its position
                }
            Message msg = mHandler.obtainMessage(TSMessageType.ChannelAltered.showCode(),server.getID(),cid,"Channel edited");
            msg.sendToTarget();
            }
            catch (ServerConnection.SCNotFoundException ex)
            {
                Log.e("Notify","Failed to edit channel, channel id:"+cid+" not found. "+ex.getErrorDescription(), ex);
            }
        }
        else if (type.equals("notifyserverupdated"))
        {
            server.updateServer(params);
            Message msg = mHandler.obtainMessage(TSMessageType.ServerAltered.showCode(),server.getID(),0,"Server edited");
            msg.sendToTarget();
        }
        else if (type.equals("notifyconnectstatuschange"))
        {
            String status =params.get("status");
            if (status.equals("connecting"))
            {   server.Reinitialise();
            }
            else if (status.equals("connected"))
            {
                server.connectionVerified();
            }
            else if (status.equals("connection_establishing"))
            {

            }
            else if(status.equals("connection_established"))
            {

            }
            else if (status.equals("disconnected"))
            {
                server.Disconnect();
            }
        }
        else if (type.equals("channellist"))
        {
            Set<String> keys = params.keySet();
            Iterator<String> iterKeys = keys.iterator();
            //Map<String,String> chanMap = new HashMap<String, String>();
            Pattern pattern = Pattern.compile("(\\d+)?(.+)");
            Matcher m;
            String s;
            TSChannel channel;
            int i = -1;
            while (iterKeys.hasNext())
            {s = iterKeys.next();
                m = pattern.matcher(s);
                if(m.find())
                {  if((m.group(1)!=null)&&(Integer.valueOf(m.group(1))>i))
                {   channel = new TSChannel(params ,i);
                    server.AddChannel(channel);
                    //chanMap.clear();
                    i++;
                }

                    //chanMap.put(m.group(2),params.get(s));
                }
                else
                {
                    throw new TSRemoteException("Unable to build channel parameter map");
                }
            }
            channel=new TSChannel(params,i);
            server.AddChannel(channel);
        }
        else if (type.equals("notifyclientneededpermissions"))
        {
            int id = Integer.valueOf(params.get("permid"));
            boolean b = params.get("permid").equals("1");
            Log.e("notify", "notifyneededpermissions occoured");
        }
        else if (type.equals("notifychanneldeleted"))
        {
            int cid = Integer.valueOf(params.get("cid"));
            int i = 0;
            try{
                server.removeChannelByID(cid);
                Message msg = mHandler.obtainMessage(TSMessageType.ChannelDeleted.showCode(),server.getID(),cid,"Channel deleted");
                msg.sendToTarget();
            }
            catch (ServerConnection.SCNotFoundException ex)
            {
            Log.e("Notify","Failed to remove channel from view, channel id:"+cid+" not found in channellist. "+ex.getErrorDescription(), ex);
            }
        }
        else if (type.equals("notifychannelsubscribed"))
        {int cid = Integer.valueOf(params.get("cid"));
        server.getChannelByID(cid).subscibe();
        Message msg = mHandler.obtainMessage(TSMessageType.ChannelAltered.showCode(),server.getID(),cid,"Subscribed to channel");
        msg.sendToTarget();
        }
        else if (type.equals("notifychannelpermlist"))
        {

        }
        else if (type.equals("notifyservergrouppermlist"))
        {

        }
        else if(type.equals("notifychannelmoved"))
        {int cid = Integer.valueOf(params.get("cid"));
         int cpid = Integer.valueOf(params.get("cpid"));
            server.moveChannel(cid,cpid);
            Message msg = mHandler.obtainMessage(TSMessageType.ChannelMoved.showCode(),server.getID(),cid,"Channel moved");
            msg.sendToTarget();
        }
        else if (type.equals("notifychannelpasswordchanged"))
        {int cid = Integer.valueOf(params.get("cid"));
         TSChannel channel = server.getChannelByID(cid);
         channel.passwordChanged();
         Message msg = mHandler.obtainMessage(TSMessageType.ChannelAltered.showCode(),server.getID(),cid,"Channel password changed");
         msg.sendToTarget();
        }
        else if (type.equals("notifyserveredited")){
        server.edited(params);
        Message msg = mHandler.obtainMessage(TSMessageType.ServerAltered.showCode(),server.getID(),0,"Server edited");
        msg.sendToTarget();
        }
        else if (type.equals("notifyfilelist"))
        {

        }
        else if (type.equals("notifyfilelistfinished"))
        {

        }
        else if (type.equals("notifychanneldescriptionchanged"))
        {int cid = Integer.valueOf(params.get("cid"));

        }
        else if (type.equals("notifystartdownload"))
        {

        }
        else if (type.equals("channellistfinished"))
        {
        server.processChannels();
        server.ChannelsReceived();//this means the client has finished sending us the channel list
        }
        else if (type.equals("notifymutedclientdisconnected"))
        {

        }
        else
        {   Log.w("Notify","Unhandled notify type:"+type);
            UI.Received("received  "+type+" from SCHandler="+server.getID());
        }
    }
}
