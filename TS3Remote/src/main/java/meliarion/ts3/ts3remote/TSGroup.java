package meliarion.ts3.ts3remote;

import java.util.Map;

/**
 * Created by Meliarion on 30/07/13.
 * Class that represents a teamspeak group
 */
public class TSGroup {
    private String name;
    private int ID;
    private int iconID;
    private GroupType type;
    private int sortID;
    private String saveDB;
    private String nameMode;
    public TSGroup(int _ID, String _Name, int _Type, int _IconID)
    {
        this.name = _Name;
        this.ID = _ID;
        this.setType(_Type);
        this.iconID = _IconID;
    }
    public TSGroup(Map<String,String> params)
    {
        processParams(params,-1);
    }
    public TSGroup(Map<String,String> params,int index)
    {
        processParams(params,index);
    }
    private void setType(int _type){
        this.type = GroupType.getGroupType(_type);
    }
    public void updateGroup(TSGroup group){
        this.name = group.getName();
        this.type = group.getType();
        this.iconID = group.iconID;
        this.saveDB = group.getSaveDB();
        this.sortID = group.getSortID();
        this.nameMode = group.getNameMode();
    }
    public void updateGroup(Map<String,String> params)
    {
        processParams(params,-1);
    }
    public void updateGroup(Map<String,String> params, int index)
    {
        processParams(params,index);
    }
    private void processParams(Map<String,String> params, int index)
    {   String identifier= "";
        if(index!=-1)
        {
            identifier+=index;
        }
        this.ID = Integer.valueOf(params.get(identifier+"sgid"));
        this.name = params.get(identifier+"name");
        this.setType(Integer.valueOf(params.get(identifier+"type")));
        this.iconID = Integer.valueOf(params.get(identifier+"iconid"));
        this.saveDB = params.get(identifier+"savedb");
        this.sortID = Integer.valueOf(params.get(identifier+"sortid"));
        this.nameMode = params.get(identifier+"namemode");
    }
    @Override
    public String toString() {
        return name;
    }
    public int getID(){
        return this.ID;
    }
    public GroupType getType(){
        return this.type;
    }
    public String getNameMode(){
        return this.nameMode;
    }
    public String getName(){
        return this.name;
    }
    public String getSaveDB(){
        return saveDB;
    }
    public int getIconID(){
        return this.iconID;
    }
    public int getSortID(){
        return sortID;
    }
    public enum GroupType{
        InvalidGroup(-1),
        TemplateGroup(0),
        ServerGroup(1),
        ServerQueryGroup(2);

        private final int i;

        GroupType(int _i){
            this.i = _i;
        }
        public static GroupType getGroupType(int i)
        {
            switch (i){
                case 0:
                return TemplateGroup;
                case 1:
                return ServerGroup;
                case 2:
                return ServerQueryGroup;
                default:
                return InvalidGroup;
            }
        }
    }
}
