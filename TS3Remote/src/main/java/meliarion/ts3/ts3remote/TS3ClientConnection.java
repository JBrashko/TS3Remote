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
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.TrustManagerFactory;

/**
 * Created by Meliarion on 01/07/13.
 * Class handling the connection to the teamspeak client
 */
public class TS3ClientConnection implements Runnable, ClientConnectionInterface {

    final ClientConnectionType connectionType;
    private NetworkInterface networkInterface;
    private final String password;
    private final TrustManagerFactory tmf;
    private StringBuilder returnStringBuffer = new StringBuilder();
    private InputStreamReader isr;
    private Writer osw;
    private final String TSClientIP;
    private final RemoteUserInterface UI;
    private final Handler mHandler;
    private KeepAlive keepAlive;
    public int DisplayedSCHandler = -1;
    private String sentCommand = "";
    private Queue<String> sentCommands = new ArrayBlockingQueue<String>(10);
    private int UsedSCHandlerIndex = -1;
    private boolean connected = false;
    private boolean notifyRegistered = false;
    private static final Pattern response = Pattern.compile("error\\sid=(\\d+)\\smsg=([^$]*)");
    private static final Pattern notify = Pattern.compile("(\\S+)\\sschandlerid=(\\d+)\\s*(.*)");
    private final Map<Integer, Integer> SCIDTOSCIndex = new HashMap<Integer, Integer>();
    private final List<ServerConnection> SCHandlers = new ArrayList<ServerConnection>();
    private List<String> messageLog = new ArrayList<String>();


    public TS3ClientConnection(RemoteUserInterface _ui, ConnectionInfo connectionInfo) {

        this.TSClientIP = connectionInfo.getIP();
        this.UI = _ui;
        this.mHandler = _ui.getHandler();
        this.connectionType = connectionInfo.getType();
        this.password = connectionInfo.getPassword();
        this.tmf = connectionInfo.getTrustManagerFactory();
    }

    public boolean usingNetwork() {
        return (connectionType.equals(ClientConnectionType.DirectNetwork) || connectionType.equals(ClientConnectionType.ManagedNetwork) || connectionType.equals(ClientConnectionType.SecureNetwork));
    }

    public ServerConnection getSCHandler(int scid) throws TSRemoteException {
        for (ServerConnection serverConnection : SCHandlers) {
            if (serverConnection.getID() == scid) {
                return serverConnection;
            }
        }
        throw new TSRemoteException("SCHandler id:" + scid + " not found");
    }


    private void Connect() throws IOException, TSRemoteException {
        if (this.networkInterface == null) {
            switch (connectionType) {
                case DirectNetwork:
                    networkInterface = new SocketNetworkInterface(TSClientIP);
                    break;
                case ManagedNetwork:
                    networkInterface = new ManagedSocketNetworkInterface(TSClientIP, password);
                    break;
                case SecureNetwork:
                    networkInterface = new SecureSocketNetworkInterface(TSClientIP, password, tmf);
                    break;
                case USBADB:
                    networkInterface = new USBADBNetworkInterface();
                    break;
                default:
                    throw new TSRemoteException("Invalid client connection type");
            }
        } else {
            throw new IOException();
        }
        Log.i("Connect", "Making ISR");
        this.isr = new InputStreamReader(this.networkInterface.getInputStream());
        Log.i("Connect", "Making OSR");
        this.osw = new BufferedWriter(new OutputStreamWriter(this.networkInterface.getOutputStream()));
        if (this.networkInterface.usesKeepAlive()) {
            startKeepAlive();
        }
        Log.i("Connect", "Initialisation done");
    }

    private void startKeepAlive() {
        Log.i("Connect", "Making keepAlive");
        long keepAliveTime = 5 * 60 * 1000;
        keepAlive = new KeepAlive(this, keepAliveTime);
        Thread keepAliveThread = new Thread(keepAlive);
        keepAliveThread.setName("Keep alive thread");
        Log.i("Connect", "Starting keepAlive");
        keepAliveThread.start();
        keepAlive.addSleepTime();
    }

    private void ChangeUsedSCHandlerByIndex(int scindex) {
        UsedSCHandlerIndex = scindex;
        SendCQMessage("use " + SCHandlers.get(UsedSCHandlerIndex).getID());
        //SendCQMessage("use "+scid);
    }

    private void ChangeUsedSCHandler(int scid) {//ChangeUsedSCHandlerByIndex(scid);
        try {
            UsedSCHandlerIndex = SCHandlers.indexOf(getSCHandler(scid));
            SendCQMessage("use " + scid);
        } catch (TSRemoteException ex) {
            Log.e("TS3ClientConnection", "Invalid scid", ex);
        }
        //SendCQMessage("use "+scid);
    }

