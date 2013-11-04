package meliarion.ts3.ts3remote;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Meliarion on 28/07/13.
 * Class which represents a teamspeak client
 */

 public class TSClient {
    private String Name = "Uninitialised client";
    private String UID;
    private String Chat ="";
    private int CLID;
    private int ChannelID;
    private int CDBID;
    private int talkPower;
    private int iconID;
    private boolean canTalk;//has permission to talk
    private boolean Away;
    private boolean talking;
    private boolean inputMuted;
    private boolean outputMuted;
    private boolean inputEnabled;
    private boolean outputEnabled;
    private boolean prioritySpeaker;
    private boolean recording;
    private boolean channelCommander;
    private boolean localMuted;
    private boolean whispering = false;
    private String AwayMsg;
    private List<Integer> serverGroupsID = new ArrayList<Integer>();
    private int channelGroupID;
    private String country;
    private ClientType type;

    TSClient (String _Name, String _UID, int _CLID, int _CID)
    {
        this.Name=_Name;
        this.UID=_UID;
        this.CLID=_CLID;
        this.ChannelID =_CID;

    }
    TSClient (String dataBlock)throws Exception{
        String s = "clid=(\\d+)\\scid=(\\d+)\\sclient_database_id=(\\d+)\\sclient_nickname=(\\S+)\\sclient_type=(\\d+) client_away=(\\d) client_away_message=?(\\S*) client_flag_talking=(\\d) client_input_muted=(\\d) client_output_muted=(\\d) client_input_hardware=(\\d) client_output_hardware=(\\d) client_talk_power=(\\d+) client_is_talker=(\\d) client_is_priority_speaker=(\\d) client_is_recording=(\\d) client_is_channel_commander=(\\d) client_is_muted=(\\d) client_unique_identifier=(\\S+) client_servergroups=(\\S+) client_channel_group_id=(\\d+) client_icon_id=(\\d+) client_country=?([^\\|\\s]*)";
        Pattern pattern = Pattern.compile(s);
        Matcher m = pattern.matcher(dataBlock.trim());
        if (m.find()){
        this.CLID = Integer.valueOf(m.group(1));
        this.ChannelID = Integer.valueOf(m.group(2));
        this.CDBID=Integer.valueOf(m.group(3));
        this.Name=m.group(4);
        this.type = ClientType.getClientTypeByID(Integer.valueOf(m.group(5)));
        this.Away = m.group(6).equals("1");
        this.AwayMsg =m.group(7);
        this.talking=m.group(8).equals("1");
        this.inputMuted=m.group(6).equals("1");
        this.outputMuted = m.group(10).equals("1");
        this.inputEnabled = m.group(11).equals("1");
        this.outputEnabled = m.group(12).equals("1");
        this.talkPower = Integer.valueOf(m.group(13));
        this.canTalk = m.group(14).equals("1");
        this.prioritySpeaker = m.group(15).equals("1");
        this.recording = m.group(16).equals("1");
        this.channelCommander = m.group(17).equals("1");
        this.localMuted = false;//m.group(18).equals("1");
        this.UID = m.group(19);
        String grps = m.group(20);
        channelGroupID = Integer.valueOf(m.group(21));
        iconID = Integer.valueOf(m.group(22));
        country = m.group(23);
        populateServerGroupIDs(grps);
        }
        else
        {
          throw new Exception("Failed to parse data block for client");
        }

    }
    TSClient (Map<String, String> params)
    {
    parseParams(params,-1);
    }
    TSClient (Map<String, String>params, int index)
    {
        parseParams(params,index);
    }
    private void parseParams(Map<String, String>params, int index)
    {   try{
        String ident = getident(index);
        this.CLID = Integer.valueOf(params.get(ident+"clid"));
        this.Name=params.get(ident+"client_nickname");
        this.ChannelID = Integer.valueOf(params.get("ctid"));//Clientquery groups all Clients in the same channel together
        this.CDBID=Integer.valueOf(params.get(ident+"client_database_id"));
        this.type = ClientType.getClientTypeByID(Integer.valueOf(params.get(ident+"client_type")));
        this.Away = params.get(ident+"client_away").equals("1");
        this.AwayMsg = params.get(ident+"client_away_message");
        this.talking=false;
        this.inputMuted=params.get(ident+"client_input_muted").equals("1");
        this.outputMuted = params.get(ident+"client_output_muted").equals("1");
        this.inputEnabled = params.get(ident+"client_input_hardware").equals("1");
        this.outputEnabled = params.get(ident+"client_output_hardware").equals("1");
        this.talkPower = Integer.valueOf(params.get(ident+"client_talk_power"));
        this.canTalk = params.get(ident+"client_is_talker").equals("1");
        this.prioritySpeaker = params.get(ident+"client_is_priority_speaker").equals("1");
        this.recording = params.get(ident+"client_is_recording").equals("1");
        this.channelCommander =  params.get(ident+"client_is_channel_commander").equals("1");
        this.localMuted = false;//m.group(18).equals("1");
        this.UID = params.get(ident+"client_unique_identifier");
        String grps = params.get(ident+"client_servergroups");
        channelGroupID = Integer.valueOf(params.get(ident+"client_channel_group_id"));
        iconID = Integer.valueOf(params.get(ident+"client_icon_id"));
        country = params.get(ident+"client_country");
        populateServerGroupIDs(grps);
        }
        catch (NumberFormatException ex)
        {
        Log.e("TSClient","Error parsing params for client:"+getName(),ex);
        }
    }
    public static String getident(int index)
    {
        if (index==-1)
        {
            return "";
        }
        else
        {
            return index+"";
        }
    }
    private void populateServerGroupIDs(String idblock){
        serverGroupsID.clear();
        for(String a : idblock.split(",")){
            serverGroupsID.add(Integer.valueOf(a));
        }
    }
    public ClientType getClientType(){
        return type;
    }

    public String getName(){
        return ServerConnection.unmakeTransmitSafe(Name);
    }

   public String getUID(){
       return UID;
   }
   public  int getIconID(){
       return this.iconID;
   }
    public int getClientID(){
        return CLID;
    }
    public int getChannelID(){
        return ChannelID;
    }
    public void changeChannel(int _CID){
        this.ChannelID =_CID;
    }
    @Override
    public String toString() {
        return getName();
    }
    public void SetTalkStatus(boolean _talking, boolean _whispering){
        this.talking = _talking;
        this.whispering=_whispering&&_talking;

    }
    public TSClientStatus getStatus(){
        if(localMuted){
            return TSClientStatus.LocalMuted;
        }
        else if (Away){
            return TSClientStatus.Away;
        }
        else if(!outputEnabled)
        {
            return TSClientStatus.OutputDisabled;
        }
        else if (outputMuted)
        {
            return TSClientStatus.OutputMuted;
        }
        else if(!inputEnabled){
            return TSClientStatus.InputDisabled;
        }
        else if(inputMuted){
            return TSClientStatus.InputMuted;
        }
        else if (whispering)
        {
            return TSClientStatus.Whispering;
        }
        else if (channelCommander){
            if(talking){
            return TSClientStatus.CommanderTalking;
            }
            else
            {
                return TSClientStatus.CommanderNotTalking;
            }
        }
        else if (talking)
        {
            return TSClientStatus.Talking;
        }
        else
        {
            return TSClientStatus.NotTalking;
        }
    }

    public void moveClient(Map<String, String> params)
    {
    String s;
        this.ChannelID=Integer.valueOf(params.get("ctid"));
    }

    public void updateClient(Map<String, String> params) {
         if(params.containsKey("client_is_channel_commander"))
         {
             channelCommander=params.get("client_is_channel_commander").equals("1");
             params.remove("client_is_channel_commander");
         }
         if(params.containsKey("client_input_disabled"))
         {
             inputMuted=params.get("client_input_disabled").equals("1");
             params.remove("client_input_disabled");
         }
         if(params.containsKey("client_output_disabled"))
         {
             inputMuted=params.get("client_output_disabled").equals("1");
             params.remove("client_output_disabled");
         }
         if (params.containsKey("client_input_hardware")){

             inputEnabled = params.get("client_input_hardware").equals("1");
             params.remove("client_input_hardware");
         }
         if (params.containsKey("client_output_hardware")){

             inputEnabled = params.get("client_output_hardware").equals("1");
             params.remove("client_output_hardware");
         }
         if (params.containsKey("client_is_talker")){

             canTalk = params.get("client_is_talker").equals("1");
             params.remove("client_is_talker");
         }
         if (params.containsKey("client_is_priority_speaker")){

             prioritySpeaker = params.get("client_is_priority_speaker").equals("1");
             params.remove("client_is_priority_speaker");
         }
         if (params.containsKey("client_is_recording")){

             recording = params.get("client_is_recording").equals("1");
             params.remove("client_is_recording");
         }
         if (params.containsKey("client_away")){

             Away = params.get("client_away").equals("1");
             params.remove("client_away");
             if (!Away)
             {
                 AwayMsg = "";
             }
             else if (params.containsKey("client_away_message"))
             {AwayMsg = params.get("client_away_message");
                 params.remove("client_away_message");
             }
         }
         if (params.containsKey("client_servergroups"))
         {
             populateServerGroupIDs(params.get("client_servergroups"));
             params.remove("client_servergroups");
         }
         if (params.containsKey("client_icon_id"))
         {
             this.iconID = Integer.valueOf(params.get("client_icon_id"));
             params.remove("client_icon_id");
         }
         if (params.containsKey("client_input_muted"))
         {
             this.inputMuted = params.get("client_input_muted").equals("1");
             params.remove("client_input_muted");
         }
         if (params.containsKey("client_output_muted"))
         {
             this.outputMuted = params.get("client_output_muted").equals("1");
             params.remove("client_output_muted");
         }
         if (params.containsKey("client_talk_power"))
         {
             this.talkPower = Integer.valueOf(params.get("client_talk_power"));
             params.remove("client_talk_power");
         }
         if (params.containsKey("client_nickname"))
         {
             this.Name = params.get("client_nickname");
             params.remove("client_nickname");
         }
         if (params.containsKey("client_meta_data")){
             String metadata = params.get("client_meta_data");
             params.remove("client_meta_data");
         }
         if (params.containsKey("client_badges"))
         {
             String badges = params.get("client_badges");
             params.remove("client_badges");
         }
         if (params.containsKey("clid"))
         {
            int clid = Integer.valueOf(params.get("clid"));
            params.remove("clid");
         }
         if (params.containsKey("client_version"))
         {
             String version = params.get("client_version");
             params.remove("client_version");
         }
         if (params.containsKey("client_platform")){
            String platform = params.get("client_platform");
             params.remove("client_platform");
         }
         if (params.containsKey("client_login_name"))
         {
             String loginName = params.get("client_login_name");
             params.remove("client_login_name");
         }
         if (params.containsKey("client_created"))
         {
             String client_created = params.get("client_created");
             params.remove("client_created");
         }
        if (params.containsKey("client_lastconnected"))
        {
            String client_lastconnected = params.get("client_lastconnected");
            params.remove("client_lastconnected");
        }
        if (params.containsKey("client_totalconnections"))
        {
            String client_totalconnections = params.get("client_totalconnections");
            params.remove("client_totalconnections");
        }
        if (params.containsKey("client_month_bytes_uploaded"))
        {
            String client_month_bytes_uploaded = params.get("client_month_bytes_uploaded");
            params.remove("client_month_bytes_uploaded");
        }
        if (params.containsKey("client_month_bytes_downloaded"))
        {
            String client_month_bytes_downloaded = params.get("client_month_bytes_downloaded");
            params.remove("client_month_bytes_downloaded");
        }
        if (params.containsKey("client_total_bytes_uploaded"))
        {
            String client_total_bytes_uploaded = params.get("client_total_bytes_uploaded");
            params.remove("client_total_bytes_uploaded");
        }
        if (params.containsKey("client_total_bytes_downloaded"))
        {
            String client_total_bytes_downloaded = params.get("client_total_bytes_downloaded");
            params.remove("client_total_bytes_downloaded");
        }
        if (params.containsKey("client_flag_avatar"))
        {
            String avatar = params.get("client_flag_avatar");
            params.remove("client_flag_avatar");
        }
         if(params.size()>0)
         {String errorString = "Unused parameters received :";
             for (String key :params.keySet())
             {
                 errorString+=" "+key+",";
             }
             Log.w("TSClient",errorString);
         }

    }

    public void changeChannelGroup(int _channelGroupID, int _channelID){
        this.channelGroupID = _channelGroupID;
        this.ChannelID = _channelID;
    }
    public List<Integer> getServerGroups(){
        return serverGroupsID;
    }
    public int getChannelGroupID(){
        return channelGroupID;
    }
    public int getTalkPower(){
        return talkPower;
    }
    public boolean isRecording()
    {
        return recording;
    }
    public boolean isTalker(){
        return this.canTalk;
    }
    public boolean isPrioritySpeaker(){
        return prioritySpeaker;
    }
    public String getCountry(){
        return this.country;
    }
    public static enum ClientType {
        InvalidClient (-1),
        NormalClient (0),
        ServerQueryClient (1);

        private int type;
        ClientType(int _type)
        {
            this.type = _type;
        }
        int showType(){
            return type;
        }
        public static ClientType getClientTypeByID(int ID)
        {switch (ID)
        {
            case 0:
            return NormalClient;
            case 1:
            return ServerQueryClient;
            default:
            return InvalidClient;
        }
        }
    }
}
