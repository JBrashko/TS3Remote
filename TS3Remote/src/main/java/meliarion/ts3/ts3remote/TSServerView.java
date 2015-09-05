package meliarion.ts3.ts3remote;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Meliarion on 26/07/13.
 * Class which makes the View that displays the clients and channels of the server
 */
public class TSServerView extends View {

    private static Paint channelPaint;
    private static Paint defaultClientPaint;
    private static Paint recordingClientPaint;
    private static Paint debugPaint;
    private static Paint errorPaint;
    private ServerConnection connection;
    private boolean showCountry = false;
    private Rect bounds = new Rect();
    private float yPointer = 0f;
    private float xWidth=358;
    private static Resources res;
    private Map<Integer,TSRow> clientDisplay = new TreeMap<Integer, TSRow>();
    private Map<Integer,TSRow> channelDisplay = new TreeMap<Integer, TSRow>();
    private boolean altered = true;
    private boolean connected = true;

    public TSServerView(Context context){
        super(context);
        initialiseView(context);
    }
    public TSServerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialiseView(context);
    }
    public int getSCHandlerid(){
        if(connection!=null){
        return connection.getID();
        }
        else
        {
            return -1;
        }
    }
    public void test()
    {}
    public void setShowCountry(boolean value)
    {
        showCountry=value;
    }
    private void requestRedraw()
    {
        invalidate();
        requestLayout();
    }
    public void refresh()
    {requestRedraw();}
    public void setHandler(ServerConnection _curr){
        connection =_curr;
        buildChannels();
        requestRedraw();
    }
    public void clientChanged(int clientID){
        try{
        clientUpdate(clientID);
        }
        catch (ServerConnection.SCNotFoundException ex)
        {
            Log.e("TSView","Error finding client to update: "+ex,ex);
            clientLeft(clientID);
        }
        catch (Exception ex)
        {
            Log.e("TSView","Error updating client: "+ex,ex);
            setHandler(connection);
        }
    }
    public void clientLeft(int clientID) {
    requestRedraw();
    }

    public void clientMoved(int clientID) {
    try {
    if (!clientDisplay.containsKey(clientID)){
    clientUpdate(clientID);
    }
    TSRow row = clientDisplay.get(clientID);
    TSClient client = connection.getClientByCLID(clientID);
    TSChannel channel = connection.getChannelByID(client.getChannelID());
    int width = 20+row.getWidth();
    while (channel.getID()!=0)
    {
        width += 20;
        channel = connection.getChannelByID(channel.getParentID());
    }
        setObjectMinimumWidth(width);
    }
    catch (Exception e)
    {
        Log.e("TSView", "Error updating client position for client id:"+clientID,e);
    }
    }
    public void clientJoined(int clientID){
    try{
    clientUpdate(clientID);
    }
    catch (Exception e)
    {
        Log.e("TSView","Error building client row for newly joined client:"+clientID+". "+e,e);
    }
    }
    public void channelCreated(int channelID) {
        try{
        channelUpdate(channelID);
        }
        catch (Exception e)
        {
        Log.e("TSView","Error building row for newly created channel:"+channelID+". "+e,e);
        }
    }
    public void channelChanged(int channelID) {
        try{
        channelUpdate(channelID);
        }
        catch (Exception e)
        {
            Log.e("TSView","Error updating row for channel:"+channelID+". "+e,e);
        }
    }
    public void clientConnectionClosed() {
        connected= false;
        requestRedraw();
    }
    private void clientUpdate(int clientID) throws ServerConnection.SCNotFoundException{
        Log.i("TSView","Processing client, clientID:"+clientID);
        int width = 0;
        TSClient client = connection.getClientByCLID(clientID);
        TSRow clientRow = clientRow(client);
        clientDisplay.put(clientID,clientRow);
        width+=clientRow.getWidth()+20;
        int cid = client.getChannelID();
        TSChannel channel = connection.getChannelByID(cid);
        while(channel.getID()!=0)
        {width+= 20;
            channel = connection.getChannelByID(channel.getParentID());
        }
        setObjectMinimumWidth(width);
        requestRedraw();
    }
    private void channelUpdate(int channelID)throws ServerConnection.SCNotFoundException{
        int width = 0;
        TSChannel channel = connection.getChannelByID(channelID);
        TSRow row = channelRow(channel);
        channelDisplay.put(channelID,row);
        width+=row.getWidth()+20;
        TSChannel parentChannel = connection.getChannelByID(channel.getParentID());
        while(parentChannel.getID()!=0)
        {width+= 20;
            parentChannel = connection.getChannelByID(channel.getParentID());
        }
        setObjectMinimumWidth(width);
        requestRedraw();
    }

    private static void initialiseView(Context context){
        res = context.getResources();
        defaultClientPaint = new Paint();
        defaultClientPaint.setTextSize(16);
        defaultClientPaint.setTextAlign(Paint.Align.LEFT);
        channelPaint = new Paint(defaultClientPaint);
        recordingClientPaint = new Paint(defaultClientPaint);
        debugPaint = new Paint(defaultClientPaint);
        errorPaint = new Paint(debugPaint);
        errorPaint.setTextSize(20);
        channelPaint.setColor(Color.BLACK);
        defaultClientPaint.setColor(Color.BLACK);
        debugPaint.setColor(Color.GREEN);
        recordingClientPaint.setColor(Color.RED);
        errorPaint.setColor(Color.RED);
    }

    private void buildChannels(){
        if (connected){
            try{
            buildChannel(connection.getChannelByID(0),0);
            }
            catch (Exception e)
            {
            Log.e("TSView","TSView error building channels: "+e,e);
            }
        }
    }
    private void buildChannel(TSChannel channel, int indent)
    {   TSRow cRow = channelRow(channel);
        channelDisplay.put(channel.getID(), cRow);
        setObjectMinimumWidth(indent+cRow.getWidth());
        try{
            List<Integer> clientIDs = connection.getClientsByChannelID(channel.getID());
            if((clientIDs != null)&&(clientIDs.size()>0)){
                TSRow clRow;
                for (int clientID:clientIDs)
                {TSClient client = connection.getClientByCLID(clientID);
                 clRow = clientRow(client);
                 clientDisplay.put(clientID,clRow);
                 setObjectMinimumWidth(indent+20+clRow.getWidth());
              }
            }
        }
        catch (Exception e)
        {
            Log.e("TSView", "unable to build clients for channel cid="+channel.getID()+" error:"+e,e);
        }
        TSChannel subChannel;
        for (int subChannelID : channel.getSubchannelIDs()){
        try{
            subChannel = connection.getChannelByID(subChannelID);
            buildChannel(subChannel,indent+20);
            }
        catch (Exception e)
            {
            Log.e("TSView","TSView error building subchannel id:"+subChannelID+" error"+e,e);
            }
        }
    }
    private void drawMaps(Canvas canvas){
        try{
          drawChannelMaps(canvas,0,0);
        }catch (Exception e){
            Log.e("TSView","TSView error displaying server, error:"+e,e);
        }
    }
    private void drawChannelMaps(Canvas canvas, int channelID, int indent){
        Log.d("TSView","Drawing channel id:"+channelID);
        TSRow row = channelDisplay.get(channelID);
        int xPointer = indent;
        if (row.hasIcon()){
            Drawable icon = res.getDrawable(row.getLeftIcon());
            icon.setBounds(xPointer,(int)yPointer,xPointer+16,(int)yPointer+16);
            xPointer +=16;
            icon.draw(canvas);
        }
            canvas.drawText(row.getLeftName(),xPointer,yPointer+15,channelPaint);
        if (row.hasRight())
        {   bounds = new Rect();
            channelPaint.getTextBounds(row.getLeftName(),0,row.getLeftName().length(),bounds);
            canvas.drawBitmap(row.getRightContent(),xPointer+bounds.width(),yPointer,channelPaint);
        }
        yPointer += 16;
        try {
            TSChannel channel = connection.getChannelByID(channelID);
            List<Integer> clientIDs = connection.getClientsByChannelID(channelID);
            if ((clientIDs != null)&&(clientIDs.size()>0)){
                for (int clientID:clientIDs)
                {
                    drawClientMap(canvas,clientID,indent+20);
                }
            }
            for(int subChannelID : channel.getSubchannelIDs())
            {drawChannelMaps(canvas, subChannelID,indent+20);
            }
        }
        catch (Exception e)
        {
            Log.w("TSView","Error displaying channel map, cid:"+channelID+" error: "+e,e);
            if (!(channelDisplay.containsKey(channelID))){
                channelCreated(channelID);
            }
        }
    }
    private void drawClientMap(Canvas canvas,int clientID, int indent){
        try{
        Log.d("TSView","Drawing client id:"+clientID);
        TSClient client = connection.getClientByCLID(clientID);
        TSRow row = clientDisplay.get(clientID);
        Paint clientPaint = getClientPaint(client);
        int xPointer = indent;
        if (row.hasIcon())
        {
            Drawable icon = res.getDrawable(row.getLeftIcon());
            icon.setBounds(xPointer,(int)yPointer,xPointer+16,(int)yPointer+16);
            xPointer +=16;
            icon.draw(canvas);
        }
        canvas.drawText(row.getLeftName(),xPointer,yPointer+15,clientPaint);
        if (row.hasRight())
        {   bounds = new Rect();
            clientPaint.getTextBounds(row.getLeftName(),0,row.getLeftName().length(),bounds);
            canvas.drawBitmap(row.getRightContent(),xPointer+2+bounds.width(),yPointer,clientPaint);
        }
        yPointer += 16;
        }
        catch (Exception e)
        {
            Log.w("TSView", "Error displaying client map for client id:"+clientID+" error: "+e,e);
            if (!(clientDisplay.containsKey(clientID))){
            clientJoined(clientID);
            }
        }
    }
    private static int getClientStatusIconReference(TSClient client)
    {if(client.getClientType()== TSClient.ClientType.ServerQueryClient)
    {
        return R.drawable.client_server_query;
    }
        TSClientStatus status = client.getStatus();
        switch (status){
        case Talking:
            return R.drawable.client_talking;
        case NotTalking:
            return R.drawable.client_not_talking;
        case OutputDisabled:
            return R.drawable.client_output_disabled;
        case InputDisabled:
            return R.drawable.client_input_disabled;
        case InputMuted:
            return R.drawable.client_input_muted;
        case OutputMuted:
            return R.drawable.client_output_muted;
        case Away:
            return R.drawable.client_away;
        case CommanderNotTalking:
            return R.drawable.client_channel_commander_not_talking;
        case CommanderTalking:
            return R.drawable.client_channel_commander_talking;
        case Whispering:
            return R.drawable.client_whispering;
        case LocalMuted:
            return R.drawable.client_local_muted;
        default:
            return R.drawable.error;
    }

    }
    private void setObjectMinimumWidth(float width){
        if (width>xWidth){
            xWidth = width;
        }
    }
    private static Paint getClientPaint  (TSClient client)
    {   if(client.isRecording())
        {
        return recordingClientPaint;
        }
        return defaultClientPaint;
    }
    private static int getChannelIconReference(TSChannel channel)
    {
    if(channel.getType().equals(TSChannel.ChannelType.ServerTopLevel)){
        return R.drawable.server_icon;
    }
        if(channel.isSubscribed())
        {
            if (channel.isFull()){
                return  R.drawable.channel_red_subscribed;
            }
            else if (channel.isPassworded()&&!channel.isPasswordKnown())
            {
                return R.drawable.channel_yellow_subscribed;
            }
            else
            {
                return R.drawable.channel_subscribed;
            }
        }
        else
        {   if(channel.isFull())
           {
            return  R.drawable.channel_red_unsubscribed;
           }
           else if (channel.isPassworded()&&!channel.isPasswordKnown())
            {
            return R.drawable.channel_yellow_unsubscribed;
            }
            else
           {
               return R.drawable.channel_unsubscribed;
           }
        }
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int h = 0;
        if(connection != null)
        {
            h=16*(connection.getChannelCount()+connection.getClientCount());
        }
        h+=(int)errorPaint.getTextSize();
        int i = (int)xWidth;
        setMeasuredDimension(i,h);
    }
   private static int getIconReferenceByID(int i){
        if(i==0){
            return 0;
        }
        String name = "icon_"+i;
       return (res.getIdentifier(name,"drawable","Meliarion.TS3.TS3Remote"));
    }
    private static Bitmap buildRightIcons(List<Integer> iconRefs){
        if (iconRefs.size()==0)
        {
            return null;
        }
        Bitmap map = Bitmap.createBitmap(iconRefs.size()*16,16, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(map);
        //canvas.drawColor(Color.GREEN);
        int xPointer = 0;
        float heightOffset;
        float widthOffset;
        for (int iconRef : iconRefs){
        try{Bitmap icon = BitmapFactory.decodeResource(res,iconRef);
        heightOffset = 0;
        widthOffset = 0;
        if(icon.getWidth()!=16){
            widthOffset = (16-icon.getWidth())/2;
        }
        if(icon.getHeight()!=16)
        {heightOffset = (16-icon.getHeight())/2;
        }
        canvas.drawBitmap(icon,xPointer+widthOffset,heightOffset,null);
        xPointer += 16;
        }
        catch (Exception ex)
        {
            Log.e("TSView","Error building right icons",ex);
        }
        }
        return map;
    }
    private Bitmap clientRightIconDisplay(TSClient client){
        List<Integer> serverGroups = client.getServerGroups();
        List<Integer> iconRefs = new ArrayList<Integer>();
        TSGroup group;
        int iconRef;
        try{//add priority speaker
            if(client.isPrioritySpeaker())
            {
                iconRefs.add(R.drawable.client_is_priority_speaker);
            }
        } catch (Resources.NotFoundException e)
        {
            Log.e("TSView", "Error building priority speaker icon for client:" + client.getClientID(), e);
        }
        try{//add talk power related icons
            int talk = client.getTalkPower();
            TSChannel channel = connection.getChannelByID(client.getChannelID());
            if(channel.getNeededTalkPower()>talk)
            {
                if(client.isTalker()){
                    iconRefs.add(R.drawable.client_is_talker);
                }
                else
                {
                    iconRefs.add(R.drawable.client_input_muted);
                }
            }
        } catch (Resources.NotFoundException e)
        {
            Log.e("TSView", "Error retrieving talk power icon for client:" + client.getClientID() + ":" + e.toString(), e);
        } catch (ServerConnection.SCNotFoundException e) {
            Log.e("TSView", "Error retrieving " + e.getItemNotFoundType().toString() + " for client:" + client.getClientID() + ":" + e.toString(), e);
            connection.addRequest(e.getItemNotFoundType());
        }

        try{//add channel group icon
            group = connection.getChannelGroupByID(client.getChannelGroupID());
            if (group.getIconID() != 0){
                iconRef = getIconReferenceByID(group.getIconID());
                if (iconRef !=0)
                {
                    iconRefs.add(iconRef);
                }
            }
        } catch (Resources.NotFoundException e)
        {
            Log.e("TSView", "Error getting client channel group id for client:" + client.getClientID(), e);
        } catch (ServerConnection.SCNotFoundException e) {
            Log.e("TSView", "Error retrieving " + e.getItemNotFoundType().toString() + " for client:" + client.getClientID(), e);
            connection.addRequest(e.getItemNotFoundType());
        }
        for(int id: serverGroups){//add server group icons
            try{
            group = connection.getServerGroupByID(id);
            if(group.getIconID()==0)
            {
                continue;
            }
            iconRef= getIconReferenceByID(group.getIconID());
            if (iconRef !=0)
                {
                    iconRefs.add(iconRef);
                }
            }
            catch (Resources.NotFoundException exception){
            Log.d("TSView", "Failed to find icon for server group id ="+id,exception);
            } catch (ServerConnection.SCNotFoundException e)
            {
                Log.e("TSView", "Error getting " + e.getItemNotFoundType().toString() + " for client:" + client.getClientID(), e);
                connection.addRequest(e.getItemNotFoundType());
            }
        }
        try{
            if(showCountry)
            {
                iconRef = getCountyIconRef(client.getCountry());
                if(iconRef!=0){
                    iconRefs.add(iconRef);
                }
            }

        } catch (Resources.NotFoundException ex)
        {
            Log.e("TSView", "Error getting country icon for client:" + client.getClientID(), ex);
        }
        try{//add client icon
        if (client.getIconID() != 0)
        {
            iconRef = getIconReferenceByID(client.getIconID());
            if (iconRef !=0)
            {
                iconRefs.add(iconRef);
            }
        }
        } catch (Resources.NotFoundException e)
        {
            Log.e("TSView", "Error getting client icon id for client:" + client.getClientID(), e);
        }
        return buildRightIcons(iconRefs);
    }

    private static int getCountyIconRef(String country) {
        if(country.equals("")){
            return 0;
        }
        String name = "country_"+country.toLowerCase();
        int ref = res.getIdentifier(name,"drawable","Meliarion.TS3.TS3Remote");
        if (ref==0)
        {
            Log.e("TSView","Unable to retrieve icon for country:"+country.toLowerCase());
        }
        return ref;
    }

    private static Bitmap channelRightIcons(TSChannel channel){
        List<Integer> iconRefs = new ArrayList<Integer>();
        if (channel.isDefaultChannel()){
            iconRefs.add(R.drawable.channel_default);
        }
        if (channel.isPassworded()){
            iconRefs.add(R.drawable.channel_register);
        }
        if (channel.getCodec().isMusicCodec())
        {
            iconRefs.add(R.drawable.channel_music_codec);
        }
        if (channel.getNeededTalkPower()>0)
        {   iconRefs.add(R.drawable.channel_moderated);
        }
        if (channel.getIconID() !=0)
        {   int id= getIconReferenceByID(channel.getIconID());
            if(id!=0){
            iconRefs.add(id);
            }
        }
        return buildRightIcons(iconRefs);
    }


    private TSRow channelRow(TSChannel channel){
        try{
        Log.d("TSView", "building channel "+channel.getName());
        int xWidth = 0;
        String name = ServerConnection.unmakeTransmitSafe(channel.getName());
        bounds = new Rect();
        channelPaint.getTextBounds(name,0,name.length(),bounds);
        xWidth += bounds.width();
        if (channel.isSpacer()){
            return new TSRow(-1,name,null,channelPaint,xWidth);
            }
        else{
            int iconRef = getChannelIconReference(channel);
            Bitmap right = channelRightIcons(channel);
            xWidth += 16;
            if (right != null){
            xWidth += right.getWidth();
            }
            return new TSRow(iconRef,name,right,channelPaint,xWidth);
        }
        }
        catch (Exception e)
        {
            Log.e("TSView", "Error building channel:"+channel.getName() +" error:"+e,e);
            return null;
        }
    }
    private TSRow clientRow(TSClient client){
        try{
        Log.d("TSView", "building client "+client.getName());
        Paint clientPaint = getClientPaint(client);
        int xWidth = 0;
        String name = ServerConnection.unmakeTransmitSafe(client.getName());
        bounds = new Rect();
        clientPaint.getTextBounds(name,0,name.length(),bounds);
        xWidth +=bounds.width();
        int icon = getClientStatusIconReference(client);
        xWidth += 16;
        Bitmap map = clientRightIconDisplay(client);
        if (map!=null){
        xWidth +=map.getWidth();
        }
        return new TSRow(icon,name,map,clientPaint,xWidth);
        }
        catch (Exception e)
        {
            Log.e("TSView", "Error building client:"+client.getName()+" error:"+e,e);
            return null;
        }
    }
    @Override
    protected void onDraw(Canvas canvas){
        Log.d("TSView","Starting draw");
        super.onDraw(canvas);
        String s;
        Paint usedPaint=debugPaint;
        if (connected){
            if((connection != null)){
                try{
                    if(connection.isConnected()){
                    drawMaps(canvas);
                    s = "Initialisation done";
                    }
                    else {
                        s = "The displayed server connection is not connected";
                    }
                    canvas.drawText(s,0,yPointer+16,usedPaint);

                }
                catch (Exception e)
                {   s = "Initialisation failed";
                    canvas.drawText(s,0,yPointer+16,usedPaint);
                    Log.e("Meliarion.TS3.TS3Remote",e.toString(),e);
                }
            }
            else
            {   usedPaint = errorPaint;
                s = "Not initialised";
                canvas.drawText(s, 0, yPointer+16, usedPaint);
            }
        }
        else
        {
        s = "Client connection died";
        usedPaint = errorPaint;
        canvas.drawText("Client connection died",0,yPointer+20,usedPaint);
        yPointer += 20;
        }
        canvas.save();

        usedPaint.getTextBounds(s, 0, s.length(), bounds);
        setObjectMinimumWidth(bounds.width());
        yPointer=0f;
        Log.d("TSView","Draw finished with result: "+s);
    }




    static class TSRow
    {
        private int leftIcon;
        private int width;
        private Paint paint;
        private String leftName;
        private Bitmap rightContent;

        TSRow(int _icon, String _name,Bitmap _right, Paint _paint,int _width)
        {
            leftIcon = _icon;
            leftName = _name;
            paint = _paint;
            rightContent =_right;
            width = _width;
        }
        public boolean hasIcon(){
            return leftIcon != -1;
        }
        public int getLeftIcon(){
            return leftIcon;
        }
        public int getWidth(){
        return width;
        }
        public String getLeftName(){
            return leftName;
        }
        public Bitmap getRightContent(){
            return rightContent;
        }
        public boolean hasRight(){
            return rightContent != null;
        }
        public Paint getPaint(){
            return paint;
        }
        @Override
        public String toString() {
            return getLeftName();
        }
    }


}