    public void SendCQMessageForServer(String message, int scid) {
        if (UsedSCHandlerIndex == scid) {
            SendCQMessage(message);
        } else {
            int used = UsedSCHandlerIndex;
            ChangeUsedSCHandler(scid);
            SendCQMessage(message);
            ChangeUsedSCHandler(used);
        }
    }

    public void SendCQMessageForServer(String message, ServerConnection sc) {
        SendCQMessageForServer(message, sc.getID());
    }

    public void SendCQMessage(String message) {
        SendMessage(message);
    }

    private void SendMessage(String message) {
        try {
            Log.i("CQMessage", "sending message: " + message);
            sentCommand = message;
            sentCommands.add(message);
            messageLog.add(message);
            String processedmessage = message + "\r\n";
            this.osw.write(processedmessage.toCharArray());
            this.osw.flush();
            if (keepAlive != null) {
                keepAlive.addSleepTime();
            }
        } catch (Exception e) {
            Log.e("CQMessage", "Error sending message: " + e, e);
        }
    }

    public void run() {
        Log.i("ClientConnection", "Thread started");
        Thread thisThread = Thread.currentThread();
        try {
            Connect();
            Read();
        } catch (SocketException ex) {
            Message msg = mHandler.obtainMessage(TSMessageType.ClientConnectionAltered.showCode(), 0, 0, "A socket exception error occoured");
            msg.sendToTarget();
            String stuff = thisThread.getName() + ":" + "connection timed out " + ex.toString();
            UI.Received(stuff);
        } catch (Exception ex) {
            Message msg = mHandler.obtainMessage(TSMessageType.ClientConnectionAltered.showCode(), 0, 0, "An unrecoverable error occoured");
            msg.sendToTarget();
            String stuff = thisThread.getName() + ":" + "an error happened " + ex.toString();
            Log.e("TS3ClientConnection", stuff, ex);

        } finally {
            Log.e("TS3ClientConnection", "Read finished, thread closing.");
            if (keepAlive != null) {
                keepAlive.close();
            }
        }
    }

    private void VerifyConnect(String receivedLine) throws TSRemoteException {
        String[] connectInfo = new String[2];
        connectInfo[0] = "";
        if (networkInterface.verifyConnect(receivedLine, connectInfo))  //if we are connected then
        {
            DisplayedSCHandler = Integer.valueOf(connectInfo[0]);       //connectInfo[0] will have the displayed SChandler
            parseSCHandlers(connectInfo[1]);                          //and SCHandler[1] will list all the current SCHandlers
            SendMessage("clientnotifyregister schandlerid=0 event=any");//
            connected = true;
        } else {                  //if we are not connected then
            if (connectInfo[0].equals("response")) { //connectInfo[0] will be equal to response if the connection requires a response
                String message = connectInfo[1];    //connectInfo[1] will be the response to send
                SendMessage(message);
            } else if (connectInfo[0].equals("wait")) //connectInfo[0] will be equal to "wait" if the connection doesn't require a response
            {
                return;
            } else                                    //otherwise throw an error
            {
                throw new TSRemoteException("Connection failed");
            }
        }
    }

    private void Read() throws Exception {
        if (this.networkInterface.isConnected()) {
            Log.i("Initialisation", "connected to the server " + this.networkInterface.getConnectionString() + "\n");
            int ch;
            while ((ch = isr.read()) != -1) {
                //UI.Received(((char) this.ch)+"");
                char c = (char) ch;
                this.returnStringBuffer.append(c);
                if (c == '\n') {//(this.returnStringBuffer.indexOf("\n") != -1){
                    String receivedLine = this.returnStringBuffer.toString();
                    if (receivedLine.equals("")) {
                        continue;
                    }
                    messageLog.add(receivedLine);
                    if (!connected) {
                        VerifyConnect(receivedLine);
                    } else {
                        if (EstablishedConnection(receivedLine))//returns true when the received data has been processed
                        {
                            this.returnStringBuffer.delete(0, this.returnStringBuffer.length());
                        }
                    }
                }
            }
            Message msg = mHandler.obtainMessage(TSMessageType.ClientConnectionAltered.showCode(), 0, 0, "Client connection closed");
            msg.sendToTarget();
            Log.e("TS3ClientConnection", "Client connection closed");
        }
    }

