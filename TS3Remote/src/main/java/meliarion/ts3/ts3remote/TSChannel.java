package meliarion.ts3.ts3remote;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Meliarion on 02/08/13.
 * Class that represents a Teamspeak Channel
 */
public class TSChannel implements Comparable<TSChannel>{
    private String Name;
    private int ID;
    private int ChannelParentID;
    private int order;
    private String topic ="";
    private boolean isDefaultChannel;
    private boolean hasPassword;
    private boolean knownPassword = false;
    private boolean isPermanent = false;
    private boolean isSemiPermanent = false;
    private boolean subscribed;
    private boolean isUnencrypted = false;
    private String description;
    private CodecType Codec = CodecType.INVALID_CODEC;
    private int CodecQuality = -1;
    private int neededTalkPower;
    private int iconID;
    private int maxClients;
    private int maxClientsFamily;
    private int totalClients;
    private List<Integer> subchannelsID = new ArrayList<Integer>();
    private ChannelType type;

    public TSChannel(int _ID, int _PID, String _Name, ChannelType _type){
        this.ID = _ID;
        this.ChannelParentID = _PID;
        this.Name = _Name;
        this.type = _type;
    }
    public TSChannel (String datablock)
    {   Pattern ptest = Pattern.compile("([^=\\s]+)=?(\\S*)(.*)");
        Matcher mtest = ptest.matcher(datablock);
        Map<String,String> testmap = new HashMap<String, String>();
        String remainder;
        while (mtest.find())
        {testmap.put(mtest.group(1),mtest.group(2));
         remainder = mtest.group(3);
         mtest = ptest.matcher(remainder);
        }
        initialise(testmap,-1);
        /*Pattern channel = Pattern.compile("cid=(\\d+) pid=(\\d+) channel_order=(\\d+) channel_name=(\\S+) channel_topic=?(\\S*) channel_flag_default=(\\d) channel_flag_password=(\\d) channel_flag_permanent=(\\d) channel_flag_semi_permanent=(\\d) channel_codec=(\\d+) channel_codec_quality=(\\d+) channel_needed_talk_power=(\\d+) channel_icon_id=(\\d+) channel_maxclients=(-?\\d+) channel_maxfamilyclients=(-?\\d+) channel_flag_are_subscribed=(\\d) total_clients=(\\d+)");
        Matcher m = channel.matcher(datablock);
        boolean b;
        if (b=m.find()){
        this.ID = Integer.valueOf(m.group(1));
        this.ChannelParentID = Integer.valueOf(m.group(2));
        this.order = Integer.valueOf(m.group(3));
        this.Name = m.group(4);
        this.topic =m.group(5);
        this.isDefaultChannel=m.group(6).equals("1");
        this.hasPassword = m.group(7).equals("1");
        this.isPermanent = m.group(8).equals("1");
        this.isSemiPermanent = m.group(9).equals("1");
        setCodec(Integer.valueOf(m.group(10)));
        this.CodecQuality = Integer.valueOf(m.group(11));
        this.neededTalkPower = Integer.valueOf(m.group(12));
        this.iconID = Integer.valueOf(m.group(13));
        this.maxClients = Integer.valueOf(m.group(14));
        this.maxClientsFamily = Integer.valueOf(m.group(15));
        this.subscribed = m.group(16).equals("1");
        this.totalClients = Integer.valueOf(m.group(17));
        setType();
        }
        else
        {
            throw new Exception("Failed to parse datablock for channel");
        }*/

    }
    public TSChannel (Map<String,String> params){
    initialise(params,-1);
    }
    public TSChannel (Map<String, String> params, int index)
    {
        initialise(params,index);
    }
    private void initialise(Map<String,String>params, int index)
    {String prefix = getPrefix(index);
        this.ID = Integer.valueOf(params.get(prefix+"cid"));
        if(params.containsKey(prefix+"cpid")){
        this.ChannelParentID = Integer.valueOf(params.get(prefix+"cpid"));
        }
        else
        {
        this.ChannelParentID = Integer.valueOf(params.get(prefix+"pid"));
        }
        this.order = Integer.valueOf(params.get(prefix+"channel_order"));
        this.Name = params.get(prefix+"channel_name");
        if(params.containsKey(prefix+"channel_topic")){
            this.topic = params.get(prefix+"channel_topic");
        }
        this.isDefaultChannel = (params.containsKey(prefix+"channel_flag_default")&&(params.get(prefix+"channel_flag_default").equals("1")));
        if(params.containsKey(prefix+"channel_codec")){
            setCodec(Integer.valueOf(params.get(prefix+"channel_codec")));
        }
        if (params.containsKey(prefix+"channel_codec_quality")){
            this.CodecQuality = Integer.valueOf(params.get(prefix+"channel_codec_quality"));
        }
        this.isUnencrypted = params.containsKey(prefix+"channel_codec_is_unencrypted")&&params.get(prefix+"channel_codec_is_unencrypted").equals("1");
        this.isPermanent = (params.containsKey(prefix+"channel_flag_permanent"))&&params.get(prefix+"channel_flag_permanent").equals("1");
        this.isSemiPermanent = params.containsKey(prefix+"channel_flag_semi_permanent")&&params.get(prefix+"channel_flag_permanent").equals("1");
        this.hasPassword = params.containsKey(prefix+"channel_flag_password")&&params.get(prefix+"channel_flag_password").equals("1");
        if(params.containsKey(prefix+"channel_codec"))
        {
        setCodec(Integer.valueOf(params.get(prefix+"channel_codec")));
        }
        if(params.containsKey(prefix+"channel_needed_talk_power")){
        this.neededTalkPower = Integer.valueOf(params.get(prefix+"channel_needed_talk_power"));
        }
        if(params.containsKey(prefix+"channel_icon_id"))
        {
        this.iconID = Integer.valueOf(params.get(prefix+"channel_icon_id"));
        }
        if(params.containsKey(prefix+"channel_maxclients"))
        {
        this.maxClients = Integer.valueOf(params.get(prefix+"channel_maxclients"));
        }
        if(params.containsKey(prefix+"channel_maxfamilyclients"))
        {
        this.maxClientsFamily = Integer.valueOf(params.get(prefix+"channel_maxfamilyclients"));
        }
        this.subscribed = params.containsKey(prefix+"channel_flag_are_subscirbed")&&params.get(prefix+"channel_flag_are_subscirbed").equals("1");
        if (params.containsKey(prefix+"total_clients"))
        {
        this.totalClients = Integer.valueOf(params.get(prefix+"total_clients"));
        }
        setType();

    }
    private void setType(){
        if(this.isPermanent){
            type = ChannelType.PermanentChannel;
        }
        else if(this.isSemiPermanent)
        {
         type = ChannelType.SemiPermanentChannel;
        }
        else
        {
         type = ChannelType.TemporaryChannel;
        }
    }
    private void setCodec(int _ID){
        Codec = CodecType.getCodecType(_ID);
    }
    public void updateChannel(TSChannel channel)
    {
    Log.w("TSChannel","Update channel by TSChannel method received ");
    }
    public void subscibe(){
        this.subscribed = true;
    }
    public void unSubscribe(){
        this.subscribed=false;
    }
    public void setIcon(int _id){
        iconID = _id;
    }
    public static String getPrefix(int index)
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
    public void updateChannel(Map<String,String> params){
        if (params.containsKey("cid"))
        {if(Integer.valueOf(params.get("cid"))!=ID)
        {
            Log.e("TSChannel","Channel ID missmatch when updating channel");
            return;
        }
            params.remove("cid");
        }
        if (params.containsKey("channel_flag_default"))
        {
            this.isDefaultChannel = params.get("channel_flag_default").equals("1");
            params.remove("channel_flag_default");
        }
        if (params.containsKey("channel_needed_talk_power"))
        {
            this.neededTalkPower = Integer.valueOf(params.get("channel_needed_talk_power"));
            params.remove("channel_needed_talk_power");
        }
        if (params.containsKey("channel_flag_password"))
        {
            this.hasPassword = params.get("channel_flag_password").equals("1");
            params.remove("channel_flag_password");
        }
        if (params.containsKey("channel_icon_id"))
        {
            setIcon(Integer.valueOf(params.get("channel_icon_id")));
            params.remove("channel_icon_id");
        }
        if (params.containsKey("channel_description"))
        {
            this.description = params.get("channel_description");
            params.remove("channel_description");
        }
        if (params.containsKey("channel_codec"))
        {
            this.setCodec(Integer.valueOf(params.get("channel_codec")));
            params.remove("channel_codec");
        }
        if (params.containsKey("channel_name"))
        {
            this.Name = params.get("channel_name");
            params.remove("channel_name");
        }
        if (params.containsKey("channel_order"))
        {
            this.order = Integer.valueOf(params.get("channel_order"));
            params.remove("channel_order");
        }
        if (params.containsKey("invokerid")){
            params.remove("invokerid");
        }
        if (params.containsKey("reasonid"))
        {
            params.remove("reasonid");
        }
        if (params.containsKey("invokername"))
        {
            params.remove("invokername");
        }
        if (params.containsKey("invokeruid")){
            params.remove("invokeruid");
        }
        if (params.containsKey("channel_topic"))
        {
            this.topic = params.get("channel_topic");
            params.remove("channel_topic");
        }
        if (params.containsKey("channel_codec_quality"))
        {
            this.CodecQuality = Integer.valueOf(params.get("channel_codec_quality"));
            params.remove("channel_codec_quality");
        }
        if (params.size()>0){
            String error = params.size()+" unhandled parameters received. ";
            for (String s : params.keySet())
            {
                error += s+", ";
            }
            Log.w("TSChannel",error);

        }
    }
    public ChannelType getType(){
        return type;
    }
    public boolean isSpacer(){
        Pattern p = Pattern.compile("\\[.?spacer(\\d+)\\]");
        Matcher m = p.matcher(Name);
        return (ChannelParentID==0)&&(Name.length()>=9)&&m.find();//(Name.substring(0,9).equals("[spacer0]"));
    }
    public int getID(){
        return this.ID;
    }
    public String getName(){
        if(!isSpacer()){
        return this.Name;
        }
        else {
            Pattern p =Pattern.compile("\\[(.?)spacer(\\d+)\\](.*)");
            Matcher m = p.matcher(this.Name);
            String name="";
            int spacerid=-1;
            String align ="";
            if(m.find()){
            align = m.group(1);
            spacerid = Integer.valueOf(m.group(2));
            name = m.group(3);
            }
            if(name.length()==3){
            return  handleSpecialSpacers(name);
            }
            return name;
        }
    }
    private String handleSpecialSpacers(String name)
    {   if (name.equals("..."))
        {
            return ".........................................................................................................................";
        }
        else if (name.equals("---"))
        {
            return "---------------------------------------------------------------------------------------------------------------------------";
        }
        else if (name.equals("___"))
        {
            return "___________________________________________________________________________________________________________________________";
        }
        return name;
    }
    public String getRawName (){
        return this.Name;
    }
    public void passwordChanged(){
        this.knownPassword = false;
    }
    public int getParentID()
    {
        return this.ChannelParentID;
    }
    public int getOrder(){
        return this.order;
    }
    public int getMaxClients(){
        return this.maxClients;
    }
    public int getMaxClientsFamily(){
        return this.maxClientsFamily;
    }
    public int getTotalClients()
    {
        return this.totalClients;
    }
    public void addSubchannel(Integer ID){
        subchannelsID.add(ID);
    }
    public List<Integer> getSubchannelIDs(){
        return subchannelsID;
    }
    public boolean isSubscribed(){
        return this.subscribed;
    }
    public boolean isDefaultChannel()
    {
        return isDefaultChannel;
    }
    public boolean isPassworded(){
        return hasPassword;
    }
    public int getNeededTalkPower(){
        return neededTalkPower;
    }
    public int getIconID(){
        return iconID;
    }
    public boolean isFull(){
        boolean familyFull = (getMaxClientsFamily() != -1);
        return  familyFull ||(maxClients != -1 && maxClients >= totalClients);
    }
    public boolean isPasswordKnown(){
        return knownPassword;
    }
    public CodecType getCodec()
    {
        return Codec;
    }
    public void changeParentID(int _id){
        this.ChannelParentID = _id;
    }
    @Override
    public String toString() {
        return Name;
    }
    public boolean isChannelUnencrypted(){
        return this.isUnencrypted;
    }
    @Override
    public int compareTo(TSChannel channel) {
        TSChannel currChannel = this;

        if (channel.getOrder()>currChannel.getOrder())
        {
            return -1;
        }
        else if (channel.getOrder()==currChannel.getOrder()){
            return 0;
        }
        else
        {
            return 1;
        }
    }

