package meliarion.ts3.ts3remote;

/**
 * Created by Meliarion on 30/07/13.
 * Enum describing the various Stages a server connection handler goes through
 */
public enum  ConnectionStage{
    Disconnected,
    VerifyConnection,
    RequestServerGroups,
    RequestChannelGroups,
    RequestClients,
    RequestChannels,
    FinaliseSetup,
    PopulatingDone,
    SetupDone,
    InvalidStage;

    ConnectionStage  (){
    }


}