    private boolean EstablishedConnection(String receivedLine) throws Exception {
        Log.i("TS3ClientConnection", receivedLine);
        UI.Received(receivedLine);
        Matcher match = response.matcher(receivedLine);
        if (match.find()) {
            if (Integer.valueOf(match.group(1)) != 0) {
                try {
                    ManageError(Integer.valueOf(match.group(1)), match.group(2));
                } catch (ClientQueryMessageException ex) {
                    if (ex.getCode() == 1794) {
                        ServerConnection curr = SCHandlers.get(UsedSCHandlerIndex);
                        curr.Disconnect();
                        Message msg = mHandler.obtainMessage(TSMessageType.ServerConnectionChange.showCode(), curr.getID(), 0, "Server connection is not connected");
                        msg.sendToTarget();
                        SendMessage("currentschandlerid");
                    } else {
                        throw ex;
                    }
                }
            } else//we should have a string response that ends with error id=0 msg=ok
            {
                if (!commandResponse(receivedLine.trim())) {
                    Log.e("CommandResponse", "Error processing command response for command:" + sentCommand + " and response:" + receivedLine);
                }
                if (!(UsedSCHandlerIndex >= 0 && SCHandlers.get(UsedSCHandlerIndex).getStage() == ConnectionStage.SetupDone)) {
                    setupResponse(receivedLine);
                }
                return true;
            }
        } else {
            if (SCHandlers.size() == 0 && !sentCommand.equals("serverconnectionhandlerlist")) {
                SendMessage("serverconnectionhandlerlist");
            } else {
                Matcher nMatch = notify.matcher(receivedLine);
                if (nMatch.find() && (DisplayedSCHandler >= 0)) {
                    String type = nMatch.group(1);
                    int SCHandler = Integer.valueOf(nMatch.group(2));
                    String params = nMatch.group(3);
                    notifyResponce(type, SCHandler, params);
                    return true;
                } else {
                    //commandResponse(receivedLine.trim());
                    Log.e("EstablishedConnection", "Notify matcher failed for " + receivedLine);
                }
            }

        }
        return false;
    }

    private void setupResponse(String buffer) {
        if (connected && UsedSCHandlerIndex == -1) {
            SendMessage("use " + SCHandlers.get(0).getID());
            return;
        }
        ServerConnection curr = SCHandlers.get(UsedSCHandlerIndex);
        if (!curr.Connecting()) {
            Log.e("Setup Response", "Setup response called on non connecting server connection with id: " + curr.getID());
        }
        ConnectionStage stage = curr.getStage();
        switch (stage) {
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
                Message msg = UI.getHandler().obtainMessage(TSMessageType.SetupDone.showCode(), curr.getID(), 1, "Setup done");
                msg.sendToTarget();
                SCHandlerAdvance();//curr.advanceConnectionStage();
                break;
            default:
                UI.Received(stage.toString() + ":" + buffer);
                break;
        }
    }

    private boolean parseChannelList(String buffer) {
        ServerConnection curr = SCHandlers.get(UsedSCHandlerIndex);
        curr.ClearChannelList();
        Pattern channelList = Pattern.compile("\\|*\\s*([^\\|]+)");
        Matcher m = channelList.matcher(buffer.trim());
        TSChannel channel;
        if (m.find()) {
            channel = new TSChannel(m.group(1));
            curr.AddChannel(channel);
            while (m.find()) {
                channel = new TSChannel(m.group(1));
                curr.AddChannel(channel);
            }
            curr.processChannels();
            curr.ChannelsReceived();
            return true;
        }
        return false;
    }

    private boolean parseClientList(String buffer) {
        ServerConnection curr = SCHandlers.get(UsedSCHandlerIndex);
        // curr.ClearClientList();
        Pattern clientlist = Pattern.compile("\\|*\\s*([^\\|]+)");
        Matcher m = clientlist.matcher(buffer.trim());
        if (m.find()) {
            try {
                TSClient client = new TSClient(m.group(1));
                curr.AddClient(client);
                while (m.find()) {
                    client = new TSClient(m.group(1));
                    curr.AddClient(client);
                }
            } catch (Exception ex) {
                new TSRemoteException("error making client:" + m.group(1), ex);
            }
            curr.ClientListReceived();
            return true;
        }
        return false;
    }

    private boolean parseServerInfo(String info) {
        //// TODO: Actualy parse the data
        return true;
    }

    private void SCHandlerAdvance() {

        if (UsedSCHandlerIndex == SCHandlers.size() - 1) {
            Message msg = UI.getHandler().obtainMessage(TSMessageType.SetupDone.showCode(), DisplayedSCHandler, 0, "Setup done");
            msg.sendToTarget();
            if (!sentCommand.equals("currentschandlerid")) {
                SendMessage("currentschandlerid");
            }
            if (keepAlive != null) {
                keepAlive.initialisingDone();
            }
        } else {
            ChangeUsedSCHandlerByIndex(UsedSCHandlerIndex + 1);

        }
    }

    private void parseSCHandlers(String handlerBlock) throws TSRemoteException {
        Pattern pattern = Pattern.compile("schandlerid=(\\d+)");
        Matcher m = pattern.matcher(handlerBlock);
        while (m.find()) {
            int i = Integer.valueOf(m.group(1));
            if (!AddSCHandler(i)) {
                throw new TSRemoteException("Error adding SCHandler to list");
            }
        }
        if (SCHandlers.size() == 0) {
            throw new TSRemoteException("No SCHandlers found in list");
        }
    }