    public void sortSubchannels(ChannelOrderer orderer) {
        if (subchannelsID.size()>1){
        Collections.sort(subchannelsID,orderer);
        }
    }

    public enum ChannelType {
        TemporaryChannel,
        SemiPermanentChannel,
        PermanentChannel,
        ServerTopLevel
    }

    public enum CodecType {
        INVALID_CODEC (-1),
        CODEC_SPEEX_NARROWBAND (0),
        CODEC_SPEEX_WIDEBAND (1),
        CODEC_SPEEX_ULTRAWIDEBAND (2),
        CODEC_CELT_MONO (3);

        private int id;
        CodecType (int _id){
            this.id = _id;
        }
        public int getID(){
            return this.id;
        }
        public static CodecType getCodecType(int _id){
            switch (_id)
            {
                case 0:
                return CODEC_SPEEX_NARROWBAND;
                case 1:
                return CODEC_SPEEX_WIDEBAND;
                case 2:
                return CODEC_SPEEX_ULTRAWIDEBAND;
                case 3:
                return CODEC_CELT_MONO;
                default:
                return INVALID_CODEC;
            }
        }
        public boolean isMusicCodec(){
            switch (this)
            {
                case CODEC_CELT_MONO:
                return true;
                default:
                return false;
            }

        }
    }
}