    private void ManageError(Integer code, String message) throws ClientQueryMessageException {
        Log.e("CQMessage", "server connection error id=" + code + " with message=" + message);
        switch (code) {
            case 0:
                return;
            case 1794:
                SCHandlers.get(UsedSCHandlerIndex).Disconnect();
                SendMessage("currentschandlerid");
                break;
            default:
                throw new ClientQueryMessageException(code, message);
        }

    }

    private boolean whoamiResponce(String responce) {
        ServerConnection curr = SCHandlers.get(UsedSCHandlerIndex);
        curr.connectionVerified();
        Pattern pattern = Pattern.compile(".*clid=(\\d+) cid=(\\d+).*");
        Matcher m = pattern.matcher(responce);
        boolean a = m.find();
        if (a) {
            curr.setClientID(Integer.valueOf(m.group(1)), Integer.valueOf(m.group(2)));
            return true;
        }
        return false;
    }

    private boolean currentSCHandlerIDResponce(String responce) {
        Pattern pattern = Pattern.compile("schandlerid=(\\d+)");
        Matcher m = pattern.matcher(responce);
        if (m.find()) {
            DisplayedSCHandler = Integer.valueOf(m.group(1));
            return true;
        }
        return false;
    }

    private boolean commandResponse(String responce) {//returns true if the command was handled
        String lastCommand = sentCommand;
        String lastCommandQueue = sentCommands.poll();
        String[] responses = responce.trim().split("error id=0 msg=ok", 2);
        try {


            if (responce.equals("error id=0 msg=ok")) {
                Log.i("commandResponse", "received acknowledgement response");
                return true;
                //sentCommands.poll();
            }
            if (lastCommandQueue.equals("currentschandlerid")) {
                return currentSCHandlerIDResponce(responce);
            } else if (lastCommandQueue.equals("channelgrouplist") || lastCommandQueue.equals("servergrouplist") || lastCommandQueue.substring(0, 3).equals("use")) {   //handled by notifyresponce
                return true;
            } else if (lastCommandQueue.equals("whoami")) {
                return whoamiResponce(responce);

            } else if (lastCommandQueue.equals("clientlist -uid -away -voice -groups -icon -country")) {
                return parseClientList(responce);

            } else if (lastCommandQueue.equals("clientnotifyregister schandlerid=0 event=any")) {
                notifyRegistered = true;
                return true;
            } else if (lastCommandQueue.equals("channellist -topic -flags -voice -icon -limits")) {
                return parseChannelList(responce);

            } else if (lastCommandQueue.equals("serverconnectinfo")) {
                ServerConnection server = SCHandlers.get(UsedSCHandlerIndex);
                server.UpdateConnectionInfo(responce);
                return true;
            } else if (lastCommandQueue.equals(getRequestMessage(ServerConnection.ItemNotFoundType.ServerInfo))) {
                return parseServerInfo(responce);

            }

            return false;
        } catch (Exception ex) {
            Log.e("Command response", "An error occoured responding to a command", ex);
            return false;
        } finally {//if there is another command to respond to
            if ((responses.length > 1) && (!responses[1].equals(""))) {
                commandResponse(responses[1].trim());
                //sentCommands.poll();
            }
        }
    }

    private boolean AddSCHandler(int _scid) {
        ServerConnection connection = new ServerConnection(_scid);
        Log.i("SCHandler", "Adding SCHandler id:" + _scid + " to list");
        SCIDTOSCIndex.put(_scid, SCHandlers.size());
        return SCHandlers.add(connection);
    }

    private void notifyResponce(String type, int scid, String parameters) {
        if (scid > SCHandlers.size()) {
            AddSCHandler(scid);
        }
        Map<String, String> params = new LinkedHashMap<String, String>();
        Pattern parampattern = Pattern.compile("\\s*\\|*([^=\\s]+)=?([^\\|\\s]*)");
        Matcher mparams = parampattern.matcher(parameters);
        int i = 0;
        while (mparams.find()) {
            if (params.containsKey(i + mparams.group(1))) {
                i++;
                params.put(i + mparams.group(1), mparams.group(2));
            } else if (params.containsKey(mparams.group(1))) {
                params.put(i + mparams.group(1), mparams.group(2));
            } else {
                params.put(mparams.group(1), mparams.group(2));
            }
        }
        Log.d("Notify", "Notify message of " + type + " received");
        try {
            ServerConnection server = getSCHandler(scid);
            handleNotify(type, server, params);
        } catch (ServerConnection.SCException ex) {
            Log.e("Notify", "Server connection excetion of type:" + ex.getType() + "occured in scid:" + scid + ". " + ex, ex);
        } catch (TSRemoteException ex) {
            Log.e("Notify", "Failed to find SCHandler id:" + scid + ". " + ex, ex);
        } catch (NullPointerException ex) {
            Log.e("Notify", "null pointer exception in scid:" + scid + ". " + ex, ex);
        } catch (IndexOutOfBoundsException ex) {
            Log.e("Notify", "Index out of bounds exception in scid:" + scid + ". " + ex, ex);
        } catch (NumberFormatException ex) {
            Log.e("Notify", "Number format exception in scid:" + scid + ". " + ex, ex);
        } catch (Exception ex) {
            Log.e("Notify", "An unhandled exception occured in scid:" + scid + ". " + ex, ex);
        }

    }


    private void handleNotify(String type, ServerConnection server, Map<String, String> params) throws Exception {
        NotifyMessageType notifyMessageType = NotifyMessageType.getNotifyMessageType(type);
        switch (notifyMessageType) {
            case invalid:
                break;
            case selected:
                UsedSCHandlerIndex = SCHandlers.indexOf(getSCHandler(server.getID()));
                break;
            case notifytalkstatuschange:
                handleNotifyTalkStatusChange(server, params);
                break;
            case notifytextmessage:
                handleNotifyTextMessage(server, params);
                break;
            case notifyservergrouplist:
                handleNotifyServergroupList(server, params);
                break;
            case notifychannelgrouplist:
                handleNotifyChannelGroupList(server, params);
                break;
            case notifyclientupdated:
                handleNotifyClientUpdated(server, params);
                break;
            case notifyclientmoved:
                handleNotifyClientMoved(server, params);
                break;
            case notifyclientchannelgroupchanged:
                handleNotifyClientChannelGroupChanged(server, params);
                break;
            case notifycurrentserverconnectionchanged:
                handleNotifyCurrentServerConnectionChanged(server);
                break;
            case notifyclientleftview:
                handleNotifyClientLeftView(server, params);
                break;
            case notifycliententerview:
                handleNotifyClientEnterView(server, params);
                break;
            case notifychannelcreated:
                handleNotifyChannelCreated(server, params);
                break;
            case notifychanneledited:
                handleNotifyChannelEdited(server, params);
                break;
            case notifyserverupdated:
                handleNotifyServerUpdated(server, params);
                break;
            case notifyconnectstatuschange:
                handleNotifyConnectStatusChange(server, params);
                break;
            case channellist:
                handleChannelList(server, params);
                break;
            case notifychanneldeleted:
                handleNotifyChannelDeleted(server, params);
                break;
            case notifychannelsubscribed:
                handleNotifyChannelSubscribed(server, params);
                break;
            case notifychannelmoved:
                handleNotifyChannelMoved(server, params);
                break;
            case notifychannelpasswordchanged:
                handleNotifyChannelPasswordChanged(server, params);
                break;
            case notifyserveredited:
                handleNotifyServerEdited(server, params);
                break;
            case channellistfinished:
                handleChannelListFinished(server);
                break;
            case notifyfilelist:
            case notifyfilelistfinished:
            case notifyservergroupclientadded:
            case notifychannelunsubscribed:
            case notifyclientneededpermissions:
            case notifychannelpermlist:
            case notifyservergrouppermlist:
            case notifychanneldescriptionchanged:
            case notifystartdownload:
            case notifymutedclientdisconnected:
                Log.i("Notify", "Unused notify type:" + type);
                break;
            default:
                Log.w("Notify", "Unhandled notify type:" + type);
                break;
        }
        if (server.getStage() != ConnectionStage.SetupDone) {
            String s = server.getStage().toString();
            s += "a";
        }
    }

    private void handleNotifyTalkStatusChange(ServerConnection server, Map<String, String> params) {
        int clid = Integer.valueOf(params.get("clid"));
        try {
            server.getClientByCLID(clid).SetTalkStatus(params.get("status").equals("1"), params.get("isreceivedwhisper").equals("1"));
            Message msg = mHandler.obtainMessage(TSMessageType.ClientAltered.showCode(), server.getID(), clid, "Client status changed");
            msg.sendToTarget();
        } catch (ServerConnection.SCNotFoundException ex) {
            Log.e("Notify", "Failed to update client talk status, clid:" + clid + ". " + ex.getErrorDescription(), ex);
        }

    }

    private void handleNotifyTextMessage(ServerConnection server, Map<String, String> params) {
        int mtype = Integer.valueOf(params.get("targetmode"));
        String s = params.get("msg");
        Message msg;
        try {
            if (mtype == 1) {
                server.AddTextMessage(s, mtype, params.get("invokername"), params.get("invokeruid"), Integer.parseInt(params.get("invokerid")), Integer.parseInt(params.get("target")));
                params.put("type", "New chat message received ");
                msg = mHandler.obtainMessage(TSMessageType.ChatMessage.showCode(), server.getID(), mtype, params);
            } else {
                server.AddTextMessage(s, mtype, params.get("invokername"), params.get("invokeruid"), Integer.parseInt(params.get("invokerid")));
                msg = mHandler.obtainMessage(TSMessageType.ChatMessage.showCode(), server.getID(), mtype, "New chat message received ");
            }

            msg.sendToTarget();
        } catch (ServerConnection.SCNotFoundException ex) {
            Log.e("Notify", "Failed to add text message", ex);
            handleMissingTSObject(ex, server);
        }
    }

    private void handleNotifyServergroupList(ServerConnection server, Map<String, String> params) throws TSRemoteException {
        Set<String> keys = params.keySet();
        Iterator<String> iterKeys = keys.iterator();
        Map<String, String> grpMap = new HashMap<String, String>();
        Pattern pattern = Pattern.compile("(\\d+)?(.+)");
        Matcher m;
        String s;
        int i = -1;
        while (iterKeys.hasNext()) {
            s = iterKeys.next();
            m = pattern.matcher(s);
            if (m.find()) {
                if ((m.group(1) != null) && (Integer.valueOf(m.group(1)) > i)) {
                    //group = new TSGroup(grpMap);
                    server.AddServerGroupByParams(grpMap);
                    grpMap.clear();
                    i++;
                }
                grpMap.put(m.group(2), params.get(s));
            } else {
                throw new TSRemoteException("Unable to build group parameter map");
            }
        }
        //group=new TSGroup(grpMap);
        server.AddServerGroupByParams(grpMap);
    }

    private void handleNotifyChannelGroupList(ServerConnection server, Map<String, String> params) {
        int i = 0;
        if (server.hasChannelGroups()) {
            server.ClearChannelGroups();
        }
        if (params.containsKey("name")) {
            TSGroup group = new TSGroup(Integer.valueOf(params.get("cgid")), params.get("name"), Integer.valueOf(params.get("type")), Integer.valueOf(params.get("iconid")));
            server.AddChannelGroup(group);
        }
        while (params.containsKey(i + "name")) {
            TSGroup group = new TSGroup(Integer.valueOf(params.get(i + "cgid")), params.get(i + "name"), Integer.valueOf(params.get(i + "type")), Integer.valueOf(params.get(i + "iconid")));
            server.AddChannelGroup(group);
            i++;
        }
    }

    private void handleNotifyClientUpdated(ServerConnection server, Map<String, String> params) {
        int clid = Integer.valueOf(params.get("clid"));
        try {

            TSClient client = server.getClientByCLID(clid);
            boolean needresort = params.containsKey("client_talk_power") || params.containsKey("client_nickname") || params.containsKey("client_is_talker");
            client.updateClient(params);
            if (needresort) {
                server.sortChannelClients(client.getChannelID());
            }
            Message msg = mHandler.obtainMessage(TSMessageType.ClientAltered.showCode(), server.getID(), clid, "Client status changed");
            msg.sendToTarget();
        } catch (ServerConnection.SCNotFoundException ex) {
            Log.e("Notify", "Failed to update client clid:" + clid + ". " + ex.getErrorDescription(), ex);
            handleMissingTSObject(ex, server);
        }

    }

    private void handleNotifyClientMoved(ServerConnection server, Map<String, String> params) {
        int clid = Integer.valueOf(params.get("clid"));

        try {
            server.moveClient(clid, params);
            Message msg = mHandler.obtainMessage(TSMessageType.ClientMoved.showCode(), server.getID(), clid, "Client moved");
            msg.sendToTarget();
        } catch (ServerConnection.SCNotFoundException ex) {
            Log.e("Notify", "Failed to move client clid:" + clid + ". " + ex.getErrorDescription(), ex);
            handleMissingTSObject(ex, server);
        }
    }

    private void handleNotifyClientChannelGroupChanged(ServerConnection server, Map<String, String> params) {
        int cgid = Integer.valueOf(params.get("cgid"));
        int cid = Integer.valueOf(params.get("cid"));
        int clid = Integer.valueOf(params.get("clid"));
        try {
            server.getClientByCLID(clid).changeChannelGroup(cgid, cid);
            Message msg = mHandler.obtainMessage(TSMessageType.ClientAltered.showCode(), server.getID(), clid, "Client channel group changed");
            msg.sendToTarget();
        } catch (ServerConnection.SCNotFoundException ex) {
            Log.e("Notify", "Failed to change client channel group. " + ex.getErrorDescription(), ex);
            handleMissingTSObject(ex, server);

        }
    }

    private void handleNotifyCurrentServerConnectionChanged(ServerConnection server) {
        DisplayedSCHandler = server.getID();
        Message msg = mHandler.obtainMessage(TSMessageType.DisplayedServerConnectionChange.showCode(), server.getID(), 0, "Displayed server connection changed");
        msg.sendToTarget();
    }

    private void handleNotifyClientLeftView(ServerConnection server, Map<String, String> params) {
        int clid = Integer.valueOf(params.get("clid"));
        try {
            server.removeClientByCLID(clid);
            Message msg = mHandler.obtainMessage(TSMessageType.ClientLeft.showCode(), server.getID(), clid, "Client left view");
            msg.sendToTarget();
        } catch (ServerConnection.SCNotFoundException ex) {
            Log.e("Notify", "Failed to remove client from view, client id:" + clid + " not found in clientlist. " + ex.getErrorDescription(), ex);
            handleMissingTSObject(ex, server);
        }
    }

    private void handleNotifyClientEnterView(ServerConnection server, Map<String, String> params) throws TSRemoteException {
        Set<String> keys = params.keySet();
        Iterator<String> iterKeys = keys.iterator();
        Pattern pattern = Pattern.compile("(\\d+)?(.+)");
        TSClient client;
        Matcher m;
        String s;
        int i = -1;
        while (iterKeys.hasNext()) {
            s = iterKeys.next();
            m = pattern.matcher(s);
            if (m.find()) {
                if ((m.group(1) != null) && (Integer.valueOf(m.group(1)) > i)) {
                    client = new TSClient(params, i);
                    server.AddClient(client);
                    Message msg = mHandler.obtainMessage(TSMessageType.ClientJoined.showCode(), server.getID(), client.getClientID(), "Client entered view");
                    msg.sendToTarget();
                    i++;
                }
            } else {
                throw new TSRemoteException("Unable to build client parameter map");
            }
        }
        //group=new TSGroup(grpMap);
        if (i > -1) {
            server.ClientListReceived();
        }
        client = new TSClient(params, i);
        server.AddClient(client);
        Message msg = mHandler.obtainMessage(TSMessageType.ClientJoined.showCode(), server.getID(), client.getClientID(), "Client entered view");
        msg.sendToTarget();
    }

    private void handleNotifyChannelCreated(ServerConnection server, Map<String, String> params) {
        TSChannel newChannel = new TSChannel(params);
        server.AddChannel(newChannel);
        Message msg = mHandler.obtainMessage(TSMessageType.ChannelCreated.showCode(), server.getID(), 0, "Channel created");
        msg.sendToTarget();

    }

    private void handleNotifyChannelEdited(ServerConnection server, Map<String, String> params) {
        int cid = Integer.valueOf(params.get("cid"));
        try {
            TSChannel channel = server.getChannelByID(cid);
            boolean resortNeeded = (params.containsKey("channel_order")) || (params.containsKey("channel_name"));
            channel.updateChannel(params);
            if (resortNeeded) {//if you change something that effects its sort order
                TSChannel parent = server.getChannelByID(channel.getParentID());//find its parent
                parent.sortSubchannels(new ChannelOrderer(server));//and resort its position
            }
            Message msg = mHandler.obtainMessage(TSMessageType.ChannelAltered.showCode(), server.getID(), cid, "Channel edited");
            msg.sendToTarget();
        } catch (ServerConnection.SCNotFoundException ex) {
            Log.e("Notify", "Failed to edit channel, channel id:" + cid + " not found. " + ex.getErrorDescription(), ex);
            handleMissingTSObject(ex, server);
        }
    }

    private void handleNotifyServerUpdated(ServerConnection server, Map<String, String> params) {
        server.updateServer(params);
        Message msg = mHandler.obtainMessage(TSMessageType.ServerAltered.showCode(), server.getID(), 0, "Server edited");
        msg.sendToTarget();
    }

    private void handleNotifyConnectStatusChange(ServerConnection server, Map<String, String> params) {

        String status = params.get("status");
        if (status.equals("connecting")) {
            server.Reinitialise();
        } else if (status.equals("connected")) {
            server.connectionVerified();
        } else if (status.equals("connection_establishing")) {
            Log.i("Connection", "Unused connection status:" + status);
        } else if (status.equals("connection_established")) {
            Log.i("Connection", "Unused connection status:" + status);
        } else if (status.equals("disconnected")) {
            server.Disconnect();
        }
    }

    private void handleChannelList(ServerConnection server, Map<String, String> params) throws TSRemoteException {
        Set<String> keys = params.keySet();
        Iterator<String> iterKeys = keys.iterator();
        //Map<String,String> chanMap = new HashMap<String, String>();
        Pattern pattern = Pattern.compile("(\\d+)?(.+)");
        Matcher m;
        String s;
        TSChannel channel;
        int i = -1;
        while (iterKeys.hasNext()) {
            s = iterKeys.next();
            m = pattern.matcher(s);
            if (m.find()) {
                if ((m.group(1) != null) && (Integer.valueOf(m.group(1)) > i)) {
                    channel = new TSChannel(params, i);
                    server.AddChannel(channel);
                    //chanMap.clear();
                    i++;
                }

                //chanMap.put(m.group(2),params.get(s));
            } else {
                throw new TSRemoteException("Unable to build channel parameter map");
            }
        }
        channel = new TSChannel(params, i);
        server.AddChannel(channel);
    }

    private void handleNotifyChannelDeleted(ServerConnection server, Map<String, String> params) {
        int cid = Integer.valueOf(params.get("cid"));
        try {
            server.removeChannelByID(cid);
            Message msg = mHandler.obtainMessage(TSMessageType.ChannelDeleted.showCode(), server.getID(), cid, "Channel deleted");
            msg.sendToTarget();
        } catch (ServerConnection.SCNotFoundException ex) {
            Log.e("Notify", "Failed to remove channel from view, channel id:" + cid + " not found in channellist. " + ex.getErrorDescription(), ex);
        }
    }

    private void handleNotifyChannelSubscribed(ServerConnection server, Map<String, String> params) throws ServerConnection.SCNotFoundException {
        int cid = Integer.valueOf(params.get("cid"));
        server.getChannelByID(cid).subscibe();
        Message msg = mHandler.obtainMessage(TSMessageType.ChannelAltered.showCode(), server.getID(), cid, "Subscribed to channel");
        msg.sendToTarget();
    }

    private void handleNotifyChannelMoved(ServerConnection server, Map<String, String> params) throws ServerConnection.SCNotFoundException {
        int cid = Integer.valueOf(params.get("cid"));
        int cpid = Integer.valueOf(params.get("cpid"));
        server.moveChannel(cid, cpid);
        Message msg = mHandler.obtainMessage(TSMessageType.ChannelMoved.showCode(), server.getID(), cid, "Channel moved");
        msg.sendToTarget();
    }

    private void handleNotifyChannelPasswordChanged(ServerConnection server, Map<String, String> params) throws ServerConnection.SCNotFoundException {
        int cid = Integer.valueOf(params.get("cid"));
        TSChannel channel = server.getChannelByID(cid);
        channel.passwordChanged();
        Message msg = mHandler.obtainMessage(TSMessageType.ChannelAltered.showCode(), server.getID(), cid, "Channel password changed");
        msg.sendToTarget();
    }

    private void handleNotifyServerEdited(ServerConnection server, Map<String, String> params) {
        server.edited(params);
        Message msg = mHandler.obtainMessage(TSMessageType.ServerAltered.showCode(), server.getID(), 0, "Server edited");
        msg.sendToTarget();
    }

    private void handleChannelListFinished(ServerConnection server) {
        server.processChannels();
        server.ChannelsReceived();//this means the client has finished sending us the channel list
    }

    private void handleMissingTSObject(ServerConnection.SCNotFoundException ex, ServerConnection serverConnection) {
        ConnectionStage stage = serverConnection.getStage();
        ServerConnection.ItemNotFoundType type = ex.getItemNotFoundType();
        if (stage == ConnectionStage.VerifyConnection) {
            if (!sentCommands.contains("whoami")) {
                SendCQMessageForServer("whoami", serverConnection);
            }
        } else {
            String s = stage.toString();
            String message = getRequestMessage(type);
            if (!sentCommands.contains(message)) {
                SendCQMessageForServer(message, serverConnection);
            }

        }

    }

    /**
     * This function gets the client query command used to request the client resends the missing objects
     *
     * @param type The type of info that is missing from the client
     * @return Returns the client query command to send
     */
    private String getRequestMessage(ServerConnection.ItemNotFoundType type) {
        switch (type) {
            case Client:
                return "clientlist -uid -away -voice -groups -icon -country";
            case ServerGroup:
                return "servergrouplist";
            case Channel:
                return "channellist -topic -flags -voice -icon -limits";
            case ChannelGroup:
                return "channelgrouplist";
            case ServerInfo:
                return "servervariable virtualserver_name virtualserver_platform virtualserver_version virtualserver_created virtualserver_codec_encryption_mode virtualserver_default_server_group virtualserver_default_channel_group virtualserver_hostbanner_url virtualserver_hostbanner_gfx_url virtualserver_hostbanner_gfx_interval virtualserver_priority_speaker_dimm_modificator virtualserver_id virtualserver_hostbutton_tooltip virtualserver_hostbutton_url virtualserver_hostbutton_gfx_url virtualserver_name_phonetic virtualserver_icon_id virtualserver_ip virtualserver_ask_for_privilegekey virtualserver_hostbanner_mode";
            default:
                return "";
        }
    }

    /**
     * This function handles the sending of the keep alive message, it will also request any missing objects from the client.
     *
     * @param message The additional command to send to keep the connection alive
     */
    public void SendKeepAlive(String message) {
        for (ServerConnection server : SCHandlers) {
            for (ServerConnection.ItemNotFoundType type : server.getRequests()) {
                SendCQMessageForServer(getRequestMessage(type), server);
            }
            server.clearRequests();
        }
        SendCQMessage(message);
    }
}